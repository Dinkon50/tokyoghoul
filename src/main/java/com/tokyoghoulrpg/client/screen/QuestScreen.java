package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.quest.Quest;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;

/**
 * Задания (клавиша N). Сюжетная цепочка класса по главам: заголовок главы, под ним её задания.
 * Выполненные отмечены, текущее подсвечено (под ним строчка сюжета и полоса прогресса), будущие приглушены.
 * Список длинный, поэтому листается колёсиком; при открытии сразу видно текущее задание.
 */
public class QuestScreen extends ModScreen {
    private static final int ROW_H = 16;
    private static final int ACT_H = 16;
    private static final int WIDTH = 340;
    private static final int LIST_TOP = 34;

    /** Текущее задание выше: название, что сделать и строчки сюжета (с переносом), полоса прогресса. */
    private int activeH(Quest quest) {
        return 22 + 10 * (descLines(quest).size() + storyLines(quest).size());
    }

    private List<net.minecraft.util.FormattedCharSequence> descLines(Quest quest) {
        return this.font.split(quest.description(), WIDTH - 24);
    }

    private List<net.minecraft.util.FormattedCharSequence> storyLines(Quest quest) {
        return this.font.split(quest.story(), WIDTH - 24);
    }

    private double scroll = 0.0;
    private boolean scrolledToCurrent = false;

    public QuestScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.quests.title"));
    }

    private static PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    /** Высота всего списка и положение текущего задания (для прокрутки к нему). */
    private int[] layout(List<Quest> quests, int current) {
        int y = 0;
        int act = -1;
        int currentY = 0;
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            if (quest.act() != act) {
                act = quest.act();
                y += ACT_H;
            }
            if (i == current) {
                currentY = y;
            }
            y += i == current ? activeH(quest) : ROW_H;
        }
        return new int[] {y, currentY};
    }

    private int listBottom() {
        return this.height - 16;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        modBackground(gfx);
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = data();

        Quest.Chain chain = data == null ? null : Quest.Chain.of(data.getPlayerClass());
        List<Quest> quests = chain == null ? List.of() : Quest.chain(chain);
        int left = (this.width - WIDTH) / 2;
        modPanel(gfx, left - 8, 2, WIDTH + 20, this.height - 4, 0xFFB01E1E);
        modTitle(gfx, this.title, this.width / 2, 6, 0xC21B1B);

        if (data == null || chain == null) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.quests.later"),
                    this.width / 2, this.height / 2 - 8, 0x9A9A9A);
            super.render(gfx, mouseX, mouseY, partialTick);
            return;
        }

        int current = data.getQuestIndex();
        Component subtitle = current >= quests.size()
                ? Component.translatable("screen.tokyoghoulrpg.quests.all_done")
                : Component.translatable("screen.tokyoghoulrpg.quests.count", current, quests.size());
        gfx.drawCenteredString(this.font, subtitle, this.width / 2, 18, 0x9A9A9A);

        int[] lay = layout(quests, current);
        int viewH = listBottom() - LIST_TOP;
        double maxScroll = Math.max(0, lay[0] - viewH);
        if (!scrolledToCurrent) {
            // сразу показываем текущее задание примерно в верхней трети
            scroll = current < quests.size() ? lay[1] - viewH / 3.0 : maxScroll;
            scrolledToCurrent = true;
        }
        scroll = Mth.clamp(scroll, 0.0, maxScroll);

        Difficulty difficulty = mc.level == null ? Difficulty.NORMAL : mc.level.getDifficulty();
        gfx.enableScissor(0, LIST_TOP, this.width, listBottom());
        int y = LIST_TOP - (int) Math.round(scroll);
        int act = -1;
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            if (quest.act() != act) {
                act = quest.act();
                if (y + ACT_H > LIST_TOP && y < listBottom()) {
                    gfx.drawString(this.font, quest.actTitle(), left + 4, y + 4, 0xE0A040, false);
                    gfx.fill(left + 4, y + 13, left + WIDTH - 4, y + 14, 0x40E0A040);
                }
                y += ACT_H;
            }
            boolean done = i < current;
            boolean active = i == current;
            int rowH = active ? activeH(quest) : ROW_H;
            if (y + rowH > LIST_TOP && y < listBottom()) {
                drawQuest(gfx, data, quest, done, active, left, y, difficulty);
            }
            y += rowH;
        }
        gfx.disableScissor();

        if (maxScroll > 0) {
            // полоса прокрутки справа и подсказка
            int barX = left + WIDTH + 4;
            int thumbH = Math.max(12, viewH * viewH / Math.max(1, lay[0]));
            int thumbY = LIST_TOP + (int) Math.round((viewH - thumbH) * (scroll / maxScroll));
            gfx.fill(barX, LIST_TOP, barX + 2, listBottom(), 0x40FFFFFF);
            gfx.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xC0D01F4A);
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.quests.scroll"),
                    this.width / 2, this.height - 12, 0x666666);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawQuest(GuiGraphics gfx, PlayerData data, Quest quest, boolean done, boolean active, int left, int y,
            Difficulty difficulty) {
        int h = active ? activeH(quest) : ROW_H;
        if (active) {
            modCard(gfx, left, y - 2, WIDTH, h - 1, modAccent(), 1.0F);
        }
        String mark = done ? "✔" : (active ? "▶" : "•");
        int markColor = done ? 0x4FB05A : (active ? 0xFF5A78 : 0x555555);
        gfx.drawString(this.font, mark, left + 6, y + 2, markColor, false);

        int textColor = done ? 0x777777 : (active ? 0xFFFFFF : 0x8A8A8A);
        gfx.drawString(this.font, quest.title(), left + 18, y + 2, textColor, false);

        // награда: сколько очков навыков игрок получит на самом деле (с x1,5 и сложностью) и сколько крови
        String blood = quest == Quest.C_FINAL ? com.tokyoghoulrpg.combat.Balance.FINAL_BLOOD_MIN + "-"
                + com.tokyoghoulrpg.combat.Balance.FINAL_BLOOD_MAX : String.valueOf(quest.bloodReward());
        Component reward = Component.translatable("screen.tokyoghoulrpg.quests.reward",
                quest.pointsFor(data.getPlayerClass(), difficulty), blood);
        gfx.drawString(this.font, reward, left + WIDTH - 6 - this.font.width(reward), y + 2,
                done ? 0x555555 : 0xC9822B, false);

        if (active) {
            int ly = y + 12;
            for (net.minecraft.util.FormattedCharSequence line : descLines(quest)) {
                gfx.drawString(this.font, line, left + 18, ly, 0xE6E6E6, false);
                ly += 10;
            }
            for (net.minecraft.util.FormattedCharSequence line : storyLines(quest)) {
                gfx.drawString(this.font, line, left + 18, ly, 0xB08080, false);
                ly += 10;
            }
            int barX = left + 18;
            int barW = WIDTH - 18 - 70;
            float fraction = Mth.clamp(data.getQuestProgress() / (float) quest.goal(), 0.0F, 1.0F);
            gfx.fill(barX, ly + 1, barX + barW, ly + 3, 0xFF2A0A14);
            gfx.fill(barX, ly + 1, barX + Math.round(barW * fraction), ly + 3, 0xFFD01F4A);
            String counter = Math.min(data.getQuestProgress(), quest.goal()) + "/" + quest.goal();
            gfx.drawString(this.font, counter, barX + barW + 4, ly - 2, 0xC9C9C9, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= delta * ROW_H * 2;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBindings.QUESTS.matches(keyCode, scanCode)) {
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
