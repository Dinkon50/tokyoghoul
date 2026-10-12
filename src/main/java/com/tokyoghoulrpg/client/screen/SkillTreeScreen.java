package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.combat.RageStats;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.UnlockSkillC2S;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Дерево навыков (клавиша B). Пока одна ветка: улучшения ярости. Клик по навыку открывает его. */
public class SkillTreeScreen extends Screen {
    private static final int NODE = 30;
    private static final int GAP = 26;

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

    private int startX(int count) {
        return this.width / 2 - (count * NODE + (count - 1) * GAP) / 2;
    }

    private int nodeY() {
        return this.height / 2 - 30;
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

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xE0120405, 0xE0000000);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 90, 0xC21B1B);

        PlayerData data = data();
        if (data == null || !data.getPlayerClass().hasKagune()) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.later"),
                    this.width / 2, this.height / 2 - 8, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        gfx.drawCenteredString(this.font,
                Component.translatable("screen.tokyoghoulrpg.skill_tree.points", data.getSkillPoints()),
                this.width / 2, this.height / 2 - 76, 0xE6E6E6);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.branch_rage"),
                this.width / 2, this.height / 2 - 62, 0x9A9A9A);

        List<SkillTree.Skill> skills = SkillTree.rageBranch();
        int x0 = startX(skills.size());
        int y = nodeY();
        SkillTree.Skill hovered = null;

        for (int i = 0; i < skills.size(); i++) {
            SkillTree.Skill skill = skills.get(i);
            int x = x0 + i * (NODE + GAP);

            // Линия к следующему навыку: красная, если следующий уже открыт
            if (i < skills.size() - 1) {
                boolean opened = data.hasSkill(skills.get(i + 1).id());
                gfx.fill(x + NODE, y + NODE / 2 - 1, x + NODE + GAP, y + NODE / 2 + 1,
                        opened ? 0xFFB01E1E : 0xFF3A3A3A);
            }

            State state = stateOf(data, skill);
            int border = 0xFF000000 | state.color;
            if (state == State.LOCKED) {
                border = 0xFF3A3A3A;
            }
            int inner = state == State.UNLOCKED ? 0xFF4A0E0E : 0xFF14090A;
            gfx.fill(x, y, x + NODE, y + NODE, border);
            gfx.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 2, inner);
            gfx.drawCenteredString(this.font, SkillTree.roman(skill.tier()), x + NODE / 2, y + NODE / 2 - 4,
                    state == State.LOCKED ? 0x777777 : 0xFFFFFF);

            if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                hovered = skill;
            }
        }

        int infoY = y + NODE + 14;
        if (hovered != null) {
            drawInfo(gfx, data, hovered, infoY);
        } else {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.hint"),
                    this.width / 2, infoY, 0x777777);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawInfo(GuiGraphics gfx, PlayerData data, SkillTree.Skill skill, int y) {
        int centerX = this.width / 2;
        State state = stateOf(data, skill);
        gfx.drawCenteredString(this.font, Component.translatable("skill.tokyoghoulrpg.rage", SkillTree.roman(skill.tier())),
                centerX, y, 0xE6E6E6);
        gfx.drawCenteredString(this.font, effectText(skill.tier()), centerX, y + 12, 0xC9C9C9);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.skill_tree.cost", skill.cost()),
                centerX, y + 24, 0x9A9A9A);
        gfx.drawCenteredString(this.font, Component.translatable(state.key), centerX, y + 36, state.color);
    }

    /** Что даёт уровень ярости: значения считаются по тем же формулам, что и в бою. */
    private static Component effectText(int level) {
        String duration = (RageStats.durationTicks(level) / 20) + " с";
        String damage = "+" + Math.round(RageStats.damageBonus(level) * 100.0) + "%";
        String speed = "+" + Math.round(RageStats.speedBonus(level) * 100.0) + "%";
        String incoming = "-" + Math.round(RageStats.damageReduction(level) * 100.0) + "%";
        return Component.translatable("skill.tokyoghoulrpg.rage.effect", duration, damage, speed, incoming);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PlayerData data = data();
        if (button == 0 && data != null && data.getPlayerClass().hasKagune()) {
            List<SkillTree.Skill> skills = SkillTree.rageBranch();
            int x0 = startX(skills.size());
            int y = nodeY();
            for (int i = 0; i < skills.size(); i++) {
                int x = x0 + i * (NODE + GAP);
                if (mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE) {
                    SkillTree.Skill skill = skills.get(i);
                    if (stateOf(data, skill) == State.AVAILABLE) {
                        NetworkHandler.CHANNEL.sendToServer(new UnlockSkillC2S(skill.id()));
                    }
                    return true;
                }
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
