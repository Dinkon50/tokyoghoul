package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Прыжок на цель с ударом о землю: урон и отбрасывание всем вокруг места приземления. */
public class LeapSlamGoal extends Goal {
    private final Mob mob;
    private final Predicate<LivingEntity> ally;
    /** Первый особый удар не раньше чем через столько тиков после появления босса. */
    private static final int FIRST_DELAY = 60;
    /** Время мира, когда удар снова готов (-1: ещё не считали). Цели проверяются раз в 2 тика, поэтому время, а не счётчик. */
    private long readyAt = -1L;
    private int airTicks;
    private boolean slammed;

    public LeapSlamGoal(Mob mob, Predicate<LivingEntity> ally) {
        this.mob = mob;
        this.ally = ally;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long now = mob.level().getGameTime();
        if (readyAt < 0L) {
            readyAt = now + FIRST_DELAY;
        }
        if (now < readyAt || !BossCombat.canAct(mob)) {   // «тишина» от «Ауры возмездия», превращение
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || !mob.onGround()) {
            return false;
        }
        double dist = mob.distanceTo(target);
        return dist >= 4.0 && dist <= 16.0;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vec3 to = target.position().subtract(mob.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        double dist = flat.length();
        Vec3 dir = dist > 0.01 ? flat.scale(1.0 / dist) : Vec3.ZERO;
        double speed = Math.min(1.6, 0.35 + dist * 0.09);
        mob.setDeltaMovement(dir.x * speed, 0.75, dir.z * speed);
        mob.hasImpulse = true;
        mob.getNavigation().stop();
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Effects.sound(mob, SoundEvents.RAVAGER_ROAR, 1.2F, 1.3F);
        airTicks = 0;
        slammed = false;
    }

    @Override
    public boolean canContinueToUse() {
        return !slammed && airTicks < 60;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        airTicks++;
        if (airTicks > 4 && mob.onGround()) {
            slam();
        }
    }

    private void slam() {
        slammed = true;
        Effects.sound(mob, SoundEvents.GENERIC_EXPLODE, 0.8F, 1.3F);
        Effects.particles(mob, ParticleTypes.EXPLOSION, 3, 1.2, 0.0);
        Effects.particles(mob, ParticleTypes.CLOUD, 30, 1.5, 0.08);
        for (LivingEntity victim : BossCombat.around(mob, Balance.BOSS_LEAP_RADIUS, ally)) {
            BossCombat.strongHit(mob, victim, Balance.BOSS_LEAP_DAMAGE * BossCombat.mult(mob), 1.2);
        }
    }

    @Override
    public void stop() {
        readyAt = mob.level().getGameTime() + BossCombat.cooldown(mob, Balance.BOSS_LEAP_COOLDOWN);
    }
}
