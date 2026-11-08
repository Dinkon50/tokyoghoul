package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * «Шквал» Мастера додзё: короткий замах (пыль из-под ног), затем пять ударов руками по очереди перед собой.
 * Первые четыре — обычные (их держит блок), пятый — сильный: пробивает блок и отбрасывает.
 */
public class MasterFlurryGoal extends BossSkillGoal {
    private static final int WINDUP = 8;
    private static final int HITS = 5;
    private static final int INTERVAL = 3;
    private static final double REACH = 3.4;

    public MasterFlurryGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 60, StyleBalance.MASTER_FLURRY_COOLDOWN, WINDUP + HITS * INTERVAL + 4);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob.distanceTo(target) <= 3.2 && mob.onGround();
    }

    @Override
    public void start() {
        super.start();
        Effects.sound(mob, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks < WINDUP) {
            if (ticks % 2 == 0 && mob.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.1, mob.getZ(), 3, 0.4, 0.0, 0.4, 0.02);
            }
            return;
        }
        int t = ticks - WINDUP;
        if (t % INTERVAL != 0 || t / INTERVAL >= HITS) {
            return;
        }
        int index = t / INTERVAL;
        boolean last = index == HITS - 1;
        // подшаг к цели: шквал идёт вперёд
        if (target != null && mob.distanceTo(target) > 2.0) {
            Vec3 to = target.position().subtract(mob.position());
            Vec3 step = new Vec3(to.x, 0.0, to.z).normalize().scale(0.45);
            mob.setDeltaMovement(step.x, mob.getDeltaMovement().y, step.z);
        }
        mob.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        float damage = StyleBalance.MASTER_FLURRY_HIT * BossCombat.mult(mob);
        Vec3 look = Vec3.directionFromRotation(0.0F, mob.getYRot());
        for (LivingEntity victim : BossCombat.around(mob, REACH, ally)) {
            Vec3 to = victim.position().subtract(mob.position());
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.lengthSqr() > 0.01 && flat.normalize().dot(look) < Math.cos(Math.toRadians(70.0))) {
                continue;
            }
            if (last) {
                BossCombat.strongHit(mob, victim, damage * StyleBalance.MASTER_FLURRY_LAST, 1.3);
            } else {
                BossCombat.hit(mob, victim, damage);
            }
        }
        Effects.sound(mob, last ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG,
                1.0F, 0.75F + index * 0.07F);
        BossFx.slash(mob, REACH, last ? 140.0 : 70.0, last ? 8 : 5);
        if (last) {
            BossFx.shockwave(mob, mob.position().add(look.scale(1.6)), 2.6, 10);
            BossFx.shake(mob, mob.position(), 8.0, 0.8F, 6);
            BossFx.debris(mob, mob.position().add(look.scale(1.6)), 1.5, 20);
        }
    }
}
