package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Маска: надевается в слот шлема. У каждой маски своя текстура на лице. */
public class MaskItem extends ArmorItem {
    private final String texture;

    public MaskItem(String name, Properties properties) {
        super(ModArmorMaterials.MASK, Type.HELMET, properties);
        this.texture = TokyoGhoulRPG.MOD_ID + ":textures/models/armor/" + name + ".png";
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return texture;
    }
}
