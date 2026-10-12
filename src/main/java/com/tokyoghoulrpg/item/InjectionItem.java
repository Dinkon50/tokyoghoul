package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.data.ClassChange;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Инъекция: зажми ПКМ на 1,5 секунды, чтобы сменить класс. Если на твой класс инъекция
 * не действует, она не тратится. Какие переходы возможны, решает ClassChange.
 */
public class InjectionItem extends Item {
    private static final int USE_TICKS = 30;

    private final ClassChange.Injection kind;

    public InjectionItem(ClassChange.Injection kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PlayerData data = PlayerData.get(player);
        PlayerClass result = data == null ? null : ClassChange.result(kind, data.getPlayerClass());
        if (result == null) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.injection_no_effect"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && ClassChange.inject(player, kind)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg." + kind.id() + ".tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.injection.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
