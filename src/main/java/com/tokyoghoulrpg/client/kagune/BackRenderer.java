package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/**
 * Рисует модель прямо в пространстве тела игрока.
 * GeoObjectRenderer рассчитан на блоки и перед отрисовкой сдвигает модель на (0,5; 0,51; 0,5) блока,
 * из-за чего кагуне висело сбоку и позади игрока. Здесь этот сдвиг отменяется.
 */
public class BackRenderer<T extends GeoAnimatable> extends GeoObjectRenderer<T> {
    public BackRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);
        poseStack.translate(-0.5F, -0.51F, -0.5F);
    }
}
