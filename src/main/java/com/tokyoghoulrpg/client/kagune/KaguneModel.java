package com.tokyoghoulrpg.client.kagune;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Модель, анимации и текстура кагуне Канеки. Файлы лежат в resources:
 * geo/kaneki_crimson_kagune.geo.json, animations/kaneki_crimson_kagune.animation.json,
 * textures/kagune/kagune_crimson.png.
 * Для варианта «Авторский» отдельной модели пока нет, поэтому он использует эту.
 * Укаку — своя модель (огненные крылья): geo/ukaku_kagune.geo.json, textures/kagune/ukaku_kagune.png.
 * Анимаций у крыльев пока нет: в animations/ukaku_kagune.animation.json те же имена, но пустые.
 * «Гурман» (Цукияма) — geo/gourmet_kagune.geo.json, textures/kagune/gourmet_kagune.png, анимации тоже пока пустые.
 */
public class KaguneModel extends GeoModel<KaguneAnimatable> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "geo/kaneki_crimson_kagune.geo.json");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "animations/kaneki_crimson_kagune.animation.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/kagune_crimson.png");

    private static final ResourceLocation UKAKU_MODEL =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "geo/ukaku_kagune.geo.json");
    private static final ResourceLocation UKAKU_ANIMATIONS =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "animations/ukaku_kagune.animation.json");
    private static final ResourceLocation UKAKU_TEXTURE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/ukaku_kagune.png");

    private static final ResourceLocation GOURMET_MODEL =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "geo/gourmet_kagune.geo.json");
    private static final ResourceLocation GOURMET_ANIMATIONS =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "animations/gourmet_kagune.animation.json");
    private static final ResourceLocation GOURMET_TEXTURE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/gourmet_kagune.png");

    private static boolean ukaku(KaguneAnimatable animatable) {
        return animatable.getVariant() == com.tokyoghoulrpg.data.KaguneVariant.WINGS;
    }

    private static boolean gourmet(KaguneAnimatable animatable) {
        return animatable.getVariant() == com.tokyoghoulrpg.data.KaguneVariant.GOURMET;
    }

    @Override
    public ResourceLocation getModelResource(KaguneAnimatable animatable) {
        return gourmet(animatable) ? GOURMET_MODEL : ukaku(animatable) ? UKAKU_MODEL : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KaguneAnimatable animatable) {
        return gourmet(animatable) ? GOURMET_TEXTURE : ukaku(animatable) ? UKAKU_TEXTURE : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(KaguneAnimatable animatable) {
        return gourmet(animatable) ? GOURMET_ANIMATIONS : ukaku(animatable) ? UKAKU_ANIMATIONS : ANIMATIONS;
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
