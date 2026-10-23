package com.tokyoghoulrpg.client.kagune;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Модель, анимации и текстура кагуне Канеки. Файлы лежат в resources:
 * geo/kaneki_crimson_kagune.geo.json, animations/kaneki_crimson_kagune.animation.json,
 * textures/kagune/kagune_crimson.png.
 * Для варианта «Авторский» отдельной модели пока нет, поэтому оба варианта используют эту.
 */
public class KaguneModel extends GeoModel<KaguneAnimatable> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "geo/kaneki_crimson_kagune.geo.json");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "animations/kaneki_crimson_kagune.animation.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/kagune_crimson.png");

    @Override
    public ResourceLocation getModelResource(KaguneAnimatable animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KaguneAnimatable animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(KaguneAnimatable animatable) {
        return ANIMATIONS;
    }

    /** Щупальце захвата: на время захвата его нет на модели — вместо него к цели тянется длинное (KaguneReach). */
    static final String REACH_BONE = "upper_left";

    @Override
    public void setCustomAnimations(KaguneAnimatable animatable, long instanceId,
                                    software.bernie.geckolib.core.animation.AnimationState<KaguneAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.core.animatable.model.CoreGeoBone bone = getAnimationProcessor().getBone(REACH_BONE);
        if (bone != null) {
            bone.setHidden(animatable.isReachTentacleHidden());
        }
    }
}
