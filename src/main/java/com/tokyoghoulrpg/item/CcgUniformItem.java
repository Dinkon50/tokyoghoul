package com.tokyoghoulrpg.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Форма CCG (плащ, штаны, ботинки). Полный комплект на агенте — «Защита CCG», см. {@link CcgSet}. */
public class CcgUniformItem extends ArmorItem {
    public CcgUniformItem(Type type, Properties properties) {
        super(ModArmorMaterials.CCG_UNIFORM, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CcgSet.tooltip(tooltip);
    }
}
