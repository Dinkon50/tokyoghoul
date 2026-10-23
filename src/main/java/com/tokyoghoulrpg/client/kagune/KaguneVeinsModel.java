package com.tokyoghoulrpg.client.kagune;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Вены ярости. Геометрия собрана из твоего `01_rage.bbmodel` (кубы `rage_veins_*`), анимации те же,
 * что у кагуне. Рисуются светящимися, как глаза паука.
 * <ul>
 * <li>Ярости нет: пустая прозрачная текстура, вен не видно. Модель при этом анимируется,
 * чтобы не отставать от кагуне.</li>
 * <li>Первые тики ярости: статичная светящаяся текстура `kagune_rage_veins_emissive.png` (вспышка).</li>
 * <li>Дальше: анимированная пульсирующая текстура `kagune_rage_veins_animated.png`.</li>
 * </ul>
 */
public class KaguneVeinsModel extends GeoModel<KaguneVeinsAnimatable> {
    /** Сколько тиков держится вспышка в начале ярости. */
    private static final long FLASH_TICKS = 8L;

    private static final ResourceLocation MODEL =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "geo/kaneki_crimson_kagune_veins.geo.json");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "animations/kaneki_crimson_kagune.animation.json");
    private static final ResourceLocation BLANK =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/blank.png");
    private static final ResourceLocation FLASH =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/kagune_rage_veins_emissive.png");
    private static final ResourceLocation PULSE =
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/kagune/kagune_rage_veins_animated.png");

    @Override
    public ResourceLocation getModelResource(KaguneVeinsAnimatable animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(KaguneVeinsAnimatable animatable) {
        KaguneAnimatable main = animatable.getMain();
        if (!main.isRage()) {
            return BLANK;
        }
        return main.rageAge() < FLASH_TICKS ? FLASH : PULSE;
    }

    @Override
    public ResourceLocation getAnimationResource(KaguneVeinsAnimatable animatable) {
        return ANIMATIONS;
    }

    /** Светящийся тип отрисовки: вены видны в темноте и не зависят от освещения. */
    @Override
    public RenderType getRenderType(KaguneVeinsAnimatable animatable, ResourceLocation texture) {
        return RenderType.eyes(texture);
    }

    /** Вены на щупальце захвата прячутся вместе с ним. */
    private static final String REACH_BONE = KaguneModel.REACH_BONE;

    @Override
    public void setCustomAnimations(KaguneVeinsAnimatable animatable, long instanceId,
                                    software.bernie.geckolib.core.animation.AnimationState<KaguneVeinsAnimatable> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        software.bernie.geckolib.core.animatable.model.CoreGeoBone bone = getAnimationProcessor().getBone(REACH_BONE);
        if (bone != null) {
            bone.setHidden(animatable.getMain().isReachTentacleHidden());
        }
    }
}
