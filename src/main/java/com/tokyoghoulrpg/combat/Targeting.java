package com.tokyoghoulrpg.combat;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Поиск целей: по взгляду игрока и в прямоугольной области перед ним. */
public final class Targeting {
    /** Результат луча взгляда. Если entity равно null, попали в блок. */
    public record Hit(LivingEntity entity, Vec3 point) {
    }

    private Targeting() {
    }

    public static boolean valid(ServerPlayer self, Entity entity) {
        return entity instanceof LivingEntity living
                && living != self
                && living.isAlive()
                && !living.isSpectator();
    }

    /**
     * Луч из глаз игрока. Блок на пути закрывает цель за ним.
     * Если allowBlock равно true и первым попался блок, возвращается точка на блоке.
     */
    public static Hit raycast(ServerPlayer player, double range, boolean allowBlock, Predicate<LivingEntity> filter) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDistSq = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : eye.distanceToSqr(blockHit.getLocation());

        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end, box,
                e -> valid(player, e) && filter.test((LivingEntity) e));

        if (entityHit != null && eye.distanceToSqr(entityHit.getLocation()) <= blockDistSq) {
            return new Hit((LivingEntity) entityHit.getEntity(), entityHit.getLocation());
        }
        if (allowBlock && blockHit.getType() == HitResult.Type.BLOCK) {
            return new Hit(null, blockHit.getLocation());
        }
        return null;
    }

    /**
     * Существа в прямоугольнике перед игроком: reach блоков вперёд, halfWidth блоков в каждую сторону.
     * Прямоугольник поворачивается вместе со взглядом игрока по горизонтали.
     */
    public static List<LivingEntity> inFront(ServerPlayer player, double reach, double halfWidth) {
        double yaw = Math.toRadians(player.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);

        double originX = player.getX();
        double originZ = player.getZ();
        double minY = player.getY() - 0.5;
        double maxY = player.getY() + player.getBbHeight() + 0.5;

        double radius = Math.max(reach, halfWidth) + 2.0;
        AABB search = player.getBoundingBox().inflate(radius, 2.0, radius);

        return player.level().getEntitiesOfClass(LivingEntity.class, search, e -> {
            if (!valid(player, e)) {
                return false;
            }
            double rx = e.getX() - originX;
            double rz = e.getZ() - originZ;
            double forward = rx * forwardX + rz * forwardZ;
            double lateral = Math.abs(rx * sideX + rz * sideZ);
            double half = e.getBbWidth() / 2.0;
            if (forward < -half || forward > reach + half) {
                return false;
            }
            if (lateral > halfWidth + half) {
                return false;
            }
            return e.getBoundingBox().maxY >= minY && e.getBoundingBox().minY <= maxY;
        });
    }
}
