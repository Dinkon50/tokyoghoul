package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Дальняя атака «Копьё-кагуне»: босс целится, линия удара видна заранее (направление фиксируется
 * за полсекунды до удара — можно уйти в сторону), затем кагуне пронзает всех на линии до 30 блоков.
 * Это сильный удар: пробивает блок.
 */
public class KaguneSpearGoal extends BossSkillGoal {
    private static final int AIM = 18;
    private static final int STRIKE = 28;
    private Vec3 dir = Vec3.ZERO;

    public KaguneSpearGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 160, Balance.HG_SPEAR_COOLDOWN, STRIKE + 6);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        double dist = mob.distanceTo(target);
        return dist >= 6.0 && dist <= Balance.HG_SPEAR_RANGE && mob.hasLineOfSight(target);
    }

    @Override
    protected String animation() {
        return "kagune_attack_right_thrust";
    }

    @Override
    protected void step(LivingEntity target) {
        Vec3 from = mob.getEyePosition().add(0.0, -0.3, 0.0);
        if (ticks <= AIM) {
            if (target != null) {
                dir = target.getBoundingBox().getCenter().subtract(from).normalize();
            }
            if (ticks == 1) {
                Effects.sound(mob, SoundEvents.WARDEN_SONIC_CHARGE, 1.5F, 1.2F);
            }
            return;
        }
        Vec3 end = BossCombat.clipToBlocks(mob, from, from.add(dir.scale(Balance.HG_SPEAR_RANGE)));
        if (ticks < STRIKE) {
            if (ticks == AIM + 1) {
                BossFx.line(mob, from, end, 1.3, STRIKE - AIM - 1);   // знак: здесь сейчас ударит
            }
            return;
        }
        if (ticks == STRIKE) {
            for (LivingEntity victim : BossCombat.onLine(mob, from, end, 0.6, ally)) {
                BossCombat.strongHit(mob, victim, Balance.HG_SPEAR_DAMAGE * BossCombat.mult(mob), 1.4);
            }
            BossFx.spear(mob, from, end, 0.55, 12);
            BossFx.shockwave(mob, end, 2.5, 10);
            BossFx.shake(mob, from, 14.0, 1.3F, 10);
            BossCombat.line(mob, Effects.BLOOD, from, end, 0.7);
            Effects.sound(mob, SoundEvents.TRIDENT_RIPTIDE_3, 1.5F, 0.7F);
        }
    }
}
