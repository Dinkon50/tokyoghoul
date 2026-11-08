package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * «Удар ладонью» Мастера додзё: присел (под ногами — полоса, куда он рванёт), стремительный рывок к цели
 * и толчок ладонью. Пробивает блок и отбрасывает далеко. Уйти с полосы — увернуться.
 */
public class PalmStrikeGoal extends BossSkillGoal {
    private static final int WINDUP = 12;
    private static final int RUSH = 8;
    private Vec3 dir = Vec3.ZERO;
    private boolean landed;

    public PalmStrikeGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 100, StyleBalance.MASTER_PALM_COOLDOWN, WINDUP + RUSH + 4);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 4.0 && dist <= 12.0 && mob.onGround() && mob.hasLineOfSight(target);
    }

    @Override
    public void start() {
        super.start();
        landed = false;
        dir = Vec3.ZERO;
        Effects.sound(mob, SoundEvents.ARMOR_EQUIP_LEATHER, 1.2F, 0.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks <= WINDUP) {
            if (target != null) {
                Vec3 to = target.position().subtract(mob.position());
                Vec3 flat = new Vec3(to.x, 0.0, to.z);
                dir = flat.lengthSqr() > 0.01 ? flat.normalize() : dir;
            }
            if (ticks == 3 && dir != Vec3.ZERO) {
                Vec3 from = mob.position().add(0.0, 0.1, 0.0);
                BossFx.line(mob, from, from.add(dir.scale(RUSH * 1.5 + 1.5)), 1.6, WINDUP - 3);
            }
            if (ticks % 3 == 0 && mob.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 0.1, mob.getZ(), 4, 0.5, 0.0, 0.5, 0.02);
            }
            return;
        }
        if (landed || ticks > WINDUP + RUSH) {
            return;
        }
        mob.setDeltaMovement(dir.x * 1.5, mob.getDeltaMovement().y, dir.z * 1.5);
        mob.hasImpulse = true;
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.3, mob.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        }
        boolean hitAny = false;
        for (LivingEntity victim : BossCombat.around(mob, 1.8, ally)) {
            hitAny = true;
            if (BossCombat.strongHit(mob, victim, StyleBalance.MASTER_PALM_DAMAGE * BossCombat.mult(mob), 2.4)) {
                victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.35, 0.0));
                victim.hurtMarked = true;
            }
        }
        if (hitAny) {
            landed = true;
            mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            Vec3 at = mob.position().add(dir.scale(1.2));
            Effects.sound(mob, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4F, 0.6F);
            Effects.sound(mob, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.6F);
            BossFx.shockwave(mob, at, 3.2, 12);
            BossFx.debris(mob, at, 2.0, 30);
            BossFx.shake(mob, at, 10.0, 1.3F, 8);
        }
    }
}
