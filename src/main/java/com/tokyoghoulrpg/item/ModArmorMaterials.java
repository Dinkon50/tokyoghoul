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
 * Материалы масок и одежды. Прочность (второе число) у всех намного выше незерита (37):
 * маска 60, одежда гуля и форма CCG 75 (в 2 раза дольше незерита), броня особого класса 110 (в 3 раза).
 * Защита: маска как кожаный шлем, одежда гуля почти как железо, форма CCG как кольчуга.
 * Порядок защиты в массивах: ботинки, штаны, нагрудник, шлем.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    MASK("mask", 60, new int[] {1, 1, 1, 1}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, () -> Ingredient.of(Items.BONE)),
    // одежда гуля сильнее кожи: почти как железо (железо 2/5/6), прочность как у железа, немного прочности брони
    GHOUL_CLOTH("ghoul_cloth", 75, new int[] {2, 4, 5, 1}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, () -> Ingredient.of(Items.LEATHER)),
    CCG_UNIFORM("ccg_uniform", 75, new int[] {1, 4, 5, 2}, 12, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, () -> Ingredient.of(Items.IRON_INGOT)),
    /**
     * Броня особого класса CCG — сильнее незеритовой (незерит: 3/6/8/3, прочность 3, отбрасывание 0,1, множитель 37).
     * Носить может только агент CCG.
     */
    CCG_ELITE("ccg_elite", 110, new int[] {4, 7, 9, 4}, 18, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F,
            () -> Ingredient.of(com.tokyoghoulrpg.registry.ModItems.RC_STEEL_INGOT.get()), 0.15F);

    /** Базовая прочность по слотам, как у ванильных материалов: ботинки, штаны, нагрудник, шлем. */
    private static final int[] BASE_DURABILITY = {13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final Supplier<Ingredient> repair;
    private final float knockbackResistance;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantmentValue, SoundEvent equipSound,
                      float toughness, Supplier<Ingredient> repair) {
        this(name, durabilityMultiplier, defense, enchantmentValue, equipSound, toughness, repair, 0.0F);
    }

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantmentValue, SoundEvent equipSound,
                      float toughness, Supplier<Ingredient> repair, float knockbackResistance) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.repair = repair;
        this.knockbackResistance = knockbackResistance;
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
        return knockbackResistance;
    }
}
