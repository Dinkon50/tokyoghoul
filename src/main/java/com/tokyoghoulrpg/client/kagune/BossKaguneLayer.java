package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Кагуне на спине Кровавого полугуля — та же модель и анимации, что у игрока-гуля.
 * Всегда выпущено; во второй стадии светятся вены ярости. Особые атаки босса проигрывают анимации ударов.
 */
public class BossKaguneLayer extends RenderLayer<HalfGhoulBoss, PlayerModel<HalfGhoulBoss>> {
    private final KaguneRenderer renderer = new KaguneRenderer();
    private final KaguneVeinsRenderer veinsRenderer = new KaguneVeinsRenderer();

    public BossKaguneLayer(RenderLayerParent<HalfGhoulBoss, PlayerModel<HalfGhoulBoss>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HalfGhoulBoss boss,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (boss.isInvisible()) {
            return;
        }
        KaguneClientState.applyState(boss.getId(), true, KaguneVariant.KANEKI, boss.getPhase() == 2, false);
        KaguneAnimatable kagune = KaguneClientState.get(boss.getId());
        if (kagune == null) {
            return;
        }
        kagune.setMotion(limbSwingAmount > 0.15F, false);
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        renderer.render(poseStack, kagune, buffer, null, null, packedLight);
        veinsRenderer.render(poseStack, kagune.getVeins(), buffer, null, null, packedLight);
        poseStack.popPose();
    }
}
