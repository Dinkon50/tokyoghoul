package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ShopActionC2S;
import com.tokyoghoulrpg.shop.BloodShop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Лавка торговца: товары за очки крови и кнопка обычной торговли за изумруды. */
public class BloodShopScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int ROW_H = 26;

    private final int traderId;
    private final List<Button> buyButtons = new ArrayList<>();

    public BloodShopScreen(int traderId) {
        super(Component.translatable("screen.tokyoghoulrpg.shop.title"));
        this.traderId = traderId;
    }

    private int top() {
        int height = 40 + BloodShop.ENTRIES.size() * ROW_H + 34;
        return Math.max(6, (this.height - height) / 2);
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    @Override
    protected void init() {
        buyButtons.clear();
        int y = top() + 40;
        for (int i = 0; i < BloodShop.ENTRIES.size(); i++) {
            final int index = i;
            Button button = Button.builder(Component.translatable("screen.tokyoghoulrpg.shop.buy"),
                            b -> NetworkHandler.CHANNEL.sendToServer(new ShopActionC2S(traderId, index)))
                    .bounds(left() + WIDTH - 64, y + 2, 58, 18)
                    .build();
            buyButtons.add(addRenderableWidget(button));
            y += ROW_H;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.shop.emeralds"), b -> {
                    NetworkHandler.CHANNEL.sendToServer(new ShopActionC2S(traderId, ShopActionC2S.EMERALD_TRADE));
                    onClose();
                })
                .bounds(left() + 6, y + 8, WIDTH / 2 - 9, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(left() + WIDTH / 2 + 3, y + 8, WIDTH / 2 - 9, 20)
                .build());
    }

    private int blood() {
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        return data == null ? 0 : data.getBloodPoints();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int top = top();
        int left = left();
        int panelH = 40 + BloodShop.ENTRIES.size() * ROW_H + 34;
        gfx.fill(left, top - 6, left + WIDTH, top + panelH, 0xC0140808);
        gfx.fill(left, top - 6, left + 2, top + panelH, 0xFFB01E1E);

        gfx.drawCenteredString(this.font, this.title, this.width / 2, top, 0xC21B1B);
        int blood = blood();
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.shop.balance", blood),
                this.width / 2, top + 14, 0xFF5A78);

        int y = top + 40;
        for (int i = 0; i < BloodShop.ENTRIES.size(); i++) {
            BloodShop.Entry entry = BloodShop.ENTRIES.get(i);
            ItemStack stack = entry.stack();
            if (i % 2 == 0) {
                gfx.fill(left + 4, y, left + WIDTH - 4, y + ROW_H - 2, 0x40FFFFFF);
            }
            gfx.renderItem(stack, left + 10, y + 4);
            gfx.drawString(this.font, stack.getHoverName(), left + 32, y + 3, 0xE6E6E6, false);
            boolean affordable = blood >= entry.price();
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.shop.price", entry.price()),
                    left + 32, y + 13, affordable ? 0xFF5A78 : 0x777777, false);
            buyButtons.get(i).active = affordable;
            y += ROW_H;
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
