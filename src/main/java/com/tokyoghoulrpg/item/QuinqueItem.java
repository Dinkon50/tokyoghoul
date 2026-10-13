package com.tokyoghoulrpg.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Квинке — оружие агента CCG. В руках агента открывает способности квинке (ЛКМ, ПКМ дважды, K, L, J,
 * Shift + ЛКМ). У других классов это просто оружие. В руке рисуется твоя 3D-модель из Blockbench,
 * в инвентаре — иконка, отрисованная из той же модели.
 */
public class QuinqueItem extends SwordItem {
    private final QuinqueType type;

    public QuinqueItem(QuinqueType type, Properties properties) {
        super(type.tier(), type.damage(), type.speed(), properties);
        this.type = type;
    }

    public QuinqueType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.quinque.tooltip").withStyle(ChatFormatting.GRAY));
        if (type != QuinqueType.TRAINING) {
            tooltip.add(Component.translatable("item.tokyoghoulrpg.quinque_" + type.id() + ".desc")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /**
     * 3D-модель в руке рисует отдельный рендерер (только на клиенте).
     * Forge вызывает этот метод прямо из конструктора Item, когда поле type ещё не записано,
     * поэтому здесь нельзя обращаться к полям квинке. Рендерер подключается всем квинке,
     * а используется только у тех, чья модель предмета builtin/entity (у учебной обычная иконка).
     */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.tokyoghoulrpg.client.quinque.QuinqueRenderer.instance();
            }
        });
    }
}
