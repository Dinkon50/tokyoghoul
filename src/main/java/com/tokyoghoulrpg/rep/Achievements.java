package com.tokyoghoulrpg.rep;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * Достижения игрока. Каждое считает свой счётчик; дошёл до цели — награда (очки крови и навыков)
 * и титул. Последний полученный титул показывается в «Профиле» и в списке достижений
 * (там же можно выбрать любой из полученных). О редких достижениях узнаёт весь сервер.
 */
public final class Achievements {
    public enum A {
        FIRST_BLOOD("first_blood", "npc_kills", 1, 50, 0, false),
        HUNTER("hunter", "npc_kills", 100, 500, 2, false),
        SLAYER("slayer", "npc_kills", 500, 2000, 5, true),
        BOSS_SLAYER("boss_slayer", "boss_kills", 1, 300, 1, false),
        BOSS_MASTER("boss_master", "boss_kills", 10, 2000, 5, true),
        MONARCH("monarch", "monarch_kills", 1, 5000, 10, true),
        MERCHANT("merchant", "market_sales", 1, 100, 0, false),
        TYCOON("tycoon", "market_sales", 25, 2000, 3, true),
        WARRIOR("warrior", "war_kills", 10, 800, 2, false),
        CONQUEROR("conqueror", "war_wins", 1, 1000, 3, true),
        AWAKENED("awakened", "kakuja_stage", 5, 3000, 5, true),
        LOYAL("loyal", "daily_streak", 7, 700, 2, false),
        FOUNDER("founder", "clan_founded", 1, 200, 0, false),
        COMBO_MASTER("combo_master", "combos", 50, 600, 2, false),
        PRESTIGE("prestige", "prestige", 1, 0, 0, true),
        LEGEND("legend", "prestige", 5, 5000, 10, true);

        public final String id;
        public final String stat;
        public final int goal;
        public final int blood;
        public final int points;
        public final boolean announce;

        A(String id, String stat, int goal, int blood, int points, boolean announce) {
            this.id = id;
            this.stat = stat;
            this.goal = goal;
            this.blood = blood;
            this.points = points;
            this.announce = announce;
        }

        public String titleKey() {
            return "achievement.tokyoghoulrpg." + id + ".title";
        }

        public String descKey() {
            return "achievement.tokyoghoulrpg." + id + ".desc";
        }
    }

    private Achievements() {
    }

    /** Прибавить к счётчику. */
    public static void add(ServerPlayer player, String stat, int amount) {
        PlayerData data = PlayerData.get(player);
        if (data == null || amount <= 0) {
            return;
        }
        data.setStat(stat, data.getStat(stat) + amount);
        check(player, data, stat);
    }

    /** Счётчик — максимум (стадия Какудзя, серия дней): не меньше, чем было. */
    public static void atLeast(ServerPlayer player, String stat, int value) {
        PlayerData data = PlayerData.get(player);
        if (data == null || value <= data.getStat(stat)) {
            return;
        }
        data.setStat(stat, value);
        check(player, data, stat);
    }

    private static void check(ServerPlayer player, PlayerData data, String stat) {
        for (A a : A.values()) {
            if (!a.stat.equals(stat) || data.hasAchievement(a.id) || data.getStat(stat) < a.goal) {
                continue;
            }
            data.addAchievement(a.id);
            data.setTitle(a.id);
            data.addBloodPoints(a.blood);
            int points = a.points > 0 ? data.earnSkillPoints(a.points) : 0;
            Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.1F);
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.achievement.got",
                    Component.translatable(a.titleKey()), Component.translatable(a.descKey(), a.goal), a.blood, points)
                    .withStyle(ChatFormatting.GOLD));
            if (a.announce && player.getServer() != null) {
                Component news = Component.translatable("message.tokyoghoulrpg.achievement.announce",
                        player.getGameProfile().getName(), Component.translatable(a.titleKey())).withStyle(ChatFormatting.YELLOW);
                for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
                    if (other != player) {
                        other.sendSystemMessage(news);
                    }
                }
            }
        }
        NetworkHandler.syncTo(player);
    }

    /** Выбрать титул из полученных (пустой — без титула). */
    public static void chooseTitle(ServerPlayer player, String id) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        if (id.isEmpty() || data.hasAchievement(id)) {
            data.setTitle(id);
            NetworkHandler.syncTo(player);
        }
    }
}
