package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.clan.ClanPerks;
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
 * Прокачка клана: уровень, казна, что даёт каждый уровень. Любой участник вкладывает очки крови в казну,
 * глава покупает следующий уровень из казны.
 */
public class ClanUpgradeScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int HEIGHT = 270;

    /** Вкладка: 0 — уровни клана, 1 — умения клана. */
    private static int tab = 0;
    private static final int SKILL_ROW = 30;
    private static final int WAR_ROWS = 5;

    private EditBox amountBox;
    private String amountText = "500";

    public ClanUpgradeScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.clan_upgrade.title"));
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

    @Override
    protected void init() {
        ClanStateS2C state = ClanScreen.state();
        int left = left();
        int top = top();
        int by = top + HEIGHT - 50;
        amountBox = new EditBox(this.font, left + 10, by, 70, 16, Component.translatable("screen.tokyoghoulrpg.clan.amount"));
        amountBox.setMaxLength(7);
        amountBox.setFilter(s -> s.matches("\\d*"));
        amountBox.setValue(amountText);
        addRenderableWidget(amountBox);
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_upgrade.donate"),
                b -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.DONATE, "", null, amount())))
                .bounds(left + 84, by - 1, 110, 18).build());
        com.tokyoghoulrpg.network.packet.ClanInfoS2C info = ClanScreen.info();
        String[] tabs = {"tab_levels", "tab_skills", "tab_quests", "tab_wars", "tab_rating", "tab_lands"};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_upgrade." + tabs[i]), b -> {
                tab = index;
                refresh();
            }).bounds(left + 8 + i * 53, top + 30, 51, 16).build()).active = tab != i;
        }
        if (tab == 5 && info != null && state != null) {
            boolean leader = isLeader(state);
            // «Домой» — любой участник; «Нанять стража» — глава, из казны
            Button home = Button.builder(Component.translatable("screen.tokyoghoulrpg.land.home"),
                    btn -> {
                        NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.HOME, "", null, 0));
                        onClose();
                    })
                    .bounds(left + WIDTH - 178, top + 48, 70, 14)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                            "screen.tokyoghoulrpg.land.home_tip", com.tokyoghoulrpg.clan.ClanTerritory.HOME_COOLDOWN_MS / 60000)))
                    .build();
            home.active = !info.lands.isEmpty();
            addRenderableWidget(home);
            int guards = 0;
            for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.Land land : info.lands) {
                guards += land.guards();
            }
            int guardMax = info.lands.size() * com.tokyoghoulrpg.combat.Balance.GUARD_PER_TERRITORY;
            Button hire = Button.builder(Component.translatable("screen.tokyoghoulrpg.land.hire",
                            com.tokyoghoulrpg.combat.Balance.GUARD_PRICE),
                    btn -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.HIRE_GUARD, "", null, 0)))
                    .bounds(left + WIDTH - 104, top + 48, 94, 14)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                            "screen.tokyoghoulrpg.land.hire_tip", guards, guardMax,
                            com.tokyoghoulrpg.combat.Balance.GUARD_PER_TERRITORY)))
                    .build();
            hire.active = leader && guards < guardMax && state.treasury >= com.tokyoghoulrpg.combat.Balance.GUARD_PRICE;
            addRenderableWidget(hire);
            int y = top + 64;
            for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.Land land : info.lands) {
                // улучшить эту территорию (пока не 80 x 80)
                Button up = Button.builder(land.upgradeCost() > 0
                                ? Component.translatable("screen.tokyoghoulrpg.land.upgrade", land.upgradeCost())
                                : Component.translatable("screen.tokyoghoulrpg.land.maxed"),
                        btn -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.LAND_UPGRADE, land.key(), null, 0)))
                        .bounds(left + WIDTH - 200, y, 92, 14).build();
                up.active = leader && land.upgradeCost() > 0 && state.treasury >= land.upgradeCost();
                addRenderableWidget(up);
                Button b;
                if (land.main()) {
                    b = Button.builder(Component.translatable("screen.tokyoghoulrpg.land.main"), btn -> { }).bounds(left + WIDTH - 104, y, 94, 14).build();
                    b.active = false;
                } else if (land.price() > 0) {
                    b = Button.builder(Component.translatable("screen.tokyoghoulrpg.land.cancel", land.price()),
                            btn -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.LAND_CANCEL, land.key(), null, 0)))
                            .bounds(left + WIDTH - 104, y, 94, 14).build();
                    b.active = leader;
                } else {
                    b = Button.builder(Component.translatable("screen.tokyoghoulrpg.land.sell"),
                            btn -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.LAND_SELL, land.key(), null, amount())))
                            .bounds(left + WIDTH - 104, y, 94, 14).build();
                    b.active = leader;
                }
                addRenderableWidget(b);
                y += 15;
            }
            y = top + 64 + Math.max(1, info.lands.size()) * 15 + 16;
            int shown = 0;
            for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.LandOffer offer : info.offers) {
                if (shown++ >= 3) {
                    break;
                }
                Button buy = Button.builder(Component.translatable("screen.tokyoghoulrpg.land.buy", offer.price()),
                        btn -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.LAND_BUY, offer.key(), offer.clan(), 0)))
                        .bounds(left + WIDTH - 104, y, 94, 14).build();
                buy.active = leader && state.treasury >= offer.price();
                addRenderableWidget(buy);
                y += 15;
            }
        }
        if (tab == 3 && info != null && state != null) {
            int y = top + 92;
            int shown = 0;
            for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.WarTarget t : info.targets) {
                if (shown++ >= WAR_ROWS) {
                    break;
                }
                Button declare = Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_war.declare"),
                        b -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.DECLARE_WAR, "", t.id(), 0)))
                        .bounds(left + WIDTH - 74, y, 64, 14)
                        .tooltip(t.reason().isEmpty() ? null : net.minecraft.client.gui.components.Tooltip.create(
                                Component.translatable("message.tokyoghoulrpg." + t.reason())))
                        .build();
                declare.active = isLeader(state) && t.reason().isEmpty();
                addRenderableWidget(declare);
                // союз: предложить / принять / отозвать / разорвать
                String allyKey = switch (t.allyState()) {
                    case com.tokyoghoulrpg.clan.ClanAlliances.ALLIED -> "break";
                    case com.tokyoghoulrpg.clan.ClanAlliances.THEY_INVITED -> "accept";
                    case com.tokyoghoulrpg.clan.ClanAlliances.WE_INVITED -> "withdraw";
                    default -> "propose";
                };
                int allyAction = t.allyState() == com.tokyoghoulrpg.clan.ClanAlliances.ALLIED
                        || t.allyState() == com.tokyoghoulrpg.clan.ClanAlliances.WE_INVITED
                        ? ClanManager.ALLY_BREAK : ClanManager.ALLY;
                Button ally = Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_ally." + allyKey),
                        b -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(allyAction, "", t.id(), 0)))
                        .bounds(left + WIDTH - 142, y, 66, 14)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                                "screen.tokyoghoulrpg.clan_ally." + allyKey + "_tip", t.name(),
                                com.tokyoghoulrpg.clan.ClanAlliances.MAX_ALLIES)))
                        .build();
                ally.active = isLeader(state);
                addRenderableWidget(ally);
                y += 15;
            }
        }
        if (tab == 1 && state != null && !state.clanName.isEmpty()) {
            ClanPerks.Skill[] all = ClanPerks.Skill.values();
            for (int i = 0; i < all.length && i < state.skills.length; i++) {
                int have = state.skills[i];
                if (have >= ClanPerks.SKILL_MAX) {
                    continue;
                }
                int next = have + 1;
                int cost = all[i].cost(next);
                final int index = i;
                Button buy = Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_upgrade.skill_buy",
                                com.tokyoghoulrpg.skill.SkillTree.roman(next), cost),
                        b -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.BUY_SKILL, "", null, index)))
                        .bounds(left + WIDTH - 104, top + 54 + i * SKILL_ROW, 94, 16)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                                "screen.tokyoghoulrpg.clan_upgrade.skill_needs", next + 1)))
                        .build();
                buy.active = isLeader(state) && state.treasury >= cost && state.level >= next + 1;
                addRenderableWidget(buy);
            }
        }
        if (tab == 0 && state != null && state.level < ClanPerks.MAX_LEVEL) {
            int cost = ClanPerks.COST[state.level + 1];
            Button upgrade = Button.builder(Component.translatable("screen.tokyoghoulrpg.clan_upgrade.buy", state.level + 1, cost),
                    b -> NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.UPGRADE, "", null, 0)))
                    .bounds(left + 198, by - 1, 122, 18).build();
            upgrade.active = isLeader(state) && state.treasury >= cost;
            addRenderableWidget(upgrade);
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
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFE0A050);
        ClanStateS2C state = ClanScreen.state();
        if (state == null || state.clanName.isEmpty()) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.loading"),
                    this.width / 2, top + HEIGHT / 2, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_upgrade.header", state.clanName,
                state.level), this.width / 2, top + 7, 0xE0A050);
        PlayerData data = Minecraft.getInstance().player == null ? null : PlayerData.get(Minecraft.getInstance().player);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_upgrade.treasury", state.treasury,
                data == null ? 0 : data.getBloodPoints()), this.width / 2, top + 19, 0xFF7A90);
        if (tab == 1) {
            renderSkills(gfx, state, left, top);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        if (tab >= 2) {
            com.tokyoghoulrpg.network.packet.ClanInfoS2C info = ClanScreen.info();
            if (info == null) {
                gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.clan.loading"),
                        this.width / 2, top + 90, 0x9A9A9A);
            } else if (tab == 2) {
                renderQuests(gfx, info, left, top);
            } else if (tab == 3) {
                renderWars(gfx, info, left, top);
            } else if (tab == 5) {
                renderLands(gfx, info, left, top);
            } else {
                renderRating(gfx, info, left, top);
            }
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }
        // таблица уровней
        int y = top + 52;
        int[] cols = {10, 42, 92, 142, 192, 242, 282};
        String[] heads = {"lvl", "cost", "storage", "blood", "hearts", "damage", "defense"};
        for (int c = 0; c < cols.length; c++) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_upgrade.col." + heads[c]),
                    left + cols[c], y, 0x9A9A9A, false);
        }
        y += 13;
        for (int level = 1; level <= ClanPerks.MAX_LEVEL; level++) {
            boolean have = level <= state.level;
            boolean next = level == state.level + 1;
            if (level == state.level) {
                gfx.fill(left + 6, y - 2, left + WIDTH - 6, y + 10, 0x40E0A050);
            }
            int color = have ? 0xE6E6E6 : next ? 0xE0C060 : 0x7A7A7A;
            String[] cells = {
                    String.valueOf(level),
                    level == 1 ? "-" : String.valueOf(ClanPerks.COST[level]),
                    String.valueOf(ClanPerks.STORAGE_ROWS[level] * 9),
                    "+" + Math.round(ClanPerks.BLOOD[level] * 100) + "%",
                    "+" + (int) (ClanPerks.HEALTH[level] / 2.0),
                    "+" + Math.round(ClanPerks.DAMAGE[level] * 100) + "%",
                    "-" + Math.round(ClanPerks.DEFENSE[level] * 100) + "%"};
            for (int c = 0; c < cols.length; c++) {
                gfx.drawString(this.font, cells[c], left + cols[c], y, color, false);
            }
            y += 12;
        }
        y += 6;
        Component hint = state.level >= ClanPerks.MAX_LEVEL
                ? Component.translatable("screen.tokyoghoulrpg.clan_upgrade.max")
                : Component.translatable(isLeader(state) ? "screen.tokyoghoulrpg.clan_upgrade.hint_leader"
                : "screen.tokyoghoulrpg.clan_upgrade.hint_member", ClanPerks.COST[state.level + 1]);
        gfx.drawWordWrap(this.font, hint, left + 10, y, WIDTH - 20, 0xB0B0B0);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    /** Вкладка «Задания»: два ежедневных и одно еженедельное с полосками, территория и клановый босс. */
    private void renderQuests(GuiGraphics gfx, com.tokyoghoulrpg.network.packet.ClanInfoS2C info, int left, int top) {
        int y = top + 52;
        for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.Quest q : info.quests) {
            com.tokyoghoulrpg.clan.ClanQuests.Kind kind = com.tokyoghoulrpg.clan.ClanQuests.kind(q.kind());
            boolean done = q.progress() >= q.target();
            gfx.drawString(this.font, Component.translatable(q.daily() ? "screen.tokyoghoulrpg.clan_quest.daily"
                            : "screen.tokyoghoulrpg.clan_quest.weekly",
                    Component.translatable("clan_quest.tokyoghoulrpg." + kind.id(), q.target())), left + 10, y,
                    done ? 0x6EE07A : 0xE6E6E6, false);
            int barW = WIDTH - 20;
            gfx.fill(left + 10, y + 11, left + 10 + barW, y + 15, 0xFF2A1A10);
            gfx.fill(left + 10, y + 11, left + 10 + (int) (barW * Math.min(1.0, q.progress() / (double) Math.max(1, q.target()))),
                    y + 15, done ? 0xFF4FB05A : 0xFFE0A050);
            Component reward = Component.translatable("screen.tokyoghoulrpg.clan_quest.progress", q.progress(), q.target(),
                    q.daily() ? com.tokyoghoulrpg.clan.ClanQuests.DAILY_BLOOD : com.tokyoghoulrpg.clan.ClanQuests.WEEKLY_BLOOD,
                    q.daily() ? com.tokyoghoulrpg.clan.ClanQuests.DAILY_POINTS : com.tokyoghoulrpg.clan.ClanQuests.WEEKLY_POINTS);
            gfx.drawString(this.font, reward, left + 10, y + 17, 0x9A9A9A, false);
            y += 30;
        }
        y += 2;
        Component territory = territoryLine(info);
        gfx.drawString(this.font, territory, left + 10, y, 0xC9C9C9, false);
        gfx.drawString(this.font, info.bossMinutes > 0
                ? Component.translatable("screen.tokyoghoulrpg.clan_boss.wait", info.bossMinutes)
                : Component.translatable("screen.tokyoghoulrpg.clan_boss.ready", com.tokyoghoulrpg.clan.ClanBoss.COST,
                com.tokyoghoulrpg.clan.ClanBoss.MIN_LEVEL), left + 10, y + 11, 0xC9C9C9, false);
    }

    /** Строка территории: «x y z|сколько|максимум». */
    private static Component territoryLine(com.tokyoghoulrpg.network.packet.ClanInfoS2C info) {
        String[] parts = info.territory.split("\\|");
        String pos = parts.length > 0 ? parts[0] : "-";
        String count = parts.length > 1 ? parts[1] : "0";
        String max = parts.length > 2 ? parts[2] : "1";
        String size = parts.length > 3 ? parts[3] : "?";
        if (count.equals("0")) {
            return Component.translatable("screen.tokyoghoulrpg.clan_territory.none", size);
        }
        return pos.equals("-")
                ? Component.translatable("screen.tokyoghoulrpg.clan_territory.captured_only", count, max, size)
                : Component.translatable("screen.tokyoghoulrpg.clan_territory.at", pos, info.income, count, max, size);
    }

    /** Вкладка «Войны»: своя война, победы, список кланов с кнопкой «Война!». */
    private void renderWars(GuiGraphics gfx, com.tokyoghoulrpg.network.packet.ClanInfoS2C info, int left, int top) {
        Component now = info.warEnemy.isEmpty()
                ? Component.translatable("screen.tokyoghoulrpg.clan_war.none", info.warWins, info.warLosses)
                : Component.translatable("screen.tokyoghoulrpg.clan_war.now", info.warEnemy, info.warOur, info.warTheir,
                com.tokyoghoulrpg.clan.ClanWars.TARGET);
        gfx.drawString(this.font, now, left + 10, top + 52, info.warEnemy.isEmpty() ? 0xC9C9C9 : 0xFF5050, false);
        if (info.protectedMinutes > 0) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_war.protected", info.protectedMinutes),
                    left + 10, top + 64, 0x7FD8E6, false);
        }
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_war.list"), left + 10, top + 80, 0xE0A050, false);
        int y = top + 92;
        int shown = 0;
        if (info.targets.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_war.no_clans"), left + 10, y + 3, 0x6A6A6A, false);
        }
        for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.WarTarget t : info.targets) {
            if (shown++ >= WAR_ROWS) {
                break;
            }
            boolean allied = t.allyState() == com.tokyoghoulrpg.clan.ClanAlliances.ALLIED;
            boolean invited = t.allyState() == com.tokyoghoulrpg.clan.ClanAlliances.THEY_INVITED;
            gfx.drawString(this.font, Component.translatable(allied ? "screen.tokyoghoulrpg.clan_war.row_ally"
                            : invited ? "screen.tokyoghoulrpg.clan_war.row_invite" : "screen.tokyoghoulrpg.clan_war.row",
                            t.name(), t.level(), t.members()),
                    left + 10, y + 3, allied ? 0x7FD8E6 : invited ? 0xE0C060 : t.reason().isEmpty() ? 0xE6E6E6 : 0x7A7A7A, false);
            y += 15;
        }
        gfx.drawWordWrap(this.font, Component.translatable("screen.tokyoghoulrpg.clan_war.rules",
                        com.tokyoghoulrpg.clan.ClanWars.TARGET, com.tokyoghoulrpg.clan.ClanWars.DURATION_MS / 60000),
                left + 10, top + 92 + WAR_ROWS * 15 + 2, WIDTH - 20, 0x8A8A8A);
    }

    /** Вкладка «Земли»: свои территории (продать захваченные) и чужие на продаже. */
    private void renderLands(GuiGraphics gfx, com.tokyoghoulrpg.network.packet.ClanInfoS2C info, int left, int top) {
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.land.own"), left + 10, top + 52, 0xE0A050, false);
        int y = top + 64;
        if (info.lands.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.land.none"), left + 10, y + 3, 0x6A6A6A, false);
        }
        for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.Land land : info.lands) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.land.row", land.place(), land.size(),
                            land.guards()),
                    left + 10, y + 3, land.main() ? 0xFFE6A0 : 0xE6E6E6, false);
            y += 15;
        }
        y = top + 64 + Math.max(1, info.lands.size()) * 15 + 4;
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.land.market"), left + 10, y, 0xE0A050, false);
        y += 12;
        if (info.offers.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.land.no_offers"), left + 10, y + 3, 0x6A6A6A, false);
        }
        int shown = 0;
        for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.LandOffer offer : info.offers) {
            if (shown++ >= 3) {
                break;
            }
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.land.offer", offer.clanName(), offer.place()),
                    left + 10, y + 3, 0xC9C9C9, false);
            y += 15;
        }
        gfx.drawWordWrap(this.font, Component.translatable("screen.tokyoghoulrpg.land.rules"), left + 10, top + HEIGHT - 82,
                WIDTH - 20, 0x8A8A8A);
    }

    /** Вкладка «Рейтинг»: десять лучших кланов. */
    private void renderRating(GuiGraphics gfx, com.tokyoghoulrpg.network.packet.ClanInfoS2C info, int left, int top) {
        int[] cols = {10, 26, 150, 190, 236, 276};
        String[] heads = {"#", "name", "level", "members", "wins", "score"};
        int y = top + 52;
        for (int c = 0; c < cols.length; c++) {
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_rating." + heads[c]),
                    left + cols[c], y, 0x9A9A9A, false);
        }
        y += 12;
        int place = 1;
        for (com.tokyoghoulrpg.network.packet.ClanInfoS2C.Rating r : info.rating) {
            if (r.own()) {
                gfx.fill(left + 6, y - 2, left + WIDTH - 6, y + 9, 0x40E0A050);
            }
            int color = place == 1 ? 0xFFD700 : r.own() ? 0xE6E6E6 : 0xB0B0B0;
            String[] cells = {String.valueOf(place), r.name(), String.valueOf(r.level()), String.valueOf(r.members()),
                    String.valueOf(r.wins()), String.valueOf(r.score())};
            for (int c = 0; c < cols.length; c++) {
                gfx.drawString(this.font, cells[c], left + cols[c], y, color, false);
            }
            y += 11;
            place++;
        }
        gfx.drawWordWrap(this.font, Component.translatable("screen.tokyoghoulrpg.clan_rating.rules"), left + 10,
                top + HEIGHT - 82, WIDTH - 20, 0x8A8A8A);
    }

    /** Вкладка «Умения»: название, уровень и что даёт сейчас / на следующем уровне. */
    private void renderSkills(GuiGraphics gfx, ClanStateS2C state, int left, int top) {
        ClanPerks.Skill[] all = ClanPerks.Skill.values();
        for (int i = 0; i < all.length && i < state.skills.length; i++) {
            ClanPerks.Skill skill = all[i];
            int have = state.skills[i];
            int y = top + 52 + i * SKILL_ROW;
            gfx.fill(left + 6, y - 2, left + WIDTH - 6, y + SKILL_ROW - 4, 0x30201010);
            gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_upgrade.skill_title",
                            Component.translatable("clan_skill.tokyoghoulrpg." + skill.id()),
                            have == 0 ? "-" : com.tokyoghoulrpg.skill.SkillTree.roman(have), ClanPerks.SKILL_MAX),
                    left + 10, y, have > 0 ? 0xE0C060 : 0xB0B0B0, false);
            int shown = Math.max(1, have);
            Component effect = Component.translatable("clan_skill.tokyoghoulrpg." + skill.id() + ".effect",
                    effectArgs(skill, shown));
            gfx.drawString(this.font, have == 0 ? Component.translatable("screen.tokyoghoulrpg.clan_upgrade.skill_first", effect)
                    : effect, left + 10, y + 11, 0x9A9A9A, false);
        }
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.clan_upgrade.skill_hint"),
                left + 10, top + 54 + all.length * SKILL_ROW - 4, 0x7A7A7A, false);
    }

    /** Числа для описания умения на этом уровне. */
    private static Object[] effectArgs(ClanPerks.Skill skill, int level) {
        long percent = Math.round(skill.value(level) * 100);
        if (skill == ClanPerks.Skill.BOND) {
            return new Object[] {percent, Math.round(ClanPerks.BOND_HP_PER_LEVEL * level * 100), ClanPerks.BOND_COOLDOWN[level] / 20};
        }
        return new Object[] {percent};
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
