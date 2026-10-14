package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Печать перерождения: ПКМ — снять все навыки и вернуть потраченные на них очки, чтобы собрать другую прокачку.
 * Без печати навыки не сбросить. Падает с возрождённого Кровавого полугуля, продаётся у торговцев за 5000 крови.
 */
public class SkillResetItem extends Item {
    public SkillResetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        PlayerData data = PlayerData.get(serverPlayer);
        if (data == null || data.getSkills().isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("message.tokyoghoulrpg.reset_seal.nothing"), true);
            return InteractionResultHolder.fail(stack);
        }
        int refund = 0;
        for (String id : List.copyOf(data.getSkills())) {
            SkillTree.Skill skill = SkillTree.find(id);
            if (skill != null) {
                refund += data.paidFor(id, skill.cost());
            }
            data.removeSkill(id);
        }
        data.addSkillPoints(refund);
        data.setRageLevel(0);
        AbilityManager.applyClassStats(serverPlayer, data, false);
        NetworkHandler.syncTo(serverPlayer);
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        Effects.sound(serverPlayer, SoundEvents.TOTEM_USE, 0.7F, 1.2F);
        Effects.particles(serverPlayer, Effects.RAGE, 40, 0.6, 0.1);
        serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.reset_seal.done", refund));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.skill_reset_seal.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.skill_reset_seal.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
