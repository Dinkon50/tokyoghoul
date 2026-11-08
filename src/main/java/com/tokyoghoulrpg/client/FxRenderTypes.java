package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Слои для эффектов боссов: GLOW — белые сердцевины, свет складывается (как у молнии); TINT — цветные части,
 * обычная полупрозрачность. Оба не закрывают друг друга и видны с обеих сторон. Наследуем RenderType только ради
 * доступа к его готовым настройкам.
 */
public final class FxRenderTypes extends RenderType {
    public static final RenderType GLOW = create("tokyoghoulrpg_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 8192, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    /** Цветные части эффектов: обычная полупрозрачность, цвет не выгорает на светлом фоне. */
    public static final RenderType TINT = create("tokyoghoulrpg_tint", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 8192, false, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private FxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling,
            boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }
}
