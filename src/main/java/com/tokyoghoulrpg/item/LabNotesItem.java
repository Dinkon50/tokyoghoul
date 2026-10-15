package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
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

/** Записи лаборатории RC: ПКМ — прочитать и получить очки навыков. Только из лаборатории и от подопытных. */
public class LabNotesItem extends Item {
    public LabNotesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerData data = PlayerData.get(serverPlayer);
            if (data == null || !data.hasChosenClass()) {
                return InteractionResultHolder.fail(stack);
            }
            int points = data.earnSkillPoints(Balance.LAB_NOTES_POINTS);
            NetworkHandler.syncTo(serverPlayer);
            Effects.sound(serverPlayer, SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.lab_notes.read", points));
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.lab_notes.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
