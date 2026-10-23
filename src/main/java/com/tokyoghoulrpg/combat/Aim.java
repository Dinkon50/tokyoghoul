package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Откуда и куда целится игрок. Обычно это глаза и взгляд.
 * Игрок в VR (Vivecraft) целится рукой: его клиент каждый тик присылает, где обе руки и куда они направлены
 * (см. VrAimC2S). Обычно целится главная (правая) рука; способность, сделанная левой рукой (захват вытянутой
 * левой рукой, удар кагуне взмахом левой), целится левой — см. runWithHand.
 * Данные старше полсекунды не берём: значит, игрок вышел из VR или связь прервалась.
 * Vivecraft на сервере для этого не нужен.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class Aim {
    /** Сколько тиков положение руки считается свежим. */
    private static final int FRESH_TICKS = 10;
    /** Рука не дальше этого от глаз (в блоках): с большим масштабом мира в Vivecraft бывает около 2. */
    private static final double MAX_OFFSET = 3.0;

    private record Hands(Vec3 offset, Vec3 dir, Vec3 offOffset, Vec3 offDir, long time) {
    }

    private static final Map<UUID, Hands> HANDS = new HashMap<>();
    /** Игрок, который прямо сейчас применяет способность левой рукой (на время вызова runWithHand). */
    private static UUID offhandUser;

    private Aim() {
    }

    /** Клиент сообщил положение рук (или что вышел из VR). */
    public static void update(ServerPlayer player, boolean active, Vec3 offset, Vec3 dir, Vec3 offOffset, Vec3 offDir) {
        if (!active) {
            HANDS.remove(player.getUUID());
            return;
        }
        if (!finite(offset) || !finite(dir) || dir.lengthSqr() < 1.0E-6) {
            return;
        }
        if (!finite(offOffset) || !finite(offDir) || offDir.lengthSqr() < 1.0E-6) {
            offOffset = offset;
            offDir = dir;
        }
        HANDS.put(player.getUUID(), new Hands(clamp(offset), dir.normalize(), clamp(offOffset), offDir.normalize(),
                player.level().getGameTime()));
    }

    private static Vec3 clamp(Vec3 offset) {
        return offset.lengthSqr() > MAX_OFFSET * MAX_OFFSET ? offset.normalize().scale(MAX_OFFSET) : offset;
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static Hands hands(ServerPlayer player) {
        Hands hands = HANDS.get(player.getUUID());
        if (hands == null) {
            return null;
        }
        long age = player.level().getGameTime() - hands.time();
        return age >= 0 && age <= FRESH_TICKS ? hands : null;
    }

    /** Способность сейчас делается левой рукой (внутри runWithHand). */
    public static boolean usingOffhand(ServerPlayer player) {
        return offhandNow(player);
    }

    private static boolean offhandNow(ServerPlayer player) {
        return offhandUser != null && offhandUser.equals(player.getUUID());
    }

    /** Выполняет действие так, будто игрок целится левой рукой (если offhand). */
    public static void runWithHand(ServerPlayer player, boolean offhand, Runnable action) {
        if (!offhand) {
            action.run();
            return;
        }
        UUID previous = offhandUser;
        offhandUser = player.getUUID();
        try {
            action.run();
        } finally {
            offhandUser = previous;
        }
    }

    /** Игрок сейчас в VR (по его последнему сообщению). */
    public static boolean inVr(ServerPlayer player) {
        return hands(player) != null;
    }

    /** Откуда целится: рука в VR, иначе глаза. */
    public static Vec3 origin(ServerPlayer player) {
        Hands hands = hands(player);
        if (hands == null) {
            return player.getEyePosition();
        }
        return player.getEyePosition().add(offhandNow(player) ? hands.offOffset() : hands.offset());
    }

    /** Куда целится (единичный вектор): рука в VR, иначе взгляд. */
    public static Vec3 dir(ServerPlayer player) {
        Hands hands = hands(player);
        if (hands == null) {
            return player.getLookAngle();
        }
        return offhandNow(player) ? hands.offDir() : hands.dir();
    }

    /**
     * Поворот прицела по горизонтали в градусах, как у getYRot().
     * Если рука направлена почти вверх или вниз, берём поворот тела.
     */
    public static float yaw(ServerPlayer player) {
        if (hands(player) == null) {
            return player.getYRot();
        }
        Vec3 d = dir(player);
        if (d.x * d.x + d.z * d.z < 0.04) {
            return player.getYRot();
        }
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HANDS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HANDS.clear();
    }
}
