package com.tokyoghoulrpg.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoulrpg.data.PlayerData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Общая основа окон мода в одном оформлении: фон в цвете класса с картинкой «кого прокачиваешь» по бокам,
 * свечение и частицы, панели с рамкой и уголками, крупный заголовок с орнаментом и кнопки в стиле мода.
 * Обычные кнопки подменять не нужно: поверх каждой {@link Button} рисуется плашка мода, а нажатия,
 * подсказки и доступность остаются у самой кнопки.
 */
public abstract class ModScreen extends Screen {
    /** Красный по умолчанию у старых окон: такие панели берут акцент класса игрока. */
    private static final int DEFAULT_RED = 0xFFB01E1E;

    private int frameAccent;
    private boolean accentSet;

    protected ModScreen(Component title) {
        super(title);
    }

    private static PlayerData modData() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    protected static float modTime() {
        return (Util.getMillis() % 3_600_000L) / 1000.0F;
    }

    protected ScreenFx.Theme modTheme() {
        PlayerData data = modData();
        return ScreenFx.theme(data == null ? null : data.getPlayerClass());
    }

    /** Акцент кнопок этого кадра: цвет первой нарисованной панели, иначе акцент класса. */
    protected int modAccent() {
        return accentSet ? frameAccent : modTheme().accent();
    }

    /** Фон окна вместо чёрной заливки. */
    protected void modBackground(GuiGraphics gfx) {
        accentSet = false;
        ScreenFx.Theme theme = modTheme();
        float time = modTime();
        gfx.fillGradient(0, 0, this.width, this.height, ScreenFx.alpha(theme.top(), 0xE8), ScreenFx.alpha(theme.bottom(), 0xF4));
        ScreenFx.backdrop(gfx, this.width, this.height, modData(), time, theme, 0.42F);
        ScreenFx.glow(gfx, this.width / 2, this.height / 2, (int) (Math.min(this.width, this.height) * 0.6F), theme.glow());
        ScreenFx.particles(gfx, 0, this.width, this.height, time, 50, theme, 0.45F);
        // мягкое затемнение по краям сверху и снизу
        gfx.fillGradient(0, 0, this.width, 28, 0x90000000, 0x00000000);
        gfx.fillGradient(0, this.height - 28, this.width, this.height, 0x00000000, 0x90000000);
    }

    /** Панель окна: тёмная, с рамкой, полосой и уголками цвета акцента. */
    protected void modPanel(GuiGraphics gfx, int x, int y, int w, int h, int accent) {
        int color = accent == DEFAULT_RED ? (0xFF000000 | modTheme().accent()) : accent;
        if (!accentSet) {
            frameAccent = color;
            accentSet = true;
        }
        int rgb = color & 0xFFFFFF;
        ScreenFx.glow(gfx, x + w / 2, y + h / 2, Math.max(w, h) / 2 + 30, ScreenFx.alpha(rgb, 0x22));
        ScreenFx.panel(gfx, x, y, w, h, color, 0.0F);
        gfx.fillGradient(x, y, x + 2, y + h, 0xFF000000 | rgb, ScreenFx.alpha(rgb, 0x50));
        ScreenFx.corners(gfx, x, y, w, h, 0xFF000000 | rgb, 7);
    }

    /** Карточка поменьше внутри окна (строка, выбранный вариант). */
    protected void modCard(GuiGraphics gfx, int x, int y, int w, int h, int accent, float highlight) {
        int rgb = accent & 0xFFFFFF;
        gfx.fillGradient(x, y, x + w, y + h, ScreenFx.alpha(ScreenFx.mix(0x1A1214, rgb, 0.08F + 0.22F * highlight), 0xD0),
                0xC0080606);
        ScreenFx.frame(gfx, x, y, w, h, ScreenFx.alpha(rgb, 0x40 + (int) (0xA0 * highlight)));
        gfx.fill(x, y, x + 2, y + h, ScreenFx.alpha(rgb, 0x90 + (int) (0x6F * highlight)));
    }

    /** Заголовок: крупнее, с тенью и орнаментом-линиями с ромбом снизу. */
    protected void modTitle(GuiGraphics gfx, Component text, int cx, int y, int color) {
        int accent = modAccent() & 0xFFFFFF;
        int rgb = color & 0xFFFFFF;
        if (rgb == 0xC21B1B || rgb == 0xE05050) {
            // старый красный заголовок — в цвет класса
            rgb = accent;
        }
        float scale = 1.25F;
        ScreenFx.bigText(gfx, this.font, text, cx, y - 2, scale, 0xFF000000 | ScreenFx.mix(0xFFFFFF, rgb, 0.65F),
                0xC0000000);
        int half = (int) (this.font.width(text) * scale / 2) + 10;
        int ly = y + 10;
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(0.0F, 0.0F, 400.0F);
        for (int i = 0; i < 8; i++) {
            int x0 = cx + 4 + (half + 22) * i / 8;
            int x1 = cx + 4 + (half + 22) * (i + 1) / 8;
            gfx.fill(x0, ly, x1, ly + 1, ScreenFx.alpha(rgb, 0xC0 * (8 - i) / 8));
        }
        for (int i = 0; i < 8; i++) {
            int x0 = cx - 4 - (half + 22) * (i + 1) / 8;
            int x1 = cx - 4 - (half + 22) * i / 8;
            gfx.fill(x0, ly, x1, ly + 1, ScreenFx.alpha(rgb, 0xC0 * (8 - i) / 8));
        }
        gfx.fill(cx - 1, ly - 2, cx + 2, ly + 3, 0xFF000000 | rgb);
        gfx.fill(cx - 2, ly - 1, cx + 3, ly + 2, 0xFF000000 | rgb);
        ps.popPose();
    }

    @Override
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        T added = super.addRenderableWidget(widget);
        if (widget instanceof Button button && button.getClass() == Button.class) {
            addRenderableOnly(new Skin(button));
        }
        return added;
    }

    /** Плашка мода поверх обычной кнопки с плавной подсветкой на наведении. */
    private final class Skin implements Renderable {
        private final Button button;
        private float hover;

        Skin(Button button) {
            this.button = button;
        }

        @Override
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            if (!button.visible) {
                return;
            }
            boolean hovered = button.active && button.isHoveredOrFocused();
            hover += ((hovered ? 1.0F : 0.0F) - hover) * 0.3F;
            ScreenFx.button(gfx, font, button.getX(), button.getY(), button.getWidth(), button.getHeight(),
                    button.getMessage(), modAccent(), button.active, hover);
        }
    }
}
