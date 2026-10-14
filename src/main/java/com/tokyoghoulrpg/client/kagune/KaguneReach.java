package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AbilitySync;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Длинное щупальце захватов (Ctrl + ЛКМ / Ctrl + ПКМ). Анимация захвата из Blockbench двигает кагуне у спины,
 * а это щупальце вытягивается от спины игрока до самой цели — существа или точки на блоке, на 10-25 блоков:
 * <ul>
 *   <li>до момента «зацеп» из анимации щупальце выстреливает к цели;</li>
 *   <li>потом держится за цель (за существом следует, когда его тянет), пока игрока или цель подтягивает;</li>
 *   <li>после «отпускания» быстро втягивается обратно.</li>
 * </ul>
 * Рисуется в мире, поэтому видно и от первого лица, и у других игроков.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class KaguneReach {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/kagune_stretch.png");
    private static final float RETRACT_TICKS = 6.0F;
    private static final float BASE_RADIUS = 0.17F;
    private static final float TIP_RADIUS = 0.07F;

    private record Reach(int targetId, Vec3 point, long start, float latch, float release) {
    }

    /** Ключ — номер сущности игрока. Одно щупальце захвата на игрока. */
    private static final Map<Integer, Reach> REACHES = new HashMap<>();

    private KaguneReach() {
    }

    public static void start(int ownerId, int targetId, double x, double y, double z, String animation) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get(animation);
        float latch = Math.max(4, timing.latch());
        float release = Math.max(latch + 2, timing.release());
        REACHES.put(ownerId, new Reach(targetId, new Vec3(x, y, z), mc.level.getGameTime(), latch, release));
    }

    public static void clear() {
        REACHES.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || REACHES.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            REACHES.clear();
            return;
        }
        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(type);
        boolean drawn = false;

        Iterator<Map.Entry<Integer, Reach>> it = REACHES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Reach> entry = it.next();
            Reach reach = entry.getValue();
            Entity ownerEntity = level.getEntity(entry.getKey());
            float t = (level.getGameTime() - reach.start()) + partial;
            if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive() || t > reach.release() + RETRACT_TICKS) {
                it.remove();
                continue;
            }
            // насколько вытянуто: выстрел до зацепа, держится, втягивается после отпускания
            float extend;
            if (t < reach.latch()) {
                float f = t / reach.latch();
                extend = 1.0F - (1.0F - f) * (1.0F - f);
            } else if (t < reach.release()) {
                extend = 1.0F;
            } else {
                extend = 1.0F - (t - reach.release()) / RETRACT_TICKS;
            }
            if (extend <= 0.01F) {
                continue;
            }

            // начало — середина спины (за плечами), с учётом поворота тела
            double ox = Mth.lerp(partial, owner.xo, owner.getX());
            double oy = Mth.lerp(partial, owner.yo, owner.getY());
            double oz = Mth.lerp(partial, owner.zo, owner.getZ());
            float bodyYaw = Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(bodyYaw);
            double fz = Mth.cos(bodyYaw);
            Vec3 from = new Vec3(ox - fx * 0.28, oy + owner.getBbHeight() * 0.68, oz - fz * 0.28);

            // конец — существо (следуем за ним) или точка на блоке
            Vec3 to = reach.point();
            Entity target = reach.targetId() >= 0 ? level.getEntity(reach.targetId()) : null;
            if (target != null && target.isAlive()) {
                to = new Vec3(Mth.lerp(partial, target.xo, target.getX()),
                        Mth.lerp(partial, target.yo, target.getY()) + target.getBbHeight() * 0.55,
                        Mth.lerp(partial, target.zo, target.getZ()));
            }
            Vec3 tip = from.add(to.subtract(from).scale(extend));
            drawTentacle(level, poseStack, consumer, cam, from, tip, t);
            drawn = true;
        }
        if (drawn) {
            buffers.endBatch(type);
        }
    }

    /** Щупальце — цепочка четырёхгранных отрезков, тоньше к концу, с лёгкой волной. */
    private static void drawTentacle(ClientLevel level, PoseStack poseStack, VertexConsumer consumer, Vec3 cam,
                                     Vec3 from, Vec3 tip, float time) {
        Vec3 line = tip.subtract(from);
        double length = line.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = line.scale(1.0 / length);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) {
            side = dir.cross(new Vec3(1, 0, 0));
        }
        side = side.normalize();
        Vec3 up = side.cross(dir).normalize();

        int segments = Math.max(4, Math.min(80, (int) Math.ceil(length * 3.0)));
        Vec3[] centers = new Vec3[segments + 1];
        float[] radii = new float[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float s = i / (float) segments;
            // волна гаснет у основания и у цели, чтобы щупальце крепко «держало» цель
            double envelope = Mth.sin((float) Math.PI * s) * Math.min(1.0, length / 4.0);
            double waveSide = Mth.sin(s * 9.0F - time * 0.45F) * 0.12 * envelope;
            double waveUp = Mth.cos(s * 7.0F - time * 0.35F) * 0.08 * envelope;
            centers[i] = from.add(line.scale(s)).add(side.scale(waveSide)).add(up.scale(waveUp));
            radii[i] = Mth.lerp(s, BASE_RADIUS, TIP_RADIUS);
        }

        // координаты считаем относительно камеры: так нет дрожания вдали от центра мира
        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        for (int i = 0; i < segments; i++) {
            Vec3 a = centers[i];
            Vec3 b = centers[i + 1];
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(a.x, a.y, a.z));
            float v0 = (i % 4) * 0.25F;
            float v1 = v0 + 0.25F;
            for (int k = 0; k < 4; k++) {
                double a0 = k * Math.PI / 2.0;
                double a1 = (k + 1) * Math.PI / 2.0;
                Vec3 o0 = side.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
                Vec3 o1 = side.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
                Vec3 n = o0.add(o1).normalize();
                float u0 = k * 0.25F;
                float u1 = u0 + 0.25F;
                vertex(consumer, matrix, normal, cam, a.add(o0.scale(radii[i])), u0, v0, light, n);
                vertex(consumer, matrix, normal, cam, a.add(o1.scale(radii[i])), u1, v0, light, n);
                vertex(consumer, matrix, normal, cam, b.add(o1.scale(radii[i + 1])), u1, v1, light, n);
                vertex(consumer, matrix, normal, cam, b.add(o0.scale(radii[i + 1])), u0, v1, light, n);
            }
        }
        // острый кончик
        Vec3 end = centers[segments];
        Vec3 point = end.add(dir.scale(0.25));
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(end.x, end.y, end.z));
        for (int k = 0; k < 4; k++) {
            double a0 = k * Math.PI / 2.0;
            double a1 = (k + 1) * Math.PI / 2.0;
            Vec3 o0 = side.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
            Vec3 o1 = side.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
            Vec3 n = o0.add(o1).add(dir).normalize();
            vertex(consumer, matrix, normal, cam, end.add(o0.scale(TIP_RADIUS)), 0.0F, 0.0F, light, n);
            vertex(consumer, matrix, normal, cam, end.add(o1.scale(TIP_RADIUS)), 0.25F, 0.0F, light, n);
            vertex(consumer, matrix, normal, cam, point, 0.25F, 0.25F, light, n);
            vertex(consumer, matrix, normal, cam, point, 0.0F, 0.25F, light, n);
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Vec3 cam, Vec3 p, float u,
                               float v, int light, Vec3 n) {
        consumer.vertex(matrix, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, (float) n.x, (float) n.y, (float) n.z)
                .endVertex();
    }
}
