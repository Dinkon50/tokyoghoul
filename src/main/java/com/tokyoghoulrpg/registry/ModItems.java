package com.tokyoghoulrpg.registry;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.ClassChange;
import com.tokyoghoulrpg.item.CoffeeItem;
import com.tokyoghoulrpg.item.InjectionItem;
import com.tokyoghoulrpg.item.QuinqueItem;
import com.tokyoghoulrpg.item.RcCoreItem;
import com.tokyoghoulrpg.item.SkillResetItem;
import com.tokyoghoulrpg.item.QuinqueType;
import com.tokyoghoulrpg.item.MaskItem;
import com.tokyoghoulrpg.item.ModArmorMaterials;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Все предметы мода. Порядок здесь = порядок во вкладке творческого режима. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TokyoGhoulRPG.MOD_ID);

    // Инъекции
    public static final RegistryObject<Item> HUMAN_INJECTION = ITEMS.register("human_injection",
            () -> new InjectionItem(ClassChange.Injection.HUMAN, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GHOUL_INJECTION = ITEMS.register("ghoul_injection",
            () -> new InjectionItem(ClassChange.Injection.GHOUL, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> CCG_INJECTION = ITEMS.register("ccg_injection",
            () -> new InjectionItem(ClassChange.Injection.CCG, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // Еда гуля
    public static final RegistryObject<Item> FLESH = ITEMS.register("flesh",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(8).saturationMod(0.8F).meat().build())));
    public static final RegistryObject<Item> COFFEE = ITEMS.register("coffee",
            () -> new CoffeeItem(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
                    .nutrition(2).saturationMod(0.4F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0), 1.0F).build())));

    // Квинке: учебная и твои четыре с 3D-моделями
    public static final RegistryObject<Item> TRAINING_QUINQUE = ITEMS.register("training_quinque",
            () -> new QuinqueItem(QuinqueType.TRAINING, new Item.Properties()));
    public static final RegistryObject<Item> QUINQUE_KURA = quinque(QuinqueType.KURA);
    public static final RegistryObject<Item> QUINQUE_IXA = quinque(QuinqueType.IXA);
    public static final RegistryObject<Item> QUINQUE_NARUKAMI = quinque(QuinqueType.NARUKAMI);
    public static final RegistryObject<Item> QUINQUE_JASON = quinque(QuinqueType.JASON);

    // Печать сброса навыков: падает с возрождённого Кровавого полугуля, у торговца за 5000 крови
    public static final RegistryObject<Item> SKILL_RESET_SEAL = ITEMS.register("skill_reset_seal",
            () -> new SkillResetItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    // Ядра гулей: улучшают квинке (I -> II -> III). Только выбиваются, продаются торговцу
    public static final RegistryObject<Item> RC_CORE_1 = ITEMS.register("rc_core_1",
            () -> new RcCoreItem(1, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> RC_CORE_2 = ITEMS.register("rc_core_2",
            () -> new RcCoreItem(2, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RC_CORE_3 = ITEMS.register("rc_core_3",
            () -> new RcCoreItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    // Маски
    public static final RegistryObject<Item> MASK_FANG = mask("mask_fang");
    public static final RegistryObject<Item> MASK_BONE = mask("mask_bone");
    public static final RegistryObject<Item> MASK_CROW = mask("mask_crow");
    public static final RegistryObject<Item> MASK_ONI = mask("mask_oni");

    // Одежда гуля
    public static final RegistryObject<Item> GHOUL_COAT = armor("ghoul_coat", ModArmorMaterials.GHOUL_CLOTH, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> GHOUL_PANTS = armor("ghoul_pants", ModArmorMaterials.GHOUL_CLOTH, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> GHOUL_BOOTS = armor("ghoul_boots", ModArmorMaterials.GHOUL_CLOTH, ArmorItem.Type.BOOTS);

    // Форма CCG
    public static final RegistryObject<Item> CCG_COAT = armor("ccg_coat", ModArmorMaterials.CCG_UNIFORM, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> CCG_PANTS = armor("ccg_pants", ModArmorMaterials.CCG_UNIFORM, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> CCG_BOOTS = armor("ccg_boots", ModArmorMaterials.CCG_UNIFORM, ArmorItem.Type.BOOTS);

    // RC-сталь: редкая руда -> сырая RC-сталь -> слиток (печь или плавильня). Из слитков — броня особого класса
    public static final RegistryObject<Item> RC_STEEL_ORE = ITEMS.register("rc_steel_ore",
            () -> new BlockItem(ModBlocks.RC_STEEL_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DEEPSLATE_RC_STEEL_ORE = ITEMS.register("deepslate_rc_steel_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_RC_STEEL_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RAW_RC_STEEL = ITEMS.register("raw_rc_steel",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> RC_STEEL_INGOT = ITEMS.register("rc_steel_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    // Броня особого класса CCG: сильнее незеритовой, носить может только агент CCG
    public static final RegistryObject<Item> CCG_ELITE_HELMET = eliteArmor("ccg_elite_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CCG_ELITE_CHESTPLATE = eliteArmor("ccg_elite_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> CCG_ELITE_LEGGINGS = eliteArmor("ccg_elite_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> CCG_ELITE_BOOTS = eliteArmor("ccg_elite_boots", ArmorItem.Type.BOOTS);

    // Яйца призыва
    public static final RegistryObject<Item> GHOUL_SPAWN_EGG = ITEMS.register("ghoul_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GHOUL, 0x1A1A1E, 0xB01E1E, new Item.Properties()));
    public static final RegistryObject<Item> CCG_AGENT_SPAWN_EGG = ITEMS.register("ccg_agent_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CCG_AGENT, 0xE8E8E8, 0x2A2A35, new Item.Properties()));
    public static final RegistryObject<Item> TRADER_SPAWN_EGG = ITEMS.register("trader_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TRADER, 0x5A3A22, 0xC9822B, new Item.Properties()));
    public static final RegistryObject<Item> INFORMANT_SPAWN_EGG = ITEMS.register("informant_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.INFORMANT, 0x3B3F4A, 0xD8C26A, new Item.Properties()));
    public static final RegistryObject<Item> GHOUL_BOSS_SPAWN_EGG = ITEMS.register("ghoul_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GHOUL_BOSS, 0x2A0A0E, 0xE0201E, new Item.Properties()));
    public static final RegistryObject<Item> CCG_BOSS_SPAWN_EGG = ITEMS.register("ccg_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CCG_BOSS, 0xF4F4F4, 0x1F4E9A, new Item.Properties()));
    public static final RegistryObject<Item> HALF_GHOUL_SPAWN_EGG = ITEMS.register("half_ghoul_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HALF_GHOUL, 0xD8D8DC, 0x8A0F1E, new Item.Properties()));
    public static final RegistryObject<Item> HALF_GHOUL_BOSS_SPAWN_EGG = ITEMS.register("half_ghoul_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HALF_GHOUL_BOSS, 0x0E0B10, 0xE01530, new Item.Properties()));

    // Алтарь арены: в творческом режиме можно поставить свою арену (босс появится, когда подойдёшь)
    public static final RegistryObject<Item> ARENA_ALTAR = ITEMS.register("arena_altar",
            () -> new BlockItem(ModBlocks.ARENA_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));

    /** Ядро по качеству 1-3. */
    public static Item core(int tier) {
        return (tier <= 1 ? RC_CORE_1 : tier == 2 ? RC_CORE_2 : RC_CORE_3).get();
    }

    // Кофейня: хозяин и знак (в творческом режиме можно поставить свою кофейню)
    public static final RegistryObject<Item> CAFE_OWNER_SPAWN_EGG = ITEMS.register("cafe_owner_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CAFE_OWNER, 0x4A2E1A, 0xE8D8B8, new Item.Properties()));
    public static final RegistryObject<Item> CAFE_SIGN = ITEMS.register("cafe_sign",
            () -> new BlockItem(ModBlocks.CAFE_SIGN.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final List<RegistryObject<Item>> MASKS = List.of(MASK_FANG, MASK_BONE, MASK_CROW, MASK_ONI);

    private ModItems() {
    }

    private static RegistryObject<Item> quinque(QuinqueType type) {
        return ITEMS.register("quinque_" + type.id(),
                () -> new QuinqueItem(type, new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    }

    private static RegistryObject<Item> mask(String name) {
        return ITEMS.register(name, () -> new MaskItem(name, new Item.Properties()));
    }

    private static RegistryObject<Item> eliteArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new com.tokyoghoulrpg.item.CcgEliteArmorItem(type,
                new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    }

    private static RegistryObject<Item> armor(String name, ModArmorMaterials material, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(material, type, new Item.Properties()));
    }
}
