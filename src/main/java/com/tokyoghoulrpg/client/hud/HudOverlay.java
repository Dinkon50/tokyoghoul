package com.tokyoghoulrpg.client.hud;

import com.tokyoghoulrpg.data.PlayerData;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Компактная панель в левом верхнем углу: класс, уровень, здоровье, голод и ярость. */
public final class HudOverlay {
    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PANEL_W = 122;
    private static final int BAR_W = 108;
    private static final int BAR_H = 5;

    private HudOverlay() {
    }

    public static void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        Optional<PlayerData> optional = player.getCapability(PlayerData.CAP).resolve();
        if (optional.isEmpty()) {
            return;
        }
        PlayerData data = optional.get();
        if (!data.hasChosenClass()) {
            return;
        }

        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        boolean showRage = data.getPlayerClass().hasKagune();
        boolean rageActive = showRage && data.isRageActive();
        int rows = showRage ? 3 : 2;
        int panelH = 18 + rows * 10 + 2 + (rageActive ? 10 : 0);

        // Тёмная панель с красной полосой слева
        gfx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + panelH, 0xB0100608);
        gfx.fill(PANEL_X, PANEL_Y, PANEL_X + 2, PANEL_Y + panelH, 0xFFB01E1E);

        Font font = mc.font;
        gfx.drawString(font, Component.translatable(data.getPlayerClass().translationKey()),
                PANEL_X + 8, PANEL_Y + 4, 0xE6E6E6, false);
        Component level = Component.translatable("hud.tokyoghoulrpg.level", player.experienceLevel);
        gfx.drawString(font, level, PANEL_X + PANEL_W - 6 - font.width(level), PANEL_Y + 4, 0x9A9A9A, false);

        int bx = PANEL_X + 8;
        int by = PANEL_Y + 17;
        drawBar(gfx, bx, by, player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0xFFB01E1E, 0xFF2E0A0A);
        by += 10;
        drawBar(gfx, bx, by, player.getFoodData().getFoodLevel() / 20.0F, 0xFFC9822B, 0xFF33200C);

        if (showRage) {
            by += 10;
            if (rageActive) {
                // Ярость действует: шкала убывает вместе со временем, под ней оставшиеся секунды
                long total = Math.max(1L, data.getRageEndTick() - data.getRageStartTick());
                long left = Math.max(0L, data.getRageEndTick() - now);
                float pulse = 0.8F + 0.2F * Mth.sin((now + partialTick) * 0.4F);
                drawBar(gfx, bx, by, left / (float) total, scale(0xFFFF2A55, pulse), 0xFF2A0A14);
                int seconds = (int) Math.ceil(left / 20.0);
                gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.rage_active", seconds),
                        bx, by + 7, 0xFF5A78, false);
            } else {
                float fraction = data.getRage() / (float) PlayerData.RAGE_MAX;
                int color = 0xFFD01F4A;
                if (data.getRage() >= PlayerData.RAGE_MAX) {
                    // Полная шкала пульсирует: ультимейт готов
                    float pulse = 0.7F + 0.3F * Mth.sin((now + partialTick) * 0.3F);
                    color = scale(0xFFFF2A55, pulse);
                }
                drawBar(gfx, bx, by, fraction, color, 0xFF2A0A14);
            }
        }
    }

    private static void drawBar(GuiGraphics gfx, int x, int y, float fraction, int fill, int back) {
        float clamped = Mth.clamp(fraction, 0.0F, 1.0F);
        gfx.fill(x, y, x + BAR_W, y + BAR_H, back);
        int width = Math.round(BAR_W * clamped);
        if (width > 0) {
            gfx.fill(x, y, x + width, y + BAR_H, fill);
        }
    }

    private static int scale(int argb, float factor) {
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
