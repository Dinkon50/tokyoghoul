package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Маска: надевается в слот шлема. У каждой маски своя текстура на лице и свой небольшой эффект
 * для гуля и полугуля (см. {@link MaskEffects}). Маска «работы мастера» из мастерской сильнее.
 */
public class MaskItem extends ArmorItem {
    private final String texture;
    private final MaskEffects.Kind kind;

    public MaskItem(String name, Properties properties) {
        super(ModArmorMaterials.MASK, Type.HELMET, properties);
        this.texture = TokyoGhoulRPG.MOD_ID + ":textures/models/armor/" + name + ".png";
        this.kind = MaskEffects.Kind.byItemName(name);
    }

    public MaskEffects.Kind kind() {
        return kind;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return texture;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        return MaskEffects.isMaster(stack)
                ? Component.empty().append(base).append(" ").append(Component.translatable("item.tokyoghoulrpg.mask.master_suffix"))
                : base;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return MaskEffects.isMaster(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int tier = MaskEffects.isMaster(stack) ? 1 : 0;
        tooltip.add(Component.translatable("item.tokyoghoulrpg.mask." + kind.id() + ".effect",
                MaskEffects.percent(kind, tier)).withStyle(ChatFormatting.RED));
        if (tier == 1) {
            tooltip.add(Component.translatable("item.tokyoghoulrpg.mask.master").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.tokyoghoulrpg.mask.ghoul_only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
