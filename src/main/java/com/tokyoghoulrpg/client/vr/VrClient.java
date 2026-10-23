package com.tokyoghoulrpg.client.vr;

import com.mojang.logging.LogUtils;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.VrAimC2S;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Связь с модом Vivecraft (версия 1.3.0 и новее, там появилось API для других модов).
 * Vivecraft не обязателен: без него или в обычном режиме (не в шлеме) всё работает как раньше.
 * API Vivecraft вызываем через отражение, поэтому для сборки мода он не нужен.
 * <ul>
 *   <li>положение головы и рук (в комнате — для жестов, в мире — для прицела);</li>
 *   <li>вибрация контроллера;</li>
 *   <li>зоны на теле (VrControls): Vivecraft сам даёт лёгкую вибрацию, когда рука в зоне,
 *       и отдаёт нам нажатие курка или хвата этой руки;</li>
 *   <li>каждый тик сообщаем серверу, где руки: удары и захваты целятся рукой, а не головой.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class VrClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Голова и руки: позиции и направления. */
    public record Pose(Vec3 head, Vec3 headDir, Vec3 main, Vec3 mainDir, Vec3 off, Vec3 offDir) {
    }

    private static boolean checked;
    /** Сломалось ли API (другая версия Vivecraft): тогда больше не пробуем. */
    private static boolean broken;
    /** Слали ли серверу положение рук в прошлый тик. */
    private static boolean wasSending;

    private static Object api;
    private static Method isVrActive;
    private static Method preTickWorldPose;
    private static Method latestRoomPose;
    private static Method poseHead;
    private static Method poseMainHand;
    private static Method poseOffHand;
    private static Method partPos;
    private static Method partDir;
    private static Method haptic;
    private static Method hapticFull;
    private static Object mainHandPart;
    private static Object offHandPart;

    private VrClient() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean ready() {
        if (!checked) {
            checked = true;
            if (ModList.get().isLoaded("vivecraft")) {
                try {
                    Class<?> apiClass = Class.forName("org.vivecraft.api.client.VRClientAPI");
                    Class<?> poseClass = Class.forName("org.vivecraft.api.data.VRPose");
                    Class<?> partClass = Class.forName("org.vivecraft.api.data.VRBodyPartData");
                    Class<?> bodyPart = Class.forName("org.vivecraft.api.data.VRBodyPart");
                    isVrActive = apiClass.getMethod("isVRActive");
                    preTickWorldPose = apiClass.getMethod("getPreTickWorldPose");
                    latestRoomPose = apiClass.getMethod("getLatestRoomPose");
                    poseHead = poseClass.getMethod("getHead");
                    poseMainHand = poseClass.getMethod("getMainHand");
                    poseOffHand = poseClass.getMethod("getOffHand");
                    partPos = partClass.getMethod("getPos");
                    partDir = partClass.getMethod("getDir");
                    haptic = apiClass.getMethod("triggerHapticPulse", bodyPart, float.class);
                    hapticFull = apiClass.getMethod("triggerHapticPulse", bodyPart, float.class, float.class,
                            float.class, float.class);
                    mainHandPart = Enum.valueOf((Class) bodyPart, "MAIN_HAND");
                    offHandPart = Enum.valueOf((Class) bodyPart, "OFF_HAND");
                    api = apiClass.getMethod("instance").invoke(null);
                    LOGGER.info("Tokyo Ghoul RPG: найден Vivecraft, включено VR-управление");
                } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                    api = null;
                    LOGGER.warn("Tokyo Ghoul RPG: Vivecraft есть, но без API (нужна версия 1.3.0 или новее). "
                            + "VR-управление мода выключено", e);
                }
            }
        }
        return api != null && !broken;
    }

    private static void fail(Throwable e) {
        if (!broken) {
            broken = true;
            LOGGER.warn("Tokyo Ghoul RPG: API Vivecraft не ответило, VR-управление мода выключено", e);
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

    private static Pose pose(Method source) {
        if (!active()) {
            return null;
        }
        try {
            Object pose = source.invoke(api);
            if (pose == null) {
                return null;
            }
            Object head = poseHead.invoke(pose);
            Object main = poseMainHand.invoke(pose);
            Object off = poseOffHand.invoke(pose);
            if (head == null || main == null || off == null) {
                return null;
            }
            return new Pose((Vec3) partPos.invoke(head), (Vec3) partDir.invoke(head),
                    (Vec3) partPos.invoke(main), (Vec3) partDir.invoke(main),
                    (Vec3) partPos.invoke(off), (Vec3) partDir.invoke(off));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
            return null;
        }
    }

    /** Голова и руки в мире (координаты Minecraft) перед тиком. null — не в VR. */
    public static Pose worldPose() {
        return ready() ? pose(preTickWorldPose) : null;
    }

    /** Голова и руки в комнате (метры, как в жизни): для жестов. null — не в VR. */
    public static Pose roomPose() {
        return ready() ? pose(latestRoomPose) : null;
    }

    /** Главная рука в мире: {позиция, направление}. null — не в VR или данных пока нет. */
    public static Vec3[] mainHand() {
        Pose pose = worldPose();
        return pose == null ? null : new Vec3[] {pose.main(), pose.mainDir()};
    }

    /** Вибрация контроллера (seconds — длительность). Тихо ничего не делает, если нельзя. */
    public static void vibrate(boolean offhand, float seconds) {
        if (!active()) {
            return;
        }
        try {
            haptic.invoke(api, offhand ? offHandPart : mainHandPart, seconds);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // вибрация не главное: не выключаем из-за неё остальное
        }
    }

    /** Вибрация по событию с сервера (VrHapticS2C). */
    public static void feedback(int kind, boolean offhand, float strength) {
        switch (kind) {
            case com.tokyoghoulrpg.network.packet.VrHapticS2C.HIT -> vibrate(offhand, 0.05F, strength);
            case com.tokyoghoulrpg.network.packet.VrHapticS2C.HURT -> {
                vibrate(false, 0.12F, strength);
                vibrate(true, 0.12F, strength);
            }
            case com.tokyoghoulrpg.network.packet.VrHapticS2C.BLOCKED -> {
                vibrate(false, 0.05F, strength);
                vibrate(true, 0.05F, strength);
            }
            case com.tokyoghoulrpg.network.packet.VrHapticS2C.PARRY, com.tokyoghoulrpg.network.packet.VrHapticS2C.BLOCK_BROKEN -> {
                vibrate(false, 0.25F, strength);
                vibrate(true, 0.25F, strength);
            }
            default -> {
            }
        }
    }

    /** Вибрация с силой amplitude (0..1). */
    public static void vibrate(boolean offhand, float seconds, float amplitude) {
        if (!active()) {
            return;
        }
        try {
            hapticFull.invoke(api, offhand ? offHandPart : mainHandPart, seconds, 160.0F,
                    Math.max(0.0F, Math.min(1.0F, amplitude)), 0.0F);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // вибрация не главное
        }
    }

    /**
     * Регистрирует зоны на теле в Vivecraft. Нужно вызвать до запуска игрового цикла (из конструктора мода):
     * Vivecraft принимает такие регистрации только при первой загрузке ресурсов.
     */
    public static void registerEarly() {
        if (!ModList.get().isLoaded("vivecraft")) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("org.vivecraft.api.client.VRClientAPI");
            Class<?> moduleClass = Class.forName("org.vivecraft.api.client.InteractModule");
            Class<?> eventClass = Class.forName("org.vivecraft.api.client.event.VivecraftClientRegistrationEvent");
            ResourceLocation id = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "body_zones");
            Object module = Proxy.newProxyInstance(moduleClass.getClassLoader(), new Class<?>[] {moduleClass},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isActive" -> VrControls.zoneActive((LocalPlayer) args[0], (InteractionHand) args[1]);
                        case "onPress" -> VrControls.zonePressed((LocalPlayer) args[0], (InteractionHand) args[1]);
                        case "reset" -> null;
                        case "getId" -> id;
                        // раньше модулей Vivecraft (у них 0..1500): зоны у самого тела, с миром не пересекаются
                        case "getPriority" -> -100;
                        case "swingsArm" -> false;
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> id.toString();
                        default -> null;
                    });
            Object modules = Array.newInstance(moduleClass, 1);
            Array.set(modules, 0, module);
            Method register = eventClass.getMethod("registerInteractModules", modules.getClass());
            Consumer<Object> handler = event -> {
                try {
                    register.invoke(event, modules);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOGGER.warn("Tokyo Ghoul RPG: не удалось добавить зоны на теле в Vivecraft", e);
                }
            };
            Object instance = apiClass.getMethod("instance").invoke(null);
            apiClass.getMethod("addClientRegistrationHandler", Consumer.class).invoke(instance, handler);
            LOGGER.info("Tokyo Ghoul RPG: зоны на теле для VR добавлены в Vivecraft");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOGGER.warn("Tokyo Ghoul RPG: зоны на теле для VR не добавлены (нужен Vivecraft 1.3.0 или новее). "
                    + "Способности в VR — через меню и жесты", e);
        }
    }

    /**
     * В начале тика, до нажатий: так сервер узнаёт положение рук раньше, чем придёт удар этого же тика.
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
        Pose pose = worldPose();
        if (pose == null) {
            if (wasSending) {
                wasSending = false;
                NetworkHandler.CHANNEL.sendToServer(VrAimC2S.off());
            }
            return;
        }
        wasSending = true;
        Vec3 eye = player.getEyePosition();
        NetworkHandler.CHANNEL.sendToServer(new VrAimC2S(true, pose.main().subtract(eye), pose.mainDir(),
                pose.off().subtract(eye), pose.offDir()));
    }
}
