package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ульта гуля и полугуля (H J K L быстро подряд, в VR — обе руки за спину и рывком вперёд). Длится 10 секунд,
 * гуль и цель всё это время не двигаются; у обоих камера плавно уходит на третье лицо и потом возвращается
 * (UltimateCinematic на клиенте). Тайминги — Balance.ULTIMATE_*:
 * <ol>
 *   <li>0–1,5 с: кагуне раскрывается за спиной;</li>
 *   <li>два верхних щупальца выстреливают, хватают цель за плечи и медленно поднимают её;</li>
 *   <li>два нижних по очереди бьют в живот — 7 ударов, всё чаще, — и добивающий: оба насквозь;</li>
 *   <li>щупальца вырывают, цель падает (без урона от падения), кагуне втягивается.</li>
 * </ol>
 * Урон за всю ульту — половина здоровья полугуля (24, 12 сердец): 7 ударов по 2 и добивающий 10.
 * Человеку и агенту CCG — 75% (18). Сквозь броню, блок и множители урона.
 * Если цель спас тотем, «Бессмертие» или «Второе дыхание», ульта на этом заканчивается.
 * Клиенты рисуют щупальца и кровь сами по тем же таймингам (KaguneUltimateFx).
 * Своих (клан, союзники), питомцев и стражей своего клана ульта не берёт. Боссов не поднимает и не обездвиживает,
 * но удары проходят. Откат 10 минут, он сохраняется при перезаходе.
 */
public final class GhoulUltimate {
    private GhoulUltimate() {
    }

    public static void use(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < data.getUltimateReady()) {
            long left = (data.getUltimateReady() - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ultimate_cooldown",
                    left / 60, left % 60), true);
            return;
        }
        for (ActiveAction action : state.actions) {
            if (action instanceof Action) {
                return;
            }
        }
        LivingEntity target = findTarget(player);
        if (target == null) {
            // секретная способность: при случайном наборе без цели ничего не пишем
            return;
        }
        data.setUltimateReady(now + Balance.ULTIMATE_COOLDOWN);
        // рывок (H из комбо) останавливается: гуль встаёт перед целью
        state.actions.removeIf(action -> action instanceof Actions.Dash);
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        player.hurtMarked = true;
        state.actions.add(new Action(player, target, now));
        NetworkHandler.kaguneUltimate(player, target.getId());
        NetworkHandler.syncTo(player);
        Effects.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.6F, 1.5F);
        Effects.sound(player, SoundEvents.SLIME_ATTACK, 1.0F, 0.6F);
    }

    /** Цель: сначала по прицелу, иначе ближайшая перед собой (не дальше 2 блоков между телами). */
    private static LivingEntity findTarget(ServerPlayer player) {
        Targeting.Hit hit = Targeting.raycast(player, Balance.ULTIMATE_RANGE + 1.5, false, e -> eligible(player, e));
        if (hit != null && hit.entity() != null && gap(player, hit.entity()) <= Balance.ULTIMATE_RANGE) {
            return hit.entity();
        }
        Vec3 look = horizontal(Aim.dir(player));
        LivingEntity best = null;
        double bestGap = Double.MAX_VALUE;
        AABB search = player.getBoundingBox().inflate(Balance.ULTIMATE_RANGE + 1.5, 1.5, Balance.ULTIMATE_RANGE + 1.5);
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, search,
                c -> c != player && eligible(player, c))) {
            double g = gap(player, e);
            if (g > Balance.ULTIMATE_RANGE) {
                continue;
            }
            Vec3 to = horizontal(e.position().subtract(player.position()));
            // перед собой: в пределах ~60° от направления прицела (вплотную — в любую сторону)
            if (g > 0.3 && to.dot(look) < 0.5) {
                continue;
            }
            if (g < bestGap) {
                best = e;
                bestGap = g;
            }
        }
        return best;
    }

    private static boolean eligible(ServerPlayer player, LivingEntity e) {
        return Targeting.valid(player, e) && !Combos.spared(player, e) && player.hasLineOfSight(e) && !held(e);
    }

    /** Цель уже держит чья-то ульта. */
    private static boolean held(LivingEntity e) {
        return Action.HELD.contains(e.getUUID());
    }

    /** Расстояние между телами (хитбоксами), в блоках. */
    private static double gap(Entity a, Entity b) {
        AABB x = a.getBoundingBox();
        AABB y = b.getBoundingBox();
        double dx = Math.max(0.0, Math.max(x.minX - y.maxX, y.minX - x.maxX));
        double dy = Math.max(0.0, Math.max(x.minY - y.maxY, y.minY - x.maxY));
        double dz = Math.max(0.0, Math.max(x.minZ - y.maxZ, y.minZ - x.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 horizontal(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0.0, v.z);
        return h.lengthSqr() < 1.0E-6 ? Vec3.ZERO : h.normalize();
    }

    /** Человек и агент CCG (игроки) получают от ульты меньше: Balance.ULTIMATE_HUMAN_FACTOR. */
    private static float humanFactor(LivingEntity target) {
        if (target instanceof ServerPlayer victim) {
            PlayerData data = PlayerData.get(victim);
            if (data != null && (data.getPlayerClass() == com.tokyoghoulrpg.data.PlayerClass.HUMAN
                    || data.getPlayerClass() == com.tokyoghoulrpg.data.PlayerClass.CCG_AGENT)) {
                return (float) Balance.ULTIMATE_HUMAN_FACTOR;
            }
        }
        return 1.0F;
    }

    private static String num(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value).replace('.', ',');
    }

    /** Сама ульта: держит обоих, поднимает цель, бьёт по таймингам из Balance. */
    static final class Action implements ActiveAction {
        static final java.util.Set<java.util.UUID> HELD = new java.util.HashSet<>();

        private final LivingEntity target;
        private final long start;
        private final Vec3 targetStart;
        private final Vec3 hold;
        private final boolean boss;
        /** Урон одного обычного удара; добивающий — в ULTIMATE_FINAL_WEIGHT раз больше. */
        private final float unit;
        private boolean shot;
        private boolean latched;
        private boolean finalDone;
        private boolean released;
        private int stab;
        private boolean finished;

        Action(ServerPlayer player, LivingEntity target, long start) {
            this.target = target;
            this.start = start;
            this.targetStart = target.position();
            this.boss = target instanceof BossMob;
            Vec3 dir = horizontal(target.position().subtract(player.position()));
            if (dir == Vec3.ZERO) {
                dir = horizontal(player.getLookAngle());
            }
            this.hold = player.position().add(dir.scale(Balance.ULTIMATE_HOLD)).add(0.0, Balance.ULTIMATE_LIFT, 0.0);
            int parts = Balance.ULTIMATE_STABS.length + Balance.ULTIMATE_FINAL_WEIGHT;
            this.unit = Balance.ULTIMATE_DAMAGE * humanFactor(target) / parts;
            long until = start + Balance.ULTIMATE_DURATION;
            // кагуне держит обоих — это не оглушение, «Выдержка» не сокращает
            LockManager.lockFull(player, until);
            LockManager.lockFull(target, until);
            HELD.add(target.getUUID());
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            long t = now - start;
            if (!player.isAlive() || player.isRemoved() || !target.isAlive() || target.isRemoved()
                    || target.level() != player.level() || target.distanceToSqr(player) > 64.0) {
                finish(player, true);
                return true;
            }
            // кагуне раскрылось — верхние щупальца выстреливают
            if (!shot && t >= Balance.ULTIMATE_SHOOT) {
                shot = true;
                Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
                Effects.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.5F, 1.8F);
            }
            // схватили за плечи
            if (!latched && t >= Balance.ULTIMATE_LATCH) {
                latched = true;
                Effects.sound(target, SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
                Effects.sound(target, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
                VrFeedback.parry(player);
            }
            // сердце бьётся, пока цель поднимают
            if (latched && (t == Balance.ULTIMATE_LATCH + 10 || t == Balance.ULTIMATE_LATCH + 24)) {
                Effects.sound(target, SoundEvents.WARDEN_HEARTBEAT, 1.2F, 1.0F);
            }
            // подъём и удержание перед гулем
            if (latched && !released && !boss) {
                double k = t >= Balance.ULTIMATE_LIFT_END ? 1.0
                        : (t - Balance.ULTIMATE_LATCH) / (double) (Balance.ULTIMATE_LIFT_END - Balance.ULTIMATE_LATCH);
                k = k * k * (3.0 - 2.0 * k);
                Vec3 want = targetStart.lerp(hold, k);
                target.setDeltaMovement(want.subtract(target.position()).scale(0.6));
                target.hurtMarked = true;
            }
            target.fallDistance = 0.0F;
            // нижние щупальца бьют в живот
            int[] stabs = Balance.ULTIMATE_STABS;
            if (stab < stabs.length && t >= stabs[stab]) {
                stab++;
                if (strike(player, unit)) {
                    Effects.sound(target, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
                    Effects.sound(target, SoundEvents.SLIME_BLOCK_BREAK, 1.0F, 0.6F);
                    Effects.sound(target, SoundEvents.TRIDENT_HIT, 0.6F, 0.6F);
                }
                if (finished) {
                    return true;
                }
            }
            // добивающий: оба нижних насквозь
            if (!finalDone && t >= Balance.ULTIMATE_FINAL) {
                finalDone = true;
                if (strike(player, unit * Balance.ULTIMATE_FINAL_WEIGHT)) {
                    Effects.sound(target, SoundEvents.PLAYER_ATTACK_CRIT, 1.2F, 0.5F);
                    Effects.sound(target, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.7F, 0.6F);
                    Effects.sound(target, SoundEvents.SLIME_SQUISH, 1.2F, 0.5F);
                    VrFeedback.parry(player);
                }
                if (finished) {
                    return true;
                }
            }
            // вырвали щупальца и отпустили: цель падает, но до конца ульты не двигается
            if (!released && t >= Balance.ULTIMATE_RELEASE) {
                released = true;
                Effects.sound(target, SoundEvents.SLIME_BLOCK_BREAK, 1.0F, 0.5F);
                Effects.sound(target, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F, 0.8F);
                if (!boss) {
                    target.setDeltaMovement(0.0, -0.1, 0.0);
                    target.hurtMarked = true;
                }
            }
            if (t >= Balance.ULTIMATE_DURATION) {
                finish(player, false);
                return true;
            }
            return false;
        }

        /**
         * Один удар. Если цель должна была умереть, а её спасли (тотем, «Бессмертие», «Второе дыхание»), — ульта
         * на этом кончается, чтобы спасение не пропало зря. Убил — тоже конец.
         */
        private boolean strike(ServerPlayer player, float damage) {
            float before = target.getHealth() + target.getAbsorptionAmount();
            boolean hurt = CombatContext.hitUltimate(player, target, damage);
            if (!target.isAlive() || before <= damage) {
                finish(player, true);
            }
            return hurt;
        }

        private void finish(ServerPlayer player, boolean early) {
            if (finished) {
                return;
            }
            finished = true;
            HELD.remove(target.getUUID());
            LockManager.unlock(player);
            LockManager.unlock(target);
            target.fallDistance = 0.0F;
            if (!boss && target.isAlive() && !released) {
                target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.0, 0.2));
                target.hurtMarked = true;
            }
            if (early) {
                // у всех: щупальца вырываются и втягиваются, камера возвращается
                NetworkHandler.kaguneUltimate(player, -1);
            }
        }

        @Override
        public void cancel(ServerPlayer player) {
            finish(player, true);
        }
    }
}
