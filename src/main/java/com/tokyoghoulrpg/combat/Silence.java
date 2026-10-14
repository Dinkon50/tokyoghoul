package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * «Тишина» после «Ауры возмездия»: какое-то время нельзя пользоваться способностями, только обычными ударами.
 * У игроков это блок, рывок, укус, барраж, захваты, ярость, способности квинке и сами ауры, у боссов — особые атаки.
 * Только на сервере, не сохраняется. Игроку время тишины приходит вместе с его данными (для подсказки и двойного прыжка).
 */
public final class Silence {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private Silence() {
    }

    public static void apply(LivingEntity target, int ticks) {
        long until = target.level().getGameTime() + ticks;
        UNTIL.merge(target.getUUID(), until, Math::max);
        if (target instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                data.setSilenceEndTick(Math.max(data.getSilenceEndTick(), until));
                // блок и барраж обрываются сразу
                AbilityManager.cancelAll(player);
                NetworkHandler.syncTo(player);
            }
        }
    }

    public static boolean isSilenced(LivingEntity entity) {
        Long until = UNTIL.get(entity.getUUID());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() >= until) {
            UNTIL.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    /** Сколько тиков тишины осталось (0 — нет). */
    public static long ticksLeft(LivingEntity entity) {
        Long until = UNTIL.get(entity.getUUID());
        return until == null ? 0L : Math.max(0L, until - entity.level().getGameTime());
    }

    public static void remove(LivingEntity entity) {
        UNTIL.remove(entity.getUUID());
    }

    public static void clear() {
        UNTIL.clear();
    }
}
