package com.tokyoghoulrpg.registry;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.world.ArenaAltarBlock;
import com.tokyoghoulrpg.world.ArenaAltarBlockEntity;
import com.tokyoghoulrpg.world.CafeSignBlock;
import com.tokyoghoulrpg.world.CafeSignBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Блоки мода: алтарь арены Кровавого полугуля. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TokyoGhoulRPG.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TokyoGhoulRPG.MOD_ID);

    /** Неразрушимый, как бедрок; слабо светится. */
    public static final RegistryObject<Block> ARENA_ALTAR = BLOCKS.register("arena_altar",
            () -> new ArenaAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(state -> 9)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ArenaAltarBlockEntity>> ARENA_ALTAR_ENTITY =
            BLOCK_ENTITIES.register("arena_altar",
                    () -> BlockEntityType.Builder.of(ArenaAltarBlockEntity::new, ARENA_ALTAR.get()).build(null));

    /** Знак кофейни: в полу кофейни, держит мирную зону и расписание. Сломать нельзя. */
    public static final RegistryObject<Block> CAFE_SIGN = BLOCKS.register("cafe_sign",
            () -> new CafeSignBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> 6)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CafeSignBlockEntity>> CAFE_SIGN_ENTITY =
            BLOCK_ENTITIES.register("cafe_sign",
                    () -> BlockEntityType.Builder.of(CafeSignBlockEntity::new, CAFE_SIGN.get()).build(null));

    /** Руда RC-стали: редкая (реже алмазов), глубоко под землёй, нужна алмазная кирка. */
    public static final RegistryObject<Block> RC_STEEL_ORE = BLOCKS.register("rc_steel_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(4.5F, 3.0F),
                    net.minecraft.util.valueproviders.UniformInt.of(4, 8)));
    public static final RegistryObject<Block> DEEPSLATE_RC_STEEL_ORE = BLOCKS.register("deepslate_rc_steel_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(6.0F, 3.0F)
                    .sound(SoundType.DEEPSLATE),
                    net.minecraft.util.valueproviders.UniformInt.of(4, 8)));

    /** Мастерская масок: верстак мастера, где маски делают на заказ за очки крови. */
    public static final RegistryObject<Block> MASK_WORKSHOP = BLOCKS.register("mask_workshop",
            () -> new com.tokyoghoulrpg.world.MaskWorkshopBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));

    /** Знамя клана: центр территории клана. Прочное, как обсидиан. */
    public static final RegistryObject<Block> CLAN_BANNER = BLOCKS.register("clan_banner",
            () -> new com.tokyoghoulrpg.world.ClanBannerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(50.0F, 1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.WOOL).lightLevel(state -> 7)));

    private ModBlocks() {
    }
}
