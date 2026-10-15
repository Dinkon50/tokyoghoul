package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * «Разряд квинке» Старшего следователя: целится копьём-квинке, линия удара видна заранее искрами
 * (направление фиксируется за полсекунды до удара — можно уйти в сторону), затем электрический выпад
 * пробивает всех на линии до 14 блоков. Сильный удар: ломает блок.
 */
public class QuinqueThrustGoal extends BossSkillGoal {
    private static final int AIM = 14;
    private static final int STRIKE = 24;
    private Vec3 dir = Vec3.ZERO;

    public QuinqueThrustGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 120, Balance.CCG_BOSS_THRUST_COOLDOWN, STRIKE + 6);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 4.0 && dist <= Balance.CCG_BOSS_THRUST_RANGE && mob.hasLineOfSight(target);
    }

    @Override
    protected void step(LivingEntity target) {
        Vec3 from = mob.getEyePosition().add(0.0, -0.4, 0.0);
        if (ticks <= AIM) {
            if (target != null) {
                dir = target.getBoundingBox().getCenter().subtract(from).normalize();
            }
            if (ticks == 1) {
                Effects.sound(mob, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.6F);
            }
            return;
        }
        Vec3 end = BossCombat.clipToBlocks(mob, from, from.add(dir.scale(Balance.CCG_BOSS_THRUST_RANGE)));
        if (ticks < STRIKE) {
            if (ticks % 2 == 0) {
                BossCombat.line(mob, ParticleTypes.ELECTRIC_SPARK, from, end, 0.5);   // знак: здесь сейчас ударит
            }
            return;
        }
        if (ticks == STRIKE) {
            for (LivingEntity victim : BossCombat.onLine(mob, from, end, 0.7, ally)) {
                BossCombat.strongHit(mob, victim, Balance.CCG_BOSS_THRUST_DAMAGE * BossCombat.mult(mob), 1.2);
            }
            BossCombat.line(mob, ParticleTypes.END_ROD, from, end, 0.3);
            BossCombat.line(mob, ParticleTypes.ELECTRIC_SPARK, from, end, 0.2);
            Effects.sound(mob, SoundEvents.TRIDENT_THUNDER, 0.8F, 1.4F);
        }
    }
}
