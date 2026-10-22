package com.tokyoghoulrpg.client.vr;

import com.mojang.logging.LogUtils;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.VrAimC2S;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * VR через мод Vivecraft (версия 1.3.0 и новее, там появилось API для других модов).
 * Vivecraft не обязателен: без него или в обычном режиме (не в шлеме) всё работает как раньше.
 * API Vivecraft вызываем через отражение, поэтому для сборки мода он не нужен.
 * <p>
 * Пока игрок в VR, каждый тик сообщаем серверу, где главная рука и куда она направлена:
 * удары, захваты кагуне и выстрелы квинке целятся рукой, а не головой.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class VrClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean checked;
    /** Сломалось ли API (другая версия Vivecraft): тогда больше не пробуем. */
    private static boolean broken;
    /** Слали ли серверу положение руки в прошлый тик. */
    private static boolean wasSending;

    private static Object api;
    private static Method isVrActive;
    private static Method preTickWorldPose;
    private static Method poseMainHand;
    private static Method partPos;
    private static Method partDir;

    private VrClient() {
    }

    private static boolean ready() {
        if (!checked) {
            checked = true;
            if (ModList.get().isLoaded("vivecraft")) {
                try {
                    Class<?> apiClass = Class.forName("org.vivecraft.api.client.VRClientAPI");
                    Class<?> poseClass = Class.forName("org.vivecraft.api.data.VRPose");
                    Class<?> partClass = Class.forName("org.vivecraft.api.data.VRBodyPartData");
                    isVrActive = apiClass.getMethod("isVRActive");
                    preTickWorldPose = apiClass.getMethod("getPreTickWorldPose");
                    poseMainHand = poseClass.getMethod("getMainHand");
                    partPos = partClass.getMethod("getPos");
                    partDir = partClass.getMethod("getDir");
                    api = apiClass.getMethod("instance").invoke(null);
                    LOGGER.info("Tokyo Ghoul RPG: найден Vivecraft, в VR удары целятся рукой");
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    api = null;
                    LOGGER.warn("Tokyo Ghoul RPG: Vivecraft есть, но без API (нужна версия 1.3.0 или новее). "
                            + "В VR удары будут целиться головой", e);
                }
            }
        }
        return api != null && !broken;
    }

    private static void fail(Throwable e) {
        if (!broken) {
            broken = true;
            LOGGER.warn("Tokyo Ghoul RPG: API Vivecraft не ответило, прицел рукой выключен", e);
        }
    }

    /** Игрок сейчас играет в шлеме. */
    public static boolean active() {
        if (!ready()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isVrActive.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return false;
        }
    }

    /** Главная рука в мире: {позиция, направление}. null — не в VR или данных пока нет. */
    public static Vec3[] mainHand() {
        if (!active()) {
            return null;
        }
        try {
            Object pose = preTickWorldPose.invoke(api);
            if (pose == null) {
                return null;
            }
            Object hand = poseMainHand.invoke(pose);
            if (hand == null) {
                return null;
            }
            Object pos = partPos.invoke(hand);
            Object dir = partDir.invoke(hand);
            if (!(pos instanceof Vec3 p) || !(dir instanceof Vec3 d)) {
                return null;
            }
            return new Vec3[] {p, d};
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return null;
        }
    }

    /**
     * В начале тика, до нажатий: так сервер узнаёт положение руки раньше, чем придёт удар этого же тика.
     * Не в VR — ничего не шлём (а если только что вышли из VR, говорим серверу целиться снова головой).
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) {
            wasSending = false;
            return;
        }
        Vec3[] hand = mainHand();
        if (hand == null) {
            if (wasSending) {
                wasSending = false;
                NetworkHandler.CHANNEL.sendToServer(VrAimC2S.off());
            }
            return;
        }
        wasSending = true;
        Vec3 offset = hand[0].subtract(player.getEyePosition());
        NetworkHandler.CHANNEL.sendToServer(new VrAimC2S(true, offset, hand[1]));
    }
}
