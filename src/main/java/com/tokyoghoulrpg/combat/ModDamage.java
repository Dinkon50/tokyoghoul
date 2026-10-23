package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Свои виды урона мода (файлы в data/tokyoghoulrpg/damage_type).
 * kagune_ultimate — ульта гуля: проходит сквозь броню и зачарования защиты (теги в data/minecraft/tags/damage_type).
 */
public final class ModDamage {
    public static final ResourceKey<DamageType> KAGUNE_ULTIMATE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(TokyoGhoulRPG.MOD_ID, "kagune_ultimate"));

    private ModDamage() {
    }

    /** Урон ульты от гуля. Если вид урона почему-то не загрузился — обычный удар игрока. */
    public static DamageSource kaguneUltimate(ServerPlayer attacker) {
        return attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(KAGUNE_ULTIMATE)
                .map(holder -> new DamageSource(holder, attacker))
                .orElseGet(() -> attacker.damageSources().playerAttack(attacker));
    }
}
