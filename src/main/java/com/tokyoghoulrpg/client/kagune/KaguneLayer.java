package com.tokyoghoulrpg.client.kagune;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Рисует кагуне за спиной игрока. Подключается к обоим видам скина (обычный и тонкие руки). */
public class KaguneLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    // Если кагуне окажется не на месте, подвинь его здесь (в блоках) или поменяй размер.
    private static final float OFFSET_X = 0.0F;
    private static final float OFFSET_Y = 0.0F;
    private static final float OFFSET_Z = 0.0F;
    private static final float SCALE = 1.0F;

    private final KaguneRenderer renderer = new KaguneRenderer();
    private final KaguneVeinsRenderer veinsRenderer = new KaguneVeinsRenderer();

    public KaguneLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        KaguneAnimatable kagune = KaguneClientState.get(player.getId());
        if (kagune == null || !kagune.shouldRender()) {
            return;
        }

        poseStack.pushPose();
        // Пространство слоя игрока перевёрнуто и опущено. Возвращаем к обычному: начало у ног, ось Y вверх,
        // как в Blockbench. Тогда модель встаёт туда, где её нарисовали относительно тела.
        poseStack.translate(OFFSET_X, 1.501F + OFFSET_Y, OFFSET_Z);
        poseStack.scale(-SCALE, -SCALE, SCALE);
        renderer.render(poseStack, kagune, buffer, null, null, packedLight);
        // Вены рисуем всегда, пока видно кагуне: так их анимация не отстаёт. Без ярости текстура прозрачная.
        veinsRenderer.render(poseStack, kagune.getVeins(), buffer, null, null, packedLight);
        poseStack.popPose();
    }
}
