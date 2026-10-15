package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.FenceActionC2S;
import com.tokyoghoulrpg.shop.FenceShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Лавка Скупщика: слева товары, справа что он покупает (продать одну штуку или все). */
public class FenceScreen extends Screen {
    private static final int WIDTH = 360;
    private static final int ROW = 22;
    private final int entityId;

    public FenceScreen(int entityId) {
        super(Component.translatable("screen.tokyoghoulrpg.fence.title"));
        this.entityId = entityId;
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int height() {
        return 46 + FenceShop.BUYS.size() * ROW + 30;
    }

    private int top() {
        return Math.max(4, (this.height - height()) / 2);
    }

    private void send(int action, int index) {
        NetworkHandler.CHANNEL.sendToServer(new FenceActionC2S(entityId, action, index));
    }

    @Override
    protected void init() {
        int left = left();
        int y = top() + 40;
        for (int i = 0; i < FenceShop.SELLS.size(); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.fence.buy",
                            FenceShop.SELLS.get(i).price()), b -> send(FenceShop.BUY, index))
                    .bounds(left + 28, y + 12, 90, 16).build());
            y += ROW + 12;
        }
        y = top() + 40;
        for (int i = 0; i < FenceShop.BUYS.size(); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal("1"), b -> send(FenceShop.SELL_ONE, index))
                    .bounds(left + WIDTH - 66, y + 2, 22, 16).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.fence.all"),
                    b -> send(FenceShop.SELL_ALL, index)).bounds(left + WIDTH - 42, y + 2, 36, 16).build());
            y += ROW;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(this.width / 2 - 50, top() + height() - 24, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + height(), 0xC0141008);
        gfx.fill(left, top, left + 2, top + height(), 0xFFB08A3A);
        gfx.fill(left + 176, top + 26, left + 177, top + height() - 30, 0x40FFFFFF);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top + 7, 0xD8B060);
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        if (data != null) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.fence.blood", data.getBloodPoints()),
                    this.width / 2, top + 18, 0xFF5A78);
        }
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.fence.sells"), left + 8, top + 30, 0xE0A050, false);
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.fence.buys"), left + 184, top + 30, 0xE0A050, false);
        int y = top + 40;
        for (FenceShop.Offer offer : FenceShop.SELLS) {
            ItemStack stack = new ItemStack(offer.item().get());
            gfx.renderItem(stack, left + 8, y + 6);
            gfx.drawString(this.font, stack.getHoverName(), left + 28, y + 1, 0xE6E6E6, false);
            y += ROW + 12;
        }
        y = top + 40;
        for (FenceShop.Offer offer : FenceShop.BUYS) {
            ItemStack stack = new ItemStack(offer.item().get());
            int have = mc.player == null ? 0 : mc.player.getInventory().countItem(stack.getItem());
            gfx.renderItem(stack, left + 184, y + 2);
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.fence.price", offer.price()),
                    left + 204, y + 2, 0xFF7A90, false);
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.fence.have", have),
                    left + 204, y + 11, have > 0 ? 0xC9C9C9 : 0x6A6A6A, false);
            y += ROW;
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
