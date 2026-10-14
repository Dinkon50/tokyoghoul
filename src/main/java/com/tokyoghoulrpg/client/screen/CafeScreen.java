package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.CafeActionC2S;
import com.tokyoghoulrpg.registry.ModItems;
import com.tokyoghoulrpg.shop.CafeShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Меню хозяина кофейни: кофе, мясо (гулям раз в сутки бесплатно) и задания. */
public class CafeScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int HEIGHT = 128;
    private static final int ROW_H = 24;

    private final int ownerId;
    private Button coffeeButton;
    private Button fleshButton;

    public CafeScreen(int ownerId) {
        super(Component.translatable("screen.tokyoghoulrpg.cafe.title"));
        this.ownerId = ownerId;
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(6, (this.height - HEIGHT) / 2);
    }

    private static PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    private static long day() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : CafeShop.day(mc.level);
    }

    private void send(int action) {
        NetworkHandler.CHANNEL.sendToServer(new CafeActionC2S(ownerId, action));
    }

    @Override
    protected void init() {
        int left = left();
        int y = top() + 30;
        coffeeButton = addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.cafe.take"),
                b -> send(CafeShop.COFFEE)).bounds(left + WIDTH - 74, y + 2, 68, 18).build());
        fleshButton = addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.cafe.take"),
                b -> send(CafeShop.FLESH)).bounds(left + WIDTH - 74, y + ROW_H + 2, 68, 18).build());
        int by = top() + HEIGHT - 26;
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.cafe.contracts"), b -> send(CafeShop.CONTRACTS))
                .bounds(left + 6, by, WIDTH / 2 - 9, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(left + WIDTH / 2 + 3, by, WIDTH / 2 - 9, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120A05, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top - 6, left + WIDTH, top + HEIGHT, 0xC0201208);
        gfx.fill(left, top - 6, left + 2, top + HEIGHT, 0xFFB0702A);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top, 0xE0A050);
        PlayerData data = data();
        int blood = data == null ? 0 : data.getBloodPoints();
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.shop.balance", blood),
                this.width / 2, top + 14, 0xFF5A78);

        long day = day();
        int y = top + 30;
        drawRow(gfx, left, y, new ItemStack(ModItems.COFFEE.get()), CafeShop.COFFEE, data, day, blood, coffeeButton);
        drawRow(gfx, left, y + ROW_H, new ItemStack(ModItems.FLESH.get()), CafeShop.FLESH, data, day, blood, fleshButton);
        boolean ghoul = data != null && data.getPlayerClass().hasKagune();
        gfx.drawCenteredString(this.font, Component.translatable(ghoul
                        ? "screen.tokyoghoulrpg.cafe.hint_ghoul" : "screen.tokyoghoulrpg.cafe.hint_human"),
                this.width / 2, y + ROW_H * 2 + 4, 0x9A8A70);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphics gfx, int left, int y, ItemStack stack, int item, PlayerData data, long day,
                         int blood, Button button) {
        gfx.fill(left + 4, y, left + WIDTH - 4, y + ROW_H - 2, 0x30FFFFFF);
        gfx.renderItem(stack, left + 10, y + 3);
        gfx.drawString(this.font, stack.getHoverName(), left + 32, y + 2, 0xE6E6E6, false);
        boolean free = CafeShop.isFree(data, day, item);
        int price = CafeShop.price(item);
        Component line = free ? Component.translatable("screen.tokyoghoulrpg.cafe.free")
                : Component.translatable("screen.tokyoghoulrpg.shop.price", price);
        boolean can = free || blood >= price;
        gfx.drawString(this.font, line, left + 32, y + 12, free ? 0x6EE07A : can ? 0xFF5A78 : 0x777777, false);
        button.active = can;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
