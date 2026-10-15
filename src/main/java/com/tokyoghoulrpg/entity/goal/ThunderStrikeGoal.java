package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * «Удар молнии» Старшего следователя (как у Narukami): под целью и рядом с ней вспыхивают круги из искр,
 * через 1,2 секунды в них бьёт молния. Успел выйти из круга — не задело.
 */
public class ThunderStrikeGoal extends BossSkillGoal {
    private static final int WINDUP = 24;
    private final List<Vec3> spots = new ArrayList<>();

    public ThunderStrikeGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 200, Balance.CCG_BOSS_THUNDER_COOLDOWN, WINDUP + 6);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 3.0 && dist <= 20.0;
    }

    @Override
    public void start() {
        super.start();
        spots.clear();
        LivingEntity target = mob.getTarget();
        if (target != null) {
            spots.add(target.position());
            for (int i = 0; i < 2; i++) {
                double a = mob.getRandom().nextDouble() * Math.PI * 2.0;
                spots.add(target.position().add(Math.cos(a) * 3.0, 0.0, Math.sin(a) * 3.0));
            }
        }
        Effects.sound(mob, SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks < WINDUP) {
            if (ticks % 3 == 0) {
                for (Vec3 spot : spots) {
                    BossCombat.ring(mob, ParticleTypes.ELECTRIC_SPARK, spot, Balance.CCG_BOSS_THUNDER_RADIUS, 18);
                }
            }
            return;
        }
        if (ticks != WINDUP || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        double r2 = Balance.CCG_BOSS_THUNDER_RADIUS * Balance.CCG_BOSS_THUNDER_RADIUS;
        for (Vec3 spot : spots) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(spot);
                bolt.setVisualOnly(true);           // видно и слышно, но огонь не поджигает
                level.addFreshEntity(bolt);
            }
            for (LivingEntity victim : BossCombat.around(mob, 30.0, ally)) {
                double dx = victim.getX() - spot.x;
                double dz = victim.getZ() - spot.z;
                if (dx * dx + dz * dz <= r2 && Math.abs(victim.getY() - spot.y) < 3.0) {
                    BossCombat.hit(mob, victim, Balance.CCG_BOSS_THUNDER_DAMAGE * BossCombat.mult(mob));
                }
            }
        }
    }
}
