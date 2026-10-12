package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Серверная проверка и открытие навыков. */
public final class SkillManager {
    private SkillManager() {
    }

    public static void unlock(ServerPlayer player, String id) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        SkillTree.Skill skill = SkillTree.find(id);
        // Навык должен быть из дерева именно этого класса
        if (skill == null || data.hasSkill(id) || !SkillTree.isAvailable(data.getPlayerClass(), skill.branch())) {
            return;
        }
        if (skill.requires() != null && !data.hasSkill(skill.requires())) {
            return;
        }
        if (data.getSkillPoints() < skill.cost()) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.skill_no_points"), true);
            return;
        }

        data.setSkillPoints(data.getSkillPoints() - skill.cost());
        data.addSkill(id);
        if (skill.branch() == SkillTree.Branch.RAGE) {
            // Ярость: уровень равен номеру последнего открытого улучшения
            data.setRageLevel(Math.max(data.getRageLevel(), skill.tier()));
        }

        NetworkHandler.syncTo(player);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.skill_unlocked",
                skill.branch().skillName(skill.tier())), true);
    }
}
