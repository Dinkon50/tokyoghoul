package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.market.Market;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.MarketActionC2S;
import com.tokyoghoulrpg.network.packet.MarketStateS2C;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Торговая площадка (клавиша M). Вкладки: «Все кланы» (лоты для всех), «Мой клан» (только для своего клана),
 * «Мои лоты». Внизу — выставить вещь из главной руки: цена в очках крови и кому видно.
 */
public class MarketScreen extends Screen {
    private static final int WIDTH = 340;
    private static final int HEIGHT = 250;
    private static final int ROW = 20;
    private static final int PER_PAGE = 7;

    private static MarketStateS2C state;
    private static int tab = 0;
    private static int page = 0;
    private static boolean sellClanOnly = false;
    /** 0 — продажа по цене, 1-3 — аукцион на 10 / 30 / 60 минут. */
    private static int sellMode = 0;

    private EditBox priceBox;
    private String priceText = "100";

    public MarketScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.market.title"));
        state = null;
    }

    public static void accept(MarketStateS2C newState) {
        state = newState;
        if (Minecraft.getInstance().screen instanceof MarketScreen screen) {
            screen.refresh();
        }
    }

    private void refresh() {
        if (priceBox != null) {
            priceText = priceBox.getValue();
        }
        this.rebuildWidgets();
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - HEIGHT) / 2);
    }

    private static void send(int action, int value, int price, boolean clanOnly) {
        NetworkHandler.CHANNEL.sendToServer(new MarketActionC2S(action, value, price, clanOnly));
    }

    /** Лоты текущей вкладки. */
    private static List<MarketStateS2C.Entry> shown() {
        List<MarketStateS2C.Entry> list = new ArrayList<>();
        if (state == null) {
            return list;
        }
        for (MarketStateS2C.Entry e : state.entries) {
            boolean fits = switch (tab) {
                case 0 -> !e.clanOnly();
                case 1 -> e.clanOnly() || e.ownClan();
                default -> e.mine();
            };
            if (fits) {
                list.add(e);
            }
        }
        return list;
    }

    private int price() {
        try {
            return priceBox == null ? 0 : Integer.parseInt(priceBox.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        String[] tabs = {"screen.tokyoghoulrpg.market.tab_all", "screen.tokyoghoulrpg.market.tab_clan",
                "screen.tokyoghoulrpg.market.tab_mine"};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            Button b = addRenderableWidget(Button.builder(Component.translatable(tabs[i]), btn -> {
                tab = index;
                page = 0;
                refresh();
            }).bounds(left + 10 + i * 108, top + 22, 104, 16).build());
            b.active = tab != i && (i != 1 || state == null || state.inClan);
        }
        List<MarketStateS2C.Entry> list = shown();
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        int y = top + 44;
        for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
            MarketStateS2C.Entry e = list.get(i);
            boolean auction = e.secondsLeft() >= 0;
            Button action;
            if (e.mine()) {
                action = Button.builder(Component.translatable("screen.tokyoghoulrpg.market.cancel"),
                        b -> send(Market.CANCEL, e.id(), 0, false)).bounds(left + WIDTH - 70, y + 2, 60, 16).build();
                action.active = !auction || e.leader().isEmpty();
            } else if (auction) {
                action = Button.builder(Component.translatable("screen.tokyoghoulrpg.market.bid"),
                        b -> send(Market.BID, e.id(), 0, false)).bounds(left + WIDTH - 70, y + 2, 60, 16).build();
                action.active = !e.leading();
            } else {
                action = Button.builder(Component.translatable("screen.tokyoghoulrpg.market.buy"),
                        b -> send(Market.BUY, e.id(), 0, false)).bounds(left + WIDTH - 70, y + 2, 60, 16).build();
            }
            addRenderableWidget(action);
            y += ROW;
        }
        int py = top + 44 + PER_PAGE * ROW + 2;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page = Math.max(0, page - 1);
            refresh();
        }).bounds(left + 10, py, 20, 14).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page++;
            refresh();
        }).bounds(left + 80, py, 20, 14).build()).active = page < pages - 1;

        // выставить вещь из руки
        int sy = top + HEIGHT - 54;
        priceBox = new EditBox(this.font, left + 10, sy + 12, 70, 16, Component.translatable("screen.tokyoghoulrpg.market.price"));
        priceBox.setMaxLength(7);
        priceBox.setFilter(s -> s.matches("\\d*"));
        priceBox.setValue(priceText);
        addRenderableWidget(priceBox);
        Button who = addRenderableWidget(Button.builder(Component.translatable(sellClanOnly
                        ? "screen.tokyoghoulrpg.market.for_clan" : "screen.tokyoghoulrpg.market.for_all"), b -> {
            sellClanOnly = !sellClanOnly;
            refresh();
        }).bounds(left + 86, sy + 11, 110, 18).build());
        who.active = state == null || state.inClan || sellClanOnly;
        if (state != null && !state.inClan) {
            sellClanOnly = false;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.market.mode_" + sellMode), b -> {
            sellMode = (sellMode + 1) % Market.AUCTION_MINUTES.length;
            refresh();
        }).bounds(left + 200, sy + 11, 62, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.market.sell"),
                b -> send(Market.SELL, sellMode, price(), sellClanOnly)).bounds(left + 266, sy + 11, 64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(this.width / 2 - 40, top + HEIGHT - 22, 80, 16).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + HEIGHT, 0xC0141008);
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFD8B060);
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        gfx.drawString(this.font, this.title, left + 10, top + 7, 0xD8B060, false);
        if (data != null) {
            Component blood = Component.translatable("screen.tokyoghoulrpg.market.blood", data.getBloodPoints());
            gfx.drawString(this.font, blood, left + WIDTH - 10 - this.font.width(blood), top + 7, 0xFF5A78, false);
        }
        if (state == null) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.loading"),
                    this.width / 2, top + 100, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        List<MarketStateS2C.Entry> list = shown();
        int y = top + 44;
        if (list.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.market.empty"), left + 10, y + 6, 0x6A6A6A, false);
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
            MarketStateS2C.Entry e = list.get(i);
            gfx.fill(left + 6, y, left + WIDTH - 6, y + ROW - 2, e.mine() ? 0x40305020 : 0x30201010);
            gfx.renderItem(e.stack(), left + 10, y + 1);
            gfx.renderItemDecorations(this.font, e.stack(), left + 10, y + 1);
            gfx.drawString(this.font, e.stack().getHoverName(), left + 30, y + 1, 0xE6E6E6, false);
            Component who;
            if (e.secondsLeft() >= 0) {
                int secs = e.secondsLeft();
                String time = (secs / 60) + ":" + String.format("%02d", secs % 60);
                who = e.leader().isEmpty()
                        ? Component.translatable("screen.tokyoghoulrpg.market.auction_line", e.seller(), time)
                        : Component.translatable("screen.tokyoghoulrpg.market.auction_line_bid", e.seller(), e.leader(), time);
            } else {
                who = e.clan().isEmpty()
                        ? Component.translatable("screen.tokyoghoulrpg.market.seller", e.seller())
                        : Component.translatable("screen.tokyoghoulrpg.market.seller_clan", e.seller(), e.clan());
            }
            gfx.drawString(this.font, who, left + 30, y + 10, e.leading() ? 0x6EE07A : 0x8A8A8A, false);
            Component price = Component.translatable(e.secondsLeft() >= 0 ? "screen.tokyoghoulrpg.market.bid_tag"
                    : "screen.tokyoghoulrpg.market.price_tag", e.price());
            gfx.drawString(this.font, price, left + WIDTH - 76 - this.font.width(price), y + 5, 0xFF7A90, false);
            if (mouseX >= left + 10 && mouseX < left + 26 && mouseY >= y + 1 && mouseY < y + 17) {
                hovered = e.stack();
            }
            y += ROW;
        }
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        gfx.drawCenteredString(this.font, (page + 1) + " / " + pages, left + 55, top + 44 + PER_PAGE * ROW + 5, 0x9A9A9A);
        // продажа
        int sy = top + HEIGHT - 54;
        ItemStack hand = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        Component inHand = hand.isEmpty() ? Component.translatable("screen.tokyoghoulrpg.market.hand_empty")
                : Component.translatable("screen.tokyoghoulrpg.market.hand", hand.getHoverName(), hand.getCount());
        gfx.drawString(this.font, inHand, left + 10, sy, 0xC9C9C9, false);
        super.render(gfx, mouseX, mouseY, partialTick);
        if (!hovered.isEmpty()) {
            gfx.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    /** Раз в 2 секунды обновить лоты: таймеры аукционов и чужие ставки. */
    @Override
    public void tick() {
        super.tick();
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 40 == 0) {
            send(Market.OPEN, 0, 0, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = priceBox != null && priceBox.isFocused();
        if (!typing && KeyBindings.MARKET.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
