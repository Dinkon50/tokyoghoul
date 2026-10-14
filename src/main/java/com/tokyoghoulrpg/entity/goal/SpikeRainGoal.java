package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.BossCombat;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Атака по области «Шипы» (только во второй стадии): под каждым игроком рядом и в паре мест вокруг
 * появляются кровавые круги, через 1,5 секунды из земли бьют шипы-кагуне. Успел выйти из круга — цел.
 */
public class SpikeRainGoal extends BossSkillGoal {
    private static final int WINDUP = 30;
    private final List<Vec3> spots = new ArrayList<>();

    public SpikeRainGoal(Mob mob, Predicate<LivingEntity> ally) {
        super(mob, ally, 60, Balance.HG_SPIKE_COOLDOWN, WINDUP + 6);
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob instanceof HalfGhoulBoss boss && boss.getPhase() == 2 && mob.distanceTo(target) <= 24.0;
    }

    @Override
    protected String animation() {
        return "kagune_grab_pull";
    }

    @Override
    public void start() {
        super.start();
        spots.clear();
        if (mob instanceof HalfGhoulBoss boss) {
            for (Player player : boss.nearbyPlayers(24.0)) {
                spots.add(player.position());
                double a = mob.getRandom().nextDouble() * Math.PI * 2.0;
                spots.add(player.position().add(Math.cos(a) * 3.5, 0.0, Math.sin(a) * 3.5));
                if (spots.size() >= 12) {
                    break;
                }
            }
        }
        Effects.sound(mob, SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.6F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks < WINDUP) {
            if (ticks % 3 == 0) {
                for (Vec3 spot : spots) {
                    BossCombat.ring(mob, Effects.RAGE, spot, Balance.HG_SPIKE_RADIUS, 16);
                }
            }
            return;
        }
        if (ticks != WINDUP || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Set<LivingEntity> hit = new HashSet<>();
        for (Vec3 spot : spots) {
            for (LivingEntity victim : BossCombat.around(mob, 40.0, ally)) {
                double dx = victim.getX() - spot.x;
                double dz = victim.getZ() - spot.z;
                if (dx * dx + dz * dz <= Balance.HG_SPIKE_RADIUS * Balance.HG_SPIKE_RADIUS
                        && Math.abs(victim.getY() - spot.y) < 3.0 && hit.add(victim)) {
                    BossCombat.strongHit(mob, victim, Balance.HG_SPIKE_DAMAGE * BossCombat.mult(mob), 0.4);
                    victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.6, 0.0));
                    victim.hurtMarked = true;
                }
            }
            for (int i = 0; i < 6; i++) {
                level.sendParticles(Effects.BLOOD, spot.x, spot.y + i * 0.4, spot.z, 6, 0.3, 0.1, 0.3, 0.02);
            }
        }
        Effects.sound(mob, SoundEvents.EVOKER_FANGS_ATTACK, 1.5F, 0.7F);
    }
}
