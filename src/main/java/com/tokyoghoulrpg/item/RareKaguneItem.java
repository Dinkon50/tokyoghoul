package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.Kakuja;
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

/**
 * Редкий кагуне — продаётся только на аукционе чёрного рынка. ПКМ:
 * гуль и полугуль поглощают его (+очки Какудзя и очки навыков), остальные изучают (+очки навыков).
 */
public class RareKaguneItem extends Item {
    public RareKaguneItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerData data = PlayerData.get(serverPlayer);
            if (data == null || !data.hasChosenClass()) {
                return InteractionResultHolder.fail(stack);
            }
            if (data.getPlayerClass().hasKagune()) {
                Kakuja.gain(serverPlayer, Balance.RARE_KAGUNE_KAKUJA);
                int points = data.earnSkillPoints(Balance.RARE_KAGUNE_POINTS);
                Effects.particles(serverPlayer, Effects.RAGE, 40, 0.6, 0.05);
                Effects.sound(serverPlayer, SoundEvents.GENERIC_EAT, 1.0F, 0.6F);
                serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.rare_kagune.absorbed",
                        Balance.RARE_KAGUNE_KAKUJA, points).withStyle(ChatFormatting.DARK_RED));
            } else {
                int points = data.earnSkillPoints(Balance.RARE_KAGUNE_POINTS_HUMAN);
                Effects.sound(serverPlayer, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 0.8F);
                serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.rare_kagune.studied", points)
                        .withStyle(ChatFormatting.AQUA));
            }
            NetworkHandler.syncTo(serverPlayer);
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.rare_kagune.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
