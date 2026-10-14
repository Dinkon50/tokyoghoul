package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Общая основа особых атак Кровавого полугуля: откат по времени мира, замах, удар.
 * Пока атака идёт, босс стоит на месте и смотрит на цель.
 */
public abstract class BossSkillGoal extends Goal {
    protected final Mob mob;
    protected final Predicate<LivingEntity> ally;
    private final int firstDelay;
    private final int cooldown;
    private final int duration;
    private long readyAt = -1L;
    protected int ticks;

    protected BossSkillGoal(Mob mob, Predicate<LivingEntity> ally, int firstDelay, int cooldown, int duration) {
        this.mob = mob;
        this.ally = ally;
        this.firstDelay = firstDelay;
        this.cooldown = cooldown;
        this.duration = duration;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Условия именно этой атаки (цель есть, дистанция подходит и т. п.). */
    protected abstract boolean ready(LivingEntity target);

    /** Каждый тик атаки, ticks от 1 до duration. */
    protected abstract void step(LivingEntity target);

    /** Анимация кагуне при начале атаки (null — без неё). */
    protected String animation() {
        return null;
    }

    @Override
    public boolean canUse() {
        long now = mob.level().getGameTime();
        if (readyAt < 0L) {
            readyAt = now + firstDelay;
        }
        if (now < readyAt || !BossCombat.canAct(mob)) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        // небольшой разброс, чтобы атаки шли вперемешку, а не всегда первая готовая
        return target != null && target.isAlive() && mob.getRandom().nextInt(3) == 0 && ready(target);
    }

    @Override
    public void start() {
        ticks = 0;
        mob.getNavigation().stop();
        String anim = animation();
        if (anim != null) {
            NetworkHandler.playAnimationFor(mob, anim);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return ticks < duration && mob.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        ticks++;
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        step(target);
    }

    @Override
    public void stop() {
        readyAt = mob.level().getGameTime() + BossCombat.cooldown(mob, cooldown);
    }
}
