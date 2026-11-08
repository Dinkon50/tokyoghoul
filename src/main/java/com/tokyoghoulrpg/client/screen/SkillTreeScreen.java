package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.UnlockSkillC2S;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;

/**
 * Дерево навыков (клавиша B). Вкладки зависят от класса и кагуне; на вкладке каждая ветка в своей строке,
 * уровни слева направо. Клик по доступному навыку открывает его.
 * Оформление в цветах класса (ScreenFx.theme): фон с частицами, панель с рамкой, «чипы» очков и крови,
 * полоса прокачки до лимита 85%, вкладки-закладки, светящиеся открытые навыки, пульсирующие доступные,
 * карточка навыка под деревом.
 */
public class SkillTreeScreen extends Screen {
    private static final int NODE = 18;
    private static final int LINK = 12;
    private static final int LABEL_W = 104;
    private static final int TAB_H = 16;
    private static final int MAX_ROWS = 8;
    private static final int HEADER_H = 54;
    private static final int INFO_H = 54;
    private static final int PAD = 12;

    /** Шаг строк: на низком экране плотнее, чтобы всё поместилось. */
    private int rowStep() {
        return this.height < 340 ? 22 : 24;
    }

    private int totalH() {
        return HEADER_H + TAB_H + 8 + rowStep() * (MAX_ROWS - 1) + NODE + 10 + INFO_H;
    }

    /** Последняя открытая вкладка, чтобы при повторном открытии меню вернуться на неё. */
    private static SkillTree.Tab lastTab = SkillTree.Tab.BASE;

    private SkillTree.Tab tab = lastTab;

    /** Вкладки дерева для класса игрока. */
    private List<SkillTree.Tab> tabs() {
        PlayerData data = data();
        return data == null ? List.of() : SkillTree.tabs(data.getPlayerClass(), data.getKaguneVariant());
    }

    /** Если сохранённая вкладка от другого класса, открываем первую вкладку своего. */
    private SkillTree.Tab currentTab() {
        List<SkillTree.Tab> tabs = tabs();
        if (!tabs.isEmpty() && !tabs.contains(tab)) {
            tab = tabs.get(0);
        }
        return tab;
    }

    private enum State {
        UNLOCKED("screen.tokyoghoulrpg.skill_tree.state_unlocked", 0xB01E1E),
        AVAILABLE("screen.tokyoghoulrpg.skill_tree.state_available", 0xE6E6E6),
        NO_POINTS("screen.tokyoghoulrpg.skill_tree.state_no_points", 0xC9822B),
        CAP("screen.tokyoghoulrpg.skill_tree.state_cap", 0x9A5A7A),
        LOCKED("screen.tokyoghoulrpg.skill_tree.state_locked", 0x777777);

        private final String key;
        private final int color;

        State(String key, int color) {
            this.key = key;
            this.color = color;
        }
    }

    public SkillTreeScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.skill_tree.title"));
    }

    private PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    // ------------------------------------------------------------------
    // Разметка
    // ------------------------------------------------------------------

    private int top() {
        return Math.max(4, (this.height - totalH()) / 2);
    }

    private int contentWidth() {
        return LABEL_W + 5 * NODE + 4 * LINK;
    }

    private int left() {
        return (this.width - contentWidth()) / 2;
    }

    private int tabsY() {
        return top() + HEADER_H;
    }

    /**
     * Границы вкладок по X (на одну больше, чем вкладок). Ширина вкладки по её названию, чтобы влезали
     * и пять вкладок; если места хватает, вкладки растягиваются на ширину дерева.
     */
    private int[] tabEdges() {
        List<SkillTree.Tab> tabs = tabs();
        int[] widths = new int[tabs.size()];
        int total = 0;
        for (int i = 0; i < tabs.size(); i++) {
            widths[i] = this.font.width(tabs.get(i).title()) + 12;
            total += widths[i];
        }
        int extra = tabs.isEmpty() ? 0 : Math.max(0, contentWidth() - total) / tabs.size();
        total += extra * tabs.size();
        int[] edges = new int[tabs.size() + 1];
        edges[0] = this.width / 2 - total / 2;
        for (int i = 0; i < tabs.size(); i++) {
            edges[i + 1] = edges[i] + widths[i] + extra;
        }
        return edges;
    }

    private int rowY(int row) {
        return tabsY() + TAB_H + 8 + row * rowStep();
    }

    private int nodeX(int index) {
        return left() + LABEL_W + index * (NODE + LINK);
    }

    private static State stateOf(PlayerData data, SkillTree.Skill skill) {
        if (data.hasSkill(skill.id())) {
            return State.UNLOCKED;
        }
        if (skill.requires() != null && !data.hasSkill(skill.requires())) {
            return State.LOCKED;
        }
        if (SkillTree.overCap(data.getPlayerClass(), data.getKaguneVariant(), data.getSkills(), skill, difficulty())) {
            return State.CAP;
        }
        if (data.getSkillPoints() < cost(skill)) {
            return State.NO_POINTS;
        }
        return State.AVAILABLE;
    }

    private SkillTree.Skill skillAt(double mouseX, double mouseY) {
        List<SkillTree.Branch> branches = SkillTree.branches(currentTab());
        for (int row = 0; row < branches.size(); row++) {
            List<SkillTree.Skill> skills = SkillTree.branch(branches.get(row));
            int y = rowY(row);
            for (int i = 0; i < skills.size(); i++) {
                int x = nodeX(i);
                if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                    return skills.get(i);
                }
            }
        }
        return null;
    }

    private SkillTree.Tab tabAt(double mouseX, double mouseY) {
        int y = tabsY();
        if (mouseY < y || mouseY >= y + TAB_H) {
            return null;
        }
        List<SkillTree.Tab> tabs = tabs();
        int[] edges = tabEdges();
        for (int i = 0; i < tabs.size(); i++) {
            if (mouseX >= edges[i] && mouseX < edges[i + 1]) {
                return tabs.get(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Отрисовка
    // ------------------------------------------------------------------

    private ScreenFx.Theme theme(PlayerData data) {
        return ScreenFx.theme(data == null ? com.tokyoghoulrpg.data.PlayerClass.GHOUL : data.getPlayerClass());
    }

    private static float time() {
        return (net.minecraft.Util.getMillis() % 1_000_000L) / 1000.0F;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        PlayerData data = data();
        ScreenFx.Theme theme = theme(data);
        float time = time();
        // фон в цветах класса
        gfx.fillGradient(0, 0, this.width, this.height, theme.top(), theme.bottom());
        ScreenFx.glow(gfx, this.width / 2, this.height / 2, (int) (this.height * 0.55F), theme.glow());
        drawBackdrop(gfx, data, time, theme);
        ScreenFx.particles(gfx, 0, this.width, this.height, time, 60, theme, 0.55F);

        int top = top();
        int panelW = Math.max(contentWidth() + PAD * 2, chipsWidth(data) + 16);
        int panelX = this.width / 2 - panelW / 2;
        int panelH = Math.min(this.height - top - 4, totalH() - INFO_H - 6 + 8);
        ScreenFx.panel(gfx, panelX, top - 6, panelW, panelH, theme.accent(), 0.35F);

        ScreenFx.bigText(gfx, this.font, this.title, this.width / 2, top, 1.5F, theme.accent(), 0xFF000000);
        if (data != null) {
            // очки, кровь и репутация — «чипы» в ряд
            Component points = Component.translatable("screen.tokyoghoulrpg.skill_tree.points", data.getSkillPoints());
            Component blood = Component.translatable("screen.tokyoghoulrpg.skill_tree.blood", data.getBloodPoints());
            Component rep = Component.translatable("screen.tokyoghoulrpg.skill_tree.rep",
                    signed(data.getStanding()), signed(data.getRepCcg()), signed(data.getRepGhouls()));
            int gap = 6;
            int total = this.font.width(points) + this.font.width(blood) + this.font.width(rep) + 16 * 3 + gap * 2;
            int x = this.width / 2 - total / 2;
            int chipY = top + 17;
            x += ScreenFx.chip(gfx, this.font, points, x, chipY, data.getSkillPoints() > 0 ? 0xFFD133 : 0xB0B0B0) + gap;
            x += ScreenFx.chip(gfx, this.font, blood, x, chipY, 0xFF5A78) + gap;
            ScreenFx.chip(gfx, this.font, rep, x, chipY, 0x9AB0C9);
            if (!tabs().isEmpty()) {
                // сколько дерева уже открыто и сколько можно открыть всего (85%)
                int spent = SkillTree.spentCost(data.getPlayerClass(), data.getKaguneVariant(), data.getSkills(), difficulty());
                int cap = SkillTree.capCost(data.getPlayerClass(), data.getKaguneVariant(), difficulty());
                int totalCost = SkillTree.totalCost(data.getPlayerClass(), data.getKaguneVariant(), difficulty());
                Component capLine = Component.translatable("screen.tokyoghoulrpg.skill_tree.cap", spent, cap, totalCost);
                int barW = contentWidth();
                int barX = left();
                int barY = top + 42;
                gfx.drawCenteredString(this.font, capLine, this.width / 2, top + 33, 0xFFB0A0B8);
                gfx.fill(barX, barY, barX + barW, barY + 4, 0xFF1A0E12);
                float fraction = cap <= 0 ? 0.0F : Mth.clamp(spent / (float) cap, 0.0F, 1.0F);
                gfx.fillGradient(barX, barY, barX + Math.round(barW * fraction), barY + 4,
                        0xFF000000 | (theme.accent2() & 0xFFFFFF), 0xFF000000 | (theme.accent() & 0xFFFFFF));
                ScreenFx.frame(gfx, barX - 1, barY - 1, barW + 2, 6, 0x40FFFFFF);
            }
        }
        if (data == null || tabs().isEmpty()) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.later"),
                    this.width / 2, this.height / 2 - 8, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        drawTabs(gfx, mouseX, mouseY, theme);

        List<SkillTree.Branch> branches = SkillTree.branches(currentTab());
        for (int row = 0; row < branches.size(); row++) {
            drawBranch(gfx, data, branches.get(row), rowY(row), mouseX, mouseY, theme, time);
        }

        int infoY = Math.min(rowY(MAX_ROWS - 1) + NODE + 14, this.height - 34);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.hint"),
                this.width / 2, infoY + 6, 0xFF8A8A8A);
        drawLegend(gfx, infoY + 22);
        super.render(gfx, mouseX, mouseY, partialTick);
        SkillTree.Skill hovered = skillAt(mouseX, mouseY);
        if (hovered != null) {
            drawTooltip(gfx, data, hovered, mouseX, mouseY);
        }
    }

    /**
     * Кого прокачиваешь — большой персонаж за деревом: слева и зеркально справа, затемнён к низу.
     * Гуль и полугуль — со своим кагуне (Канеки, крылья Тоуки, «Гурман»), человек — Стив, агент CCG — с квинке.
     */
    private void drawBackdrop(GuiGraphics gfx, PlayerData data, float time, ScreenFx.Theme theme) {
        ScreenFx.backdrop(gfx, this.width, this.height, data, time, theme, 0.8F);
    }

    /** Ширина ряда «чипов» (очки, кровь, репутация): панель не уже его. */
    private int chipsWidth(PlayerData data) {
        if (data == null) {
            return 0;
        }
        Component points = Component.translatable("screen.tokyoghoulrpg.skill_tree.points", data.getSkillPoints());
        Component blood = Component.translatable("screen.tokyoghoulrpg.skill_tree.blood", data.getBloodPoints());
        Component rep = Component.translatable("screen.tokyoghoulrpg.skill_tree.rep",
                signed(data.getStanding()), signed(data.getRepCcg()), signed(data.getRepGhouls()));
        return this.font.width(points) + this.font.width(blood) + this.font.width(rep) + 16 * 3 + 6 * 2;
    }

    /** Вкладки-закладки: выбранная залита цветом класса с полосой снизу, под мышью светлее. */
    private void drawTabs(GuiGraphics gfx, int mouseX, int mouseY, ScreenFx.Theme theme) {
        List<SkillTree.Tab> tabs = tabs();
        int[] edges = tabEdges();
        int y = tabsY();
        int accent = theme.accent() & 0xFFFFFF;
        gfx.fill(left() - PAD + 1, y + TAB_H, left() + contentWidth() + PAD - 1, y + TAB_H + 1, ScreenFx.alpha(accent, 0x60));
        for (int i = 0; i < tabs.size(); i++) {
            int x = edges[i];
            int w = edges[i + 1] - edges[i];
            boolean selected = tabs.get(i) == currentTab();
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_H;
            if (selected) {
                gfx.fillGradient(x + 1, y, x + w - 1, y + TAB_H, ScreenFx.alpha(accent, 0x70), ScreenFx.alpha(accent, 0x20));
                gfx.fill(x + 1, y + TAB_H - 2, x + w - 1, y + TAB_H, 0xFF000000 | accent);
                gfx.fill(x + 1, y, x + w - 1, y + 1, ScreenFx.alpha(accent, 0xA0));
            } else {
                gfx.fill(x + 1, y + 2, x + w - 1, y + TAB_H, hovered ? 0x22FFFFFF : 0x50000000);
            }
            gfx.drawCenteredString(this.font, tabs.get(i).title(), x + w / 2, y + 4,
                    selected ? 0xFFFFFFFF : (hovered ? 0xFFDADADA : 0xFF8A8A8A));
        }
    }

    private void drawBranch(GuiGraphics gfx, PlayerData data, SkillTree.Branch branch, int y, int mouseX, int mouseY,
            ScreenFx.Theme theme, float time) {
        List<SkillTree.Skill> skills = SkillTree.branch(branch);
        int color = branch.color() & 0xFFFFFF;
        boolean rowHover = mouseY >= y - 2 && mouseY < y + NODE + 2 && mouseX >= left() - 4 && mouseX < left() + contentWidth() + 4;
        if (rowHover) {
            gfx.fill(left() - 4, y - 2, left() + contentWidth() + 4, y + NODE + 2, 0x14FFFFFF);
        }
        // полоса цвета ветки и ромбик
        gfx.fillGradient(left() - 4, y, left() - 2, y + NODE, 0xFF000000 | color, ScreenFx.alpha(color, 0x40));
        boolean hidden = isHidden(data, branch);
        int opened = 0;
        for (SkillTree.Skill skill : skills) {
            if (data.hasSkill(skill.id())) {
                opened++;
            }
        }
        gfx.drawString(this.font, hidden ? Component.literal("???") : branch.title(), left() + 2, y + 2,
                hidden ? 0xFF9A1030 : (rowHover ? 0xFFFFFFFF : 0xFFD0D0D0), false);
        gfx.drawString(this.font, opened + "/" + skills.size(), left() + 2, y + 11,
                opened == skills.size() ? 0xFF000000 | color : 0xFF6A6A6A, false);

        float pulse = 0.5F + 0.5F * Mth.sin(time * 5.0F);
        for (int i = 0; i < skills.size(); i++) {
            SkillTree.Skill skill = skills.get(i);
            int x = nodeX(i);

            if (i < skills.size() - 1) {
                boolean nextOpen = data.hasSkill(skills.get(i + 1).id());
                boolean thisOpen = data.hasSkill(skill.id());
                int ly = y + NODE / 2 - 1;
                if (nextOpen) {
                    gfx.fill(x + NODE, ly, x + NODE + LINK, ly + 2, 0xFF000000 | color);
                } else {
                    // пунктир: дальше ещё не открыто (ярче, если этот уже открыт)
                    int dash = thisOpen ? ScreenFx.alpha(color, 0xA0) : 0xFF3A3A3A;
                    for (int dx = 0; dx < LINK; dx += 4) {
                        gfx.fill(x + NODE + dx, ly, x + NODE + Math.min(LINK, dx + 2), ly + 2, dash);
                    }
                }
            }

            State state = stateOf(data, skill);
            boolean over = mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE;
            switch (state) {
                case UNLOCKED -> {
                    gfx.fill(x - 2, y - 2, x + NODE + 2, y + NODE + 2, ScreenFx.alpha(color, 0x45));
                    gfx.fill(x, y, x + NODE, y + NODE, 0xFF000000 | color);
                    gfx.fillGradient(x + 2, y + 2, x + NODE - 2, y + NODE - 2,
                            ScreenFx.mix(0xFF000000 | color, 0xFFFFFFFF, 0.25F), dim(color));
                }
                case AVAILABLE -> {
                    int border = ScreenFx.mix(0xFF000000 | color, 0xFFFFFFFF, 0.35F + 0.5F * pulse);
                    gfx.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, ScreenFx.alpha(color, (int) (0x30 + 0x50 * pulse)));
                    gfx.fill(x, y, x + NODE, y + NODE, border);
                    gfx.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, 0xFF1C1014);
                }
                case LOCKED -> {
                    gfx.fill(x, y, x + NODE, y + NODE, 0xFF2A2A2A);
                    gfx.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, 0xFF0E0A0B);
                }
                default -> {
                    gfx.fill(x, y, x + NODE, y + NODE, 0xFF000000 | state.color);
                    gfx.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, 0xFF140A0C);
                }
            }
            if (over) {
                ScreenFx.frame(gfx, x - 2, y - 2, NODE + 4, NODE + 4, 0xFFFFFFFF);
            }
            int textColor = switch (state) {
                case LOCKED -> 0xFF666666;
                case UNLOCKED -> 0xFFFFFFFF;
                default -> 0xFFE6E6E6;
            };
            gfx.drawCenteredString(this.font, hidden ? "?" : branch.shownTier(skill.tier()), x + NODE / 2, y + NODE / 2 - 4,
                    textColor);
        }
    }

    /**
     * Подсказка к навыку рядом с курсором (поверх дерева): название цветом ветки и уровень, цитата,
     * что даёт этот уровень и что сейчас (если предыдущий уже открыт), цена и состояние. Всё переносится по
     * словам, размер — по тексту, подсказка не выходит за экран.
     */
    private void drawTooltip(GuiGraphics gfx, PlayerData data, SkillTree.Skill skill, int mouseX, int mouseY) {
        int color = skill.branch().color() & 0xFFFFFF;
        int light = ScreenFx.mix(color, 0xFFFFFF, 0.35F);
        State state = stateOf(data, skill);
        boolean hidden = isHidden(data, skill.branch());
        com.tokyoghoulrpg.data.PlayerClass cls = data == null ? null : data.getPlayerClass();
        com.tokyoghoulrpg.data.KaguneVariant variant = data == null
                ? com.tokyoghoulrpg.data.KaguneVariant.NONE : data.getKaguneVariant();
        int w = Math.min(220, this.width - 16);
        int textW = w - 16;
        net.minecraft.client.gui.Font f = this.font;

        Component name = hidden ? Component.literal("???") : skill.branch().skillName(skill.tier());
        Component level = Component.translatable("screen.tokyoghoulrpg.skill_tree.level", skill.tier(),
                skill.branch().levels());
        List<net.minecraft.util.FormattedCharSequence> flavor = hidden || skill.branch().flavor() == null ? List.of()
                : f.split(skill.branch().flavor().copy().withStyle(net.minecraft.ChatFormatting.ITALIC), textW);
        List<net.minecraft.util.FormattedCharSequence> gives = f.split(hidden ? Component.literal("? ? ?")
                : skill.branch().effect(skill.tier(), cls, variant), textW - 6);
        boolean showNow = !hidden && skill.tier() > 1
                && (state == State.AVAILABLE || state == State.NO_POINTS || state == State.CAP);
        List<net.minecraft.util.FormattedCharSequence> now = showNow
                ? f.split(skill.branch().effect(skill.tier() - 1, cls, variant), textW - 6) : List.of();
        Component cost = Component.translatable("screen.tokyoghoulrpg.skill_tree.cost", cost(skill));
        List<net.minecraft.util.FormattedCharSequence> stateLines = f.split(Component.translatable(state.key), textW - 70);

        int h = 8 + 13 + 4;                       // название и уровень
        h += flavor.size() * 10 + (flavor.isEmpty() ? 0 : 4);
        h += 6 + 10 + gives.size() * 10;          // разделитель, «Этот уровень», текст
        if (!now.isEmpty()) {
            h += 4 + 10 + now.size() * 10;
        }
        h += 8 + Math.max(1, stateLines.size()) * 10 + (hardMode() ? 10 : 0) + 6;

        int x = mouseX + 14;
        if (x + w > this.width - 4) {
            x = mouseX - 14 - w;
        }
        x = Math.max(4, x);
        int y = Mth.clamp(mouseY - 12, 4, Math.max(4, this.height - h - 4));

        com.mojang.blaze3d.vertex.PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(0.0F, 0.0F, 500.0F);
        // тень, фон, рамка цвета ветки, полоса слева и уголки
        gfx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x70000000);
        gfx.fillGradient(x, y, x + w, y + h, 0xF6180B0E, 0xF6070304);
        ScreenFx.frame(gfx, x, y, w, h, 0xC0000000 | color);
        gfx.fillGradient(x, y, x + 3, y + h, 0xFF000000 | color, ScreenFx.alpha(color, 0x50));
        ScreenFx.corners(gfx, x, y, w, h, 0xFF000000 | light, 6);
        gfx.fillGradient(x + 1, y + 1, x + w - 1, y + 24, ScreenFx.alpha(color, 0x40), 0x00000000);

        int tx = x + 9;
        int ty = y + 7;
        // название (крупнее) и уровень справа
        ps.pushPose();
        ps.translate(tx, ty, 0.0F);
        ps.scale(1.2F, 1.2F, 1.0F);
        gfx.drawString(f, name, 0, 0, 0xFF000000 | light, true);
        ps.popPose();
        int lw = f.width(level);
        gfx.drawString(f, level, x + w - 8 - lw, ty + 2, 0xFF9A8A90, true);
        ty += 13;
        gfx.fill(tx, ty + 1, x + w - 8, ty + 2, ScreenFx.alpha(color, 0x90));
        ty += 6;
        for (net.minecraft.util.FormattedCharSequence line : flavor) {
            gfx.drawString(f, line, tx, ty, 0xFFFFD27A, true);
            ty += 10;
        }
        if (!flavor.isEmpty()) {
            ty += 4;
        }
        // что даёт этот уровень
        gfx.drawString(f, Component.translatable("screen.tokyoghoulrpg.skill_tree.gives"), tx, ty, 0xFF000000 | light, true);
        ty += 10;
        for (net.minecraft.util.FormattedCharSequence line : gives) {
            gfx.fill(tx + 1, ty + 3, tx + 3, ty + 5, 0xFF000000 | color);
            gfx.drawString(f, line, tx + 6, ty, 0xFFF0ECEC, true);
            ty += 10;
        }
        if (!now.isEmpty()) {
            ty += 4;
            gfx.drawString(f, Component.translatable("screen.tokyoghoulrpg.skill_tree.now"), tx, ty, 0xFF8A8A8A, true);
            ty += 10;
            for (net.minecraft.util.FormattedCharSequence line : now) {
                gfx.fill(tx + 1, ty + 3, tx + 3, ty + 5, 0xFF6A6A6A);
                gfx.drawString(f, line, tx + 6, ty, 0xFFB0A8AA, true);
                ty += 10;
            }
        }
        // цена и состояние
        ty += 4;
        gfx.fill(tx, ty, x + w - 8, ty + 1, 0x30FFFFFF);
        ty += 4;
        gfx.drawString(f, cost, tx, ty, 0xFFFFD133, true);
        int sx = x + w - 8;
        int stateColor = 0xFF000000 | (state == State.UNLOCKED ? light : state.color);
        for (net.minecraft.util.FormattedCharSequence line : stateLines) {
            gfx.drawString(f, line, sx - f.width(line), ty, stateColor, true);
            ty += 10;
        }
        if (stateLines.isEmpty()) {
            ty += 10;
        }
        if (hardMode()) {
            ps.pushPose();
            ps.translate(tx, ty, 0.0F);
            ps.scale(0.75F, 0.75F, 1.0F);
            gfx.drawString(f, Component.translatable("screen.tokyoghoulrpg.skill_tree.hard_note"), 0, 0, 0xFF9A8A90, true);
            ps.popPose();
        }
        ps.popPose();
    }

    /** Подсказка, что значат цвета навыков. */
    private void drawLegend(GuiGraphics gfx, int y) {
        State[] shown = {State.UNLOCKED, State.AVAILABLE, State.NO_POINTS, State.LOCKED};
        int[] colors = {0xE0303A, 0xFFFFFF, State.NO_POINTS.color, 0x555555};
        int total = 0;
        for (State st : shown) {
            total += 12 + this.font.width(Component.translatable(st.key)) + 10;
        }
        int x = this.width / 2 - total / 2;
        for (int i = 0; i < shown.length; i++) {
            gfx.fill(x, y + 1, x + 8, y + 9, 0xFF000000 | colors[i]);
            gfx.fill(x + 2, y + 3, x + 6, y + 7, 0xFF140A0C);
            Component t = Component.translatable(shown[i].key);
            gfx.drawString(this.font, t, x + 12, y + 1, 0xFF8A8A8A, false);
            x += 12 + this.font.width(t) + 10;
        }
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    /** Сложность мира приходит клиенту от сервера: на сложной навыки дороже. */
    private static Difficulty difficulty() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? Difficulty.NORMAL : mc.level.getDifficulty();
    }

    private static boolean hardMode() {
        return difficulty() == Difficulty.HARD;
    }

    private static int cost(SkillTree.Skill skill) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        return SkillTree.cost(skill, difficulty(), data == null ? null : data.getPlayerClass());
    }

    /** Секретная ветка скрыта знаками вопроса, пока её навык не открыт. */
    private static boolean isHidden(PlayerData data, SkillTree.Branch branch) {
        if (!branch.isSecret()) {
            return false;
        }
        for (SkillTree.Skill skill : SkillTree.branch(branch)) {
            if (data.hasSkill(skill.id())) {
                return false;
            }
        }
        return true;
    }

    private static int dim(int rgb) {
        int r = ((rgb >> 16) & 0xFF) * 3 / 10;
        int g = ((rgb >> 8) & 0xFF) * 3 / 10;
        int b = (rgb & 0xFF) * 3 / 10;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------
    // Ввод
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PlayerData data = data();
        if (button == 0 && data != null && !tabs().isEmpty()) {
            SkillTree.Tab clickedTab = tabAt(mouseX, mouseY);
            if (clickedTab != null) {
                tab = clickedTab;
                lastTab = clickedTab;
                return true;
            }
            SkillTree.Skill skill = skillAt(mouseX, mouseY);
            if (skill != null) {
                if (stateOf(data, skill) == State.AVAILABLE) {
                    NetworkHandler.CHANNEL.sendToServer(new UnlockSkillC2S(skill.id()));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.SKILL_TREE.matches(keyCode, scanCode)) {
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
