package com.tokyoghoulrpg.quest;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.event.ModCommands;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Серверный учёт заданий: прогресс, выполнение, награда. */
public final class QuestManager {
    private QuestManager() {
    }

    /** Текущее задание игрока или null. */
    public static Quest current(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        return data == null ? null : Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
    }

    /** Задать прогресс текущего задания (для финала агента) и проверить выполнение. */
    public static void setProgress(ServerPlayer player, int value) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        data.setQuestProgress(value);
        checkComplete(player, data);
        NetworkHandler.syncTo(player);
    }

    /** Событие для заданий-счётчиков (убийства, укусы, попадания). */
    public static void progress(ServerPlayer player, Quest.Trigger trigger, int amount) {
        PlayerData data = PlayerData.get(player);
        if (data == null || Quest.Chain.of(data.getPlayerClass()) == null) {
            return;
        }
        Quest quest = Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
        if (quest == null || quest.trigger() != trigger || quest.trigger().absolute()) {
            return;
        }
        data.setQuestProgress(data.getQuestProgress() + amount);
        checkComplete(player, data);
        NetworkHandler.syncTo(player);
    }

    /** Раз в секунду: задания «по факту» (сколько открыто навыков, какой уровень). */
    public static void tick(ServerPlayer player) {
        if (player.level().getGameTime() % 20 != 0) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || Quest.Chain.of(data.getPlayerClass()) == null) {
            return;
        }
        Quest quest = Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
        if (quest == null || !quest.trigger().absolute()) {
            return;
        }
        int value = absoluteValue(player, data, quest.trigger());
        if (value != data.getQuestProgress()) {
            data.setQuestProgress(value);
            checkComplete(player, data);
            NetworkHandler.syncTo(player);
        }
    }

    private static int absoluteValue(ServerPlayer player, PlayerData data, Quest.Trigger trigger) {
        return switch (trigger) {
            case SKILLS_OPENED -> data.getSkills().size();
            case LEVEL -> player.experienceLevel;
            default -> 0;
        };
    }

    /** Команда /tgrpg quests complete: засчитать текущее задание с наградой. */
    public static boolean completeCurrent(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        Quest quest = data == null ? null : Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
        if (quest == null) {
            return false;
        }
        data.setQuestProgress(quest.goal());
        checkComplete(player, data);
        NetworkHandler.syncTo(player);
        return true;
    }

    /** Выполненные задания закрываются по очереди; награда выдаётся ровно один раз. */
    private static void checkComplete(ServerPlayer player, PlayerData data) {
        Quest quest = Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
        while (quest != null && data.getQuestProgress() >= quest.goal()) {
            int blood = quest.bloodReward(player.getRandom());
            data.addSkillPoints(quest.reward());
            data.addBloodPoints(blood);
            data.setQuestIndex(data.getQuestIndex() + 1);
            com.tokyoghoulrpg.rank.Ranks.add(player, com.tokyoghoulrpg.combat.Balance.RANK_QUEST);
            if (!ModCommands.isSilent()) {
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.quest_done",
                        quest.title(), quest.reward(), blood));
            }
            if (quest == Quest.C_FINAL && !ModCommands.isSilent()) {
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ccg_chain_done"));
            }
            quest = Quest.byIndex(data.getPlayerClass(), data.getQuestIndex());
            // Следующее задание «по факту» может быть уже выполнено
            data.setQuestProgress(quest != null && quest.trigger().absolute()
                    ? absoluteValue(player, data, quest.trigger()) : 0);
        }
    }
}
