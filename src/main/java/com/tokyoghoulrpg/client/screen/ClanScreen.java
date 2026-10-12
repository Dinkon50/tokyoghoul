package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ClanActionC2S;
import com.tokyoghoulrpg.network.packet.ClanStateS2C;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Меню клана (клавиша Y). Без клана: создать клан или принять приглашение.
 * В клане: участники (кто в игре, кто глава), передача очков навыков и очков крови участникам,
 * приглашение игроков с сервера и исключение (только глава), выход из клана.
 */
public class ClanScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 232;
    private static final int ROW = 16;
    private static final int MAX_MEMBERS_SHOWN = 7;
    private static final int MAX_CANDIDATES_SHOWN = 4;

    /** Последнее состояние от сервера (null — ещё не пришло). */
    private static ClanStateS2C state;

    private EditBox nameBox;
    private EditBox amountBox;
    private String nameText = "";
    private String amountText = "10";

    public ClanScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.clan.title"));
        state = null;   // свежие данные придут от сервера сразу после открытия
    }

    /** Пришло новое состояние: запомнить и перестроить меню, если оно открыто. */
    public static void accept(ClanStateS2C newState) {
        state = newState;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ClanScreen screen) {
            screen.refresh();
        }
    }

    private void refresh() {
        if (nameBox != null) {
            nameText = nameBox.getValue();
        }
        if (amountBox != null) {
            amountText = amountBox.getValue();
        }
        this.rebuildWidgets();
    }

    private static void send(int action, String text, UUID target, int amount) {
        NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(action, text, target, amount));
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - HEIGHT) / 2);
    }

    private UUID me() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : mc.player.getUUID();
    }

    private boolean inClan() {
        return state != null && !state.clanName.isEmpty();
    }

    private boolean isLeader() {
        return inClan() && state.leader != null && state.leader.equals(me());
    }

    private int amount() {
        try {
            return amountBox == null ? 0 : Integer.parseInt(amountBox.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void init() {
        nameBox = null;
        amountBox = null;
        if (state == null) {
            return;
        }
        int left = left();
        int top = top();
        if (!inClan()) {
            // создание клана
            nameBox = new EditBox(this.font, left + 10, top + 40, 180, 16,
                    Component.translatable("screen.tokyoghoulrpg.clan.name"));
            nameBox.setMaxLength(ClanManager.NAME_MAX);
            nameBox.setValue(nameText);
            addRenderableWidget(nameBox);
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.create"),
                    b -> send(ClanManager.CREATE, nameBox.getValue(), null, 0)).bounds(left + 196, top + 38, 94, 20).build());
            // приглашения
            int y = top + 84;
            for (ClanStateS2C.Member invite : state.invites) {
                if (y > top + HEIGHT - 30) {
                    break;
                }
                addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.accept"),
                        b -> send(ClanManager.ACCEPT, "", invite.id(), 0)).bounds(left + 170, y, 58, 14).build());
                addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.decline"),
                        b -> send(ClanManager.DECLINE, "", invite.id(), 0)).bounds(left + 232, y, 58, 14).build());
                y += ROW;
            }
            return;
        }

        // сколько передать
        amountBox = new EditBox(this.font, left + 150, top + 30, 60, 14,
                Component.translatable("screen.tokyoghoulrpg.clan.amount"));
        amountBox.setMaxLength(7);
        amountBox.setFilter(s -> s.matches("\\d*"));
        amountBox.setValue(amountText);
        addRenderableWidget(amountBox);

        // участники
        int y = top + 60;
        int shown = 0;
        for (ClanStateS2C.Member member : state.members) {
            if (shown++ >= MAX_MEMBERS_SHOWN) {
                break;
            }
            boolean self = member.id().equals(me());
            if (!self) {
                Button points = Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.give_points"),
                        b -> send(ClanManager.GIVE_POINTS, "", member.id(), amount())).bounds(left + 150, y, 52, 14).build();
                Button blood = Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.give_blood"),
                        b -> send(ClanManager.GIVE_BLOOD, "", member.id(), amount())).bounds(left + 204, y, 52, 14).build();
                points.active = member.flag();
                blood.active = member.flag();
                addRenderableWidget(points);
                addRenderableWidget(blood);
                if (isLeader()) {
                    addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.kick"),
                            b -> send(ClanManager.KICK, "", member.id(), 0)).bounds(left + 262, y, 28, 14).build());
                }
            }
            y += ROW;
        }

        // приглашение игроков с сервера (только глава)
        if (isLeader()) {
            int cy = top + 60 + MAX_MEMBERS_SHOWN * ROW + 16;
            int count = 0;
            for (ClanStateS2C.Member candidate : state.candidates) {
                if (count++ >= MAX_CANDIDATES_SHOWN) {
                    break;
                }
                Button invite = Button.builder(Component.translatable(candidate.flag()
                                ? "screen.tokyoghoulrpg.clan.invited" : "screen.tokyoghoulrpg.clan.invite"),
                        b -> send(ClanManager.INVITE, "", candidate.id(), 0)).bounds(left + 190, cy, 100, 14).build();
                invite.active = !candidate.flag();
                addRenderableWidget(invite);
                cy += ROW;
            }
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.leave"),
                b -> send(ClanManager.LEAVE, "", null, 0)).bounds(left + WIDTH - 110, top + HEIGHT - 24, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + HEIGHT, 0xC0140808);
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFB01E1E);

        if (state == null) {
            gfx.drawCenteredString(this.font, this.title, this.width / 2, top + 6, 0xC21B1B);
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.loading"),
                    this.width / 2, top + HEIGHT / 2, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        if (!inClan()) {
            gfx.drawCenteredString(this.font, this.title, this.width / 2, top + 6, 0xC21B1B);
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.no_clan"), left + 10, top + 24, 0x9A9A9A, false);
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.invites"), left + 10, top + 70, 0xE6E6E6, false);
            if (state.invites.isEmpty()) {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.no_invites"), left + 10, top + 86, 0x777777, false);
            }
            int y = top + 84;
            for (ClanStateS2C.Member invite : state.invites) {
                if (y > top + HEIGHT - 30) {
                    break;
                }
                gfx.drawString(this.font, invite.name(), left + 10, y + 3, 0xFFFFFF, false);
                y += ROW;
            }
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        gfx.drawCenteredString(this.font, Component.literal(state.clanName), this.width / 2, top + 6, 0xC21B1B);
        PlayerData data = data();
        if (data != null) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.balance",
                    data.getSkillPoints(), data.getBloodPoints()), left + 10, top + 18, 0xC9C9C9, false);
        }
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.amount"), left + 10, top + 33, 0xE6E6E6, false);
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.members", state.members.size()),
                left + 10, top + 48, 0xE6E6E6, false);

        int y = top + 60;
        int shown = 0;
        for (ClanStateS2C.Member member : state.members) {
            if (shown++ >= MAX_MEMBERS_SHOWN) {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.more",
                        state.members.size() - MAX_MEMBERS_SHOWN), left + 10, y + 3, 0x777777, false);
                break;
            }
            boolean leader = member.id().equals(state.leader);
            String name = (leader ? "★ " : "") + member.name();
            int color = member.flag() ? 0xFFFFFF : 0x777777;
            gfx.drawString(this.font, name, left + 10, y + 3, color, false);
            if (!member.flag()) {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.offline"),
                        left + 10 + this.font.width(name) + 6, y + 3, 0x555555, false);
            }
            y += ROW;
        }

        if (isLeader()) {
            int cy = top + 60 + MAX_MEMBERS_SHOWN * ROW + 4;
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.invite_title"), left + 10, cy, 0xE6E6E6, false);
            cy += 12;
            if (state.candidates.isEmpty()) {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.no_candidates"),
                        left + 10, cy + 3, 0x777777, false);
            }
            int count = 0;
            for (ClanStateS2C.Member candidate : state.candidates) {
                if (count++ >= MAX_CANDIDATES_SHOWN) {
                    break;
                }
                gfx.drawString(this.font, candidate.name(), left + 10, cy + 3, 0xFFFFFF, false);
                cy += ROW;
            }
        } else {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.leader_only"),
                    left + 10, top + HEIGHT - 20, 0x777777, false);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private static PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = (nameBox != null && nameBox.isFocused()) || (amountBox != null && amountBox.isFocused());
        if (!typing && KeyBindings.CLAN.matches(keyCode, scanCode)) {
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
