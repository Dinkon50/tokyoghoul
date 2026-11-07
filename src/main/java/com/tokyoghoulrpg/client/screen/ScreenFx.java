package com.tokyoghoulrpg.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoulrpg.data.PlayerClass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Общее оформление экранов мода: тема класса (цвета фона, акцент, частицы), мягкое свечение,
 * частицы, рамки панелей, кнопки, крупный текст и фон-картинка класса.
 */
public final class ScreenFx {
    private ScreenFx() {
    }

    /** Цвета экрана под класс игрока. */
    public record Theme(int top, int bottom, int accent, int accent2, int glow, int particle, boolean rising) {
    }

    /** Гуль и полугуль — кровь и искры; человек — сталь и пыль; агент CCG — белый, синий и золото. */
    public static Theme theme(PlayerClass cls) {
        if (cls == PlayerClass.HUMAN) {
            return new Theme(0xFF121C26, 0xFF030507, 0xFF9CC2E0, 0xFF5E86A6, 0x3080B0FF, 0xC8D8E8, false);
        }
        if (cls == PlayerClass.CCG_AGENT) {
            return new Theme(0xFF161A22, 0xFF040508, 0xFFE8ECF4, 0xFFD8B060, 0x30C8D8FF, 0xF0F4FF, false);
        }
        return new Theme(0xFF22050A, 0xFF040102, 0xFFE0303A, 0xFFFFB040, 0x30FF2030, 0xE0303A, true);
    }

    public static int mix(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int alpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Мягкое овальное свечение из колец. */
    public static void glow(GuiGraphics gfx, int cx, int cy, int radius, int argb) {
        int a = argb >>> 24;
        int rgb = argb & 0xFFFFFF;
        int rings = 10;
        for (int i = rings; i >= 1; i--) {
            int r = radius * i / rings;
            int ry = r * 3 / 4;
            gfx.fill(cx - r, cy - ry, cx + r, cy + ry, ((a * (rings - i + 1) / (rings * 3)) << 24) | rgb);
        }
    }

    /** Частицы по всей области: искры поднимаются (гуль) или пыль медленно оседает (человек, CCG). */
    public static void particles(GuiGraphics gfx, int x0, int x1, int h, float time, int count, Theme theme, float strength) {
        int span = Math.max(1, x1 - x0);
        for (int i = 0; i < count; i++) {
            float seed = i * 12.9898F;
            float rx = fract(Mth.sin(seed) * 43758.545F);
            float speed = 0.04F + 0.08F * fract(Mth.sin(seed * 1.7F) * 2345.17F);
            float phase = fract(Mth.sin(seed * 3.1F) * 9821.3F);
            float t = fract(phase + time * speed * (theme.rising() ? 1.0F : 0.45F));
            float y = theme.rising() ? h * (1.0F - t) : h * t;
            int px = x0 + (int) (rx * span + Mth.sin(time * 1.3F + i) * 6.0F);
            if (px < x0 || px >= x1) {
                continue;
            }
            int a = (int) (200 * Mth.sin(t * Mth.PI) * strength);
            int size = 1 + (i % 3 == 0 ? 1 : 0);
            int rgb = theme.rising() && i % 4 == 0 ? (theme.accent2() & 0xFFFFFF) : theme.particle();
            gfx.fill(px, (int) y, px + size, (int) y + size, alpha(rgb, a));
        }
    }

    /** Тёмная панель с тонкой рамкой акцентного цвета, тенью и светлой полосой сверху. */
    public static void panel(GuiGraphics gfx, int x, int y, int w, int h, int accent, float hover) {
        int rgb = accent & 0xFFFFFF;
        gfx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
        if (hover > 0.02F) {
            gfx.fill(x - 2, y - 2, x + w + 2, y + h + 2, alpha(rgb, (int) (0x40 * hover)));
        }
        gfx.fillGradient(x, y, x + w, y + h, mix(0xF00E0A0B, 0xF0000000 | rgb, 0.07F), 0xF0050405);
        frame(gfx, x, y, w, h, alpha(rgb, (int) (0x60 + 0x9F * hover)));
        gfx.fillGradient(x + 1, y + 1, x + w - 1, y + 16, alpha(rgb, (int) (0x28 + 0x28 * hover)), 0x00000000);
    }

    public static void frame(GuiGraphics gfx, int x, int y, int w, int h, int argb) {
        gfx.fill(x, y, x + w, y + 1, argb);
        gfx.fill(x, y + h - 1, x + w, y + h, argb);
        gfx.fill(x, y, x + 1, y + h, argb);
        gfx.fill(x + w - 1, y, x + w, y + h, argb);
    }

    /** Крупный текст по центру с тенью. */
    public static void bigText(GuiGraphics gfx, Font font, Component text, int cx, int y, float scale, int color, int shadow) {
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(cx, y, 400.0F);
        ps.scale(scale, scale, 1.0F);
        int tw = font.width(text);
        gfx.drawString(font, text, -tw / 2 + 1, 1, shadow, false);
        gfx.drawString(font, text, -tw / 2, 0, color, false);
        ps.popPose();
    }

    /** Плашка-«чип»: иконка-ромб цвета и текст. Возвращает ширину. */
    public static int chip(GuiGraphics gfx, Font font, Component text, int x, int y, int color) {
        int w = font.width(text) + 16;
        gfx.fill(x, y, x + w, y + 13, 0xA0000000);
        frame(gfx, x, y, w, 13, alpha(color, 0x70));
        gfx.fill(x + 4, y + 5, x + 7, y + 8, 0xFF000000 | color);
        gfx.fill(x + 5, y + 4, x + 6, y + 9, 0xFF000000 | color);
        gfx.drawString(font, text, x + 11, y + 3, 0xFF000000 | color, false);
        return w;
    }

    /** Картинка «кого прокачиваешь» (по классу и кагуне) слева и зеркально справа, уходящая в темноту внизу. */
    public static void backdrop(GuiGraphics gfx, int width, int height, com.tokyoghoulrpg.data.PlayerData data,
            float time, Theme theme, float opacity) {
        if (data == null) {
            return;
        }
        String name = switch (data.getPlayerClass()) {
            case HUMAN -> "human";
            case CCG_AGENT -> "ccg";
            case GHOUL, HALF_GHOUL -> switch (data.getKaguneVariant()) {
                case WINGS -> "wings";
                case GOURMET -> "gourmet";
                default -> "kaneki";
            };
            default -> null;
        };
        if (name == null) {
            return;
        }
        net.minecraft.resources.ResourceLocation image = new net.minecraft.resources.ResourceLocation(
                com.tokyoghoulrpg.TokyoGhoulRPG.MOD_ID, "textures/gui/skill/bg_" + name + ".png");
        float breathe = 1.0F + 0.012F * Mth.sin(time * 1.2F);
        int size = Math.round(Math.min(height * 1.05F, width * 0.55F) * breathe);
        int y = height - size + size / 40;
        int leftCenter = Math.round(width * 0.17F);
        int rightCenter = Math.round(width * 0.83F);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        gfx.blit(image, leftCenter - size / 2, y, size, size, 0, 0, 512, 512, 512, 512);
        gfx.pose().pushPose();
        gfx.pose().translate(rightCenter + size / 2.0F, 0.0F, 0.0F);
        gfx.pose().scale(-1.0F, 1.0F, 1.0F);
        gfx.blit(image, 0, y, size, size, 0, 0, 512, 512, 512, 512);
        gfx.pose().popPose();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        gfx.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xC0000000 | (theme.bottom() & 0xFFFFFF));
        gfx.fill(0, 0, width, height, alpha(theme.top(), 0x38));
    }

    /**
     * Кнопка в стиле мода: тёмная плашка с рамкой цвета акцента, на наведении подсвечивается и под текстом
     * растягивается полоска. Плашка непрозрачная: рисуется поверх обычной кнопки.
     */
    public static void button(GuiGraphics gfx, Font font, int x, int y, int w, int h, Component msg, int accent,
            boolean active, float hover) {
        int rgb = accent & 0xFFFFFF;
        if (active && hover > 0.02F) {
            gfx.fill(x - 2, y - 2, x + w + 2, y + h + 2, alpha(rgb, (int) (0x34 * hover)));
            gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, alpha(rgb, (int) (0x30 * hover)));
        }
        int top = active ? mix(0xFF1C1517, 0xFF000000 | rgb, 0.10F + 0.24F * hover) : 0xFF151515;
        int bottom = active ? mix(0xFF0A0809, 0xFF000000 | rgb, 0.03F + 0.10F * hover) : 0xFF0C0C0C;
        gfx.fillGradient(x, y, x + w, y + h, top, bottom);
        frame(gfx, x, y, w, h, active ? mix(0xFF000000 | mix(0x0E0A0B, rgb, 0.55F), 0xFF000000 | rgb, hover)
                : 0xFF2A2A2A);
        gfx.fill(x + 1, y + 1, x + w - 1, y + 2, active ? alpha(0xFFFFFF, 0x12 + (int) (0x22 * hover)) : 0x0CFFFFFF);
        if (active) {
            int lw = (int) ((w - 6) * (0.22F + 0.78F * hover));
            int cx = x + w / 2;
            gfx.fill(cx - lw / 2, y + h - 2, cx + lw / 2, y + h - 1, alpha(rgb, 0x90 + (int) (0x6F * hover)));
            // уголки
            int c = alpha(rgb, 0x80 + (int) (0x7F * hover));
            gfx.fill(x, y, x + 3, y + 1, c);
            gfx.fill(x, y, x + 1, y + 3, c);
            gfx.fill(x + w - 3, y + h - 1, x + w, y + h, c);
            gfx.fill(x + w - 1, y + h - 3, x + w, y + h, c);
        }
        int color = !active ? 0xFF6A6A6A : mix(0xFFD6D2D2, 0xFFFFFFFF, hover);
        int tw = font.width(msg);
        int max = w - 8;
        if (tw <= max) {
            gfx.drawString(font, msg, x + (w - tw) / 2, y + (h - 8) / 2 + 1 - (h <= 14 ? 1 : 0), color, active);
        } else {
            float sc = max / (float) Math.max(1, tw);
            PoseStack ps = gfx.pose();
            ps.pushPose();
            ps.translate(x + w / 2.0F, y + h / 2.0F, 0.0F);
            ps.scale(sc, sc, 1.0F);
            gfx.drawString(font, msg, -tw / 2, -4, color, active);
            ps.popPose();
        }
    }

    /** Уголки-скобки по углам панели. */
    public static void corners(GuiGraphics gfx, int x, int y, int w, int h, int argb, int len) {
        gfx.fill(x - 1, y - 1, x + len, y, argb);
        gfx.fill(x - 1, y - 1, x, y + len, argb);
        gfx.fill(x + w - len, y - 1, x + w + 1, y, argb);
        gfx.fill(x + w, y - 1, x + w + 1, y + len, argb);
        gfx.fill(x - 1, y + h, x + len, y + h + 1, argb);
        gfx.fill(x - 1, y + h - len, x, y + h + 1, argb);
        gfx.fill(x + w - len, y + h, x + w + 1, y + h + 1, argb);
        gfx.fill(x + w, y + h - len, x + w + 1, y + h + 1, argb);
    }

    public static float fract(float v) {
        return v - Mth.floor(v);
    }
}
