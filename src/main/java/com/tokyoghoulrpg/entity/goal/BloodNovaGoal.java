package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Атака по области «Кровавая волна»: кольцо крови стягивается к боссу (1 секунда, можно отбежать),
 * затем взрывается волной на 7 блоков вокруг. Сильный удар: пробивает блок и отбрасывает.
 */
public class BloodNovaGoal extends BossSkillGoal {
    private static final int WINDUP = 20;

    public BloodNovaGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 120, Balance.HG_NOVA_COOLDOWN, WINDUP + 6);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob.distanceTo(target) <= 6.0;
    }

    @Override
    protected String animation() {
        return "kagune_attack_double";
    }

    @Override
    protected void step(LivingEntity target) {
        Vec3 center = mob.position();
        if (ticks < WINDUP) {
            double radius = Balance.HG_NOVA_RADIUS * (1.0 - ticks / (double) WINDUP);
            if (ticks % 2 == 0) {
                BossCombat.ring(mob, Effects.RAGE, center, radius, 28);
            }
            if (ticks == 1) {
                Effects.sound(mob, SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 0.8F);
            }
            return;
        }
        if (ticks == WINDUP) {
            for (LivingEntity victim : BossCombat.around(mob, Balance.HG_NOVA_RADIUS, ally)) {
                BossCombat.strongHit(mob, victim, Balance.HG_NOVA_DAMAGE * BossCombat.mult(mob), 1.6);
            }
            for (double r = 1.0; r <= Balance.HG_NOVA_RADIUS; r += 1.5) {
                BossCombat.ring(mob, Effects.BLOOD, center, r, (int) (r * 8));
            }
            Effects.particles(mob, ParticleTypes.EXPLOSION, 4, 1.5, 0.0);
            Effects.sound(mob, SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 0.9F);
        }
    }
}
