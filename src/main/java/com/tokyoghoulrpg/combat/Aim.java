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
 * Игрок в VR (Vivecraft) целится главной рукой: его клиент каждый тик присылает, где она и куда направлена
 * (см. VrAimC2S). Данные старше полсекунды не берём: значит, игрок вышел из VR или связь прервалась.
 * Vivecraft на сервере для этого не нужен.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class Aim {
    /** Сколько тиков положение руки считается свежим. */
    private static final int FRESH_TICKS = 10;
    /** Рука не дальше этого от глаз (в блоках): с большим масштабом мира в Vivecraft бывает около 2. */
    private static final double MAX_OFFSET = 3.0;

    private record Hand(Vec3 offset, Vec3 dir, long time) {
    }

    private static final Map<UUID, Hand> HANDS = new HashMap<>();

    private Aim() {
    }

    /** Клиент сообщил положение руки (или что вышел из VR). */
    public static void update(ServerPlayer player, boolean active, Vec3 offset, Vec3 dir) {
        if (!active) {
            HANDS.remove(player.getUUID());
            return;
        }
        if (!finite(offset) || !finite(dir) || dir.lengthSqr() < 1.0E-6) {
            return;
        }
        if (offset.lengthSqr() > MAX_OFFSET * MAX_OFFSET) {
            offset = offset.normalize().scale(MAX_OFFSET);
        }
        HANDS.put(player.getUUID(), new Hand(offset, dir.normalize(), player.level().getGameTime()));
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static Hand hand(ServerPlayer player) {
        Hand hand = HANDS.get(player.getUUID());
        if (hand == null) {
            return null;
        }
        long age = player.level().getGameTime() - hand.time();
        return age >= 0 && age <= FRESH_TICKS ? hand : null;
    }

    /** Игрок сейчас в VR (по его последнему сообщению). */
    public static boolean inVr(ServerPlayer player) {
        return hand(player) != null;
    }

    /** Откуда целится: рука в VR, иначе глаза. */
    public static Vec3 origin(ServerPlayer player) {
        Hand hand = hand(player);
        return hand == null ? player.getEyePosition() : player.getEyePosition().add(hand.offset());
    }

    /** Куда целится (единичный вектор): рука в VR, иначе взгляд. */
    public static Vec3 dir(ServerPlayer player) {
        Hand hand = hand(player);
        return hand == null ? player.getLookAngle() : hand.dir();
    }

    /**
     * Поворот прицела по горизонтали в градусах, как у getYRot().
     * Если рука направлена почти вверх или вниз, берём поворот тела.
     */
    public static float yaw(ServerPlayer player) {
        Hand hand = hand(player);
        if (hand == null) {
            return player.getYRot();
        }
        double x = hand.dir().x;
        double z = hand.dir().z;
        if (x * x + z * z < 0.04) {
            return player.getYRot();
        }
        return (float) Math.toDegrees(Math.atan2(-x, z));
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
