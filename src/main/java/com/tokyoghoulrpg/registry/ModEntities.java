package com.tokyoghoulrpg.registry;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
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

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GHOUL.get(), GhoulNpc.createAttributes().build());
        event.put(CCG_AGENT.get(), CcgAgentNpc.createAttributes().build());
        event.put(TRADER.get(), TraderNpc.createAttributes().build());
        event.put(INFORMANT.get(), InformantNpc.createAttributes().build());
        event.put(GHOUL_BOSS.get(), GhoulBoss.createAttributes().build());
        event.put(CCG_BOSS.get(), CcgBoss.createAttributes().build());
    }

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(GHOUL.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
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
