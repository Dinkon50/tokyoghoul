package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.skill.SkillTree;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Обездвиживание: оглушение после пробитого блока, жертвы баррага, захваченная цель.
 * Заблокированное существо не ходит, его не отбрасывает, а игрок не может применять способности.
 */
public final class LockManager {
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e01");
    private static final UUID KNOCKBACK_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e02");
    private static final Map<UUID, Entry> LOCKED = new HashMap<>();

    private record Entry(LivingEntity entity, long until) {
    }

    private LockManager() {
    }

    /**
     * Оглушение. У игрока с веткой «Выдержка» оно короче (до -50%).
     * Для оглушения за сломанный блок и удержания кагуне — {@link #lockFull}.
     */
    public static void lock(LivingEntity entity, long untilTick) {
        if (entity instanceof ServerPlayer player) {
            double resist = SkillEffects.stunResist(SkillEffects.level(PlayerData.get(player), SkillTree.Branch.STUN_RESIST));
            if (resist > 0.0) {
                long now = player.level().getGameTime();
                untilTick = now + Math.round(Math.max(0L, untilTick - now) * (1.0 - resist));
            }
        }
        lockFull(entity, untilTick);
    }

    /** Обездвижить на полный срок: «Выдержка» не действует. */
    public static void lockFull(LivingEntity entity, long untilTick) {
        // Боссов нельзя обездвижить
        if (entity instanceof BossMob) {
            return;
        }
        Entry old = LOCKED.get(entity.getUUID());
        long end = old == null ? untilTick : Math.max(old.until(), untilTick);
        LOCKED.put(entity.getUUID(), new Entry(entity, end));

        AttrUtil.set(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, "tg_lock_speed", -1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        AttrUtil.set(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "tg_lock_knockback", 1.0,
                AttributeModifier.Operation.ADDITION);

        if (entity instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.getLockEndTick() != end) {
                data.setLockEndTick(end);
                NetworkHandler.syncTo(player);
            }
        }
    }

    public static void unlock(LivingEntity entity) {
        boolean was = LOCKED.remove(entity.getUUID()) != null;
        AttrUtil.remove(entity, Attributes.MOVEMENT_SPEED, SPEED_ID);
        AttrUtil.remove(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);

        if (was && entity instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.getLockEndTick() != 0L) {
                data.setLockEndTick(0L);
                NetworkHandler.syncTo(player);
            }
        }
    }

    public static boolean isLocked(LivingEntity entity) {
        return LOCKED.containsKey(entity.getUUID());
    }

    /** Раз в тик сервера: снимает истёкшие блокировки. */
    public static void tick() {
        if (LOCKED.isEmpty()) {
            return;
        }
        for (Entry entry : new ArrayList<>(LOCKED.values())) {
            LivingEntity entity = entry.entity();
            if (entity.isRemoved()) {
                LOCKED.remove(entity.getUUID());
            } else if (!entity.isAlive() || entity.level().getGameTime() >= entry.until()) {
                unlock(entity);
            }
        }
    }

    public static void clear() {
        LOCKED.clear();
    }
}
