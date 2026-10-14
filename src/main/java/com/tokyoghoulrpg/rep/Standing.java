package com.tokyoghoulrpg.rep;

import com.tokyoghoulrpg.clan.ClanData;
import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.event.ModCommands;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.raid.HuntWaves;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;

/**
 * Личная репутация игрока и репутация его клана (обе от -100 до 100, начинаются с 0).
 * Это отдельно от репутации у CCG и у гулей.
 * <ul>
 *   <li>Поднимают: сделка с любым торговцем — жителем, странствующим торговцем, торговцем мода
 *       (+1 тебе и клану, до 10 сделок за игровые сутки); убийство любого босса (+10, за Кровавого
 *       полугуля +20 каждому участнику).</li>
 *   <li>Снижают: убийство мирного — жителя, странствующего торговца, торговца или информатора мода
 *       (-15 тебе, -10 клану); смерть (-2 тебе, -1 клану).</li>
 *   <li>Высокая — от 40, низкая — до -25, между ними обычная. Без клана репутация клана считается обычной.</li>
 * </ul>
 * Что из этого следует (волна — гули и агенты CCG вместе, идут бить тебя):
 * <pre>
 *                  клан низкая     клан обычная/нет   клан высокая
 *   ты низкая      большая волна   маленькая волна    никто не идёт, бафов нет
 *   ты обычная     маленькая волна —                  —
 *   ты высокая     маленькая волна бафы               бафы получше
 * </pre>
 * Бафы — «удачный день»: иногда (раз в игровые сутки бросок) скидка в лавке торговца мода,
 * а если и у клана репутация высокая, скидка больше и жители деревни дают скидки («Герой деревни»).
 */
public final class Standing {
    /** Уровень репутации. */
    public enum Tier {
        LOW, NEUTRAL, HIGH;

        public String id() {
            return name().toLowerCase();
        }

        public ChatFormatting color() {
            return this == HIGH ? ChatFormatting.GREEN : this == LOW ? ChatFormatting.RED : ChatFormatting.GRAY;
        }
    }

    /** Какая волна идёт за игроком. */
    public enum Wave {
        NONE, SMALL, BIG
    }

    private Standing() {
    }

    public static Tier tier(int value) {
        if (value >= Balance.STANDING_HIGH) {
            return Tier.HIGH;
        }
        return value <= Balance.STANDING_LOW ? Tier.LOW : Tier.NEUTRAL;
    }

    /** Матрица: твоя репутация и репутация клана → волна. */
    public static Wave wave(Tier own, Tier clan) {
        if (own == Tier.LOW) {
            return clan == Tier.LOW ? Wave.BIG : clan == Tier.HIGH ? Wave.NONE : Wave.SMALL;
        }
        return clan == Tier.LOW ? Wave.SMALL : Wave.NONE;
    }

    /** Бафы: только при высокой своей репутации и если клан не опозорен. */
    public static boolean buffs(Tier own, Tier clan) {
        return own == Tier.HIGH && clan != Tier.LOW;
    }

    // ------------------------------------------------------------------ клан игрока

    public static ClanData.Clan clanOf(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server == null ? null : ClanData.get(server).clanOf(player.getUUID());
    }

    /** Уровень репутации клана игрока; без клана — обычная. */
    public static Tier clanTier(ServerPlayer player) {
        ClanData.Clan clan = clanOf(player);
        return clan == null ? Tier.NEUTRAL : tier(clan.reputation);
    }

    public static Wave waveFor(ServerPlayer player, PlayerData data) {
        return wave(tier(data.getStanding()), clanTier(player));
    }

    // ------------------------------------------------------------------ изменения

    /** Изменить свою репутацию и репутацию своего клана. О переходе через пороги игрок и клан узнают в чате. */
    public static void change(ServerPlayer player, int own, int clan) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        if (own != 0) {
            Tier before = tier(data.getStanding());
            data.setStanding(data.getStanding() + own);
            Tier after = tier(data.getStanding());
            if (before != after && !ModCommands.isSilent()) {
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.standing." + after.id()));
            }
            NetworkHandler.syncTo(player);
        }
        ClanData.Clan c = clanOf(player);
        if (c != null && clan != 0 && player.getServer() != null) {
            changeClan(player.getServer(), c, clan);
        }
    }

    /** Изменить репутацию клана. Все участники в игре узнают о переходе через пороги. */
    public static void changeClan(MinecraftServer server, ClanData.Clan clan, int delta) {
        if (delta == 0) {
            return;
        }
        int before = ClanData.get(server).changeReputation(clan, delta);
        Tier was = tier(before);
        Tier now = tier(clan.reputation);
        for (UUID uuid : List.copyOf(clan.members.keySet())) {
            ServerPlayer member = server.getPlayerList().getPlayer(uuid);
            if (member == null) {
                continue;
            }
            if (was != now && !ModCommands.isSilent()) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan_rep." + now.id(), clan.name));
            }
        }
        ClanManager.refresh(server, clan);
    }

    /** Сделка с торговцем (житель, странствующий торговец, торговец мода — и за изумруды, и за кровь). */
    public static void onTrade(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        long day = player.level().getGameTime() / 24000L;
        if (data.countTrade(day, Balance.STANDING_TRADE_DAILY)) {
            change(player, Balance.STANDING_TRADE, Balance.STANDING_TRADE);
        }
    }

    /** Убийство игроком: мирный — минус, босс — плюс. Кровавого полугуля считает BossRewards (всем участникам). */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (isPeaceful(victim)) {
            change(killer, -Balance.STANDING_KILL_PEACEFUL, -Balance.STANDING_KILL_PEACEFUL_CLAN);
            killer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.standing.peaceful",
                    Balance.STANDING_KILL_PEACEFUL, Balance.STANDING_KILL_PEACEFUL_CLAN));
        } else if (isBoss(victim) && !(victim instanceof HalfGhoulBoss)) {
            onBossKill(List.of(killer), Balance.STANDING_BOSS);
        }
    }

    /** Победа над боссом: каждому +amount, каждому клану тоже +amount, но один раз, сколько бы его участников ни было. */
    public static void onBossKill(Collection<ServerPlayer> players, int amount) {
        Set<UUID> clansDone = new HashSet<>();
        for (ServerPlayer player : players) {
            ClanData.Clan clan = clanOf(player);
            boolean clanGets = clan != null && clansDone.add(clan.id);
            change(player, amount, clanGets ? amount : 0);
            player.sendSystemMessage(Component.translatable(clanGets
                    ? "message.tokyoghoulrpg.standing.boss_clan" : "message.tokyoghoulrpg.standing.boss", amount));
        }
    }

    /** Смерть игрока. */
    public static void onDeath(ServerPlayer player) {
        change(player, -Balance.STANDING_DEATH, -Balance.STANDING_DEATH_CLAN);
    }

    /** Мирные: жители, странствующие торговцы, торговец и информатор мода. */
    public static boolean isPeaceful(LivingEntity entity) {
        return entity instanceof AbstractVillager || entity instanceof InformantNpc;
    }

    /** Любой босс: боссы мода, Иссушитель, Эндер-дракон, Хранитель (Warden), Древний страж. */
    public static boolean isBoss(LivingEntity entity) {
        return entity instanceof BossMob || entity instanceof WitherBoss || entity instanceof EnderDragon
                || entity instanceof Warden || entity instanceof ElderGuardian;
    }

    // ------------------------------------------------------------------ каждые 5 секунд

    public static void tick(ServerPlayer player, PlayerData data) {
        long now = player.level().getGameTime();
        if (now % 100 != 0 || !data.hasChosenClass()) {
            return;
        }
        Tier own = tier(data.getStanding());
        Tier clan = clanTier(player);
        updateDeal(player, data, own, clan);
        HuntWaves.tick(player, data, wave(own, clan));
    }

    /**
     * «Удачный день»: раз в игровые сутки бросок. Пока репутация высокая, в лавке торговца мода скидка,
     * а при высокой репутации клана ещё и «Герой деревни» (жители продают дешевле).
     */
    private static void updateDeal(ServerPlayer player, PlayerData data, Tier own, Tier clan) {
        long day = player.level().getGameTime() / 24000L;
        boolean buffs = buffs(own, clan);
        if (day != data.getDealDay()) {
            int percent = 0;
            if (buffs) {
                boolean best = clan == Tier.HIGH;
                int chance = best ? Balance.DEAL_CHANCE_CLAN : Balance.DEAL_CHANCE;
                if (player.getRandom().nextInt(100) < chance) {
                    percent = best ? Balance.DEAL_PERCENT_CLAN : Balance.DEAL_PERCENT;
                }
            }
            data.setDeal(day, percent);
            if (percent > 0) {
                player.sendSystemMessage(Component.translatable(clan == Tier.HIGH
                        ? "message.tokyoghoulrpg.deal.clan" : "message.tokyoghoulrpg.deal", percent));
            }
            NetworkHandler.syncTo(player);
        } else if (!buffs && data.getDealPercent() > 0) {
            // репутация упала — скидка пропадает до следующего дня
            data.setDeal(day, 0);
            NetworkHandler.syncTo(player);
        }
        if (buffs && clan == Tier.HIGH && data.getDealPercent() >= Balance.DEAL_PERCENT_CLAN) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 300, 0, true, false, true));
        }
    }

    /** Что сейчас происходит из-за репутации — ключ строки для меню и /tgrpg info. */
    public static String statusKey(Tier own, Tier clan) {
        Wave wave = wave(own, clan);
        if (wave == Wave.BIG) {
            return "big";
        }
        if (wave == Wave.SMALL) {
            return "small";
        }
        if (own == Tier.LOW) {
            return "protected";
        }
        if (buffs(own, clan)) {
            return clan == Tier.HIGH ? "buffs_clan" : "buffs";
        }
        return "none";
    }

    /** Число со знаком и цветом уровня: зелёное — высокая, красное — низкая. */
    public static Component colored(int value) {
        return Component.literal(value > 0 ? "+" + value : String.valueOf(value)).withStyle(tier(value).color());
    }

    /** Скидка «удачного дня» для лавки (0 — нет). Работает и у клиента: процент приходит с данными игрока. */
    public static int dealPercent(PlayerData data) {
        return data == null ? 0 : data.getDealPercent();
    }
}
