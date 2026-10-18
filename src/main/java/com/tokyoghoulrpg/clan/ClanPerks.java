package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.combat.AttrUtil;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Прокачка клана: уровни 1-5. Участники вкладывают очки крови в казну, глава платит из казны за новый уровень.
 * Бонусы действуют на всех участников клана, пока они в клане:
 * <pre>
 * Уровень  Цена   Склад   Кровь   Здоровье   Урон   Входящий урон   Территория (1 / 12+ чел.)
 *   1        —    3 ряда    —        —         —        —             10 / 32
 *   2      2000   4 ряда   +5%       —         —        —             16 / 38
 *   3      5000   5 рядов  +5%    +1 сердце    —        —             22 / 44
 *   4     10000   6 рядов  +5%    +1 сердце   +5%       —             28 / 50
 *   5     20000   6 рядов  +10%   +2 сердца   +5%      -5%            34 / 56
 *   6     30000   6 рядов  +10%   +2 сердца   +7%      -5%            40 / 62
 *   7     45000   6 рядов  +10%   +2 сердца   +7%      -7%            46 / 68
 *   8     65000   6 рядов  +12%   +3 сердца   +8%      -8%            52 / 74
 *   9     90000   6 рядов  +15%   +3 сердца   +10%     -10%           58 / 80
 *  10    130000   6 рядов  +20%   +3 сердца   +10%     -10%           58 / 80 (больше не растёт) — весь сервер узнаёт
 * </pre>
 */
public final class ClanPerks {
    public static final int MAX_LEVEL = 10;
    public static final int STORAGE_SLOTS = 54;
    /** Цена перехода на уровень (индекс — новый уровень). */
    public static final int[] COST = {0, 0, 2000, 5000, 10000, 20000, 30000, 45000, 65000, 90000, 130000};
    public static final int[] STORAGE_ROWS = {0, 3, 4, 5, 6, 6, 6, 6, 6, 6, 6};
    public static final double[] BLOOD = {0, 0, 0.05, 0.05, 0.05, 0.10, 0.10, 0.10, 0.12, 0.15, 0.20};
    public static final double[] HEALTH = {0, 0, 0, 2.0, 2.0, 4.0, 4.0, 4.0, 6.0, 6.0, 6.0};
    public static final double[] DAMAGE = {0, 0, 0, 0, 0.05, 0.05, 0.07, 0.07, 0.08, 0.10, 0.10};
    public static final double[] DEFENSE = {0, 0, 0, 0, 0, 0.05, 0.05, 0.07, 0.08, 0.10, 0.10};

    /**
     * Умения клана (покупает глава из казны, у каждого 3 уровня). Уровень умения N можно купить,
     * когда клан не ниже уровня N + 1.
     */
    public enum Skill {
        /** «Сила числа»: за каждого другого участника клана в игре (до 5) — больше урона. */
        NUMBERS("numbers", new double[] {0, 0.01, 0.02, 0.03}),
        /** «Плечом к плечу»: за каждого участника рядом (24 блока, до 4) — меньше входящего урона. */
        SHOULDER("shoulder", new double[] {0, 0.02, 0.03, 0.04}),
        /** «Узы силы»: передача силы сильнее (бонус и здоровье получателю), откат короче. */
        BOND("bond", new double[] {0, 0.20, 0.40, 0.60}),
        /** «Общая кровь»: часть очков крови участников за убийства дополнительно идёт в казну (у игрока не отнимается). */
        SHARED_BLOOD("shared_blood", new double[] {0, 0.05, 0.10, 0.15});

        private final String id;
        private final double[] values;

        Skill(String id, double[] values) {
            this.id = id;
            this.values = values;
        }

        public String id() {
            return id;
        }

        public double value(int level) {
            return values[Math.max(0, Math.min(SKILL_MAX, level))];
        }

        /** Цена уровня (1-3) из казны. */
        public int cost(int level) {
            int[] costs = this == SHARED_BLOOD ? SHARED_COST : SKILL_COST;
            return costs[Math.max(1, Math.min(SKILL_MAX, level))];
        }
    }

    public static final int SKILL_MAX = 3;
    public static final int[] SKILL_COST = {0, 3000, 6000, 10000};
    public static final int[] SHARED_COST = {0, 2000, 4000, 7000};
    public static final double BOND_HP_PER_LEVEL = 0.10;            // «Узы силы»: здоровья получателю ещё +10% за уровень
    public static final int[] BOND_COOLDOWN = {2 * 60 * 20, 100 * 20, 80 * 20, 60 * 20};
    public static final int NUMBERS_CAP = 5;
    public static final int SHOULDER_CAP = 4;
    public static final double SHOULDER_RADIUS = 24.0;

    private static final UUID HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea8");

    private ClanPerks() {
    }

    public static ClanData.Clan clan(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server == null ? null : ClanData.get(server).clanOf(player.getUUID());
    }

    /** Уровень умения клана игрока (0 — нет клана или не куплено). */
    public static int skill(ServerPlayer player, Skill skill) {
        ClanData.Clan clan = clan(player);
        return clan == null ? 0 : clan.skills[skill.ordinal()];
    }

    /** Сколько других участников клана сейчас в игре (до capped) и сколько из них рядом. */
    private static int othersOnline(ServerPlayer player, ClanData.Clan clan, boolean nearOnly, int cap) {
        int count = 0;
        for (UUID id : clan.members.keySet()) {
            if (id.equals(player.getUUID())) {
                continue;
            }
            ServerPlayer other = player.getServer().getPlayerList().getPlayer(id);
            if (other == null || !other.isAlive()) {
                continue;
            }
            if (nearOnly && (other.level() != player.level()
                    || other.distanceToSqr(player) > SHOULDER_RADIUS * SHOULDER_RADIUS)) {
                continue;
            }
            if (++count >= cap) {
                break;
            }
        }
        return count;
    }

    /** «Сила числа»: прибавка к урону сейчас. */
    public static double numbersBonus(ServerPlayer player) {
        ClanData.Clan clan = clan(player);
        int level = clan == null ? 0 : clan.skills[Skill.NUMBERS.ordinal()];
        return level <= 0 ? 0.0 : Skill.NUMBERS.value(level) * othersOnline(player, clan, false, NUMBERS_CAP);
    }

    /** «Плечом к плечу»: защита сейчас. */
    public static double shoulderBonus(ServerPlayer player) {
        ClanData.Clan clan = clan(player);
        int level = clan == null ? 0 : clan.skills[Skill.SHOULDER.ordinal()];
        return level <= 0 ? 0.0 : Skill.SHOULDER.value(level) * othersOnline(player, clan, true, SHOULDER_CAP);
    }

    /** «Общая кровь»: участник получил очки крови за убийство — часть сверху идёт в казну. */
    public static void shareBlood(ServerPlayer player, int blood) {
        ClanData.Clan clan = clan(player);
        int level = clan == null ? 0 : clan.skills[Skill.SHARED_BLOOD.ordinal()];
        int share = level <= 0 ? 0 : (int) Math.round(blood * Skill.SHARED_BLOOD.value(level));
        if (share > 0) {
            clan.treasury = (int) Math.min(Integer.MAX_VALUE, (long) clan.treasury + share);
            ClanData.get(player.getServer()).setDirty();
        }
    }

    /** Уровень клана игрока (0 — нет клана). */
    public static int level(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return 0;
        }
        ClanData.Clan clan = ClanData.get(server).clanOf(player.getUUID());
        return clan == null ? 0 : Math.max(1, Math.min(MAX_LEVEL, clan.level));
    }

    public static double bloodBonus(ServerPlayer player) {
        ClanData.Clan clan = clan(player);
        double top = clan != null && ClanRating.isTop(player.getServer(), clan) ? ClanRating.TOP_BONUS : 0.0;
        return BLOOD[level(player)] + top;
    }

    public static double damageFactor(ServerPlayer player) {
        return (1.0 + DAMAGE[level(player)]) * (1.0 + numbersBonus(player));
    }

    public static double defenseFactor(ServerPlayer player) {
        return (1.0 - DEFENSE[level(player)]) * (1.0 - shoulderBonus(player));
    }

    /** Раз в секунду: прибавка к здоровью от уровня клана (вышел из клана — снимается). */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 7) {
            return;
        }
        double health = HEALTH[level(player)];
        if (health > 0.0) {
            AttrUtil.setPermanent(player, Attributes.MAX_HEALTH, HEALTH_ID, "tg_clan_health", health,
                    AttributeModifier.Operation.ADDITION);
        } else {
            AttrUtil.remove(player, Attributes.MAX_HEALTH, HEALTH_ID);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
