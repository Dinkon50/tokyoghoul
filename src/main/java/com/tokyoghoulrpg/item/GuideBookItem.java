package com.tokyoghoulrpg.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Книга новичка «Записки выжившего»: ПКМ — открыть подсказки по игре.
 * Первые 30 минут игры книга привязана (тег Bound): её нельзя выбросить и потерять при смерти.
 */
public class GuideBookItem extends Item {
    public static final String BOUND = "TgGuideBound";

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    public static boolean isBound(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(BOUND);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.tokyoghoulrpg.client.ClientHooks::openGuide);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.guide_book.tooltip").withStyle(ChatFormatting.GRAY));
        if (isBound(stack)) {
            tooltip.add(Component.translatable("item.tokyoghoulrpg.guide_book.bound").withStyle(ChatFormatting.GOLD));
        }
    }
}
