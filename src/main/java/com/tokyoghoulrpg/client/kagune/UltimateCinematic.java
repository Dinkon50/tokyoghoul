package com.tokyoghoulrpg.client.kagune;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.hud.BiteVignette;
import com.tokyoghoulrpg.client.vr.VrClient;
import com.tokyoghoulrpg.combat.Balance;
import java.lang.reflect.Method;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Камера во время ульты — у гуля, который её сделал, и у того, кого схватили.
 * <ul>
 *   <li>камера плавно уходит из глаз на третье лицо и облетает обоих сбоку (гуль — со своей стороны, цель — со своей);</li>
 *   <li>когда бьют в живот — камера ближе, на каждом ударе встряска и лёгкое приближение;</li>
 *   <li>сверху и снизу чёрные полосы, как в кино, остальной интерфейс скрыт (чат остаётся);</li>
 *   <li>у цели на каждом ударе краснеют края экрана;</li>
 *   <li>после ульты камера плавно возвращается в глаза, и вид снова тот, что был (обычно первое лицо).</li>
 * </ul>
 * В VR камеру не трогаем: там это укачивает. Остальные игроки рядом камеру не меняют, только видят ульту.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class UltimateCinematic {
    private static final ResourceLocation BITE_VIGNETTE = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "bite_vignette");

    private static boolean active;
    private static boolean victim;
    private static int ownerId;
    private static int targetId;
    private static long start;
    /** Когда камера начинает возвращаться (тик от начала): обычно ULTIMATE_RELEASE, раньше — если ульту прервали. */
    private static float release = Balance.ULTIMATE_RELEASE;
    private static CameraType saved;
    /** Насколько сейчас камера «в кино» (0 — в глазах, 1 — полностью): для полос и интерфейса. */
    private static float weight;
    private static int doneTick = -1;

    private static boolean searched;
    private static Method setPosition;

    private UltimateCinematic() {
    }

    /** Ульта началась: если мы гуль или цель — включаем кино. */
    static void start(int owner, int target, long now) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer me = mc.player;
        if (me == null || VrClient.active()) {
            return;
        }
        boolean caster = me.getId() == owner;
        boolean isVictim = me.getId() == target;
        if (!caster && !isVictim) {
            return;
        }
        if (!active) {
            saved = mc.options.getCameraType();
        }
        active = true;
        victim = isVictim && !caster;
        ownerId = owner;
        targetId = target;
        start = now;
        release = Balance.ULTIMATE_RELEASE;
        doneTick = -1;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    /** Ульту прервали: камера возвращается с этого тика. */
    static void stop(int owner, long releaseTick) {
        if (active && owner == ownerId) {
            release = Math.min(release, releaseTick - start);
        }
    }

    private static void end() {
        Minecraft mc = Minecraft.getInstance();
        active = false;
        weight = 0.0F;
        mc.options.setCameraType(saved != null ? saved : CameraType.FIRST_PERSON);
        saved = null;
    }

    // ------------------------------------------------------------------ каждый тик

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            active = false;
            weight = 0.0F;
            saved = null;
            return;
        }
        int t = (int) (level.getGameTime() - start);
        if (t >= release + Balance.ULTIMATE_CAMERA_OUT) {
            end();
            return;
        }
        // F5 во время ульты не сбивает камеру
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        // у цели на каждом ударе краснеют края экрана
        if (victim) {
            for (int tick = doneTick + 1; tick <= t; tick++) {
                if (tick > release) {
                    break;
                }
                for (int st : Balance.ULTIMATE_STABS) {
                    if (tick == st) {
                        BiteVignette.start();
                    }
                }
                if (tick == Balance.ULTIMATE_FINAL || tick == Balance.ULTIMATE_LATCH) {
                    BiteVignette.start();
                }
            }
        }
        doneTick = t;
    }

    // ------------------------------------------------------------------ камера

    /** После того как игра поставила камеру: двигаем её и поворачиваем сами. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer me = mc.player;
        if (level == null || me == null) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float t = (level.getGameTime() - start) + partial;
        Entity owner = level.getEntity(ownerId);
        Entity target = level.getEntity(targetId);
        if (owner == null || target == null) {
            // кого-то из двоих уже нет рядом: сразу обычный вид
            end();
            return;
        }
        float in = smooth(t / Balance.ULTIMATE_CAMERA_IN);
        float out = smooth((t - release) / Balance.ULTIMATE_CAMERA_OUT);
        float w = in * (1.0F - out);
        weight = w;

        Vec3 eye = me.getEyePosition(partial);
        float yaw = me.getViewYRot(partial);
        float pitch = me.getViewXRot(partial);
        // даже при w = 0 ставим камеру сами (в глаза): иначе на миг мелькнёт обычный вид от третьего лица

        Vec3 o = lerp(owner, partial);
        Vec3 tg = lerp(target, partial);
        Vec3 axis = new Vec3(tg.x - o.x, 0.0, tg.z - o.z);
        if (axis.lengthSqr() < 1.0E-4) {
            float r = owner.getYRot() * Mth.DEG_TO_RAD;
            axis = new Vec3(-Mth.sin(r), 0.0, Mth.cos(r));
        }
        axis = axis.normalize();
        // поднимаем точку, на которую смотрим, вместе с целью
        double lifted = Mth.clamp(tg.y - o.y, 0.0, 2.0);
        Vec3 pivot = o.lerp(tg, 0.45).add(0.0, 1.2 + lifted * 0.5, 0.0);

        // облёт: от «из-за плеча» к виду сбоку; гуль и цель — с разных сторон
        float orbit = 35.0F + 45.0F * smooth((t - Balance.ULTIMATE_CAMERA_IN) / (Balance.ULTIMATE_FINAL - Balance.ULTIMATE_CAMERA_IN))
                + 15.0F * smooth((t - Balance.ULTIMATE_FINAL) / 20.0F);
        Vec3 behind = victim ? axis : axis.scale(-1.0);
        Vec3 dir = rotateY(behind, (victim ? -orbit : orbit) * Mth.DEG_TO_RAD);
        // ближе, когда бьют в живот, ещё ближе на добивающем
        float stabs = smooth((t - Balance.ULTIMATE_LIFT_END) / 20.0F);
        float fin = smooth((t - (Balance.ULTIMATE_FINAL - 12)) / 12.0F);
        double dist = 4.6 - 1.0 * stabs - 0.6 * fin;
        double height = 1.0 - 0.3 * stabs - 0.1 * fin;
        Vec3 wanted = pivot.add(dir.scale(dist)).add(0.0, height, 0.0);
        Vec3 cam = clip(level, me, pivot, wanted);

        // смотрим на обоих, а когда бьют — на живот цели
        Vec3 belly = tg.add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 lookAt = pivot.lerp(belly, 0.4 * stabs);
        Vec3 shake = shake(t);
        Vec3 pos = eye.lerp(cam, w).add(shake.scale(w));

        Vec3 look = lookAt.subtract(cam);
        float camYaw = (float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
        float camPitch = (float) (-Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);

        if (moveCamera(event.getCamera(), pos)) {
            event.setYaw(yaw + Mth.wrapDegrees(camYaw - yaw) * w);
            event.setPitch(Mth.lerp(w, pitch, camPitch));
        } else {
            // не смогли сдвинуть камеру: остаётся обычный вид от третьего лица, только смотрим на цель
            event.setYaw(yaw + Mth.wrapDegrees(camYaw - yaw) * w);
        }
    }

    /** Приближение на ударах. */
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!active || weight <= 0.001F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float t = (mc.level.getGameTime() - start) + (float) event.getPartialTick();
        double zoom = 1.0 - 0.06 * weight - 0.10 * impact(t) * weight;
        event.setFOV(event.getFOV() * zoom);
    }

    /** Насколько сильный удар был только что (0..1), для встряски и приближения. */
    private static float impact(float t) {
        float strength = 0.0F;
        for (int st : Balance.ULTIMATE_STABS) {
            if (st <= release) {
                strength = Math.max(strength, pulse(t - st, 8.0F) * 0.5F);
            }
        }
        if (Balance.ULTIMATE_FINAL <= release) {
            strength = Math.max(strength, pulse(t - Balance.ULTIMATE_FINAL, 14.0F));
        }
        if (Balance.ULTIMATE_LATCH <= release) {
            strength = Math.max(strength, pulse(t - Balance.ULTIMATE_LATCH, 8.0F) * 0.4F);
        }
        return strength;
    }

    private static float pulse(float dt, float length) {
        return dt < 0.0F || dt >= length ? 0.0F : 1.0F - dt / length;
    }

    private static Vec3 shake(float t) {
        double a = 0.12 * impact(t);
        return new Vec3(Math.sin(t * 13.1) * a, Math.sin(t * 17.3) * a * 0.7, Math.sin(t * 11.7) * a);
    }

    private static Vec3 rotateY(Vec3 v, float angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    /** Камера не заходит в стены: если между точкой взгляда и камерой блок — ставим камеру перед ним. */
    private static Vec3 clip(ClientLevel level, Entity me, Vec3 pivot, Vec3 wanted) {
        BlockHitResult hit = level.clip(new ClipContext(pivot, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, me));
        if (hit.getType() == HitResult.Type.MISS) {
            return wanted;
        }
        Vec3 back = pivot.subtract(hit.getLocation());
        double len = back.length();
        return len < 1.0E-4 ? hit.getLocation() : hit.getLocation().add(back.scale(Math.min(0.3, len) / len));
    }

    /** Camera.setPosition — защищённый метод; ищем его по виду (void, один Vec3), имя в собранной игре другое. */
    private static boolean moveCamera(Camera camera, Vec3 pos) {
        if (!searched) {
            searched = true;
            for (Method m : Camera.class.getDeclaredMethods()) {
                if (m.getReturnType() == void.class && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == Vec3.class) {
                    try {
                        m.setAccessible(true);
                        setPosition = m;
                    } catch (RuntimeException e) {
                        setPosition = null;
                    }
                    break;
                }
            }
        }
        if (setPosition == null) {
            return false;
        }
        try {
            setPosition.invoke(camera, pos);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            setPosition = null;
            return false;
        }
    }

    private static Vec3 lerp(Entity e, float partial) {
        return new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()),
                Mth.lerp(partial, e.zo, e.getZ()));
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    // ------------------------------------------------------------------ интерфейс

    /** Во время кино интерфейс скрыт: остаются чат и красные края экрана. */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!active || weight <= 0.05F) {
            return;
        }
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CHAT_PANEL.id()) || id.equals(BITE_VIGNETTE)) {
            return;
        }
        event.setCanceled(true);
    }

    /** Чёрные полосы сверху и снизу, как в кино. */
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        if (!active || weight <= 0.001F) {
            return;
        }
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int bar = Math.round(height * 0.11F * weight);
        if (bar > 0) {
            event.getGuiGraphics().fill(0, 0, width, bar, 0xFF000000);
            event.getGuiGraphics().fill(0, height - bar, width, height, 0xFF000000);
        }
    }
}
