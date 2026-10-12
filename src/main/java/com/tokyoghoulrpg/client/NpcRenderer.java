package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.entity.SkinVariant;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * NPC мода рисуются моделью игрока со всеми внешними слоями (капюшоны, плащи, объём волос).
 * Скины лежат в textures/entity/<имя>_<номер>.png, размер 128 x 128: это обычная развёртка
 * скина 64 x 64 в двойной детализации. Номер берётся из варианта внешности NPC.
 * Поверх видны маски, одежда и предмет в руке.
 */
public class NpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation[] textures;
    private final float size;

    public NpcRenderer(EntityRendererProvider.Context context, String skin, int variants) {
        this(context, skin, variants, 1.0F);
    }

    /** size: во сколько раз NPC крупнее игрока (боссы ~1.25). */
    public NpcRenderer(EntityRendererProvider.Context context, String skin, int variants, float size) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F * size);
        this.size = size;
        this.textures = new ResourceLocation[Math.max(1, variants)];
        for (int i = 0; i < this.textures.length; i++) {
            this.textures[i] = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/entity/" + skin + "_" + i + ".png");
        }
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        if (size != 1.0F) {
            poseStack.scale(size, size, size);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        int variant = entity instanceof SkinVariant skin ? skin.getSkinVariant() : 0;
        return textures[Mth.clamp(variant, 0, textures.length - 1)];
    }
}
