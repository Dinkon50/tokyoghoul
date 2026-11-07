package com.tokyoghoulrpg.registry;

import com.mojang.serialization.Codec;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.world.LandJigsawStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Типы построек мода: «tokyoghoulrpg:land_jigsaw» — как jigsaw, но только на сухой земле. */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, TokyoGhoulRPG.MOD_ID);

    public static final RegistryObject<StructureType<LandJigsawStructure>> LAND_JIGSAW =
            STRUCTURE_TYPES.register("land_jigsaw", () -> typed(LandJigsawStructure.CODEC));

    private ModStructures() {
    }

    private static <T extends Structure> StructureType<T> typed(Codec<T> codec) {
        return () -> codec;
    }
}
