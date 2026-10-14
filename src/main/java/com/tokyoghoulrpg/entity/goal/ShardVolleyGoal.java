package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Дальняя атака «Залп осколков»: короткий замах, затем пять кристаллов-осколков в цель с небольшим разбросом.
 * Каждый осколок бьёт первого на пути; его можно поймать в блок.
 */
public class ShardVolleyGoal extends BossSkillGoal {
    private static final int WINDUP = 12;
    private static final int SHOTS = 5;
    private static final int INTERVAL = 4;

    public ShardVolleyGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 100, Balance.HG_SHARD_COOLDOWN, WINDUP + SHOTS * INTERVAL + 4);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 5.0 && dist <= 28.0 && mob.hasLineOfSight(target);
    }

    @Override
    protected String animation() {
        return "kagune_attack_left_sweep";
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks < WINDUP) {
            Effects.particles(mob, Effects.RAGE, 4, 0.5, 0.02);
            if (ticks == 1) {
                Effects.sound(mob, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 0.6F);
            }
            return;
        }
        int shot = ticks - WINDUP;
        if (shot % INTERVAL != 0 || shot / INTERVAL >= SHOTS || target == null) {
            return;
        }
        Vec3 from = mob.getEyePosition();
        Vec3 aim = target.getEyePosition().add(0.0, -0.4, 0.0).add(
                (mob.getRandom().nextDouble() - 0.5) * 1.2, (mob.getRandom().nextDouble() - 0.5) * 0.8,
                (mob.getRandom().nextDouble() - 0.5) * 1.2);
        Vec3 dir = aim.subtract(from).normalize();
        Vec3 end = BossCombat.clipToBlocks(mob, from, from.add(dir.scale(32.0)));
        List<LivingEntity> hits = BossCombat.onLine(mob, from, end, 0.3, ally);
        if (!hits.isEmpty()) {
            LivingEntity victim = hits.get(0);
            end = victim.getBoundingBox().getCenter();
            BossCombat.hit(mob, victim, Balance.HG_SHARD_DAMAGE * BossCombat.mult(mob));
        }
        BossCombat.line(mob, Effects.RAGE, from, end, 0.4);
        Effects.sound(mob, SoundEvents.TRIDENT_THROW, 1.0F, 1.4F);
    }
}
