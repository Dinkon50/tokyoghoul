package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.QuinqueAbilities;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Удары боссов. Особые атаки боссов считаются сильными: пробивают блок гуля. */
public final class BossCombat {
    private BossCombat() {
    }

    /** Сильный удар: если цель держит блок, блок ломается (оглушение и откат), потом проходит урон. */
    public static boolean strongHit(Mob boss, LivingEntity target, float damage, double knockback) {
        if (target instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.isBlocking()) {
                // Идеальный блок гасит и особую атаку босса (сам босс не оглушается)
                if (QuinqueAbilities.tryParry(player, data, boss)) {
                    return false;
                }
                AbilityManager.breakBlock(player, data);
            }
        }
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(boss.damageSources().mobAttack(boss), damage);
        if (hurt) {
            Vec3 push = target.position().subtract(boss.position());
            target.knockback(knockback, -push.x, -push.z);
            Effects.blood(target, 10);
        }
        return hurt;
    }

    /** Все живые в радиусе, кроме самого босса и тех, кого filter исключает (свои). */
    public static List<LivingEntity> around(Mob boss, double radius, Predicate<LivingEntity> ally) {
        return boss.level().getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(radius),
                e -> e != boss && e.isAlive() && !e.isSpectator() && !ally.test(e)
                        && e.distanceToSqr(boss) <= radius * radius
                        && !(e instanceof ServerPlayer p && (p.isCreative())));
    }
}
