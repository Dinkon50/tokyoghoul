package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.QuinqueAbilities;
import com.tokyoghoulrpg.combat.Silence;
import com.tokyoghoulrpg.difficulty.DifficultyScaling;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
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

    /** Множитель урона особых атак этого босса. */
    public static float mult(Mob boss) {
        return boss instanceof BossMob b ? b.damageMultiplier() : 1.0F;
    }

    /** Откат особой атаки с учётом сложности и самого босса. */
    public static int cooldown(Mob boss, int base) {
        double k = boss instanceof BossMob b ? b.cooldownMultiplier() : 1.0;
        return DifficultyScaling.bossCooldown(boss, (int) Math.round(base * k));
    }

    /** Можно ли начать особую атаку: не под «тишиной» и не во время превращения. */
    public static boolean canAct(Mob boss) {
        return !Silence.isSilenced(boss) && (!(boss instanceof BossMob b) || b.canUseSpecials());
    }

    /** Обычный удар (не пробивает блок, его можно поймать в блок). */
    public static boolean hit(Mob boss, LivingEntity target, float damage) {
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(boss.damageSources().mobAttack(boss), damage);
        if (hurt) {
            Effects.blood(target, 6);
        }
        return hurt;
    }

    /** Где луч от from к to упирается в блок (или to, если не упирается). */
    public static Vec3 clipToBlocks(Mob boss, Vec3 from, Vec3 to) {
        HitResult result = boss.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        return result.getType() == HitResult.Type.MISS ? to : result.getLocation();
    }

    /** Все живые (кроме своих), кого задевает отрезок from-to, от ближнего к дальнему. */
    public static List<LivingEntity> onLine(Mob boss, Vec3 from, Vec3 to, double thickness, Predicate<LivingEntity> ally) {
        AABB area = new AABB(from, to).inflate(thickness + 1.0);
        List<LivingEntity> list = new ArrayList<>(boss.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != boss && e.isAlive() && !e.isSpectator() && !ally.test(e)
                        && !(e instanceof ServerPlayer p && p.isCreative())
                        && e.getBoundingBox().inflate(thickness).clip(from, to).isPresent()));
        list.sort((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from)));
        return list;
    }

    /** Линия частиц (след выстрела или знак будущего удара). */
    public static void line(Mob boss, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 d = to.subtract(from);
        int points = Math.max(2, (int) (d.length() / step));
        for (int i = 0; i <= points; i++) {
            Vec3 p = from.add(d.scale(i / (double) points));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** Кольцо частиц на земле. */
    public static void ring(Mob boss, ParticleOptions particle, Vec3 center, double radius, int points) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2.0 / points;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Все живые в радиусе, кроме самого босса и тех, кого filter исключает (свои). */
    public static List<LivingEntity> around(Mob boss, double radius, Predicate<LivingEntity> ally) {
        return boss.level().getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(radius),
                e -> e != boss && e.isAlive() && !e.isSpectator() && !ally.test(e)
                        && e.distanceToSqr(boss) <= radius * radius
                        && !(e instanceof ServerPlayer p && (p.isCreative())));
    }
}
