package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.KaguneType;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectKaguneTypeC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Выбор типа кагуне: четыре карточки с описанием бонуса. Первый выбор бесплатный, сменить потом
 * можно здесь же (открывается из «Профиля») за очки крови.
 */
public class KaguneTypeScreen extends Screen {
    private static final int CARD_W = 200;
    private static final int CARD_H = 46;
    private static final int GAP = 6;

    public KaguneTypeScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.kagune_type.title"));
    }

    private int top() {
        return Math.max(30, this.height / 2 - (CARD_H * 2 + GAP) - 6);
    }

    private static KaguneType current() {
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        return data == null ? KaguneType.NONE : data.getKaguneType();
    }

    @Override
    protected void init() {
        KaguneType current = current();
        int cx = this.width / 2;
        int top = top();
        for (int i = 0; i < KaguneType.CHOICES.length; i++) {
            KaguneType type = KaguneType.CHOICES[i];
            int x = cx + (i % 2 == 0 ? -CARD_W - GAP / 2 : GAP / 2);
            int y = top + (i / 2) * (CARD_H + GAP);
            Button button = Button.builder(Component.translatable(type == current
                            ? "screen.tokyoghoulrpg.kagune_type.current" : "screen.tokyoghoulrpg.kagune_type.choose"),
                    b -> {
                        NetworkHandler.CHANNEL.sendToServer(new SelectKaguneTypeC2S(type));
                        onClose();
                    }).bounds(x + CARD_W - 74, y + CARD_H - 18, 70, 14).build();
            button.active = type != current;
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(cx - 50, top + 2 * (CARD_H + GAP) + 22, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xE0120405, 0xE0000000);
        int cx = this.width / 2;
        int top = top();
        gfx.drawCenteredString(this.font, this.title, cx, top - 26, 0xC21B1B);
        KaguneType current = current();
        Component sub = current == KaguneType.NONE
                ? Component.translatable("screen.tokyoghoulrpg.kagune_type.free")
                : Component.translatable("screen.tokyoghoulrpg.kagune_type.cost", Balance.KAGUNE_TYPE_CHANGE_COST);
        gfx.drawCenteredString(this.font, sub, cx, top - 14, 0x9A9A9A);
        for (int i = 0; i < KaguneType.CHOICES.length; i++) {
            KaguneType type = KaguneType.CHOICES[i];
            int x = cx + (i % 2 == 0 ? -CARD_W - GAP / 2 : GAP / 2);
            int y = top + (i / 2) * (CARD_H + GAP);
            boolean chosen = type == current;
            gfx.fill(x, y, x + CARD_W, y + CARD_H, chosen ? 0xC0401010 : 0xC0181010);
            gfx.fill(x, y, x + 2, y + CARD_H, chosen ? 0xFFFF4040 : 0xFFB01E1E);
            gfx.drawString(this.font, Component.translatable(type.translationKey()), x + 7, y + 5, 0xFF5A5A, false);
            gfx.drawWordWrap(this.font, Component.translatable(type.descriptionKey()), x + 7, y + 16, CARD_W - 84, 0xC9C9C9);
        }
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.kagune_type.note"),
                cx, top + 2 * (CARD_H + GAP) + 6, 0x7A7A7A);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
