package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * «Выпад» Белого жнеца: клинок отведён назад, под ногами — длинная полоса, затем стремительный рывок насквозь.
 * Задевает всех на полосе (пробивает блок). Уйти с полосы — увернуться.
 */
public class ReaperLungeGoal extends BossSkillGoal {
    private static final int WINDUP = 14;
    private static final int RUSH = 7;
    private final Set<Integer> struck = new HashSet<>();
    private Vec3 dir = Vec3.ZERO;

    public ReaperLungeGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 50, StyleBalance.REAPER_LUNGE_COOLDOWN, WINDUP + RUSH + 4);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 3.0 && dist <= 11.0 && mob.onGround() && mob.hasLineOfSight(target);
    }

    @Override
    public void start() {
        super.start();
        struck.clear();
        LivingEntity target = mob.getTarget();
        Vec3 to = target != null ? target.position().subtract(mob.position()) : mob.getLookAngle();
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        dir = flat.lengthSqr() > 0.01 ? flat.normalize() : Vec3.directionFromRotation(0.0F, mob.getYRot());
        Vec3 from = mob.position().add(0.0, 0.1, 0.0);
        BossFx.line(mob, from, from.add(dir.scale(RUSH * 1.7 + 1.0)), 1.4, WINDUP);
        Effects.sound(mob, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks <= WINDUP || ticks > WINDUP + RUSH) {
            return;
        }
        if (ticks == WINDUP + 1) {
            mob.swing(InteractionHand.MAIN_HAND);
            Effects.sound(mob, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        }
        mob.setDeltaMovement(dir.x * 1.7, mob.getDeltaMovement().y, dir.z * 1.7);
        mob.hasImpulse = true;
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 1.0, mob.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
        }
        for (LivingEntity victim : BossCombat.around(mob, 1.7, ally)) {
            if (struck.add(victim.getId())) {
                BossCombat.strongHit(mob, victim, StyleBalance.REAPER_LUNGE * BossCombat.mult(mob), 0.9);
                BossFx.slash(mob, 2.4, 100.0, 5);
            }
        }
    }
}
