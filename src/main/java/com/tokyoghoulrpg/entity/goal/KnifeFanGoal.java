package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * «Веер ножей» Клоуна: замах (видны линии полёта пяти ножей веером), затем бросок.
 * Каждый нож — обычный удар (блок его держит), стена останавливает нож. Встать между линиями — увернуться.
 */
public class KnifeFanGoal extends BossSkillGoal {
    private static final int WINDUP = 14;
    private static final double RANGE = 14.0;
    private static final double SPREAD = 15.0;
    private Vec3[] ends = new Vec3[0];
    private Vec3 from = Vec3.ZERO;

    public KnifeFanGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 40, StyleBalance.CLOWN_FAN_COOLDOWN, WINDUP + 4);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 3.5 && dist <= 12.0 && mob.hasLineOfSight(target);
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = mob.getTarget();
        from = mob.position().add(0.0, 1.3, 0.0);
        Vec3 aim = target != null ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(from)
                : mob.getLookAngle();
        aim = aim.normalize();
        int n = StyleBalance.CLOWN_KNIVES;
        ends = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double angle = (i - (n - 1) / 2.0) * SPREAD;
            Vec3 dir = aim.yRot((float) Math.toRadians(angle));
            ends[i] = BossCombat.clipToBlocks(mob, from, from.add(dir.scale(RANGE)));
            BossFx.line(mob, from.subtract(0.0, 1.2, 0.0), ends[i].subtract(0.0, 1.2, 0.0), 0.5, WINDUP);
        }
        Effects.sound(mob, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 1.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks != WINDUP) {
            return;
        }
        mob.swing(InteractionHand.MAIN_HAND);
        Effects.sound(mob, SoundEvents.TRIDENT_THROW, 1.0F, 1.5F);
        float damage = StyleBalance.CLOWN_KNIFE * BossCombat.mult(mob);
        for (Vec3 end : ends) {
            BossCombat.line(mob, ParticleTypes.CRIT, from, end, 0.6);
            for (LivingEntity victim : BossCombat.onLine(mob, from, end, 0.4, ally)) {
                BossCombat.hit(mob, victim, damage);
                break;                                  // нож застревает в первом
            }
        }
    }
}
