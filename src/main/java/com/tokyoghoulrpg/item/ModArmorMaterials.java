package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Материалы масок и одежды. По твоей просьбе они не сильнее обычного снаряжения:
 * маски как кожаный шлем, одежда гуля чуть лучше кожи, форма CCG как кольчуга.
 * Порядок защиты в массивах: ботинки, штаны, нагрудник, шлем.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    MASK("mask", 5, new int[] {1, 1, 1, 1}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, () -> Ingredient.of(Items.BONE)),
    GHOUL_CLOTH("ghoul_cloth", 7, new int[] {1, 2, 3, 1}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, () -> Ingredient.of(Items.LEATHER)),
    CCG_UNIFORM("ccg_uniform", 15, new int[] {1, 4, 5, 2}, 12, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, () -> Ingredient.of(Items.IRON_INGOT));

    /** Базовая прочность по слотам, как у ванильных материалов: ботинки, штаны, нагрудник, шлем. */
    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final Supplier<Ingredient> repair;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantmentValue, SoundEvent equipSound,
                      float toughness, Supplier<Ingredient> repair) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.repair = repair;
    }

    private static int slot(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[slot(type)] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense[slot(type)];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    /** Отсюда берётся путь к текстуре: tokyoghoulrpg:textures/models/armor/<name>_layer_1.png */
    @Override
    public String getName() {
        return TokyoGhoulRPG.MOD_ID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
