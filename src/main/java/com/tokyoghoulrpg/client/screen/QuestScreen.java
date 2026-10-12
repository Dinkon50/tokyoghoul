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

/**
 * Задания (клавиша N). Цепочка сверху вниз: выполненные отмечены, текущее подсвечено
 * с полосой прогресса, будущие приглушены.
 */
public class QuestScreen extends Screen {
    private static final int ROW_H = 18;
    private static final int WIDTH = 320;

    public QuestScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.quests.title"));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xE0120405, 0xE0000000);
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);

        Quest.Chain chain = data == null ? null : Quest.Chain.of(data.getPlayerClass());
        List<Quest> quests = chain == null ? List.of() : Quest.chain(chain);
        int totalH = 30 + quests.size() * ROW_H;
        int top = Math.max(4, (this.height - totalH) / 2);
        int left = (this.width - WIDTH) / 2;
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top, 0xC21B1B);

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
        gfx.drawCenteredString(this.font, subtitle, this.width / 2, top + 12, 0x9A9A9A);

        int y = top + 30;
        for (int i = 0; i < quests.size(); i++) {
            Quest quest = quests.get(i);
            boolean done = i < current;
            boolean active = i == current;

            if (active) {
                gfx.fill(left, y - 2, left + WIDTH, y + ROW_H - 3, 0x803A0A0E);
                gfx.fill(left, y - 2, left + 2, y + ROW_H - 3, 0xFFB01E1E);
            }

            String mark = done ? "✔" : (active ? "▶" : "•");
            int markColor = done ? 0x4FB05A : (active ? 0xFF5A78 : 0x555555);
            gfx.drawString(this.font, mark, left + 6, y + 2, markColor, false);

            int textColor = done ? 0x777777 : (active ? 0xFFFFFF : 0x8A8A8A);
            Component text = active ? quest.description() : quest.title();
            gfx.drawString(this.font, text, left + 18, y + 2, textColor, false);

            Component reward = Component.translatable("screen.tokyoghoulrpg.quests.reward", quest.reward(), quest.bloodReward());
            gfx.drawString(this.font, reward, left + WIDTH - 6 - this.font.width(reward), y + 2,
                    done ? 0x555555 : 0xC9822B, false);

            if (active) {
                // Полоса прогресса под текущим заданием
                int barX = left + 18;
                int barW = WIDTH - 18 - 70;
                float fraction = Mth.clamp(data.getQuestProgress() / (float) quest.goal(), 0.0F, 1.0F);
                gfx.fill(barX, y + 11, barX + barW, y + 13, 0xFF2A0A14);
                gfx.fill(barX, y + 11, barX + Math.round(barW * fraction), y + 13, 0xFFD01F4A);
                String counter = Math.min(data.getQuestProgress(), quest.goal()) + "/" + quest.goal();
                gfx.drawString(this.font, counter, barX + barW + 4, y + 8, 0xC9C9C9, false);
            }
            y += ROW_H;
        }
        super.render(gfx, mouseX, mouseY, partialTick);
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
