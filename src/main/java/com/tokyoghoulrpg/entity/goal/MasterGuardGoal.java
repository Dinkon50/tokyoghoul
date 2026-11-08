package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.entity.DojoMaster;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * «Стойка» Мастера додзё: руки перед лицом, 2 секунды. Удары спереди слабее на 80% (DojoMaster.hurt).
 * Кто ударит его спереди вблизи, через мгновение получает ответ: сильный удар, пробивает блок.
 * Совет игроку: в стойке не бить в лоб — зайти за спину или подождать.
 */
public class MasterGuardGoal extends BossSkillGoal {
    private static final int COUNTER_DELAY = 4;
    private final DojoMaster master;
    private int counterAt = -1;
    private LivingEntity counterTarget = null;

    public MasterGuardGoal(DojoMaster master, Predicate<LivingEntity> ally) {
        super(master, ally, 140, StyleBalance.MASTER_GUARD_COOLDOWN, StyleBalance.MASTER_GUARD_TICKS);
        this.master = master;
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob.distanceTo(target) <= 6.0;
    }

    @Override
    public void start() {
        super.start();
        counterAt = -1;
        counterTarget = null;
        master.setGuarding(true);
        Effects.sound(mob, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.5F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (counterAt < 0 && master.hasCounterTarget()) {
            counterTarget = master.takeCounterTarget();
            counterAt = ticks + COUNTER_DELAY;
        }
        if (counterAt >= 0 && ticks >= counterAt) {
            counter();
            ticks = StyleBalance.MASTER_GUARD_TICKS;      // стойка закончилась ответом
            return;
        }
        if (ticks % 8 == 0 && mob.level() instanceof ServerLevel level) {
            Vec3 at = mob.position().add(mob.getLookAngle().scale(0.5)).add(0.0, 1.7, 0.0);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 3, 0.25, 0.25, 0.25, 0.0);
        }
    }

    private void counter() {
        master.setGuarding(false);
        LivingEntity victim = counterTarget;
        counterTarget = null;
        if (victim == null || !victim.isAlive() || victim.distanceTo(mob) > 4.5) {
            return;
        }
        mob.getLookControl().setLookAt(victim, 90.0F, 90.0F);
        mob.swing(InteractionHand.MAIN_HAND);
        BossCombat.strongHit(mob, victim, StyleBalance.MASTER_COUNTER_DAMAGE * BossCombat.mult(mob), 1.6);
        Effects.sound(mob, SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 0.6F);
        BossFx.slash(mob, 3.0, 120.0, 8);
        BossFx.shake(mob, victim.position(), 6.0, 1.0F, 6);
    }

    @Override
    public void stop() {
        master.setGuarding(false);
        counterAt = -1;
        counterTarget = null;
        super.stop();
    }
}
