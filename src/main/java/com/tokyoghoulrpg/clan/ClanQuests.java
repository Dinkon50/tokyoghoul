package com.tokyoghoulrpg.clan;

import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Задания клана: каждый день два ежедневных и каждую неделю одно еженедельное (по календарю сервера).
 * Прогресс общий — считается от всех участников. Цели растут с числом участников (+25% за каждого
 * сверх первого, не больше чем в 3 раза), чтобы маленьким кланам было так же реально, как большим.
 * Награда — в банк клана.
 */
public final class ClanQuests {
    public enum Kind {
        KILL_GHOUL("kill_ghoul", 20, true),
        KILL_CCG("kill_ccg", 20, true),
        KILL_NPC("kill_npc", 40, true),
        EARN_BLOOD("earn_blood", 500, true),
        MARKET_SALE("market_sale", 3, true),
        KILL_BOSS("kill_boss", 4, false),
        WIN_WAR("win_war", 1, false),
        EARN_BLOOD_WEEK("earn_blood_week", 6000, false);

        private final String id;
        private final int base;
        private final boolean daily;

        Kind(String id, int base, boolean daily) {
            this.id = id;
            this.base = base;
            this.daily = daily;
        }

        public String id() {
            return id;
        }

        public boolean daily() {
            return daily;
        }
    }

    public static final int DAILY_BLOOD = 600;
    public static final int DAILY_POINTS = 2;
    public static final int WEEKLY_BLOOD = 3000;
    public static final int WEEKLY_POINTS = 8;
    private static final Kind[] DAILY = {Kind.KILL_GHOUL, Kind.KILL_CCG, Kind.KILL_NPC, Kind.EARN_BLOOD, Kind.MARKET_SALE};
    private static final Kind[] WEEKLY = {Kind.KILL_BOSS, Kind.WIN_WAR, Kind.EARN_BLOOD_WEEK};

    private ClanQuests() {
    }

    public static long today() {
        return LocalDate.now().toEpochDay();
    }

    /** Цель задания для клана такого размера. */
    public static int target(Kind kind, int members) {
        if (kind == Kind.WIN_WAR) {
            return kind.base;
        }
        double scale = Math.min(3.0, 1.0 + 0.25 * Math.max(0, members - 1));
        return (int) Math.ceil(kind.base * scale);
    }

    public static Kind kind(int ordinal) {
        Kind[] all = Kind.values();
        return all[Math.max(0, Math.min(all.length - 1, ordinal))];
    }

    /** Новый день / неделя — новые задания. */
    public static void ensure(ClanData.Clan clan) {
        long day = today();
        long week = Math.floorDiv(day, 7);
        if (clan.quests.length != 3) {
            clan.quests = new int[] {-1, -1, -1};
            clan.questProgress = new int[3];
            clan.questDay = -1L;
            clan.questWeek = -1L;
        }
        if (clan.questDay != day) {
            Random random = new Random(clan.id.getMostSignificantBits() ^ day * 31L);
            int first = random.nextInt(DAILY.length);
            int second = (first + 1 + random.nextInt(DAILY.length - 1)) % DAILY.length;
            clan.quests[0] = DAILY[first].ordinal();
            clan.quests[1] = DAILY[second].ordinal();
            clan.questProgress[0] = 0;
            clan.questProgress[1] = 0;
            clan.questDay = day;
        }
        if (clan.questWeek != week) {
            Random random = new Random(clan.id.getLeastSignificantBits() ^ week * 131L);
            clan.quests[2] = WEEKLY[random.nextInt(WEEKLY.length)].ordinal();
            clan.questProgress[2] = 0;
            clan.questWeek = week;
        }
    }

    /** Прогресс от участника. */
    public static void progress(ServerPlayer player, Kind kind, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ClanData.Clan clan = ClanData.get(server).clanOf(player.getUUID());
        if (clan != null) {
            progress(server, clan, kind, amount);
        }
    }

    public static void progress(MinecraftServer server, ClanData.Clan clan, Kind kind, int amount) {
        if (amount <= 0) {
            return;
        }
        ensure(clan);
        ClanData data = ClanData.get(server);
        for (int i = 0; i < clan.quests.length; i++) {
            Kind k = kind(clan.quests[i]);
            boolean match = k == kind || (kind == Kind.EARN_BLOOD && k == Kind.EARN_BLOOD_WEEK);
            if (!match) {
                continue;
            }
            int target = target(k, clan.members.size());
            int before = clan.questProgress[i];
            if (before >= target) {
                continue;
            }
            clan.questProgress[i] = Math.min(target, before + amount);
            data.setDirty();
            if (clan.questProgress[i] >= target) {
                boolean daily = k.daily();
                int blood = daily ? DAILY_BLOOD : WEEKLY_BLOOD;
                int points = daily ? DAILY_POINTS : WEEKLY_POINTS;
                clan.treasury = (int) Math.min(Integer.MAX_VALUE, (long) clan.treasury + blood);
                clan.bankPoints = (int) Math.min(Integer.MAX_VALUE, (long) clan.bankPoints + points);
                clan.questsDone++;
                Component msg = Component.translatable("message.tokyoghoulrpg.clan_quest.done",
                        Component.translatable("clan_quest.tokyoghoulrpg." + k.id(), target), blood, points)
                        .withStyle(ChatFormatting.GOLD);
                for (UUID id : clan.members.keySet()) {
                    ServerPlayer member = server.getPlayerList().getPlayer(id);
                    if (member != null) {
                        member.sendSystemMessage(msg);
                    }
                }
            }
        }
    }
}
