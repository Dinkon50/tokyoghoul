package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Своё кагуне от первого лица и в VR.
 * <p>
 * Когда смотришь от первого лица (и в шлеме Vivecraft), Minecraft не рисует модель своего игрока,
 * а значит, и кагуне на ней. Тогда рисуем кагуне сами, прямо в мире, в том же месте, что и слой KaguneLayer:
 * основание на спине, поворот по телу (а не по голове), при приседании наклон вместе с телом.
 * Поэтому оно всегда растёт из спины.
 * <p>
 * Чтобы не отвлекать, от первого лица кагуне видно только в действии (удары, барраж, захваты, рывок, блок,
 * копание, появление и убирание). Показывать всегда можно в настройках (kaguneFirstPersonAlways).
 * <p>
 * Если модель своего игрока всё же нарисована в этом проходе (третье лицо, зеркало или камера Vivecraft),
 * второй раз не рисуем.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class KaguneFirstPerson {
    /** Масштаб модели игрока (PlayerRenderer.scale). */
    private static final float PLAYER_SCALE = 0.9375F;

    /** Модель своего игрока с кагуне уже нарисована в текущем проходе отрисовки мира. */
    private static boolean drawnByModel;

    private static KaguneRenderer renderer;
    private static KaguneVeinsRenderer veinsRenderer;

    private KaguneFirstPerson() {
    }

    private static boolean alwaysVisible() {
        try {
            return com.tokyoghoulrpg.ClientConfig.KAGUNE_FIRST_PERSON_ALWAYS.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    static void markDrawnByModel(AbstractClientPlayer player) {
        if (player == Minecraft.getInstance().player) {
            drawnByModel = true;
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Начало прохода (в VR их несколько за кадр: по одному на глаз): сущности ещё не рисовались
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            drawnByModel = false;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        boolean skip = drawnByModel;
        drawnByModel = false;
        if (skip) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.getCameraEntity() != player
                || player.isInvisible() || player.isSpectator() || player.isSleeping()) {
            return;
        }
        KaguneAnimatable kagune = KaguneClientState.get(player.getId());
        if (kagune == null || !kagune.shouldRender()) {
            return;
        }
        if (renderer == null) {
            renderer = new KaguneRenderer();
            veinsRenderer = new KaguneVeinsRenderer();
        }

        float partial = event.getPartialTick();
        boolean breaking = mc.gameMode != null && mc.gameMode.isDestroying();
        kagune.setMotion(player.walkAnimation.speed(partial) > 0.15F, breaking);
        // Пока кагуне просто включено (стоишь, идёшь, ярость), от первого лица его не видно, чтобы не отвлекало.
        // Видно только в действии: удары, барраж, захваты, рывок, блок, копание, появление и убирание.
        if (!alwaysVisible() && !kagune.visibleInFirstPerson()) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        double x = Mth.lerp(partial, player.xo, player.getX());
        double y = Mth.lerp(partial, player.yo, player.getY());
        double z = Mth.lerp(partial, player.zo, player.getZ());
        boolean crouching = player.isCrouching();
        if (crouching) {
            // как PlayerRenderer.getRenderOffset: присевший игрок нарисован чуть ниже
            y -= 0.125;
        }
        float bodyYaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(player.getEyePosition(partial)));

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        poseStack.pushPose();
        poseStack.translate(x - cam.x, y - cam.y, z - cam.z);
        // То же, что делает LivingEntityRenderer перед слоями: поворот по телу, переворот модели, масштаб игрока
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        // То же, что body.translateAndRotate у модели игрока (HumanoidModel: при приседании тело наклонено)
        if (crouching) {
            poseStack.translate(0.0F, 3.2F / 16.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotation(0.5F));
        }
        KaguneLayer.renderOnBody(poseStack, buffers, light, kagune, renderer, veinsRenderer);
        poseStack.popPose();
        buffers.endBatch();
    }
}
