package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Одежда гуля (плащ, штаны, ботинки). Полный комплект и любая маска — «Облик гуля», см. {@link MaskEffects}. */
public class GhoulClothItem extends ArmorItem {
    public GhoulClothItem(Type type, Properties properties) {
        super(ModArmorMaterials.GHOUL_CLOTH, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.ghoul_cloth.set").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.ghoul_cloth.set_effect",
                Math.round(Balance.GHOUL_SET_DAMAGE * 100), Math.round(Balance.GHOUL_SET_DEFENSE * 100),
                Math.round(Balance.GHOUL_SET_SPEED * 100)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.mask.ghoul_only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
