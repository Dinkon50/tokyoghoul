package com.tokyoghoulrpg.mixin;

import com.tokyoghoulrpg.client.kagune.KaguneAnimatable;
import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import com.tokyoghoulrpg.data.KaguneVariant;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Блок «Гурмана»: обе руки поднимаются перед лицом и скрещиваются, как блок в файтингах; кагуне на правой руке
 * идёт вместе с рукой (его виток сжимается, кончик загибается над кулаком — kagune_block_*).
 * Позу рук ставим после обычной (в конце setupAnim), рукава копируют руки.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Unique
    private static final float TOKYOGHOULRPG_GUARD_X = (float) Math.toRadians(-120.0);
    @Unique
    private static final float TOKYOGHOULRPG_GUARD_Y = (float) Math.toRadians(40.0);

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), require = 0)
    private void tokyoghoulrpg$gourmetGuard(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        KaguneAnimatable kagune = KaguneClientState.get(entity.getId());
        if (kagune == null || kagune.getVariant() != KaguneVariant.GOURMET || !kagune.isBlocking()
                || !kagune.shouldRender()) {
            return;
        }
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        model.rightArm.xRot = TOKYOGHOULRPG_GUARD_X + model.body.xRot;
        model.rightArm.yRot = -TOKYOGHOULRPG_GUARD_Y;
        model.rightArm.zRot = 0.0F;
        model.leftArm.xRot = TOKYOGHOULRPG_GUARD_X + model.body.xRot;
        model.leftArm.yRot = TOKYOGHOULRPG_GUARD_Y;
        model.leftArm.zRot = 0.0F;
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }
}
