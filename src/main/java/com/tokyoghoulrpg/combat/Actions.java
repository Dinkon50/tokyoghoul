package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Способности, которые длятся несколько тиков. */
public final class Actions {
    private Actions() {
    }

    /** Резкий рывок вперёд: несколько тиков фиксированной скорости. */
    public static final class Dash implements ActiveAction {
        private final Vec3 direction;
        private int ticksLeft = Balance.DASH_TICKS;

        public Dash(Vec3 direction) {
            this.direction = direction;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(direction.x * Balance.DASH_SPEED, motion.y, direction.z * Balance.DASH_SPEED);
            player.hurtMarked = true;
            ticksLeft--;
            return ticksLeft <= 0;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    /** Обычная или сильная атака кагуне: удары приходятся на моменты из анимации. */
    public static final class Attack implements ActiveAction {
        private final AttackType type;
        private final long start;
        private final int[] hitTicks;
        private final float damage;
        private final double reach;
        private final double halfWidth;
        private int next = 0;

        public Attack(AttackType type, long start, int[] hitTicks, float damage, double reach, double halfWidth) {
            this.type = type;
            this.start = start;
            this.hitTicks = hitTicks;
            this.damage = damage;
            this.reach = reach;
            this.halfWidth = halfWidth;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            long elapsed = now - start;
            while (next < hitTicks.length && elapsed >= hitTicks[next]) {
                strike(player);
                next++;
            }
            return next >= hitTicks.length;
        }

        private void strike(ServerPlayer player) {
            for (LivingEntity target : Targeting.inFront(player, reach, halfWidth)) {
                CombatContext.hit(player, target, damage, type);
            }
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    /** Барраж: серия ударов в моменты из анимации. Урон наносится только за реальные попадания. */
    public static final class Barrage implements ActiveAction {
        private final long start;
        private final int[] hitTicks;
        private final int duration;
        private int next = 0;
        private final Set<LivingEntity> victims = Collections.newSetFromMap(new IdentityHashMap<>());

        public Barrage(long start, int[] hitTicks, int duration) {
            this.start = start;
            this.hitTicks = hitTicks;
            this.duration = duration;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            // Оглушили или убили: серия прерывается, жертвы освобождаются
            if (!player.isAlive() || LockManager.isLocked(player)) {
                release();
                NetworkHandler.playAnimation(player, "");
                return true;
            }
            long elapsed = now - start;
            while (next < hitTicks.length && elapsed >= hitTicks[next]) {
                strike(player);
                next++;
            }
            if (elapsed >= duration) {
                release();
                return true;
            }
            return false;
        }

        private void strike(ServerPlayer player) {
            long end = start + duration;
            for (LivingEntity target : Targeting.inFront(player, Balance.BARRAGE_LENGTH, Balance.BARRAGE_WIDTH / 2.0)) {
                boolean hurt = CombatContext.hit(player, target, Balance.BARRAGE_DAMAGE_PER_HIT, AttackType.BARRAGE);
                // Попавший под барраж не может двигаться до конца серии.
                // Если он сам в баррагe (встречный барраж), его не обездвиживаем.
                if (hurt && !AbilityManager.isBarraging(target)) {
                    LockManager.lock(target, end);
                    victims.add(target);
                }
            }
        }

        private void release() {
            for (LivingEntity victim : victims) {
                LockManager.unlock(victim);
            }
            victims.clear();
        }

        @Override
        public void cancel(ServerPlayer player) {
            release();
        }
    }

    /** Shift + ПКМ: кагуне цепляется за цель, и игрока подтягивает к ней в окне из анимации. */
    public static final class GrappleSelf implements ActiveAction {
        private final long start;
        private final LivingEntity entity;
        private final Vec3 point;
        private final int pullStart;
        private final int pullEnd;
        private final int total;

        /** entity равно null, если цель блок: тогда игрока тянет к точке point. */
        public GrappleSelf(long start, LivingEntity entity, Vec3 point, int pullStart, int pullEnd, int total) {
            this.start = start;
            this.entity = entity;
            this.point = point;
            this.pullStart = pullStart;
            this.pullEnd = pullEnd;
            this.total = total;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            long elapsed = now - start;
            if (entity != null && (entity.isRemoved() || !entity.isAlive())) {
                return true;
            }
            if (elapsed >= pullStart && elapsed < pullEnd) {
                Vec3 goal = entity != null ? entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0) : point;
                Vec3 from = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
                Vec3 delta = goal.subtract(from);
                double distance = delta.length();
                double stop = entity != null ? Balance.GRAPPLE_STOP_ENTITY : Balance.GRAPPLE_STOP_BLOCK;
                if (distance > stop) {
                    long remaining = Math.max(1L, pullEnd - elapsed);
                    double speed = Math.min(Balance.GRAPPLE_MAX_SPEED, (distance - stop) / remaining);
                    player.setDeltaMovement(delta.scale(speed / distance));
                    player.hurtMarked = true;
                    player.fallDistance = 0.0F;
                }
            } else if (elapsed == pullEnd) {
                player.setDeltaMovement(player.getDeltaMovement().scale(0.2));
                player.hurtMarked = true;
            }
            return elapsed >= total;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    /** Shift + ЛКМ: кагуне хватает цель и подтягивает её к игроку. На время захвата цель обездвижена. */
    public static final class PullTarget implements ActiveAction {
        private final long start;
        private final LivingEntity target;
        private final int latch;
        private final int pullStart;
        private final int pullEnd;
        private final int release;
        private final int total;
        private boolean latched = false;

        public PullTarget(long start, LivingEntity target, AbilitySync.Timing timing) {
            this.start = start;
            this.target = target;
            this.latch = timing.latch();
            this.pullStart = timing.pullStart();
            this.pullEnd = timing.pullEnd();
            this.release = timing.release();
            this.total = timing.duration();
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            long elapsed = now - start;
            if (target.isRemoved() || !target.isAlive()) {
                releaseTarget();
                return true;
            }
            if (!latched && elapsed >= latch && elapsed < release) {
                latched = true;
                LockManager.lock(target, start + release);
            }
            if (latched && elapsed >= pullStart && elapsed < pullEnd) {
                Vec3 delta = player.position().subtract(target.position());
                double distance = delta.length();
                if (distance > Balance.PULL_STOP) {
                    long remaining = Math.max(1L, pullEnd - elapsed);
                    double speed = Math.min(Balance.PULL_MAX_SPEED, (distance - Balance.PULL_STOP) / remaining);
                    target.setDeltaMovement(delta.scale(speed / distance));
                    target.hurtMarked = true;
                    target.fallDistance = 0.0F;
                }
            }
            if (latched && elapsed >= release) {
                releaseTarget();
            }
            return elapsed >= total;
        }

        private void releaseTarget() {
            if (latched) {
                LockManager.unlock(target);
                latched = false;
            }
        }

        @Override
        public void cancel(ServerPlayer player) {
            releaseTarget();
        }
    }
}
