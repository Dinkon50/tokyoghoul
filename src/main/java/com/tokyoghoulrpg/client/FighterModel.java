package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.entity.DojoMaster;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Модель Мастера додзё: обычная модель игрока, но в «Стойке» он держит предплечья перед лицом
 * (как боксёр в защите), а когда просто идёт к цели — руки чуть согнуты и подняты.
 */
public class FighterModel<T extends Mob> extends PlayerModel<T> {
    public FighterModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!(entity instanceof DojoMaster master)) {
            return;
        }
        if (master.isGuarding()) {
            float breathe = Mth.sin(ageInTicks * 0.3F) * 0.03F;
            this.rightArm.xRot = -1.75F + breathe + this.body.xRot;
            this.rightArm.yRot = -0.55F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.xRot = -1.75F - breathe + this.body.xRot;
            this.leftArm.yRot = 0.55F;
            this.leftArm.zRot = 0.0F;
        } else if (this.attackTime <= 0.0F) {
            // боевая готовность: кулаки чуть выше пояса
            this.rightArm.xRot += -0.45F;
            this.leftArm.xRot += -0.45F;
            this.rightArm.zRot += 0.08F;
            this.leftArm.zRot -= 0.08F;
        }
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
    }
}
