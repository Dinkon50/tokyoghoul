package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Рывок к цели: короткий разгон, затем стремительный бросок. Каждого на пути бьёт один раз. */
public class ChargeStrikeGoal extends Goal {
    private static final int WINDUP = 10;
    private static final int RUSH = 10;

    private final Mob mob;
    private final Predicate<LivingEntity> ally;
    private final Set<LivingEntity> hit = new HashSet<>();
    /** Первый особый удар не раньше чем через столько тиков после появления босса. */
    private static final int FIRST_DELAY = 80;
    /** Время мира, когда удар снова готов (-1: ещё не считали). Цели проверяются раз в 2 тика, поэтому время, а не счётчик. */
    private long readyAt = -1L;
    private int ticks;
    private Vec3 dir = Vec3.ZERO;

    public ChargeStrikeGoal(Mob mob, Predicate<LivingEntity> ally) {
        this.mob = mob;
        this.ally = ally;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long now = mob.level().getGameTime();
        if (readyAt < 0L) {
            readyAt = now + FIRST_DELAY;
        }
        if (now < readyAt) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || !mob.onGround()) {
            return false;
        }
        double dist = mob.distanceTo(target);
        return dist >= 5.0 && dist <= 14.0 && mob.hasLineOfSight(target);
    }

    @Override
    public void start() {
        ticks = 0;
        hit.clear();
        mob.getNavigation().stop();
        Effects.sound(mob, SoundEvents.RAVAGER_ROAR, 0.8F, 1.6F);
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < WINDUP + RUSH;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        LivingEntity target = mob.getTarget();
        if (ticks <= WINDUP) {
            // разгон: стоит на месте и смотрит на цель
            if (target != null) {
                mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                Vec3 to = target.position().subtract(mob.position());
                Vec3 flat = new Vec3(to.x, 0.0, to.z);
                dir = flat.lengthSqr() > 0.01 ? flat.normalize() : Vec3.ZERO;
            }
            mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
            if (ticks == WINDUP) {
                Effects.sound(mob, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.7F);
            }
            return;
        }
        mob.setDeltaMovement(dir.x * 1.3, mob.getDeltaMovement().y, dir.z * 1.3);
        mob.hasImpulse = true;
        Effects.particles(mob, ParticleTypes.CLOUD, 3, 0.3, 0.02);
        for (LivingEntity victim : BossCombat.around(mob, 1.6, ally)) {
            if (hit.add(victim)) {
                BossCombat.strongHit(mob, victim, Balance.BOSS_CHARGE_DAMAGE, 1.0);
                Effects.sound(victim, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F);
            }
        }
    }

    @Override
    public void stop() {
        readyAt = mob.level().getGameTime() + Balance.BOSS_CHARGE_COOLDOWN;
        mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.2, 1.0, 0.2));
    }
}
