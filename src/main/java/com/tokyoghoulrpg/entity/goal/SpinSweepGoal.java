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

/** Круговой удар: короткий замах на месте, затем удар по всем вокруг. */
public class SpinSweepGoal extends Goal {
    private static final int WINDUP = 12;

    private final Mob mob;
    private final Predicate<LivingEntity> ally;
    /** Первый особый удар не раньше чем через столько тиков после появления босса. */
    private static final int FIRST_DELAY = 100;
    /** Время мира, когда удар снова готов (-1: ещё не считали). Цели проверяются раз в 2 тика, поэтому время, а не счётчик. */
    private long readyAt = -1L;
    private int ticks;

    public SpinSweepGoal(Mob mob, Predicate<LivingEntity> ally) {
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
        if (now < readyAt || !BossCombat.canAct(mob)) {   // «тишина» от «Ауры возмездия», превращение
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && mob.distanceTo(target) <= 3.5;
    }

    @Override
    public void start() {
        ticks = 0;
        mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return ticks <= WINDUP;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        mob.setYRot(mob.getYRot() + 30.0F);
        if (ticks == WINDUP) {
            Effects.sound(mob, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.6F);
            Effects.particles(mob, ParticleTypes.SWEEP_ATTACK, 8, 1.8, 0.0);
            for (LivingEntity victim : BossCombat.around(mob, Balance.BOSS_SWEEP_RADIUS, ally)) {
                BossCombat.strongHit(mob, victim, Balance.BOSS_SWEEP_DAMAGE * BossCombat.mult(mob), 0.9);
            }
        }
    }

    @Override
    public void stop() {
        readyAt = mob.level().getGameTime() + BossCombat.cooldown(mob, Balance.BOSS_SWEEP_COOLDOWN);
    }
}
