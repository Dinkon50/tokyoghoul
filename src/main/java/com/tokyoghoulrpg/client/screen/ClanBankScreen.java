package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.clan.ClanBank;
import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ClanActionC2S;
import com.tokyoghoulrpg.network.packet.ClanStateS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Банк клана: общие очки навыков и очки крови. Положить может любой; глава забирает сразу,
 * остальные просят, а глава одобряет или отклоняет просьбы здесь же.
 */
public class ClanBankScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 250;
    private static final int MAX_SHOWN = 6;

    private EditBox amountBox;
    private String amountText = "100";

    public ClanBankScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.clan_bank.title"));
    }

    void refresh() {
        if (amountBox != null) {
            amountText = amountBox.getValue();
        }
        this.rebuildWidgets();
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - HEIGHT) / 2);
    }

    private static boolean isLeader(ClanStateS2C state) {
        Minecraft mc = Minecraft.getInstance();
        return state != null && mc.player != null && mc.player.getUUID().equals(state.leader);
    }

    private int amount() {
        try {
            return amountBox == null ? 0 : Integer.parseInt(amountBox.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void send(int action, int value) {
        NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(action, "", null, value));
    }

    @Override
    protected void init() {
        ClanStateS2C state = ClanScreen.state();
        int left = left();
        int top = top();
        amountBox = new EditBox(this.font, left + 10, top + 52, 70, 16, Component.translatable("screen.tokyoghoulrpg.clan.amount"));
        amountBox.setMaxLength(7);
        amountBox.setFilter(s -> s.matches("\\d*"));
        amountBox.setValue(amountText);
        addRenderableWidget(amountBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_bank.put_points"),
                b -> send(ClanManager.BANK_PUT_POINTS, amount())).bounds(left + 86, top + 51, 110, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_bank.put_blood"),
                b -> send(ClanManager.BANK_PUT_BLOOD, amount())).bounds(left + 200, top + 51, 110, 18).build());
        boolean leader = isLeader(state);
        addRenderableWidget(Button.builder(Component.translatable(leader ? "screen.tokyoghoulrpg.clan_bank.take_points"
                        : "screen.tokyoghoulrpg.clan_bank.ask_points"),
                b -> send(ClanManager.BANK_TAKE_POINTS, amount())).bounds(left + 86, top + 73, 110, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(leader ? "screen.tokyoghoulrpg.clan_bank.take_blood"
                        : "screen.tokyoghoulrpg.clan_bank.ask_blood"),
                b -> send(ClanManager.BANK_TAKE_BLOOD, amount())).bounds(left + 200, top + 73, 110, 18).build());
        if (leader && state != null) {
            int y = top + 132;
            int shown = 0;
            for (ClanBank.Request r : state.requests) {
                if (shown++ >= MAX_SHOWN) {
                    break;
                }
                addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_bank.approve"),
                        b -> send(ClanManager.BANK_APPROVE, r.id())).bounds(left + WIDTH - 104, y, 46, 14).build());
                addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_bank.decline"),
                        b -> send(ClanManager.BANK_DECLINE, r.id())).bounds(left + WIDTH - 54, y, 44, 14).build());
                y += 16;
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.back"),
                b -> Minecraft.getInstance().setScreen(ClanScreen.reopen())).bounds(this.width / 2 - 50, top + HEIGHT - 24, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + HEIGHT, 0xC0140808);
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFE0C060);
        ClanStateS2C state = ClanScreen.state();
        if (state == null || state.clanName.isEmpty()) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.loading"),
                    this.width / 2, top + HEIGHT / 2, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_bank.header", state.clanName),
                this.width / 2, top + 7, 0xE0C060);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_bank.total",
                state.bankPoints, state.treasury), this.width / 2, top + 20, 0xFFFFFF);
        PlayerData data = Minecraft.getInstance().player == null ? null : PlayerData.get(Minecraft.getInstance().player);
        if (data != null) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_bank.yours",
                    data.getSkillPoints(), data.getBloodPoints()), this.width / 2, top + 32, 0x9A9A9A);
        }
        gfx.drawWordWrap(this.font, Component.translatable(isLeader(state) ? "screen.tokyoghoulrpg.clan_bank.rules_leader"
                : "screen.tokyoghoulrpg.clan_bank.rules"), left + 10, top + 96, WIDTH - 20, 0x8A8A8A);
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_bank.requests", state.requests.size()),
                left + 10, top + 120, 0xE6E6E6, false);
        int y = top + 132;
        int shown = 0;
        if (state.requests.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_bank.no_requests"),
                    left + 10, y + 3, 0x6A6A6A, false);
        }
        for (ClanBank.Request r : state.requests) {
            if (shown++ >= MAX_SHOWN) {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.more",
                        state.requests.size() - MAX_SHOWN), left + 10, y + 3, 0x6A6A6A, false);
                break;
            }
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_bank.request_line", r.name(),
                    r.points(), r.blood()), left + 10, y + 3, 0xC9C9C9, false);
            y += 16;
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
