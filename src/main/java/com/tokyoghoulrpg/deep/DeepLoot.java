package com.tokyoghoulrpg.deep;

import com.mojang.serialization.Codec;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Регистрация модификатора добычи «добыча других модов» (tokyoghoulrpg:foreign_loot). */
public final class DeepLoot {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TokyoGhoulRPG.MOD_ID);

    public static final RegistryObject<Codec<ForeignLootModifier>> FOREIGN_LOOT =
            LOOT_MODIFIERS.register("foreign_loot", ForeignLootModifier.CODEC);

    private DeepLoot() {
    }
}
