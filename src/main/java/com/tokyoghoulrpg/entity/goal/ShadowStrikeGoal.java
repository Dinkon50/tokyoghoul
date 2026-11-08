package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.entity.ClownBoss;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * «Удар из тени» Клоуна: растворяется в дыму (невидим), через секунду за спиной цели вспыхивает дым и слышен смех,
 * ещё полсекунды — и он появляется там с ударом, пробивающим блок. Успел обернуться и отойти — промах.
 */
public class ShadowStrikeGoal extends BossSkillGoal {
    private static final int VANISH = 20;
    private static final int APPEAR = 30;
    private final ClownBoss clown;
    private Vec3 spot = null;

    public ShadowStrikeGoal(ClownBoss clown, Predicate<LivingEntity> ally) {
        super(clown, ally, 80, StyleBalance.CLOWN_SHADOW_COOLDOWN, APPEAR + 6);
        this.clown = clown;
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob.distanceTo(target) <= 16.0 && mob.onGround();
    }

    @Override
    public void start() {
        super.start();
        spot = null;
        clown.smokePuff(mob.position());
        mob.setInvisible(true);
        Effects.sound(mob, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.0F);
    }

    /** Точка за спиной цели (или сбоку, если сзади стена). */
    private Vec3 behind(LivingEntity target) {
        Vec3 look = Vec3.directionFromRotation(0.0F, target.getYRot());
        Vec3 from = mob.position();
        for (double angle : new double[] {0.0, 50.0, -50.0, 100.0, -100.0}) {
            Vec3 dir = look.yRot((float) Math.toRadians(angle));
            Vec3 p = target.position().subtract(dir.scale(1.8));
            mob.setPos(p.x, target.getY(), p.z);
            boolean free = mob.level().noCollision(mob);
            mob.setPos(from.x, from.y, from.z);
            if (free) {
                return new Vec3(p.x, target.getY(), p.z);
            }
        }
        return null;
    }

    @Override
    protected void step(LivingEntity target) {
        if (target == null) {
            return;
        }
        if (ticks == VANISH) {
            spot = behind(target);
            if (spot != null) {
                clown.smokePuff(spot);
                Effects.sound(target, com.tokyoghoulrpg.registry.ModSounds.CLOWN_LAUGH.get(), 1.2F, 1.1F);
                BossFx.circle(mob, spot, 1.4, APPEAR - VANISH);
            }
        }
        if (ticks == APPEAR) {
            mob.setInvisible(false);
            if (spot == null) {
                return;
            }
            mob.teleportTo(spot.x, spot.y, spot.z);
            mob.getLookControl().setLookAt(target, 180.0F, 180.0F);
            clown.smokePuff(spot);
            mob.swing(InteractionHand.MAIN_HAND);
            BossFx.slash(mob, 2.6, 120.0, 6);
            Effects.sound(mob, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 1.3F);
            if (target.distanceToSqr(spot) <= 2.8 * 2.8) {
                BossCombat.strongHit(mob, target, StyleBalance.CLOWN_SHADOW_STRIKE * BossCombat.mult(mob), 0.8);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        mob.setInvisible(false);
    }
}
