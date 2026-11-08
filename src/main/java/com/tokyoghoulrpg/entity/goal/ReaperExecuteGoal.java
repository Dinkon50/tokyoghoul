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
 * «Казнь» Белого жнеца: поднимает клинок над головой, под целью загорается круг (1 секунда),
 * затем удар сверху в круг. Пробивает блок. Если у цели меньше половины здоровья — урон вдвое больше.
 */
public class ReaperExecuteGoal extends BossSkillGoal {
    private static final int WINDUP = 20;
    private static final double RADIUS = 1.8;
    private Vec3 spot = Vec3.ZERO;

    public ReaperExecuteGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 100, StyleBalance.REAPER_EXECUTE_COOLDOWN, WINDUP + 6);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob.distanceTo(target) <= 4.0;
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = mob.getTarget();
        spot = target != null ? target.position() : mob.position().add(mob.getLookAngle().scale(2.0));
        BossFx.circle(mob, spot, RADIUS, WINDUP);
        Effects.sound(mob, SoundEvents.BELL_RESONATE, 1.0F, 0.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks < WINDUP && ticks % 4 == 0 && mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY() + 3.0, mob.getZ(), 2, 0.1, 0.2, 0.1, 0.01);
        }
        if (ticks != WINDUP) {
            return;
        }
        mob.swing(InteractionHand.MAIN_HAND);
        Effects.sound(mob, com.tokyoghoulrpg.registry.ModSounds.EXECUTION.get(), 1.2F, 0.85F);
        Effects.sound(mob, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
        BossFx.bolt(mob, spot, 3.0);
        BossFx.shockwave(mob, spot, RADIUS + 0.6, 10);
        BossFx.shake(mob, spot, 10.0, 1.0F, 6);
        for (LivingEntity victim : BossCombat.around(mob, 6.0, ally)) {
            if (victim.position().distanceToSqr(spot) > RADIUS * RADIUS + 0.5) {
                continue;
            }
            boolean low = victim.getHealth() < victim.getMaxHealth() * 0.5F;
            float damage = (low ? StyleBalance.REAPER_EXECUTE_LOW : StyleBalance.REAPER_EXECUTE) * BossCombat.mult(mob);
            BossCombat.strongHit(mob, victim, damage, 0.6);
            if (low && mob.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0, victim.getZ(), 16,
                        0.3, 0.5, 0.3, 0.04);
            }
        }
    }
}
