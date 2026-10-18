package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * Войны кланов. Глава объявляет войну другому клану; война длится не дольше 20 минут или пока
 * кто-нибудь не наберёт 10 очков (или не сломает знамя противника — сразу победа).
 * <p>
 * Баланс, чтобы большой клан не «съел» маленькие:
 * <ul>
 *   <li>нельзя объявить войну клану, в котором меньше половины твоего числа участников (если он не выше тебя уровнем);</li>
 *   <li>убийство в меньшем клане стоит больше: меньший клан получает за убийство столько очков,
 *       во сколько раз больший клан больше (до 3), а его участники бьют сильнее и получают меньше урона
 *       от врагов (+10% / -10% за каждую ступень, до 30%);</li>
 *   <li>добыча победителя — 10% казны проигравшего (не больше 5000); если победил больший клан —
 *       меньше (в отношении размеров), если меньший — в 1,5 раза больше;</li>
 *   <li>после войны проигравший защищён от новых войн 2 часа, при ничьей оба — 1 час, победитель — 30 минут;
 *       сам объявить новую войну клан может через 30 минут; у клана одновременно только одна война.</li>
 * </ul>
 * Война хранится, пока сервер работает (перезапуск — войны нет).
 */
public final class ClanWars {
    public static final long DURATION_MS = 20L * 60 * 1000;
    public static final int TARGET = 10;
    public static final int MAX_WEIGHT = 3;
    public static final double UNDERDOG_STEP = 0.10;
    public static final double LOOT_SHARE = 0.10;
    public static final int LOOT_CAP = 5000;
    public static final long PROTECT_LOSER = 2L * 60 * 60 * 1000;
    public static final long PROTECT_DRAW = 60L * 60 * 1000;
    public static final long PROTECT_WINNER = 30L * 60 * 1000;
    public static final long DECLARE_COOLDOWN = 30L * 60 * 1000;

    /** Одна война. a — объявивший, b — защищающийся. */
    public static final class War {
        public final UUID a;
        public final UUID b;
        public final String nameA;
        public final String nameB;
        public final int weightA;
        public final int weightB;
        public final long endMs;
        public final long endTick;
        public int scoreA;
        public int scoreB;

        War(UUID a, UUID b, String nameA, String nameB, int weightA, int weightB, long endMs, long endTick) {
            this.a = a;
            this.b = b;
            this.nameA = nameA;
            this.nameB = nameB;
            this.weightA = weightA;
            this.weightB = weightB;
            this.endMs = endMs;
            this.endTick = endTick;
        }

        boolean involves(UUID clan) {
            return a.equals(clan) || b.equals(clan);
        }

        /** Множитель силы участников этого клана против врага (меньший клан сильнее). */
        double bonus(UUID clan) {
            int weight = a.equals(clan) ? weightA : weightB;
            return (weight - 1) * UNDERDOG_STEP;
        }
    }

    private static final List<War> WARS = new ArrayList<>();
    /** Право захвата после победы: клан-победитель → {клан-побеждённый, до какого времени (мс)}. */
    public static final long CAPTURE_MS = 15L * 60 * 1000;
    private static final Map<UUID, Object[]> CAPTURE = new HashMap<>();

    /** Может ли клан сейчас захватить знамя этого клана (выиграл у него войну меньше 15 минут назад). */
    public static boolean canCapture(UUID winner, UUID loser) {
        Object[] right = CAPTURE.get(winner);
        return right != null && loser.equals(right[0]) && (long) right[1] > System.currentTimeMillis();
    }

    /** Право захвата использовано (одна территория за победу). */
    public static void useCapture(UUID winner) {
        CAPTURE.remove(winner);
    }

    private ClanWars() {
    }

    public static War warOf(UUID clan) {
        if (clan == null) {
            return null;
        }
        for (War war : WARS) {
            if (war.involves(clan)) {
                return war;
            }
        }
        return null;
    }

    /**
     * На чьей стороне клан в этой войне: war.a или war.b (сам воюет или союзник воюющего).
     * null — не участвует (или союзник обеих сторон — тогда нейтрален).
     */
    public static UUID sideOf(ClanData data, War war, UUID clan) {
        if (clan == null) {
            return null;
        }
        if (war.a.equals(clan)) {
            return war.a;
        }
        if (war.b.equals(clan)) {
            return war.b;
        }
        ClanData.Clan c = data.byId(clan);
        boolean withA = ClanAlliances.allied(c, data.byId(war.a));
        boolean withB = ClanAlliances.allied(c, data.byId(war.b));
        return withA == withB ? null : withA ? war.a : war.b;
    }

    /** Война, в которой клан участвует сам или как союзник. */
    public static War warFor(ClanData data, UUID clan) {
        War own = warOf(clan);
        if (own != null || clan == null) {
            return own;
        }
        for (War war : WARS) {
            if (sideOf(data, war, clan) != null) {
                return war;
            }
        }
        return null;
    }

    /** Враги ли кланы прямо сейчас (по разные стороны какой-то войны, союзники учитываются). */
    public static boolean enemies(ClanData data, UUID x, UUID y) {
        return enemyWar(data, x, y) != null;
    }

    public static boolean enemies(UUID x, UUID y) {
        if (WARS.isEmpty() || x == null || y == null || x.equals(y)) {
            return false;
        }
        for (War war : WARS) {
            if ((war.a.equals(x) && war.b.equals(y)) || (war.a.equals(y) && war.b.equals(x))) {
                return true;
            }
        }
        return false;
    }

    private static War enemyWar(ClanData data, UUID x, UUID y) {
        if (WARS.isEmpty() || x == null || y == null || x.equals(y)) {
            return null;
        }
        for (War war : WARS) {
            UUID sx = sideOf(data, war, x);
            UUID sy = sideOf(data, war, y);
            if (sx != null && sy != null && !sx.equals(sy)) {
                return war;
            }
        }
        return null;
    }

    public static List<War> wars() {
        return WARS;
    }

    public static void clear() {
        WARS.clear();
        CAPTURE.clear();
    }

    /** Почему нельзя объявить войну (ключ сообщения) или null — можно. */
    public static String canDeclare(MinecraftServer server, ClanData.Clan own, ClanData.Clan target) {
        long now = System.currentTimeMillis();
        if (own == null || target == null || own == target) {
            return "war.bad_target";
        }
        if (ClanAlliances.allied(own, target)) {
            return "war.ally";
        }
        ClanData data = ClanData.get(server);
        if (warFor(data, own.id) != null) {
            return "war.already_own";
        }
        if (warFor(data, target.id) != null) {
            return "war.already_target";
        }
        if (own.declareCooldownUntil > now) {
            return "war.cooldown";
        }
        if (target.protectedUntil > now) {
            return "war.protected";
        }
        if (online(server, own) == 0 || online(server, target) == 0) {
            return "war.nobody_online";
        }
        // большой клан не может нападать на маленький (если тот не выше уровнем)
        if (target.members.size() * 2 < own.members.size() && target.level < own.level) {
            return "war.too_small";
        }
        return null;
    }

    private static int online(MinecraftServer server, ClanData.Clan clan) {
        int count = 0;
        for (UUID id : clan.members.keySet()) {
            if (server.getPlayerList().getPlayer(id) != null) {
                count++;
            }
        }
        return count;
    }

    /** Объявить войну (проверки — canDeclare). */
    public static void declare(ServerPlayer leader, ClanData data, ClanData.Clan own, ClanData.Clan target) {
        MinecraftServer server = leader.getServer();
        String reason = canDeclare(server, own, target);
        if (reason != null) {
            leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg." + reason).withStyle(ChatFormatting.RED));
            return;
        }
        int sizeA = Math.max(1, own.members.size());
        int sizeB = Math.max(1, target.members.size());
        int weightA = Math.max(1, Math.min(MAX_WEIGHT, (int) Math.round(sizeB / (double) sizeA)));
        int weightB = Math.max(1, Math.min(MAX_WEIGHT, (int) Math.round(sizeA / (double) sizeB)));
        long endTick = server.overworld().getGameTime() + DURATION_MS / 50;
        War war = new War(own.id, target.id, own.name, target.name, weightA, weightB,
                System.currentTimeMillis() + DURATION_MS, endTick);
        WARS.add(war);
        Component news = Component.translatable("message.tokyoghoulrpg.war.declared", own.name, target.name,
                TARGET, DURATION_MS / 60000).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(news);
        }
        if (weightA > 1 || weightB > 1) {
            Component note = Component.translatable("message.tokyoghoulrpg.war.underdog",
                    weightA > 1 ? own.name : target.name, Math.max(weightA, weightB),
                    Math.round(Math.max(war.bonus(own.id), war.bonus(target.id)) * 100));
            for (ServerPlayer p : members(server, data, war)) {
                p.sendSystemMessage(note.copy().withStyle(ChatFormatting.GOLD));
            }
        }
        for (ServerPlayer p : members(server, data, war)) {
            Effects.sound(p, SoundEvents.RAID_HORN.value(), 1.0F, 1.0F);
        }
        refresh(server, data, war);
    }

    /** Участники обеих сторон, кто в игре. */
    private static List<ServerPlayer> members(MinecraftServer server, ClanData data, War war) {
        List<ServerPlayer> list = new ArrayList<>();
        for (ClanData.Clan clan : data.all()) {
            if (sideOf(data, war, clan.id) == null) {
                continue;
            }
            for (UUID id : clan.members.keySet()) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    list.add(p);
                }
            }
        }
        return list;
    }

    /** Обновить у всех участников надпись войны на экране. */
    private static void refresh(MinecraftServer server, ClanData data, War war) {
        for (ServerPlayer p : members(server, data, war)) {
            show(p, data, war);
        }
    }

    private static void show(ServerPlayer player, ClanData data, War war) {
        PlayerData pd = PlayerData.get(player);
        ClanData.Clan clan = data.clanOf(player.getUUID());
        if (pd == null || clan == null) {
            return;
        }
        boolean isA = war.a.equals(sideOf(data, war, clan.id));
        pd.setWar(isA ? war.nameB : war.nameA, isA ? war.scoreA : war.scoreB, isA ? war.scoreB : war.scoreA,
                TARGET, war.endTick);
        NetworkHandler.syncTo(player);
    }

    /** При входе: показать идущую войну или убрать старую надпись. */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        PlayerData pd = PlayerData.get(player);
        if (server == null || pd == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.clanOf(player.getUUID());
        War war = clan == null ? null : warFor(data, clan.id);
        if (war != null) {
            show(player, data, war);
        } else if (!pd.getWarEnemy().isEmpty()) {
            pd.setWar("", 0, 0, 0, 0L);
        }
    }

    /** Враги ли эти два игрока (их кланы воюют друг с другом). */
    public static War between(ServerPlayer x, ServerPlayer y) {
        MinecraftServer server = x.getServer();
        if (server == null || WARS.isEmpty()) {
            return null;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan cx = data.clanOf(x.getUUID());
        ClanData.Clan cy = data.clanOf(y.getUUID());
        if (cx == null || cy == null || cx == cy) {
            return null;
        }
        return enemyWar(data, cx.id, cy.id);
    }

    /** Бонус меньшего клана в бою с врагом: множитель урона атакующего. */
    public static double attackFactor(ServerPlayer attacker, ServerPlayer victim) {
        War war = between(attacker, victim);
        if (war == null) {
            return 1.0;
        }
        ClanData data = ClanData.get(attacker.getServer());
        return 1.0 + war.bonus(sideOf(data, war, data.clanOf(attacker.getUUID()).id));
    }

    /** Бонус меньшего клана: множитель входящего урона от врага. */
    public static double defenseFactor(ServerPlayer victim, ServerPlayer attacker) {
        War war = between(attacker, victim);
        if (war == null) {
            return 1.0;
        }
        ClanData data = ClanData.get(victim.getServer());
        return 1.0 - war.bonus(sideOf(data, war, data.clanOf(victim.getUUID()).id));
    }

    /** Убийство игрока из враждебного клана — очки войны. */
    public static void onKill(ServerPlayer killer, ServerPlayer victim) {
        War war = between(killer, victim);
        if (war == null) {
            return;
        }
        MinecraftServer server = killer.getServer();
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.clanOf(killer.getUUID());
        boolean isA = war.a.equals(sideOf(data, war, clan.id));
        int points = isA ? war.weightA : war.weightB;
        if (isA) {
            war.scoreA += points;
        } else {
            war.scoreB += points;
        }
        com.tokyoghoulrpg.rep.Achievements.add(killer, "war_kills", 1);
        Component msg = Component.translatable("message.tokyoghoulrpg.war.kill", killer.getGameProfile().getName(),
                victim.getGameProfile().getName(), points, war.nameA, war.scoreA, war.scoreB, war.nameB);
        for (ServerPlayer p : members(server, data, war)) {
            p.sendSystemMessage(msg);
        }
        if (war.scoreA >= TARGET || war.scoreB >= TARGET) {
            finish(server, data, war, war.scoreA >= TARGET ? war.a : war.b, "war.reason_score");
        } else {
            refresh(server, data, war);
        }
    }

    /** Знамя клана сломал враг во время войны — сразу победа. */
    public static void onFlagBroken(MinecraftServer server, UUID flagClan, ServerPlayer breaker) {
        War war = warOf(flagClan);
        if (war == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.clanOf(breaker.getUUID());
        UUID side = clan == null ? null : sideOf(data, war, clan.id);
        if (side != null && !side.equals(flagClan)) {
            finish(server, data, war, side, "war.reason_flag");
            useCapture(side);        // знамя уже захвачено — это и есть захват за победу
        }
    }

    /** Раз в секунду: войны, у которых вышло время. */
    public static void tick(MinecraftServer server) {
        if (WARS.isEmpty() || server.getTickCount() % 20 != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        ClanData data = ClanData.get(server);
        for (War war : new ArrayList<>(WARS)) {
            ClanData.Clan a = data.byId(war.a);
            ClanData.Clan b = data.byId(war.b);
            if (a == null || b == null) {
                WARS.remove(war);
                continue;
            }
            if (now >= war.endMs) {
                UUID winner = war.scoreA == war.scoreB ? null : war.scoreA > war.scoreB ? war.a : war.b;
                finish(server, data, war, winner, "war.reason_time");
            }
        }
    }

    /** Конец войны: добыча, защита, сообщения. winner = null — ничья. */
    private static void finish(MinecraftServer server, ClanData data, War war, UUID winner, String reason) {
        List<ServerPlayer> everyone = members(server, data, war);
        WARS.remove(war);
        ClanData.Clan a = data.byId(war.a);
        ClanData.Clan b = data.byId(war.b);
        long now = System.currentTimeMillis();
        Component result;
        if (winner == null || a == null || b == null) {
            if (a != null) {
                a.protectedUntil = now + PROTECT_DRAW;
                a.declareCooldownUntil = now + DECLARE_COOLDOWN;
            }
            if (b != null) {
                b.protectedUntil = now + PROTECT_DRAW;
            }
            result = Component.translatable("message.tokyoghoulrpg.war.draw", war.nameA, war.scoreA, war.scoreB, war.nameB);
        } else {
            ClanData.Clan win = winner.equals(war.a) ? a : b;
            ClanData.Clan lose = win == a ? b : a;
            double loot = Math.min(LOOT_CAP, lose.treasury * LOOT_SHARE);
            int winSize = Math.max(1, win.members.size());
            int loseSize = Math.max(1, lose.members.size());
            if (winSize > loseSize) {
                loot *= loseSize / (double) winSize;      // большой победил маленький — добыча меньше
            } else if (winSize < loseSize) {
                loot *= 1.5;                               // маленький победил большой — добыча больше
            }
            int amount = (int) Math.min(lose.treasury, Math.round(loot));
            lose.treasury -= amount;
            win.treasury = (int) Math.min(Integer.MAX_VALUE, (long) win.treasury + amount);
            win.warWins++;
            lose.warLosses++;
            win.protectedUntil = now + PROTECT_WINNER;
            lose.protectedUntil = now + PROTECT_LOSER;
            a.declareCooldownUntil = now + DECLARE_COOLDOWN;   // объявивший ждёт перед новой войной
            ClanQuests.progress(server, win, ClanQuests.Kind.WIN_WAR, 1);
            // право захватить территорию побеждённого
            if (ClanTerritory.territoryCount(lose) > 0) {
                CAPTURE.put(win.id, new Object[] {lose.id, now + CAPTURE_MS});
                Component hint = Component.translatable("message.tokyoghoulrpg.territory.capture_hint", lose.name,
                        CAPTURE_MS / 60000).withStyle(ChatFormatting.YELLOW);
                for (UUID id : win.members.keySet()) {
                    ServerPlayer member = server.getPlayerList().getPlayer(id);
                    if (member != null) {
                        member.sendSystemMessage(hint);
                    }
                }
            }
            for (UUID id : win.members.keySet()) {
                ServerPlayer member = server.getPlayerList().getPlayer(id);
                if (member != null) {
                    com.tokyoghoulrpg.rep.Achievements.add(member, "war_wins", 1);
                }
            }
            result = Component.translatable("message.tokyoghoulrpg.war.won", win.name, lose.name,
                    war.scoreA, war.scoreB, amount, Component.translatable("message.tokyoghoulrpg." + reason));
        }
        data.setDirty();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(result.copy().withStyle(ChatFormatting.GOLD));
        }
        for (ServerPlayer p : everyone) {
            PlayerData pd = PlayerData.get(p);
            if (pd != null) {
                pd.setWar("", 0, 0, 0, 0L);
                NetworkHandler.syncTo(p);
            }
            Effects.sound(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
        }
    }

    /** Сколько минут защиты осталось (для меню). */
    public static long protectedMinutes(ClanData.Clan clan) {
        return Math.max(0L, (clan.protectedUntil - System.currentTimeMillis() + 59_999) / 60_000);
    }

    /** Карта для меню: клан → почему нельзя объявить войну (null — можно). */
    public static Map<UUID, String> reasons(MinecraftServer server, ClanData.Clan own) {
        Map<UUID, String> map = new HashMap<>();
        for (ClanData.Clan other : ClanData.get(server).all()) {
            if (other != own) {
                map.put(other.id, canDeclare(server, own, other));
            }
        }
        return map;
    }
}
