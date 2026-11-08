package com.tokyoghoulrpg.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.screen.ScreenFx;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Полоски здоровья боссов в стиле мода (вместо полосок Minecraft): тёмная плашка с рамкой и уголками цвета босса,
 * имя крупнее с орнаментом, полоса с градиентом, бликом и делениями, светлый «след» от урона, проценты справа,
 * при малом здоровье — пульсация. Цвет — по цвету полоски: красный — гуль (и вторая стадия полугуля),
 * фиолетовый — полугуль, белый — агент CCG (сталь); у остальных (Иссушитель, дракон) — их цвет.
 * Таймеры мода (арена, кафе) — тонкой полоской с названием.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class BossBarOverlay {
    private static final int WIDTH = 140;
    private static final int BAR_H = 5;

    private BossBarOverlay() {
    }

    private record Style(int top, int bottom, int accent) {
    }

    private static Style style(BossEvent.BossBarColor color) {
        return switch (color) {
            case RED -> new Style(0xFF4650, 0x7E0A16, 0xE0303A);
            case PURPLE -> new Style(0xD0407A, 0x500A2C, 0xC8457E);
            case WHITE -> new Style(0xEEF3F8, 0x6E7E8C, 0x9DB8C8);
            case BLUE -> new Style(0x6AB0F0, 0x1E4A80, 0x5A9AE0);
            case GREEN -> new Style(0x8AD860, 0x2A6A18, 0x6AC048);
            case YELLOW -> new Style(0xFFD260, 0x9A6A10, 0xE8B040);
            case PINK -> new Style(0xF08AD0, 0x7A2A6A, 0xE070C0);
        };
    }

    private static boolean isTimer(Component name) {
        if (name.getContents() instanceof TranslatableContents t) {
            String key = t.getKey();
            return key.startsWith("bossbar.tokyoghoulrpg.") && (key.endsWith(".respawn") || key.contains(".closing"));
        }
        return false;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        GuiGraphics gfx = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int cx = screenW / 2;
        int y = event.getY();
        Style st = style(boss.getColor());
        event.setCanceled(true);
        if (isTimer(boss.getName())) {
            timer(gfx, font, boss, cx, y, st);
            event.setIncrement(16);
            return;
        }
        float progress = Mth.clamp(boss.getProgress(), 0.0F, 1.0F);
        float ghost = ghost(boss.getId(), progress);
        float time = (Util.getMillis() % 3_600_000L) / 1000.0F;
        float pulse = progress <= 0.25F ? 0.75F + 0.25F * Mth.sin(time * 7.0F) : 1.0F;

        // имя: крупнее, с тенью, по бокам гаснущие линии с ромбами
        Component name = boss.getName();
        float s = 0.8F;
        int nw = Math.round(font.width(name) * s);
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(cx - nw / 2.0F, y - 1, 0.0F);
        ps.scale(s, s, 1.0F);
        gfx.drawString(font, name, 1, 1, 0xC0000000 | ScreenFx.mix(0x000000, st.accent(), 0.2F), false);
        gfx.drawString(font, name, 0, 0, 0xFF000000 | ScreenFx.mix(0xFFFFFF, st.accent(), 0.3F), false);
        ps.popPose();
        int ly = y + 3;
        int half = nw / 2 + 6;
        for (int i = 0; i < 10; i++) {
            int a = 0xB0 * (10 - i) / 10;
            int x0 = cx + half + 24 * i / 10;
            int x1 = cx + half + 24 * (i + 1) / 10;
            gfx.fill(x0, ly, x1, ly + 1, ScreenFx.alpha(st.accent(), a));
            gfx.fill(2 * cx - x1, ly, 2 * cx - x0, ly + 1, ScreenFx.alpha(st.accent(), a));
        }
        diamond(gfx, cx - half - 2, ly, st.accent());
        diamond(gfx, cx + half + 2, ly, st.accent());

        // плашка
        int x = cx - WIDTH / 2;
        int by = y + 10;
        gfx.fill(x - 2, by - 2, x + WIDTH + 2, by + BAR_H + 2, 0xC0080304);
        ScreenFx.frame(gfx, x - 2, by - 2, WIDTH + 4, BAR_H + 4, ScreenFx.alpha(st.accent(), 0x70));
        ScreenFx.corners(gfx, x - 2, by - 2, WIDTH + 4, BAR_H + 4, 0xFF000000 | st.accent(), 3);
        // полоса
        gfx.fill(x - 1, by - 1, x + WIDTH + 1, by + BAR_H + 1, 0xFF000000);
        gfx.fillGradient(x, by, x + WIDTH, by + BAR_H, 0xFF1A0C0E, 0xFF0A0506);
        int fw = Math.round(WIDTH * progress);
        int gw = Math.round(WIDTH * ghost);
        if (gw > fw) {
            gfx.fill(x + fw, by, x + gw, by + BAR_H, 0xB0F0C8C8);
        }
        if (fw > 0) {
            gfx.fillGradient(x, by, x + fw, by + BAR_H, 0xFF000000 | scale(st.top(), pulse),
                    0xFF000000 | scale(st.bottom(), pulse));
            gfx.fill(x, by, x + fw, by + 1, 0x70FFFFFF);
            gfx.fill(x, by + BAR_H - 1, x + fw, by + BAR_H, 0x60000000);
            gfx.fill(x + fw - 1, by, x + fw, by + BAR_H, 0xB0FFFFFF);
        }
        // деления (по 10 %), у отметок фаз полоски Minecraft — тоже по 10
        int notches = boss.getOverlay() == BossEvent.BossBarOverlay.NOTCHED_6 ? 6
                : boss.getOverlay() == BossEvent.BossBarOverlay.NOTCHED_12 ? 12
                : boss.getOverlay() == BossEvent.BossBarOverlay.NOTCHED_20 ? 20 : 10;
        for (int i = 1; i < notches; i++) {
            int tx = x + WIDTH * i / notches;
            gfx.fill(tx, by + 1, tx + 1, by + BAR_H - 1, 0x50000000);
        }
        // проценты — мелко справа от плашки (полоса теперь тонкая)
        String pct = Math.round(progress * 100) + "%";
        ps.pushPose();
        ps.translate(x + WIDTH + 6, by - 0.5F, 0.0F);
        ps.scale(0.6F, 0.6F, 1.0F);
        gfx.drawString(font, pct, 0, 0, 0xFFFFFFFF, true);
        ps.popPose();

        event.setIncrement(BAR_H + 14);
    }

    /** Таймер мода (арена, кафе): название и тонкая полоска. */
    private static void timer(GuiGraphics gfx, Font font, LerpingBossEvent boss, int cx, int y, Style st) {
        Component name = boss.getName();
        gfx.drawCenteredString(font, name, cx, y - 1, 0xFF000000 | ScreenFx.mix(0xFFFFFF, st.accent(), 0.3F));
        int w = 120;
        int x = cx - w / 2;
        int by = y + 9;
        gfx.fill(x - 1, by - 1, x + w + 1, by + 4, 0xFF000000);
        gfx.fill(x, by, x + w, by + 3, 0xFF1A0C0E);
        int fw = Math.round(w * Mth.clamp(boss.getProgress(), 0.0F, 1.0F));
        gfx.fillGradient(x, by, x + fw, by + 3, 0xFF000000 | st.top(), 0xFF000000 | st.bottom());
    }

    /** След урона одной полоски: где светлая часть сейчас, прошлое здоровье и когда был последний урон. */
    private static final class Ghost {
        float trail;
        float last;
        long dropAt;
    }

    private static final Map<UUID, Ghost> TRAILS = new HashMap<>();

    private static float ghost(UUID id, float progress) {
        long now = Util.getMillis();
        Ghost g = TRAILS.get(id);
        if (g == null) {
            if (TRAILS.size() > 32) {
                TRAILS.clear();
            }
            g = new Ghost();
            g.trail = progress;
            g.last = progress;
            TRAILS.put(id, g);
        }
        if (progress < g.last - 1.0E-4F) {
            g.dropAt = now;
        }
        g.last = progress;
        if (progress >= g.trail) {
            g.trail = progress;
        } else if (now - g.dropAt > 500L) {
            g.trail = Math.max(progress, g.trail - Math.max(0.003F, (g.trail - progress) * 0.06F));
        }
        return g.trail;
    }

    private static void diamond(GuiGraphics gfx, int x, int y, int rgb) {
        gfx.fill(x - 1, y - 2, x + 2, y + 3, 0xFF000000 | rgb);
        gfx.fill(x - 2, y - 1, x + 3, y + 2, 0xFF000000 | rgb);
    }

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
        int b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }
}
