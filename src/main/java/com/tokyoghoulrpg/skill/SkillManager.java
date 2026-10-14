package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

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
        int cost = SkillTree.cost(skill, player.level().getDifficulty());
        // Обычной прокачкой — не больше 85% дерева: приходится выбирать, что важнее
        if (SkillTree.overCap(data.getPlayerClass(), data.getSkills(), skill, player.level().getDifficulty())) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.skill_cap",
                    SkillTree.spentCost(data.getPlayerClass(), data.getSkills(), player.level().getDifficulty()),
                    SkillTree.capCost(data.getPlayerClass(), player.level().getDifficulty())), true);
            return;
        }
        if (data.getSkillPoints() < cost) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.skill_no_points"), true);
            return;
        }

        data.setSkillPoints(data.getSkillPoints() - cost);
        data.addSkill(id, cost);
        if (skill.branch() == SkillTree.Branch.RAGE) {
            // Ярость: уровень равен номеру последнего открытого улучшения
            data.setRageLevel(Math.max(data.getRageLevel(), skill.tier()));
        }

        NetworkHandler.syncTo(player);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.skill_unlocked",
                skill.branch().skillName(skill.tier())), true);
        if (skill.branch().isSecret()) {
            Effects.sound(player, SoundEvents.ENDER_DRAGON_GROWL, 0.6F, 1.4F);
            Effects.particles(player, Effects.RAGE, 40, 0.6, 0.05);
        }
    }
}
