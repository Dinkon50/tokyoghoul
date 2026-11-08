package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.network.packet.BossFxS2C;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Эффекты особых атак боссов (combat/BossFx) в стиле мода, без «неона»:
 * предупреждение — тёмное кровавое пятно на земле растекается к удару, по краю капает кровь (у CCG — тёмный след
 * и искры), полоса удара — тёмная полоса с каплями; удар о землю — пыль и кровь кольцом и тёмный след волны
 * (куски земли шлёт сервер); копьё кагуне — настоящий кристаллический шип выстреливает и втягивается, взрыв
 * кагуне — шипы во все стороны; шипы из-под земли — твёрдые кристаллы на кровавом пятне; взмахи и рывок —
 * тёмно-красный смазанный след (у CCG — бледный стальной и искры); тряска камеры при сильных ударах.
 * Пятна и следы — обычная полупрозрачность (FxRenderTypes.TINT), шипы — твёрдые, текстурой шипов кагуне.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class BossFxRenderer {
    private static final ResourceLocation SPIKE = new ResourceLocation(TokyoGhoulRPG.MOD_ID,
            "textures/entity/kagune_spike.png");
    private static final int MAX = 400;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.5F, 0.02F, 0.05F), 1.3F);
    private static final DustParticleOptions BLOOD_DARK = new DustParticleOptions(new Vector3f(0.28F, 0.0F, 0.03F), 1.6F);

    private static final class Fx {
        final BossFxS2C msg;
        final long seed;
        final boolean steel;
        int age;

        Fx(BossFxS2C msg, long seed) {
            this.msg = msg;
            this.seed = seed;
            // агент CCG — сталь и искры вместо крови
            this.steel = (msg.color & 0xFF) > 0x80;
        }
    }

    private static final List<Fx> LIVE = new ArrayList<>();
    private static long counter;

    private BossFxRenderer() {
    }

    public static void add(BossFxS2C msg) {
        if (LIVE.size() < MAX) {
            LIVE.add(new Fx(msg, ++counter * 7919L + (long) (msg.x * 31 + msg.z * 17)));
        }
    }

    // ------------------------------------------------------------------ тики: частицы и время жизни

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LIVE.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        Iterator<Fx> it = LIVE.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            particles(mc.level, fx);
            if (++fx.age > fx.msg.duration + extraLife(fx.msg.type)) {
                it.remove();
            }
        }
    }

    private static int extraLife(int type) {
        return type == BossFx.SPIKES ? 10 : (type == BossFx.CIRCLE || type == BossFx.SHOCKWAVE ? 20 : 0);
    }

    /** Частицы Minecraft поверх рисунка: кровь, пыль, искры — то, из чего сделан стиль мода. */
    private static void particles(ClientLevel level, Fx fx) {
        BossFxS2C f = fx.msg;
        Random rnd = new Random(fx.seed * 31 + fx.age);
        float p = fx.age / (float) Math.max(1, f.duration);
        ParticleOptions drop = fx.steel ? ParticleTypes.ELECTRIC_SPARK : BLOOD;
        switch (f.type) {
            case BossFx.CIRCLE -> {
                if (fx.age > f.duration) {
                    return;
                }
                int n = 1 + (int) (p * 4);
                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2.0;
                    double r = f.size * (0.85 + rnd.nextDouble() * 0.15);
                    level.addParticle(drop, f.x + Math.cos(a) * r, f.y + 0.1, f.z + Math.sin(a) * r, 0.0, 0.02, 0.0);
                }
            }
            case BossFx.LINE -> {
                if (fx.age % 2 == 0) {
                    Vec3 a = new Vec3(f.x, f.y, f.z);
                    Vec3 d = new Vec3(f.x2, f.y2, f.z2).subtract(a);
                    for (int i = 0; i < 3; i++) {
                        Vec3 q = a.add(d.scale(rnd.nextDouble() * Math.min(1.0, p * 1.3)));
                        level.addParticle(drop, q.x, q.y, q.z, 0.0, -0.02, 0.0);
                    }
                }
            }
            case BossFx.SHOCKWAVE -> {
                if (fx.age > f.duration) {
                    return;
                }
                float r = f.size * easeOut(p);
                int n = (int) (6 + f.size * 2);
                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2.0;
                    double x = f.x + Math.cos(a) * r;
                    double z = f.z + Math.sin(a) * r;
                    level.addParticle(ParticleTypes.POOF, x, f.y + 0.15, z, Math.cos(a) * 0.08, 0.03, Math.sin(a) * 0.08);
                    if (i % 2 == 0) {
                        level.addParticle(fx.steel ? ParticleTypes.CRIT : BLOOD_DARK, x, f.y + 0.3, z, 0.0, 0.05, 0.0);
                    }
                }
            }
            case BossFx.SPEAR -> {
                if (fx.age < 5) {
                    Vec3 a = new Vec3(f.x, f.y, f.z);
                    Vec3 d = new Vec3(f.x2, f.y2, f.z2).subtract(a);
                    for (int i = 0; i < 10; i++) {
                        Vec3 q = a.add(d.scale(rnd.nextDouble() * spearReach(fx.age)));
                        level.addParticle(BLOOD, q.x, q.y, q.z, (rnd.nextDouble() - 0.5) * 0.1, -0.05,
                                (rnd.nextDouble() - 0.5) * 0.1);
                    }
                }
            }
            case BossFx.BEAM -> {
                if (fx.age < 3) {
                    Vec3 a = new Vec3(f.x, f.y, f.z);
                    Vec3 d = new Vec3(f.x2, f.y2, f.z2).subtract(a);
                    for (int i = 0; i < 6; i++) {
                        Vec3 q = a.add(d.scale(rnd.nextDouble()));
                        level.addParticle(fx.steel ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMOKE, q.x, q.y, q.z,
                                0.0, 0.01, 0.0);
                    }
                }
            }
            case BossFx.BOLT -> {
                if (fx.age < 4) {
                    for (int i = 0; i < 14; i++) {
                        double a = rnd.nextDouble() * Math.PI * 2.0;
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, f.x, f.y + 0.2, f.z, Math.cos(a) * 0.4, 0.3,
                                Math.sin(a) * 0.4);
                    }
                    level.addParticle(ParticleTypes.LARGE_SMOKE, f.x, f.y + 0.3, f.z, 0.0, 0.05, 0.0);
                }
            }
            case BossFx.SPIKES -> {
                if (fx.age < 6) {
                    for (int i = 0; i < 6; i++) {
                        double a = rnd.nextDouble() * Math.PI * 2.0;
                        double r = rnd.nextDouble() * f.size;
                        level.addParticle(BLOOD, f.x + Math.cos(a) * r, f.y + 0.2 + rnd.nextDouble() * 1.5,
                                f.z + Math.sin(a) * r, 0.0, 0.1, 0.0);
                    }
                }
            }
            case BossFx.BURST -> {
                if (fx.age < 4) {
                    for (int i = 0; i < 30; i++) {
                        double a = rnd.nextDouble() * Math.PI * 2.0;
                        double up = (rnd.nextDouble() - 0.3) * 0.8;
                        double s = 0.3 + rnd.nextDouble() * 0.6;
                        level.addParticle(i % 3 == 0 ? BLOOD_DARK : BLOOD, f.x, f.y, f.z, Math.cos(a) * s, up * s,
                                Math.sin(a) * s);
                    }
                }
            }
            case BossFx.ARC, BossFx.SLASH -> {
                if (fx.age < 4 && !fx.steel) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.toRadians(f.extra) + rnd.nextDouble() * Math.toRadians(
                                f.type == BossFx.SLASH ? (f.x2 - f.x) : 360.0);
                        level.addParticle(BLOOD, f.x + Math.cos(a) * f.size, f.y + 1.0, f.z + Math.sin(a) * f.size,
                                0.0, -0.05, 0.0);
                    }
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LIVE.clear();
    }

    // ------------------------------------------------------------------ тряска камеры

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || LIVE.isEmpty()) {
            return;
        }
        float power = 0.0F;
        for (Fx fx : LIVE) {
            if (fx.msg.type != BossFx.SHAKE) {
                continue;
            }
            double dist = mc.player.position().distanceTo(new Vec3(fx.msg.x, fx.msg.y, fx.msg.z));
            if (dist > fx.msg.size) {
                continue;
            }
            float t = (fx.age + (float) event.getPartialTick()) / Math.max(1, fx.msg.duration);
            power += fx.msg.extra * (1.0F - (float) (dist / fx.msg.size)) * Math.max(0.0F, 1.0F - t);
        }
        if (power <= 0.001F) {
            return;
        }
        power = Math.min(power, 4.0F);
        float time = (mc.level.getGameTime() + (float) event.getPartialTick()) * 1.7F;
        event.setYaw(event.getYaw() + power * Mth.sin(time * 2.3F));
        event.setPitch(event.getPitch() + power * Mth.cos(time * 2.9F) * 0.8F);
        event.setRoll(event.getRoll() + power * Mth.sin(time * 3.7F) * 0.6F);
    }

    // ------------------------------------------------------------------ рисование

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // твёрдые кристаллы кагуне
        RenderType solid = RenderType.entityCutoutNoCull(SPIKE);
        VertexConsumer sv = buffers.getBuffer(solid);
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        for (Fx fx : LIVE) {
            float age = fx.age + partial;
            switch (fx.msg.type) {
                case BossFx.SPIKES -> groundSpikes(sv, ps, fx, age);
                case BossFx.SPEAR -> spear(sv, ps, fx, age);
                case BossFx.BURST -> burst(sv, ps, fx, age);
                default -> {
                }
            }
        }
        ps.popPose();
        buffers.endBatch(solid);

        // пятна, следы, смазанные взмахи
        VertexConsumer vc = buffers.getBuffer(FxRenderTypes.TINT);
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = ps.last().pose();
        for (Fx fx : LIVE) {
            float age = fx.age + partial;
            switch (fx.msg.type) {
                case BossFx.CIRCLE -> stain(vc, m, fx, age);
                case BossFx.LINE -> line(vc, m, fx, age, cam);
                case BossFx.SHOCKWAVE -> scar(vc, m, fx, age);
                case BossFx.BEAM -> smear(vc, m, fx, age, cam);
                case BossFx.BOLT -> scorch(vc, m, fx, age);
                case BossFx.ARC, BossFx.SLASH -> arc(vc, m, fx, age);
                case BossFx.SPIKES -> pool(vc, m, fx, age);
                default -> {
                }
            }
        }
        ps.popPose();
        buffers.endBatch(FxRenderTypes.TINT);
    }

    /**
     * Предупреждение: тёмное кровавое пятно растекается от центра к краю к моменту удара, край темнее и неровный
     * (как лужа), еле заметно пульсирует. После удара пятно тает.
     */
    private static void stain(VertexConsumer vc, Matrix4f m, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        float p = Mth.clamp(age / Math.max(1, f.duration), 0.0F, 1.0F);
        float fade = age <= f.duration ? 1.0F : Math.max(0.0F, 1.0F - (age - f.duration) / 20.0F);
        float y = (float) f.y + 0.03F;
        int seg = Math.max(28, (int) (f.size * 10));
        Random rnd = new Random(fx.seed);
        float[] wobble = new float[seg + 1];
        for (int i = 0; i < seg; i++) {
            wobble[i] = 0.9F + rnd.nextFloat() * 0.18F;
        }
        wobble[seg] = wobble[0];
        int dark = fx.steel ? 0x1A2228 : 0x2A0306;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.5F);
        float spread = easeOut(p);
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg;
            float a1 = (i + 1) * Mth.TWO_PI / seg;
            float r0 = f.size * wobble[i];
            float r1 = f.size * wobble[i + 1];
            // слабый след всей зоны (видно, куда достанет)
            disc(vc, m, f, 0.0F, 0.0F, r0, r1, a0, a1, y, dark, 0.10F * fade, 0.22F * fade);
            // растекающаяся кровь
            disc(vc, m, f, 0.0F, 0.0F, r0 * spread, r1 * spread, a0, a1, y + 0.004F, f.color,
                    0.48F * fade * pulse, 0.62F * fade * pulse);
            // тёмный край лужи
            disc(vc, m, f, Math.max(0.0F, r0 * spread - 0.18F), Math.max(0.0F, r1 * spread - 0.18F), r0 * spread,
                    r1 * spread, a0, a1, y + 0.008F, dark, 0.0F, 0.6F * fade);
        }
    }

    /** Полоса удара: тёмная полоса растекается от босса вдоль линии, по ней капает кровь (частицы). */
    private static void line(VertexConsumer vc, Matrix4f m, Fx fx, float age, Vec3 cam) {
        BossFxS2C f = fx.msg;
        float p = Mth.clamp(age / Math.max(1, f.duration), 0.0F, 1.0F);
        Vec3 a = new Vec3(f.x, f.y, f.z);
        Vec3 b = new Vec3(f.x2, f.y2, f.z2);
        int dark = fx.steel ? 0x1A2228 : 0x2A0306;
        strip(vc, m, a, b, cam, f.size, dark, 0.18F, 0.1F);
        strip(vc, m, a, a.add(b.subtract(a).scale(easeOut(p))), cam, f.size * 0.6F, f.color, 0.45F, 0.3F);
    }

    /** След ударной волны на земле: тёмное кольцо-трещина расходится и тает (пыль и кровь — частицами). */
    private static void scar(VertexConsumer vc, Matrix4f m, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        float p = Mth.clamp(age / Math.max(1, f.duration), 0.0F, 1.0F);
        float fade = age <= f.duration ? 1.0F - 0.4F * p : Math.max(0.0F, 0.6F * (1.0F - (age - f.duration) / 20.0F));
        float r = f.size * easeOut(p);
        float y = (float) f.y + 0.035F;
        int seg = Math.max(32, (int) (f.size * 10));
        int dark = fx.steel ? 0x22282C : 0x1E0204;
        float ri = Math.max(0.0F, r - 0.9F);
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg;
            float a1 = (i + 1) * Mth.TWO_PI / seg;
            disc(vc, m, f, ri, ri, r, r, a0, a1, y, dark, 0.0F, 0.45F * fade);
        }
    }

    /** Смазанный след (рывок, выпад квинке): тёмно-красный или бледно-стальной, быстро тает. */
    private static void smear(VertexConsumer vc, Matrix4f m, Fx fx, float age, Vec3 cam) {
        BossFxS2C f = fx.msg;
        float p = Mth.clamp(age / Math.max(1, f.duration), 0.0F, 1.0F);
        float a = (1.0F - p) * (1.0F - p);
        Vec3 from = new Vec3(f.x, f.y, f.z);
        Vec3 to = new Vec3(f.x2, f.y2, f.z2);
        int c = fx.steel ? 0xD8E6EE : f.color;
        strip(vc, m, from, to, cam, f.size, c, 0.05F * a, 0.3F * a);
        strip(vc, m, from, to, cam, f.size * 0.35F, c, 0.15F * a, 0.5F * a);
    }

    /** Подпалина от молнии: тёмное пятно на земле. */
    private static void scorch(VertexConsumer vc, Matrix4f m, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        float a = Mth.clamp(1.0F - age / Math.max(1, f.duration), 0.0F, 1.0F);
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg;
            float a1 = (i + 1) * Mth.TWO_PI / seg;
            disc(vc, m, f, 0.0F, 0.0F, 1.6F, 1.6F, a0, a1, (float) f.y + 0.03F, 0x101214, 0.7F * a, 0.0F);
        }
    }

    /** Кровавое пятно под шипами из-под земли. */
    private static void pool(VertexConsumer vc, Matrix4f m, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        float a = Mth.clamp(1.0F - (age - f.duration) / 10.0F, 0.0F, 1.0F) * Math.min(1.0F, age / 3.0F);
        int seg = 28;
        for (int i = 0; i < seg; i++) {
            float a0 = i * Mth.TWO_PI / seg;
            float a1 = (i + 1) * Mth.TWO_PI / seg;
            disc(vc, m, f, 0.0F, 0.0F, f.size + 0.5F, f.size + 0.5F, a0, a1, (float) f.y + 0.03F, 0x2A0306,
                    0.6F * a, 0.0F);
        }
    }

    /**
     * Взмах: тёмно-красный размытый след (у CCG — бледный стальной) по дуге на уровне груди, плотнее у кончика,
     * хвост гаснет; без светящихся кромок.
     */
    private static void arc(VertexConsumer vc, Matrix4f m, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        float p = Mth.clamp(age / Math.max(1, f.duration), 0.0F, 1.0F);
        boolean slash = f.type == BossFx.SLASH;
        float sweep = slash ? (float) Math.toRadians(f.x2 - f.x) : Mth.TWO_PI;
        float head = easeOut(Math.min(1.0F, p * 1.6F)) * sweep;
        float fade = p < 0.5F ? 1.0F : 1.0F - (p - 0.5F) / 0.5F;
        float start = (float) Math.toRadians(f.extra);
        int seg = slash ? 16 : 40;
        int c = fx.steel ? 0xC8D8E2 : f.color;
        for (int layer = 0; layer < (slash ? 1 : 2); layer++) {
            float yBase = (float) f.y + 0.9F + layer * 0.4F;
            float tilt = layer == 0 ? 0.2F : -0.15F;
            float dir = layer == 0 ? 1.0F : -1.0F;
            float r1 = f.size * (layer == 0 ? 1.0F : 0.85F);
            float r0 = r1 - (slash ? 0.5F : 0.7F);
            for (int i = 0; i < seg; i++) {
                float t0 = i / (float) seg;
                float t1 = (i + 1) / (float) seg;
                if (t1 * sweep > head) {
                    break;
                }
                float lead0 = 1.0F - (head - t0 * sweep) / sweep;
                float lead1 = 1.0F - (head - t1 * sweep) / sweep;
                float a0 = start + dir * t0 * sweep;
                float a1 = start + dir * t1 * sweep;
                float h0 = tilt * Mth.sin(a0);
                float h1 = tilt * Mth.sin(a1);
                float al0 = 0.5F * lead0 * lead0 * fade;
                float al1 = 0.5F * lead1 * lead1 * fade;
                quad(vc, m,
                        (float) f.x + Mth.cos(a0) * r0, yBase + h0, (float) f.z + Mth.sin(a0) * r0, c, 0.0F,
                        (float) f.x + Mth.cos(a0) * r1, yBase + h0, (float) f.z + Mth.sin(a0) * r1, c, al0,
                        (float) f.x + Mth.cos(a1) * r1, yBase + h1, (float) f.z + Mth.sin(a1) * r1, c, al1,
                        (float) f.x + Mth.cos(a1) * r0, yBase + h1, (float) f.z + Mth.sin(a1) * r0, c, 0.0F);
            }
        }
    }

    // ------------------------------------------------------------------ кристаллы кагуне

    /** Как вылезает шип: с перехлёстом за 4 тика, потом стоит; после end уходит обратно за sink тиков. */
    private static float rise(float t, float end, float age, float sink) {
        if (t <= 0.0F) {
            return 0.0F;
        }
        float up = Math.min(1.0F, t / 4.0F);
        float r = 1.0F + 2.70158F * (float) Math.pow(up - 1.0F, 3) + 1.70158F * (float) Math.pow(up - 1.0F, 2);
        if (age > end) {
            r *= Math.max(0.0F, 1.0F - (age - end) / sink);
        }
        return r;
    }

    private static void groundSpikes(VertexConsumer vc, PoseStack ps, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        Random rnd = new Random(fx.seed);
        int count = Math.max(1, (int) f.extra);
        for (int i = 0; i < count; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double dist = i == 0 ? 0.0 : Math.sqrt(rnd.nextDouble()) * f.size;
            float height = i == 0 ? 3.0F : 1.2F + rnd.nextFloat() * 1.6F;
            float width = i == 0 ? 0.38F : 0.16F + rnd.nextFloat() * 0.16F;
            float tiltX = (float) ((rnd.nextDouble() - 0.5) * 0.5 + (dist > 0 ? Math.cos(ang) * 0.3 : 0));
            float tiltZ = (float) ((rnd.nextDouble() - 0.5) * 0.5 + (dist > 0 ? Math.sin(ang) * 0.3 : 0));
            float r = rise(age - i * 0.6F, f.duration, age, 10.0F);
            if (r <= 0.01F) {
                continue;
            }
            ps.pushPose();
            ps.translate(f.x + Math.cos(ang) * dist, f.y - 0.3, f.z + Math.sin(ang) * dist);
            ps.mulPose(new Quaternionf().rotationXYZ(tiltZ, (float) ang, -tiltX));
            spike(vc, ps.last().pose(), ps.last().normal(), height * r, width);
            ps.popPose();
        }
    }

    /** Насколько далеко дотянулось копьё: выстрел за 3 тика. */
    private static float spearReach(float age) {
        return Math.min(1.0F, age / 3.0F);
    }

    /** Кагуне-копьё: длинный шип выстреливает к цели, держится и втягивается обратно в босса. */
    private static void spear(VertexConsumer vc, PoseStack ps, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        Vec3 from = new Vec3(f.x, f.y, f.z);
        Vec3 d = new Vec3(f.x2, f.y2, f.z2).subtract(from);
        double len = d.length();
        if (len < 0.1) {
            return;
        }
        float reach = spearReach(age);
        float hold = f.duration * 0.6F;
        if (age > hold) {
            reach *= Math.max(0.0F, 1.0F - (age - hold) / Math.max(1.0F, f.duration - hold));
        }
        if (reach <= 0.01F) {
            return;
        }
        Vec3 n = d.scale(1.0 / len);
        ps.pushPose();
        ps.translate(from.x, from.y, from.z);
        ps.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) n.x, (float) n.y, (float) n.z));
        ps.mulPose(new Quaternionf().rotationY(age * 0.4F));
        spike(vc, ps.last().pose(), ps.last().normal(), (float) (len * reach), f.size);
        // второй, тоньше и с поворотом — копьё из двух сплетённых отростков
        ps.mulPose(new Quaternionf().rotationY(0.8F));
        spike(vc, ps.last().pose(), ps.last().normal(), (float) (len * reach * 0.9), f.size * 0.6F);
        ps.popPose();
    }

    /** Взрыв кагуне: шипы разом выходят из тела во все стороны (и немного вверх-вниз) и втягиваются. */
    private static void burst(VertexConsumer vc, PoseStack ps, Fx fx, float age) {
        BossFxS2C f = fx.msg;
        Random rnd = new Random(fx.seed);
        int count = Math.max(6, (int) f.extra);
        for (int i = 0; i < count; i++) {
            float yaw = (float) (i * Math.PI * 2.0 / count + rnd.nextDouble() * 0.3);
            float pitch = (float) ((rnd.nextDouble() - 0.4) * 0.9);
            float len = f.size * (0.6F + rnd.nextFloat() * 0.45F);
            float width = 0.16F + rnd.nextFloat() * 0.12F;
            float r = rise(age - rnd.nextFloat() * 1.5F, f.duration * 0.5F, age, f.duration * 0.5F);
            if (r <= 0.01F) {
                continue;
            }
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            ps.pushPose();
            ps.translate(f.x, f.y, f.z);
            ps.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) dir.x, (float) dir.y, (float) dir.z));
            spike(vc, ps.last().pose(), ps.last().normal(), len * r, width);
            ps.popPose();
        }
    }

    /** Четырёхгранный шип вверх по +Y: основание в 0, острие на высоте h. */
    private static void spike(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float h, float w) {
        Vector3f tip = new Vector3f(0.0F, h, 0.0F);
        Vector3f[] base = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI;
            base[k] = new Vector3f(Mth.cos(a) * w, 0.0F, Mth.sin(a) * w);
        }
        for (int k = 0; k < 4; k++) {
            Vector3f a = base[k];
            Vector3f b = base[(k + 1) & 3];
            float u0 = k * 0.25F + 0.02F;
            float u1 = (k + 1) * 0.25F - 0.02F;
            Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(tip).sub(a));
            if (n.lengthSquared() < 1.0E-10F) {
                n.set(0.0F, 1.0F, 0.0F);
            } else {
                n.normalize();
            }
            if (n.x * (a.x + b.x) + n.z * (a.z + b.z) < 0.0F) {
                n.negate();
            }
            spikeVertex(vc, pose, normal, tip, (u0 + u1) * 0.5F, 0.02F, n);
            spikeVertex(vc, pose, normal, a, u0, 0.9F, n);
            spikeVertex(vc, pose, normal, b, u1, 0.9F, n);
            spikeVertex(vc, pose, normal, b, u1, 0.9F, n);
        }
    }

    private static void spikeVertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vector3f p, float u, float v,
            Vector3f n) {
        vc.vertex(pose, p.x, p.y, p.z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, n.x, n.y, n.z).endVertex();
    }

    // ------------------------------------------------------------------ примитивы

    /** Кусок плоского кольца на высоте y: внутренние радиусы (ri0, ri1) и внешние (ro0, ro1) на углах a0 и a1. */
    private static void disc(VertexConsumer vc, Matrix4f m, BossFxS2C f, float ri0, float ri1, float ro0, float ro1,
            float a0, float a1, float y, int color, float alphaInner, float alphaOuter) {
        float x = (float) f.x;
        float z = (float) f.z;
        float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
        quad(vc, m,
                x + c0 * ri0, y, z + s0 * ri0, color, alphaInner,
                x + c0 * ro0, y, z + s0 * ro0, color, alphaOuter,
                x + c1 * ro1, y, z + s1 * ro1, color, alphaOuter,
                x + c1 * ri1, y, z + s1 * ri1, color, alphaInner);
    }

    /** Полоса от a к b, повёрнутая к камере, ширины w; alpha в начале и в конце. */
    private static void strip(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 cam, float w, int color, float alphaA,
            float alphaB) {
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = d.cross(cam.subtract(mid));
        if (side.lengthSqr() < 1.0E-6) {
            side = d.cross(new Vec3(0.0, 1.0, 0.0));
            if (side.lengthSqr() < 1.0E-6) {
                side = new Vec3(1.0, 0.0, 0.0);
            }
        }
        side = side.normalize().scale(w * 0.5);
        quad(vc, m,
                (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z), color, alphaA,
                (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z), color, alphaA,
                (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z), color, alphaB,
                (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z), color, alphaB);
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
            float x0, float y0, float z0, int c0, float a0,
            float x1, float y1, float z1, int c1, float a1,
            float x2, float y2, float z2, int c2, float a2,
            float x3, float y3, float z3, int c3, float a3) {
        v(vc, m, x0, y0, z0, c0, a0);
        v(vc, m, x1, y1, z1, c1, a1);
        v(vc, m, x2, y2, z2, c2, a2);
        v(vc, m, x3, y3, z3, c3, a3);
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, int rgb, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        vc.vertex(m, x, y, z).color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, a).endVertex();
    }

    private static float easeOut(float t) {
        float u = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - u * u * u;
    }
}
