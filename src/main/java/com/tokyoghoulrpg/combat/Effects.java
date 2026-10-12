package com.tokyoghoulrpg.combat;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/** Звуки и частицы способностей. Всё вызывается на сервере и видно всем игрокам рядом. */
public final class Effects {
    /** Тёмно-красные брызги крови. */
    public static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.06F), 1.2F);
    /** Яркие красные частицы ауры ярости. */
    public static final DustParticleOptions RAGE = new DustParticleOptions(new Vector3f(0.9F, 0.08F, 0.18F), 1.0F);

    private Effects() {
    }

    public static void sound(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void particles(Entity at, ParticleOptions type, int count, double spread, double speed) {
        if (at.level() instanceof ServerLevel level) {
            level.sendParticles(type, at.getX(), at.getY(0.6), at.getZ(), count, spread, spread, spread, speed);
        }
    }

    /** Брызги крови из цели. */
    public static void blood(Entity target, int count) {
        particles(target, BLOOD, count, 0.25, 0.05);
    }
}
