package com.tokyoghoulrpg.style;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * «Метка смерти»: меченый светится сквозь стены и получает больше урона от всех.
 * Её ставит игрок в стиле «Жнец» (Shift + C) и Белый жнец на свою цель.
 * Хранится в данных самого существа: до какого тика мира действует.
 */
public final class DeathMark {
    private static final String KEY = "tokyoghoulrpg_death_mark";

    private DeathMark() {
    }

    public static void apply(LivingEntity target, int ticks) {
        long until = target.level().getGameTime() + ticks;
        target.getPersistentData().putLong(KEY, Math.max(until, target.getPersistentData().getLong(KEY)));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false, true));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), com.tokyoghoulrpg.registry.ModSounds.DEATH_MARK.get(),
                target.getSoundSource(), 1.0F, 1.0F);
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() + 0.3,
                    target.getZ(), 12, 0.3, 0.2, 0.3, 0.02);
        }
    }

    public static boolean isMarked(LivingEntity target) {
        return target.getPersistentData().getLong(KEY) > target.level().getGameTime();
    }

    /** Множитель урона по меченому (1, если метки нет). */
    public static float factor(LivingEntity target) {
        return isMarked(target) ? (float) (1.0 + StyleBalance.MARK_BONUS) : 1.0F;
    }
}
