package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Летящие шипы крыльев Тоуки: маленькие кристаллические шипы (большой осколок — крупный шип). Сервер шлёт,
 * когда шип вылетел и когда кончился; между этим клиент ведёт его по прямой, как сервер, и рисует
 * четырёхгранный шип остриём вперёд, который медленно крутится вокруг своей оси.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class ShardSpikes {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TokyoGhoulRPG.MOD_ID,
            "textures/entity/kagune_spike.png");
    /**
     * Длина и толщина (полуширина) шипа в блоках по размеру: крошечный (шквал K), маленький (ЛКМ, рикошет),
     * средний (ульта Alt+J), большой (ПКМ дважды).
     */
    private static final float[] LENGTH = {0.32F, 0.47F, 0.6F, 0.77F};
    private static final float[] WIDTH = {0.03F, 0.047F, 0.06F, 0.085F};
    /** Самая широкая часть шипа: доля длины от острия (длинное острое жало, короткий тупой хвост). */
    private static final float RING = 0.72F;
    private static final int MAX = 3000;
    /** Шип без вести (пакет конца потерялся) живёт не дольше этого. */
    private static final int MAX_AGE = 200;

    private static final class Spike {
        Vec3 pos;
        Vec3 prev;
        final Vec3 velocity;
        double left;
        final int size;
        final float phase;
        int age;

        Spike(Vec3 pos, Vec3 velocity, double left, int size, float phase) {
            this.pos = pos;
            this.prev = pos;
            this.velocity = velocity;
            this.left = left;
            this.size = Math.max(0, Math.min(LENGTH.length - 1, size));
            this.phase = phase;
        }
    }

    private static final Map<Integer, Spike> LIVE = new HashMap<>();

    private ShardSpikes() {
    }

    public static void spawn(int id, double x, double y, double z, float vx, float vy, float vz, float range,
            int size) {
        if (LIVE.size() >= MAX) {
            return;
        }
        Vec3 velocity = new Vec3(vx, vy, vz);
        if (velocity.lengthSqr() < 1.0E-8) {
            return;
        }
        LIVE.put(id, new Spike(new Vec3(x, y, z), velocity, range, size, (id * 0.618F) % 1.0F * Mth.TWO_PI));
    }

    public static void end(int id) {
        LIVE.remove(id);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            LIVE.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        Iterator<Spike> it = LIVE.values().iterator();
        while (it.hasNext()) {
            Spike s = it.next();
            s.age++;
            s.prev = s.pos;
            if (s.left <= 0.0 || s.age > MAX_AGE) {
                it.remove();
                continue;
            }
            double length = Math.min(s.velocity.length(), s.left);
            Vec3 to = s.pos.add(s.velocity.normalize().scale(length));
            // в стену шип втыкается и пропадает (сервер всё равно пришлёт конец)
            BlockHitResult hit = mc.level.clip(new ClipContext(s.pos, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS) {
                s.pos = hit.getLocation();
                s.left = 0.0;
            } else {
                s.pos = to;
                s.left -= length;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LIVE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || LIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer vc = buffers.getBuffer(type);
        for (Spike s : LIVE.values()) {
            Vec3 p = s.prev.lerp(s.pos, partial);
            if (p.distanceToSqr(cam) > 128.0 * 128.0) {
                continue;
            }
            Vec3 d = s.velocity.normalize();
            poseStack.pushPose();
            poseStack.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 0.0F, 1.0F, (float) d.x, (float) d.y, (float) d.z));
            poseStack.mulPose(new Quaternionf().rotationZ(s.phase + (s.age + partial) * 0.35F));
            draw(vc, poseStack.last().pose(), poseStack.last().normal(),
                    LENGTH[s.size], WIDTH[s.size]);
            poseStack.popPose();
        }
        buffers.endBatch(type);
    }

    /** Четырёхгранный шип вдоль +Z: остриё в (0,0,0), самое широкое место на RING длины, хвост в (0,0,-length). */
    private static void draw(VertexConsumer vc, Matrix4f pose, Matrix3f normal, float length, float width) {
        Vector3f tip = new Vector3f(0.0F, 0.0F, 0.0F);
        Vector3f tail = new Vector3f(0.0F, 0.0F, -length);
        Vector3f[] ring = new Vector3f[4];
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.HALF_PI;
            ring[k] = new Vector3f(Mth.cos(a) * width, Mth.sin(a) * width, -length * RING);
        }
        for (int k = 0; k < 4; k++) {
            Vector3f a = ring[k];
            Vector3f b = ring[(k + 1) & 3];
            float u0 = k * 0.25F + 0.02F;
            float u1 = (k + 1) * 0.25F - 0.02F;
            float um = (u0 + u1) * 0.5F;
            // передняя грань: от острия (светлое, v=0) к широкому месту (v=0.65)
            triangle(vc, pose, normal, tip, um, 0.02F, a, u0, 0.65F, b, u1, 0.65F);
            // задняя грань: от широкого места к тёмному хвосту (v=1)
            triangle(vc, pose, normal, b, u1, 0.65F, a, u0, 0.65F, tail, um, 0.98F);
        }
    }

    private static void triangle(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
            Vector3f a, float ua, float va, Vector3f b, float ub, float vb, Vector3f c, float uc, float vc2) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        // нормаль наружу от оси шипа
        Vector3f center = new Vector3f(a).add(b).add(c).div(3.0F);
        if (n.x * center.x + n.y * center.y < 0.0F) {
            n.negate();
        }
        if (n.lengthSquared() < 1.0E-12F) {
            n.set(0.0F, 1.0F, 0.0F);
        } else {
            n.normalize();
        }
        // форма рисует четырёхугольники: последний угол повторяем
        vertex(vc, pose, normal, a, ua, va, n);
        vertex(vc, pose, normal, b, ub, vb, n);
        vertex(vc, pose, normal, c, uc, vc2, n);
        vertex(vc, pose, normal, c, uc, vc2, n);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal, Vector3f p, float u, float v,
            Vector3f n) {
        vc.vertex(pose, p.x, p.y, p.z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(normal, n.x, n.y, n.z).endVertex();
    }
}
