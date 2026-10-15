package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.TutorialC2S;
import com.tokyoghoulrpg.tutorial.Tutorial;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** Подсказки для новичков на клиенте: отсчёт времени и окошко справа на экране. */
public final class TutorialClient {
    private static final int BOX_W = 190;

    private static int shownStep = -1;
    private static int ticks = 0;
    private static boolean sent = false;

    private TutorialClient() {
    }

    public static void reset() {
        shownStep = -1;
        ticks = 0;
        sent = false;
    }

    private static PlayerData active(Minecraft mc) {
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        if (data == null || !data.hasChosenClass() || data.isTutorialDone()) {
            return null;
        }
        return data;
    }

    /** Каждый тик клиента. Пока открыт экран или игра на паузе, подсказка не сменяется. */
    public static void tick(Minecraft mc) {
        PlayerData data = active(mc);
        if (data == null) {
            reset();
            return;
        }
        int step = data.getTutorialStep();
        if (step != shownStep) {
            shownStep = step;
            ticks = 0;
            sent = false;
        }
        if (mc.screen == null && !mc.isPaused()) {
            ticks++;
        }
        if (ticks >= Balance.TUTORIAL_HINT_TICKS && !sent) {
            sent = true;
            NetworkHandler.CHANNEL.sendToServer(new TutorialC2S(TutorialC2S.NEXT, step));
        }
    }

    /** Окошко с подсказкой: справа, на трети высоты экрана. */
    public static void render(GuiGraphics gfx, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = active(mc);
        if (data == null || shownStep < 0) {
            return;
        }
        List<Tutorial.Hint> hints = Tutorial.hints(data.getPlayerClass());
        int index = Mth.clamp(shownStep, 0, hints.size() - 1);
        Tutorial.Hint hint = hints.get(index);
        Object[] args = new Object[hint.keys().length];
        for (int i = 0; i < args.length; i++) {
            args[i] = keyName(mc, hint.keys()[i]);
        }
        Font font = mc.font;
        List<FormattedCharSequence> lines = font.split(Component.translatable(hint.translationKey(), args), BOX_W - 14);
        int boxH = 22 + lines.size() * 10 + 12;
        int x = screenWidth - BOX_W - 6;
        int y = Math.max(40, screenHeight / 3 - boxH / 2);
        // плавно выезжает справа в начале и уходит в конце
        float slide = Math.min(1.0F, ticks / 8.0F);
        int left = Balance.TUTORIAL_HINT_TICKS - ticks;
        if (left < 8) {
            slide = Math.max(0.0F, left / 8.0F);
        }
        x += Math.round((1.0F - slide) * (BOX_W + 10));

        gfx.fill(x, y, x + BOX_W, y + boxH, 0xC0100608);
        gfx.fill(x, y, x + 2, y + boxH, 0xFFE0A050);
        gfx.drawString(font, Component.translatable("tutorial.tokyoghoulrpg.header", index + 1, hints.size()),
                x + 8, y + 5, 0xE0A050, false);
        int ly = y + 18;
        for (FormattedCharSequence line : lines) {
            gfx.drawString(font, line, x + 8, ly, 0xE6E6E6, false);
            ly += 10;
        }
        gfx.drawString(font, Component.translatable("tutorial.tokyoghoulrpg.footer", keyName(mc, "key.tokyoghoulrpg.profile")),
                x + 8, y + boxH - 11, 0x7A7A7A, false);
        // полоска времени подсказки
        int bar = Math.round((BOX_W - 2) * Mth.clamp(ticks / (float) Balance.TUTORIAL_HINT_TICKS, 0.0F, 1.0F));
        gfx.fill(x + 2, y + boxH - 1, x + 2 + bar, y + boxH, 0xFFE0A050);
    }

    /** Текущая клавиша привязки по её названию (с учётом переназначения). */
    private static Component keyName(Minecraft mc, String name) {
        for (KeyMapping mapping : mc.options.keyMappings) {
            if (mapping.getName().equals(name)) {
                return Component.literal("[").append(mapping.getTranslatedKeyMessage()).append("]")
                        .withStyle(ChatFormatting.YELLOW);
            }
        }
        return Component.literal("[?]").withStyle(ChatFormatting.YELLOW);
    }
}
