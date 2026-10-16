package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.MercenaryNpc;
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

/** Контракт наёмника: ПКМ — рядом появляется наёмник и служит 10 минут. Продаёт Скупщик. */
public class MercContractItem extends Item {
    public MercContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (MercenaryNpc.hire(serverPlayer) == null) {
                return InteractionResultHolder.fail(stack);
            }
            Effects.sound(serverPlayer, SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.8F);
            serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.mercenary.hired",
                    Balance.MERC_TICKS / 1200).withStyle(ChatFormatting.GOLD));
            serverPlayer.getCooldowns().addCooldown(this, 40);
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.merc_contract.tooltip", Balance.MERC_TICKS / 1200)
                .withStyle(ChatFormatting.GRAY));
    }
}
