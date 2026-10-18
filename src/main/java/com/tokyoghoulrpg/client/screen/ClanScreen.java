package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ClanActionC2S;
import com.tokyoghoulrpg.network.packet.ClanStateS2C;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.rep.Standing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Меню клана (клавиша Y). Без клана: создать клан или принять приглашение.
 * В клане: участники (кто в игре, кто глава), передача очков навыков и очков крови участникам,
 * приглашение игроков с сервера и исключение (только глава), выход из клана.
 */
public class ClanScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 262;
    private static final int ROW = 16;
    private static final int MAX_MEMBERS_SHOWN = 7;
    private static final int MAX_CANDIDATES_SHOWN = 3;

    /** Последнее состояние от сервера (null — ещё не пришло). */
    private static ClanStateS2C state;

    private EditBox nameBox;
    /** Кнопки «+» и в игре ли участник: доступность обновляется каждый кадр (откат идёт и при открытом меню). */
    private final Map<Button, Boolean> powerButtons = new HashMap<>();
    private EditBox amountBox;
    private String nameText = "";
    private String amountText = "10";

    public ClanScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.clan.title"));
        state = null;   // свежие данные придут от сервера сразу после открытия
    }

    /** Задания, войны, рейтинг (null — ещё не пришли). */
    private static com.tokyoghoulrpg.network.packet.ClanInfoS2C info;

    public static com.tokyoghoulrpg.network.packet.ClanInfoS2C info() {
        return info;
    }

    public static void acceptInfo(com.tokyoghoulrpg.network.packet.ClanInfoS2C newInfo) {
        info = newInfo;
        if (Minecraft.getInstance().screen instanceof ClanUpgradeScreen upgrade) {
            upgrade.refresh();
        }
    }

    /** Последнее состояние клана (для меню прокачки). */
    public static ClanStateS2C state() {
        return state;
    }

    /** Пришло новое состояние: запомнить и перестроить меню, если оно открыто. */
    public static void accept(ClanStateS2C newState) {
        state = newState;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ClanScreen screen) {
            screen.refresh();
        } else if (mc.screen instanceof ClanUpgradeScreen upgrade) {
            upgrade.refresh();
        } else if (mc.screen instanceof ClanBankScreen bank) {
            bank.refresh();
        }
    }

    /** Открыть меню клана, не сбрасывая уже полученное состояние (возврат из меню прокачки). */
    public static ClanScreen reopen() {
        ClanStateS2C kept = state;
        ClanScreen screen = new ClanScreen();
        state = kept;
        return screen;
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
        powerButtons.clear();
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
                // «+»: передать силу и половину здоровья на 30 секунд
                Button power = Button.builder(Component.literal("+"),
                        b -> send(ClanManager.GIVE_POWER, "", member.id(), 0)).bounds(left + 132, y, 14, 14)
                        .tooltip(Tooltip.create(Component.translatable("screen.tokyoghoulrpg.clan.give_power_tip")))
                        .build();
                power.active = member.flag() && canGivePower();
                powerButtons.put(power, member.flag());
                addRenderableWidget(power);
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

        // склад клана и прокачка клана
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.storage"),
                b -> send(ClanManager.STORAGE, "", null, 0)).bounds(left + 10, top + HEIGHT - 24, 60, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.bank"),
                b -> Minecraft.getInstance().setScreen(new ClanBankScreen())).bounds(left + 74, top + HEIGHT - 24, 60, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.upgrade_open", state.level),
                b -> Minecraft.getInstance().setScreen(new ClanUpgradeScreen())).bounds(left + 138, top + HEIGHT - 24, 84, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan.leave"),
                b -> send(ClanManager.LEAVE, "", null, 0)).bounds(left + 226, top + HEIGHT - 24, 64, 18).build());
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
            // своя репутация (без клана репутация клана считается обычной)
            PlayerData own = data();
            int standing = own == null ? 0 : own.getStanding();
            Component repLine = Component.translatable("screen.tokyoghoulrpg.clan.rep_own", Standing.colored(standing));
            int ry = top + HEIGHT - 20;
            gfx.drawString(this.font, repLine, left + 10, ry, 0xC9C9C9, false);
            super.render(gfx, mouseX, mouseY, partialTick);
            if (inside(mouseX, mouseY, left + 10, ry - 2, this.font.width(repLine), 12)) {
                gfx.renderComponentTooltip(this.font, repTooltip(standing, null), mouseX, mouseY);
            }
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
        // репутация клана и своя (наведи — подсказка, что они дают)
        int standing = data == null ? 0 : data.getStanding();
        Component repLine = Component.translatable("screen.tokyoghoulrpg.clan.rep",
                Standing.colored(state.reputation), Standing.colored(standing));
        int repX = left + WIDTH - 10 - this.font.width(repLine);
        gfx.drawString(this.font, repLine, repX, top + 48, 0xC9C9C9, false);
        boolean repHover = inside(mouseX, mouseY, repX, top + 46, this.font.width(repLine), 12);

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
                    left + 10, top + 60 + MAX_MEMBERS_SHOWN * ROW + 4, 0x777777, false);
        }
        boolean canGive = canGivePower();
        powerButtons.forEach((button, online) -> button.active = online && canGive);
        super.render(gfx, mouseX, mouseY, partialTick);
        if (repHover) {
            gfx.renderComponentTooltip(this.font, repTooltip(standing, state.reputation), mouseX, mouseY);
        }
    }

    /** Сам сейчас можешь передать силу: нет связи и нет отката (сервер всё равно проверит). */
    private static boolean canGivePower() {
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = data();
        if (mc.level == null || data == null) {
            return false;
        }
        long now = mc.level.getGameTime();
        return data.getBondEndTick() <= now && data.getBondCooldownEnd() <= now;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /** Подсказка: откуда берётся репутация и что она даёт сейчас. clanRep = null — игрок без клана. */
    private static List<Component> repTooltip(int own, Integer clanRep) {
        Standing.Tier ownTier = Standing.tier(own);
        Standing.Tier clanTier = clanRep == null ? Standing.Tier.NEUTRAL : Standing.tier(clanRep);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.tokyoghoulrpg.rep_tip.title").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("screen.tokyoghoulrpg.rep_tip.up").withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("screen.tokyoghoulrpg.rep_tip.down").withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("screen.tokyoghoulrpg.rep_tip.levels", Balance.STANDING_HIGH, Balance.STANDING_LOW)
                .withStyle(ChatFormatting.GRAY));
        if (clanRep == null) {
            lines.add(Component.translatable("screen.tokyoghoulrpg.rep_tip.no_clan").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("screen.tokyoghoulrpg.rep_status." + Standing.statusKey(ownTier, clanTier)));
        return lines;
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
