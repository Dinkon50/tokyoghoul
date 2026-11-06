package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.KaguneVariant;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * «Гурман» от первого лица: лента рисуется прямо на руке, которую Minecraft показывает внизу экрана
 * (пустая правая рука). Руку не заменяем: сначала рисуем ленту, потом игра рисует саму руку.
 * Своя рука в этом месте нарисована так же, как PlayerRenderer.renderRightHand: точка плеча (-5, 2, 0),
 * наклон 0,1 рад от покачивания (setupAnim с нулевым временем), xRot = 0.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class GourmetFirstPerson {
    private static KaguneRenderer renderer;
    private static KaguneVeinsRenderer veinsRenderer;

    private GourmetFirstPerson() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (event.getArm() != HumanoidArm.RIGHT) {
            return;
        }
        AbstractClientPlayer player = event.getPlayer();
        if (player == null || player.isInvisible()) {
            return;
        }
        KaguneAnimatable kagune = KaguneClientState.get(player.getId());
        if (kagune == null || !kagune.shouldRender() || kagune.getVariant() != KaguneVariant.GOURMET) {
            return;
        }
        if (renderer == null) {
            renderer = new KaguneRenderer();
            veinsRenderer = new KaguneVeinsRenderer();
        }
        boolean slim = "slim".equals(player.getModelName());
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // то же, что rightArm.translateAndRotate у руки от первого лица
        poseStack.translate(-5.0F / 16.0F, (slim ? 2.5F : 2.0F) / 16.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(0.1F));
        KaguneLayer.renderOnArm(poseStack, event.getMultiBufferSource(), event.getPackedLight(), kagune,
                renderer, veinsRenderer, slim);
        poseStack.popPose();
    }
}
