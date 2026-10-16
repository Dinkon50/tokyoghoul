package com.tokyoghoulrpg.registry;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import com.tokyoghoulrpg.entity.HalfGhoulNpc;
import com.tokyoghoulrpg.entity.CafeOwnerNpc;
import com.tokyoghoulrpg.entity.CrimsonMonarch;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.entity.TraderNpc;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * NPC мода. Где они появляются в мире, задают файлы в data/tokyoghoulrpg/forge/biome_modifier.
 * Гули — монстры (ночью и в темноте), агенты CCG, торговцы и информаторы появляются при создании мира.
 * Боссы живут в постройках (офис CCG и логово гулей), Пожиратель изредка бродит и сам по себе.
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TokyoGhoulRPG.MOD_ID);

    public static final RegistryObject<EntityType<GhoulNpc>> GHOUL = ENTITIES.register("ghoul",
            () -> EntityType.Builder.of(GhoulNpc::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("ghoul"));
    public static final RegistryObject<EntityType<CcgAgentNpc>> CCG_AGENT = ENTITIES.register("ccg_agent",
            () -> EntityType.Builder.of(CcgAgentNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("ccg_agent"));
    public static final RegistryObject<EntityType<TraderNpc>> TRADER = ENTITIES.register("trader",
            () -> EntityType.Builder.of(TraderNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("trader"));
    public static final RegistryObject<EntityType<InformantNpc>> INFORMANT = ENTITIES.register("informant",
            () -> EntityType.Builder.of(InformantNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("informant"));
    public static final RegistryObject<EntityType<GhoulBoss>> GHOUL_BOSS = ENTITIES.register("ghoul_boss",
            () -> EntityType.Builder.of(GhoulBoss::new, MobCategory.MONSTER)
                    .sized(0.75F, 2.4F).clientTrackingRange(10).build("ghoul_boss"));
    public static final RegistryObject<EntityType<CcgBoss>> CCG_BOSS = ENTITIES.register("ccg_boss",
            () -> EntityType.Builder.of(CcgBoss::new, MobCategory.CREATURE)
                    .sized(0.75F, 2.4F).clientTrackingRange(10).build("ccg_boss"));

    /** Свита Кровавого полугуля: в мире сама не появляется, её зовёт босс. */
    public static final RegistryObject<EntityType<HalfGhoulNpc>> HALF_GHOUL = ENTITIES.register("half_ghoul",
            () -> EntityType.Builder.of(HalfGhoulNpc::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("half_ghoul"));
    /** Кровавый полугуль: самый сильный босс, живёт на своей арене. */
    public static final RegistryObject<EntityType<HalfGhoulBoss>> HALF_GHOUL_BOSS = ENTITIES.register("half_ghoul_boss",
            () -> EntityType.Builder.of(HalfGhoulBoss::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.6F).fireImmune().clientTrackingRange(12).build("half_ghoul_boss"));

    /** Алый Монарх: финальный босс, призывается Алой короной. */
    public static final RegistryObject<EntityType<CrimsonMonarch>> CRIMSON_MONARCH = ENTITIES.register("crimson_monarch",
            () -> EntityType.Builder.of(CrimsonMonarch::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.7F).fireImmune().clientTrackingRange(12).build("crimson_monarch"));

    /** Хозяин кофейни: стоит за стойкой, продаёт кофе и мясо, даёт задания. */
    public static final RegistryObject<EntityType<CafeOwnerNpc>> CAFE_OWNER = ENTITIES.register("cafe_owner",
            () -> EntityType.Builder.of(CafeOwnerNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("cafe_owner"));

    /** Подопытный: враг из заброшенной лаборатории RC. */
    public static final RegistryObject<EntityType<com.tokyoghoulrpg.entity.TestSubjectNpc>> TEST_SUBJECT =
            ENTITIES.register("test_subject", () -> EntityType.Builder.of(com.tokyoghoulrpg.entity.TestSubjectNpc::new,
                    MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8).build("test_subject"));
    /** Скупщик: торговец с чёрного рынка. */
    public static final RegistryObject<EntityType<com.tokyoghoulrpg.entity.FenceNpc>> FENCE = ENTITIES.register("fence",
            () -> EntityType.Builder.of(com.tokyoghoulrpg.entity.FenceNpc::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("fence"));

    /** Наёмник: нанимается контрактом у Скупщика. */
    public static final RegistryObject<EntityType<com.tokyoghoulrpg.entity.MercenaryNpc>> MERCENARY = ENTITIES.register("mercenary",
            () -> EntityType.Builder.of(com.tokyoghoulrpg.entity.MercenaryNpc::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("mercenary"));

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GHOUL.get(), GhoulNpc.createAttributes().build());
        event.put(CCG_AGENT.get(), CcgAgentNpc.createAttributes().build());
        event.put(TRADER.get(), TraderNpc.createAttributes().build());
        event.put(INFORMANT.get(), InformantNpc.createAttributes().build());
        event.put(GHOUL_BOSS.get(), GhoulBoss.createAttributes().build());
        event.put(CCG_BOSS.get(), CcgBoss.createAttributes().build());
        event.put(HALF_GHOUL.get(), HalfGhoulNpc.createAttributes().build());
        event.put(HALF_GHOUL_BOSS.get(), HalfGhoulBoss.createAttributes().build());
        event.put(CAFE_OWNER.get(), InformantNpc.createAttributes().build());
        event.put(CRIMSON_MONARCH.get(), CrimsonMonarch.createMonarchAttributes().build());
        event.put(TEST_SUBJECT.get(), com.tokyoghoulrpg.entity.TestSubjectNpc.createAttributes().build());
        event.put(FENCE.get(), InformantNpc.createAttributes().build());
        event.put(MERCENARY.get(), com.tokyoghoulrpg.entity.MercenaryNpc.createAttributes().build());
    }

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(GHOUL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        // подопытные появляются только в лабораториях (поверх их поселяет постройка), при любом свете
        event.register(TEST_SUBJECT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(CCG_AGENT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(TRADER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(INFORMANT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(GHOUL_BOSS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GhoulBoss::checkBossSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(CCG_BOSS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
