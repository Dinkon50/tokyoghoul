package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ульта гуля на экране: четыре щупальца из спины и кровь, по тем же таймингам, что на сервере (Balance.ULTIMATE_*).
 * <ol>
 *   <li>кагуне медленно раскрывается за спиной: верхние поднимаются над плечами, как кобра, нижние выходят к бёдрам;</li>
 *   <li>верхние выстреливают, хватают цель за плечи и держат, пока её поднимают; с плеч капает кровь;</li>
 *   <li>нижние по очереди замахиваются и бьют в живот насквозь (кончик выходит из спины цели) — брызги крови
 *       сзади и спереди, кровь капает с раны;</li>
 *   <li>добивающий: оба нижних долго замахиваются и вместе пробивают цель, остаются в ней;</li>
 *   <li>щупальца вырывают (ещё брызги), цель падает, кагуне втягивается.</li>
 * </ol>
 * Обычная модель кагуне на это время скрыта, чтобы щупалец не было восемь. Камеру у гуля и у цели ведёт
 * UltimateCinematic. Рисуется в мире: видно всем рядом, и от первого лица, и в VR.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class KaguneUltimateFx {
    /** Брызги — маленькие падающие кусочки (красный и тёмно-красный), и облачка крови. */
    private static final ParticleOptions BLOOD_BRIGHT =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
    private static final ParticleOptions BLOOD_DARK =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.defaultBlockState());

    private static final float BASE_RADIUS = 0.21F;
    private static final float TIP_RADIUS = 0.07F;

    /** Одна идущая ульта (ключ — номер сущности гуля). */
    private static final class Ult {
        final int targetId;
        final long start;
        /** Когда отпустили (обычно ULTIMATE_RELEASE; раньше, если ульту прервали). */
        float release = Balance.ULTIMATE_RELEASE;
        /** До какого тика уже сделаны брызги и капли. */
        int doneTick = -1;

        Ult(int targetId, long start) {
            this.targetId = targetId;
            this.start = start;
        }
    }

    private static final Map<Integer, Ult> ACTIVE = new HashMap<>();

    private KaguneUltimateFx() {
    }

    /** Сервер: ульта началась (targetId) или прервалась (-1). */
    public static void start(int ownerId, int targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = mc.level.getGameTime();
        if (targetId < 0) {
            Ult old = ACTIVE.get(ownerId);
            if (old != null) {
                old.release = Math.min(old.release, now - old.start);
                KaguneClientState.hide(ownerId, old.start + (long) Math.ceil(old.release) + Balance.ULTIMATE_RETRACT);
                UltimateCinematic.stop(ownerId, old.start + (long) Math.ceil(old.release));
            }
            return;
        }
        ACTIVE.put(ownerId, new Ult(targetId, now));
        KaguneClientState.hide(ownerId, now + Balance.ULTIMATE_RELEASE + Balance.ULTIMATE_RETRACT);
        UltimateCinematic.start(ownerId, targetId, now);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    // ------------------------------------------------------------------ положения

    /** Всё, что нужно в этот момент: тело гуля, корни щупалец, точки на цели. */
    private record Frame(Vec3 forward, Vec3 right, Vec3 upperRoot, Vec3 lowerRoot,
                         Vec3 shoulders, Vec3 belly, Vec3 through, Vec3 away, double targetWidth) {
    }

    private static Frame frame(LivingEntity owner, LivingEntity target, float partial) {
        Vec3 base = lerp(owner, partial);
        float bodyYaw = Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(bodyYaw), 0.0, Mth.cos(bodyYaw));
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 back = forward.scale(-0.20);
        // корни на спине — как у модели кагуне (верхние на ~15-16 пикселях, нижние на ~13)
        Vec3 upperRoot = base.add(0.0, 0.93, 0.0).add(back);
        Vec3 lowerRoot = base.add(0.0, 0.75, 0.0).add(back);
        Vec3 tpos = lerp(target, partial);
        double th = target.getBbHeight();
        double tw = target.getBbWidth();
        Vec3 toTarget = new Vec3(tpos.x - base.x, 0.0, tpos.z - base.z);
        Vec3 away = toTarget.lengthSqr() < 1.0E-4 ? forward : toTarget.normalize();
        Vec3 shoulders = tpos.add(0.0, th * 0.82, 0.0);
        Vec3 belly = tpos.add(0.0, th * 0.50, 0.0).subtract(away.scale(tw * 0.5));
        Vec3 through = belly.add(away.scale(tw * 1.25));
        return new Frame(forward, right, upperRoot, lowerRoot, shoulders, belly, through, away, tw);
    }

    private static Vec3 lerp(Entity e, float partial) {
        return new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()),
                Mth.lerp(partial, e.zo, e.getZ()));
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static Vec3 mix(Vec3 a, Vec3 b, float k) {
        return a.lerp(b, k);
    }

    // ------------------------------------------------------------------ рисование

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        float partial = event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType type = RenderType.entityCutoutNoCull(KaguneReach.TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(type);
        boolean drawn = false;

        Iterator<Map.Entry<Integer, Ult>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Ult> entry = it.next();
            Ult ult = entry.getValue();
            float t = (level.getGameTime() - ult.start) + partial;
            Entity ownerEntity = level.getEntity(entry.getKey());
            Entity targetEntity = level.getEntity(ult.targetId);
            if (t > ult.release + Balance.ULTIMATE_RETRACT || !(ownerEntity instanceof LivingEntity owner)
                    || !(targetEntity instanceof LivingEntity target)) {
                it.remove();
                continue;
            }
            Frame f = frame(owner, target, partial);
            for (int s = -1; s <= 1; s += 2) {
                drawUpper(level, poseStack, consumer, cam, f, s, t, ult.release);
                drawLower(level, poseStack, consumer, cam, f, s, t, ult.release);
            }
            drawn = true;
        }
        if (drawn) {
            buffers.endBatch(type);
        }
    }

    /** Верхнее щупальце (s = +1 справа, -1 слева). */
    private static void drawUpper(ClientLevel level, PoseStack ps, VertexConsumer c, Vec3 cam, Frame f, int s,
                                  float t, float release) {
        Vec3 root = f.upperRoot().add(f.right().scale(0.12 * s));
        // поднято над плечом и чуть назад, как у кобры; медленно покачивается
        double sway = Mth.sin(t * 0.25F + s) * 0.08;
        Vec3 rest = root.add(0.0, 1.5 + sway, 0.0).add(f.forward().scale(-0.45)).add(f.right().scale(0.75 * s));
        Vec3 restBend = root.add(0.0, 0.7, 0.0).add(f.forward().scale(-0.3)).add(f.right().scale(0.45 * s));
        // к цели: над плечом и вперёд, к плечу цели
        Vec3 grip = f.shoulders().add(f.right().scale(f.targetWidth() * 0.5 * s));
        Vec3 grabBend = root.add(0.0, 0.9, 0.0).add(f.forward().scale(0.15)).add(f.right().scale(0.45 * s));

        Vec3 to;
        Vec3 bend;
        float extend;
        float shoot = Balance.ULTIMATE_SHOOT;
        float latch = Balance.ULTIMATE_LATCH;
        if (t < shoot) {
            to = rest;
            bend = restBend;
            extend = smooth(t / shoot);
        } else if (t < latch && t < release) {
            float k = (t - shoot) / (latch - shoot);
            k = k * k;                          // разгон: удар в конце
            to = mix(rest, grip, k);
            bend = mix(restBend, grabBend, k);
            extend = 1.0F;
        } else if (t < release) {
            // держит; лёгкое сжатие
            to = grip.add(0.0, Mth.sin(t * 0.6F + s) * 0.02, 0.0);
            bend = grabBend;
            extend = 1.0F;
        } else {
            float k = (t - release) / Balance.ULTIMATE_RETRACT;
            to = mix(grip, rest, smooth(k * 2.0F));
            bend = mix(grabBend, restBend, smooth(k * 2.0F));
            extend = 1.0F - smooth((k - 0.5F) * 2.0F);
        }
        if (extend > 0.01F) {
            KaguneReach.drawTentacle(level, ps, c, cam, root, bend, to, extend, t + s * 2.0F, BASE_RADIUS, TIP_RADIUS);
        }
    }

    /** Нижнее щупальце: правое (s = +1) бьёт первым, третьим..., левое — вторым, четвёртым...; добивают оба. */
    private static void drawLower(ClientLevel level, PoseStack ps, VertexConsumer c, Vec3 cam, Frame f, int s,
                                  float t, float release) {
        int which = s > 0 ? 0 : 1;
        Vec3 root = f.lowerRoot().add(f.right().scale(0.13 * s));
        double sway = Mth.sin(t * 0.3F + s * 2) * 0.06;
        // наготове у бедра, кончиком вперёд
        Vec3 rest = root.add(f.right().scale(0.85 * s)).add(f.forward().scale(0.35)).add(0.0, -0.15 + sway, 0.0);
        Vec3 bendRest = root.add(f.right().scale(0.5 * s)).add(f.forward().scale(-0.05));
        Vec3 bendStrike = root.add(f.right().scale(0.45 * s)).add(f.forward().scale(0.3));
        Vec3 windup = rest.add(f.forward().scale(-0.25)).add(0.0, 0.1, 0.0);
        Vec3 deepWindup = rest.add(f.forward().scale(-0.45)).add(0.0, 0.2, 0.0)
                .add(Mth.sin(t * 2.3F) * 0.02, 0.0, Mth.cos(t * 2.1F) * 0.02);   // дрожит от напряжения
        Vec3 through = f.through().add(f.right().scale(0.06 * s));

        float emerge = smooth((t - 10.0F) / 40.0F);
        Vec3 to = rest;
        Vec3 bend = bendRest;
        float extend = emerge;

        // обычные удары
        int[] stabs = Balance.ULTIMATE_STABS;
        for (int i = which; i < stabs.length; i += 2) {
            float dt = t - stabs[i];
            if (stabs[i] > release) {
                break;
            }
            if (dt >= -5.0F && dt < -2.0F) {
                to = mix(rest, windup, smooth((dt + 5.0F) / 3.0F));
            } else if (dt >= -2.0F && dt < 0.0F) {
                to = mix(windup, through, (dt + 2.0F) / 2.0F);
                bend = bendStrike;
            } else if (dt >= 0.0F && dt < 2.0F) {
                to = through;                   // насквозь, на миг остаётся в ране
                bend = bendStrike;
            } else if (dt >= 2.0F && dt < 7.0F) {
                to = mix(through, rest, smooth((dt - 2.0F) / 5.0F));
                bend = mix(bendStrike, bendRest, smooth((dt - 2.0F) / 5.0F));
            }
        }
        // добивающий: долгий замах, оба насквозь и остаются до отпускания
        float fin = Balance.ULTIMATE_FINAL;
        if (fin <= release) {
            float dt = t - fin;
            if (dt >= -12.0F && dt < -2.0F) {
                to = mix(rest, deepWindup, smooth((dt + 12.0F) / 10.0F));
            } else if (dt >= -2.0F && dt < 0.0F) {
                to = mix(deepWindup, through, (dt + 2.0F) / 2.0F);
                bend = bendStrike;
            } else if (dt >= 0.0F && t < release) {
                to = through;
                bend = bendStrike;
            }
        }
        // вырвали и втянули
        if (t >= release) {
            float k = (t - release) / Balance.ULTIMATE_RETRACT;
            Vec3 from = t - fin >= 0.0F && fin <= release ? through : rest;
            to = mix(from, rest, smooth(k * 3.0F));
            bend = mix(bendStrike, bendRest, smooth(k * 3.0F));
            extend = 1.0F - smooth((k - 0.4F) / 0.6F);
        }
        if (extend > 0.01F) {
            KaguneReach.drawTentacle(level, ps, c, cam, root, bend, to, extend, t + s * 3.0F, BASE_RADIUS, TIP_RADIUS);
        }
    }

    // ------------------------------------------------------------------ кровь

    /** Брызги и капли — в нужные тики, у всех, кто видит. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        RandomSource r = level.getRandom();
        for (Map.Entry<Integer, Ult> entry : ACTIVE.entrySet()) {
            Ult ult = entry.getValue();
            int now = (int) (level.getGameTime() - ult.start);
            if (!(level.getEntity(entry.getKey()) instanceof LivingEntity owner)
                    || !(level.getEntity(ult.targetId) instanceof LivingEntity target)) {
                continue;
            }
            Frame f = frame(owner, target, 1.0F);
            for (int tick = ult.doneTick + 1; tick <= now; tick++) {
                bloodAt(level, r, f, tick, ult.release);
            }
            ult.doneTick = now;
        }
    }

    private static void bloodAt(ClientLevel level, RandomSource r, Frame f, int tick, float release) {
        if (tick > release + 30) {
            return;
        }
        // схватили за плечи
        if (tick == Balance.ULTIMATE_LATCH && tick <= release) {
            for (int s = -1; s <= 1; s += 2) {
                Vec3 at = f.shoulders().add(f.right().scale(f.targetWidth() * 0.5 * s));
                spray(level, r, at, f.right().scale(s).add(0.0, 0.4, 0.0), 10, 0.5, 0.25);
            }
        }
        // удары в живот: насквозь — брызги из спины цели, немного обратно на гуля
        for (int st : Balance.ULTIMATE_STABS) {
            if (tick == st && st <= release) {
                spray(level, r, f.through(), f.away().add(0.0, 0.25, 0.0), 22, 0.8, 0.35);
                spray(level, r, f.belly(), f.away().scale(-1.0).add(0.0, 0.3, 0.0), 8, 0.5, 0.3);
                mist(level, r, f.belly(), 5);
            }
        }
        // добивающий — много крови
        if (tick == Balance.ULTIMATE_FINAL && Balance.ULTIMATE_FINAL <= release) {
            spray(level, r, f.through(), f.away().add(0.0, 0.35, 0.0), 60, 1.0, 0.5);
            spray(level, r, f.belly(), f.away().scale(-1.0).add(0.0, 0.4, 0.0), 22, 0.7, 0.45);
            mist(level, r, f.belly(), 14);
            mist(level, r, f.through(), 10);
        }
        // вырвали щупальца
        if (tick == (int) Math.ceil(release)) {
            spray(level, r, f.belly(), f.away().scale(-1.0).add(0.0, 0.2, 0.0), 30, 0.8, 0.4);
            mist(level, r, f.belly(), 8);
        }
        // кровь капает: с плеч, пока держат, и с раны после первого удара
        if (tick > Balance.ULTIMATE_LATCH && tick < release && tick % 5 == 0) {
            for (int s = -1; s <= 1; s += 2) {
                drip(level, r, f.shoulders().add(f.right().scale(f.targetWidth() * 0.5 * s)));
            }
        }
        if (tick > Balance.ULTIMATE_STABS[0] && tick < release + 30 && tick % 2 == 0) {
            drip(level, r, f.belly());
            if (tick > Balance.ULTIMATE_FINAL && tick < release) {
                drip(level, r, f.through());
            }
        }
    }

    /** Брызги: маленькие падающие капли вдоль direction (с разбросом). */
    private static void spray(ClientLevel level, RandomSource r, Vec3 at, Vec3 direction, int count, double speed,
                              double spread) {
        Vec3 dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : direction.normalize();
        for (int i = 0; i < count; i++) {
            double k = speed * (0.4 + r.nextDouble() * 0.8);
            double vx = dir.x * k + (r.nextDouble() - 0.5) * spread * 2.0;
            double vy = dir.y * k + (r.nextDouble() - 0.5) * spread * 2.0 + 0.1;
            double vz = dir.z * k + (r.nextDouble() - 0.5) * spread * 2.0;
            level.addParticle(r.nextInt(3) == 0 ? BLOOD_BRIGHT : BLOOD_DARK,
                    at.x + (r.nextDouble() - 0.5) * 0.15, at.y + (r.nextDouble() - 0.5) * 0.15,
                    at.z + (r.nextDouble() - 0.5) * 0.15, vx, vy, vz);
        }
    }

    /** Облачко мелкой кровавой пыли. */
    private static void mist(ClientLevel level, RandomSource r, Vec3 at, int count) {
        for (int i = 0; i < count; i++) {
            level.addParticle(Effects.BLOOD, at.x + (r.nextDouble() - 0.5) * 0.4, at.y + (r.nextDouble() - 0.5) * 0.4,
                    at.z + (r.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
        }
    }

    /** Одна капля, падает вниз. */
    private static void drip(ClientLevel level, RandomSource r, Vec3 at) {
        level.addParticle(r.nextBoolean() ? BLOOD_DARK : BLOOD_BRIGHT,
                at.x + (r.nextDouble() - 0.5) * 0.2, at.y - 0.05, at.z + (r.nextDouble() - 0.5) * 0.2,
                0.0, -0.05, 0.0);
    }
}
