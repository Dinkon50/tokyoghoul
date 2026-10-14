package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
 * Ядро гуля (кагуховое ядро) I, II или III качества. Только выбивается:
 * I — с обычных гулей и полугулей, II — с Пожирателя, III — с Кровавого полугуля. Купить нельзя,
 * продать торговцу можно. Привязано к тому, кто его выбил: улучшить квинке им может только он.
 * <p>
 * ПКМ с ядром — улучшить квинке: сначала ту, что во второй руке, иначе первую подходящую в инвентаре.
 * Ядро I поднимает квинке с 0 до I, ядро II — с I до II, ядро III — с II до III.
 * После улучшения квинке 10 минут нельзя выбросить или положить куда-то, кроме инвентаря.
 */
public class RcCoreItem extends Item {
    private final int tier;

    public RcCoreItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack core = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(core, level.isClientSide());
        }
        UUID owner = QuinqueUpgrade.coreOwner(core);
        if (owner != null && !owner.equals(player.getUUID())) {
            tell(serverPlayer, "not_owner", QuinqueUpgrade.coreOwnerName(core));
            return InteractionResultHolder.fail(core);
        }
        ItemStack quinque = findQuinque(player, hand);
        if (quinque == null) {
            tell(serverPlayer, "no_quinque", QuinqueUpgrade.roman(tier - 1), QuinqueUpgrade.roman(tier));
            return InteractionResultHolder.fail(core);
        }
        int before = QuinqueUpgrade.level(quinque);
        if (before >= tier) {
            tell(serverPlayer, "already", quinque.getHoverName());
            return InteractionResultHolder.fail(core);
        }
        if (before < tier - 1) {
            tell(serverPlayer, "need_lower", quinque.getHoverName(), QuinqueUpgrade.roman(before + 1));
            return InteractionResultHolder.fail(core);
        }
        QuinqueUpgrade.setLevel(quinque, tier);
        long until = level.getGameTime() + Balance.QUINQUE_LOCK_TICKS;
        QuinqueUpgrade.lock(quinque, until);
        core.shrink(1);
        Effects.sound(serverPlayer, SoundEvents.SMITHING_TABLE_USE, 1.0F, 0.8F);
        Effects.sound(serverPlayer, SoundEvents.BEACON_POWER_SELECT, 0.8F, 0.6F + tier * 0.2F);
        Effects.particles(serverPlayer, Effects.BLOOD, 30, 0.5, 0.08);
        Effects.particles(serverPlayer, ParticleTypes.ENCHANT, 40, 0.6, 0.5);
        serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.core.upgraded",
                quinque.getHoverName(), QuinqueUpgrade.roman(tier),
                Math.round(Balance.QUINQUE_LEVEL_BONUS[tier] * 100), Balance.QUINQUE_LOCK_TICKS / 20 / 60));
        return InteractionResultHolder.consume(core);
    }

    /** Квинке для улучшения: во второй руке, иначе первая в инвентаре с уровнем на один ниже ядра. */
    private ItemStack findQuinque(Player player, InteractionHand coreHand) {
        InteractionHand other = coreHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(other);
        if (QuinqueUpgrade.upgradable(held)) {
            return held;
        }
        ItemStack any = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!QuinqueUpgrade.upgradable(stack)) {
                continue;
            }
            if (QuinqueUpgrade.level(stack) == tier - 1) {
                return stack;
            }
            if (any == null) {
                any = stack;
            }
        }
        return any;
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.core." + key, args), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        String owner = QuinqueUpgrade.coreOwnerName(stack);
        if (!owner.isEmpty()) {
            tooltip.add(Component.translatable("item.tokyoghoulrpg.rc_core.owner", owner).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.tokyoghoulrpg.rc_core.use",
                QuinqueUpgrade.roman(tier - 1), QuinqueUpgrade.roman(tier)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.rc_core.sell", Balance.CORE_PRICE[tier - 1])
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier >= 3;
    }
}
