package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * «Подавитель RC» агента CCG: после его удара гуль какое-то время не лечится
 * (ни регенерация, ни укус, ни навыки). Только на сервере, не сохраняется.
 */
public final class RcSuppress {
    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private RcSuppress() {
    }

    /** Гуль: NPC-гуль, Пожиратель, игрок-гуль или полугуль. */
    public static boolean isGhoul(LivingEntity entity) {
        if (entity instanceof GhoulNpc || entity instanceof GhoulBoss
                || entity instanceof com.tokyoghoulrpg.entity.HalfGhoulNpc || entity instanceof com.tokyoghoulrpg.entity.HalfGhoulBoss
                || entity instanceof com.tokyoghoulrpg.entity.TestSubjectNpc) {
            return true;
        }
        if (entity instanceof Player player) {
            PlayerData data = PlayerData.get(player);
            return data != null && data.getPlayerClass().hasKagune();
        }
        return false;
    }

    public static void apply(LivingEntity target, int ticks) {
        long until = target.level().getGameTime() + ticks;
        UNTIL.merge(target.getUUID(), until, Math::max);
        Effects.particles(target, Effects.RC, 8, 0.3, 0.02);
    }

    public static boolean isSuppressed(LivingEntity entity) {
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

    public static void clear() {
        UNTIL.clear();
    }
}
