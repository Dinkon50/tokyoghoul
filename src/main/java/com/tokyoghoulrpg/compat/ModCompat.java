package com.tokyoghoulrpg.compat;

import com.mojang.serialization.Codec;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Совместимость с другими модами: регистрация модификатора появления (tokyoghoulrpg:share_spawns). */
public final class ModCompat {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, TokyoGhoulRPG.MOD_ID);

    public static final RegistryObject<Codec<ShareSpawnsBiomeModifier>> SHARE_SPAWNS =
            BIOME_MODIFIERS.register("share_spawns", () -> ShareSpawnsBiomeModifier.CODEC);

    private ModCompat() {
    }
}
