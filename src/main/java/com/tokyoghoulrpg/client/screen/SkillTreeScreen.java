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

/**
 * Дерево навыков (клавиша B). У гуля и полугуля четыре вкладки (Основа, Способности, Выживание,
 * Ярость и кагуне), у человека одна. У агента CCG дерева пока нет.
 * На вкладке каждая ветка в своей строке, уровни слева направо. Клик по доступному навыку открывает его.
 */
public class SkillTreeScreen extends Screen {
    private static final int NODE = 18;
    private static final int LINK = 12;
    private static final int LABEL_W = 104;
    private static final int ROW_STEP = 24;
    private static final int TAB_H = 14;
    private static final int MAX_ROWS = 6;
    private static final int TOTAL_H = 26 + TAB_H + 10 + ROW_STEP * (MAX_ROWS - 1) + NODE + 8 + 36;

    /** Последняя открытая вкладка, чтобы при повторном открытии меню вернуться на неё. */
    private static SkillTree.Tab lastTab = SkillTree.Tab.BASE;

    private SkillTree.Tab tab = lastTab;

    /** Вкладки дерева для класса игрока. */
    private List<SkillTree.Tab> tabs() {
        PlayerData data = data();
        return data == null ? List.of() : SkillTree.tabs(data.getPlayerClass());
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
        return Math.max(4, (this.height - TOTAL_H) / 2);
    }

    private int contentWidth() {
        return LABEL_W + 5 * NODE + 4 * LINK;
    }

    private int left() {
        return (this.width - contentWidth()) / 2;
    }

    private int tabsY() {
        return top() + 26;
    }

    private int tabWidth() {
        return contentWidth() / Math.max(1, tabs().size());
    }

    private int rowY(int row) {
        return tabsY() + TAB_H + 10 + row * ROW_STEP;
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
        if (data.getSkillPoints() < skill.cost()) {
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
        for (int i = 0; i < tabs.size(); i++) {
            int x = left() + i * tabWidth();
            if (mouseX >= x && mouseX < x + tabWidth()) {
                return tabs.get(i);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Отрисовка
    // ------------------------------------------------------------------

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xE0120405, 0xE0000000);
        int top = top();
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top, 0xC21B1B);

        PlayerData data = data();
        if (data != null) {
            Component points = Component.translatable("screen.tokyoghoulrpg.skill_tree.points", data.getSkillPoints());
            Component blood = Component.translatable("screen.tokyoghoulrpg.skill_tree.blood", data.getBloodPoints());
            int gap = 16;
            int total = this.font.width(points) + gap + this.font.width(blood);
            int x = this.width / 2 - total / 2;
            gfx.drawString(this.font, points, x, top + 12, 0xE6E6E6, false);
            gfx.drawString(this.font, blood, x + this.font.width(points) + gap, top + 12, 0xFF5A78, false);
        }
        if (data == null || tabs().isEmpty()) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.later"),
                    this.width / 2, this.height / 2 - 8, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        drawTabs(gfx, mouseX, mouseY);

        List<SkillTree.Branch> branches = SkillTree.branches(currentTab());
        for (int row = 0; row < branches.size(); row++) {
            drawBranch(gfx, data, branches.get(row), rowY(row), mouseX, mouseY);
        }

        int infoY = rowY(MAX_ROWS - 1) + NODE + 8;
        SkillTree.Skill hovered = skillAt(mouseX, mouseY);
        if (hovered != null) {
            drawInfo(gfx, data, hovered, infoY);
        } else {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.hint"),
                    this.width / 2, infoY, 0x777777);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphics gfx, int mouseX, int mouseY) {
        List<SkillTree.Tab> tabs = tabs();
        int y = tabsY();
        for (int i = 0; i < tabs.size(); i++) {
            int x = left() + i * tabWidth();
            boolean selected = tabs.get(i) == currentTab();
            boolean hovered = mouseX >= x && mouseX < x + tabWidth() && mouseY >= y && mouseY < y + TAB_H;
            int back = selected ? 0xFF4A0E0E : (hovered ? 0xFF2A1416 : 0xFF180A0C);
            gfx.fill(x + 1, y, x + tabWidth() - 1, y + TAB_H, back);
            if (selected) {
                gfx.fill(x + 1, y + TAB_H - 2, x + tabWidth() - 1, y + TAB_H, 0xFFB01E1E);
            }
            gfx.drawCenteredString(this.font, tabs.get(i).title(), x + tabWidth() / 2, y + 3,
                    selected ? 0xFFFFFF : 0x9A9A9A);
        }
    }

    private void drawBranch(GuiGraphics gfx, PlayerData data, SkillTree.Branch branch, int y, int mouseX, int mouseY) {
        List<SkillTree.Skill> skills = SkillTree.branch(branch);
        gfx.fill(left(), y + 3, left() + 2, y + NODE - 3, 0xFF000000 | branch.color());
        gfx.drawString(this.font, branch.title(), left() + 6, y + NODE / 2 - 4, 0xD0D0D0, false);

        for (int i = 0; i < skills.size(); i++) {
            SkillTree.Skill skill = skills.get(i);
            int x = nodeX(i);

            if (i < skills.size() - 1) {
                boolean opened = data.hasSkill(skills.get(i + 1).id());
                gfx.fill(x + NODE, y + NODE / 2 - 1, x + NODE + LINK, y + NODE / 2 + 1,
                        opened ? 0xFF000000 | branch.color() : 0xFF3A3A3A);
            }

            State state = stateOf(data, skill);
            int border = switch (state) {
                case UNLOCKED -> 0xFF000000 | branch.color();
                case LOCKED -> 0xFF3A3A3A;
                default -> 0xFF000000 | state.color;
            };
            int inner = state == State.UNLOCKED ? dim(branch.color()) : 0xFF14090A;
            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                gfx.fill(x - 1, y - 1, x + NODE + 1, y + NODE + 1, 0xFFFFFFFF);
            }
            gfx.fill(x, y, x + NODE, y + NODE, border);
            gfx.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, inner);
            gfx.drawCenteredString(this.font, branch.shownTier(skill.tier()), x + NODE / 2, y + NODE / 2 - 4,
                    state == State.LOCKED ? 0x777777 : 0xFFFFFF);
        }
    }

    private void drawInfo(GuiGraphics gfx, PlayerData data, SkillTree.Skill skill, int y) {
        int centerX = this.width / 2;
        State state = stateOf(data, skill);
        gfx.drawCenteredString(this.font, skill.branch().skillName(skill.tier()), centerX, y, 0xE6E6E6);
        gfx.drawCenteredString(this.font, skill.branch().effect(skill.tier()), centerX, y + 12, 0xC9C9C9);
        Component costLine = Component.translatable("screen.tokyoghoulrpg.skill_tree.cost", skill.cost())
                .append("   ")
                .append(Component.translatable(state.key));
        gfx.drawCenteredString(this.font, costLine, centerX, y + 24, state.color);
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
