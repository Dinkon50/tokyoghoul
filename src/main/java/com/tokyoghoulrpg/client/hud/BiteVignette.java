package com.tokyoghoulrpg.client.hud;

import com.tokyoghoulrpg.combat.Balance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Красные края экрана после укуса. Середина экрана остаётся чистой: краснеют только полосы
 * по бокам, сверху и снизу, от тёмно-красного у края к прозрачному внутрь. Через 1,5 секунды гаснет.
 */
public final class BiteVignette {
    private static long startTick = Long.MIN_VALUE;

    private BiteVignette() {
    }

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            startTick = mc.level.getGameTime();
        }
    }

    public static void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) {
            return;
        }
        float age = mc.level.getGameTime() - startTick + partialTick;
        float duration = Balance.BITE_SCREEN_FLASH;
        if (age < 0.0F || age >= duration) {
            return;
        }
        // Быстро вспыхивает, держится и плавно гаснет
        float strength = age < 3.0F ? age / 3.0F : 1.0F - (age - 3.0F) / (duration - 3.0F);
        int maxAlpha = Math.round(170 * Math.max(0.0F, strength));
        if (maxAlpha <= 0) {
            return;
        }

        int side = Math.max(24, width / 6);
        int band = Math.max(16, height / 7);
        int red = 0x8A0010;

        // Сверху и снизу: вертикальный градиент
        gfx.fillGradient(0, 0, width, band, color(maxAlpha, red), color(0, red));
        gfx.fillGradient(0, height - band, width, height, color(0, red), color(maxAlpha, red));

        // Слева и справа: полосками по 2 пикселя, прозрачность убывает к середине
        for (int x = 0; x < side; x += 2) {
            float t = 1.0F - x / (float) side;
            int alpha = Math.round(maxAlpha * t * t);
            if (alpha <= 0) {
                continue;
            }
            gfx.fill(x, 0, x + 2, height, color(alpha, red));
            gfx.fill(width - x - 2, 0, width - x, height, color(alpha, red));
        }
    }

    private static int color(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }
}
