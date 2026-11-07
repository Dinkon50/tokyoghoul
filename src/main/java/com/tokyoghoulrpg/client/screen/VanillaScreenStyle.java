package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.PlayerData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Обычные меню Minecraft в стиле мода. Кнопки, ползунки, вкладки, галочки, фон списков и панорама главного меню
 * подменены текстурами мода (assets/minecraft/textures/gui, их делает tools/vanilla_gui.py). Здесь — фон:
 * вместо серого затемнения (пауза, настройки, инвентарь) и земли — цвет класса, свечение, искры или пыль,
 * а в меню паузы и настройках ещё и фигура «кого прокачиваешь» по бокам. Ещё здесь — подсказки к предметам
 * (тёмные с красной рамкой) и экран смерти (тёмно-кровавый фон, искры и орнамент под надписью).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class VanillaScreenStyle {
    private VanillaScreenStyle() {
    }

    @SubscribeEvent
    public static void onBackground(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        if (screen == null || screen.getClass().getName().startsWith("com.tokyoghoulrpg")) {
            return;
        }
        GuiGraphics gfx = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        ScreenFx.Theme theme = ScreenFx.theme(data == null ? null : data.getPlayerClass());
        float time = (Util.getMillis() % 3_600_000L) / 1000.0F;
        int w = screen.width;
        int h = screen.height;
        boolean inWorld = mc.level != null;
        boolean container = screen instanceof AbstractContainerScreen<?>;
        // в мире мир чуть виден сквозь фон, в главном меню фон плотный вместо земли
        gfx.fillGradient(0, 0, w, h, ScreenFx.alpha(theme.top(), inWorld ? 0xA8 : 0xF0),
                ScreenFx.alpha(theme.bottom(), inWorld ? 0xD0 : 0xFF));
        if (inWorld && !container) {
            ScreenFx.backdrop(gfx, w, h, data, time, theme, 0.35F);
        }
        ScreenFx.glow(gfx, w / 2, h / 2, (int) (Math.min(w, h) * 0.6F), theme.glow());
        ScreenFx.particles(gfx, 0, w, h, time, container ? 30 : 50, theme, 0.45F);
        gfx.fillGradient(0, 0, w, 24, 0x80000000, 0x00000000);
        gfx.fillGradient(0, h - 24, w, h, 0x00000000, 0x80000000);
    }

    /** Подсказки к предметам: тёмный фон, рамка от алого сверху к тёмно-красному снизу. */
    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        event.setBackgroundStart(0xF0160709);
        event.setBackgroundEnd(0xF0080304);
        event.setBorderStart(0xD0E0303A);
        event.setBorderEnd(0x80600A14);
    }

    /** Экран смерти: под его красной заливкой — тёмный фон, свечение и искры. */
    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof DeathScreen screen)) {
            return;
        }
        GuiGraphics gfx = event.getGuiGraphics();
        int w = screen.width;
        int h = screen.height;
        float time = (Util.getMillis() % 3_600_000L) / 1000.0F;
        ScreenFx.Theme blood = ScreenFx.theme(com.tokyoghoulrpg.data.PlayerClass.GHOUL);
        gfx.fillGradient(0, 0, w, h, 0xB0140306, 0xF0050102);
        ScreenFx.glow(gfx, w / 2, h / 3, (int) (Math.min(w, h) * 0.7F), 0x50FF1020);
        ScreenFx.particles(gfx, 0, w, h, time, 80, blood, 0.7F);
        gfx.fillGradient(0, 0, w, 40, 0xA0000000, 0x00000000);
        gfx.fillGradient(0, h - 40, w, h, 0x00000000, 0xA0000000);
    }

    /** Экран смерти: орнамент из гаснущих линий с ромбом под надписью «Вы умерли!». */
    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof DeathScreen screen)) {
            return;
        }
        GuiGraphics gfx = event.getGuiGraphics();
        int cx = screen.width / 2;
        int y = 82;
        int half = 110;
        for (int i = 0; i < 12; i++) {
            int a = 0xD0 * (12 - i) / 12;
            int x0 = cx + 5 + half * i / 12;
            int x1 = cx + 5 + half * (i + 1) / 12;
            gfx.fill(x0, y, x1, y + 1, ScreenFx.alpha(0xE0303A, a));
            gfx.fill(2 * cx - x1, y, 2 * cx - x0, y + 1, ScreenFx.alpha(0xE0303A, a));
        }
        gfx.fill(cx - 1, y - 2, cx + 2, y + 3, 0xFFE0303A);
        gfx.fill(cx - 2, y - 1, cx + 3, y + 2, 0xFFE0303A);
    }
}
