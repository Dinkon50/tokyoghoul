package com.tokyoghoulrpg.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Книга новичка: слева разделы, справа текст раздела постранично (стрелки или колесо мыши).
 * Тексты — в языковых файлах: guide.tokyoghoulrpg.&lt;раздел&gt;.title / .text («\n» — новая строка).
 */
public class GuideScreen extends Screen {
    public static final String[] CHAPTERS = {
            "start", "classes", "keys", "gamepad", "vr", "ghoul", "kagune_types", "kakuja", "ccg", "human", "blood", "skills",
            "combos", "prestige",
            "quests", "masks", "places", "events", "market", "clans", "territory", "tips"};
    private static final int WIDTH = 380;
    private static final int HEIGHT = 290;
    private static final int LIST_W = 112;
    private static final int ROW = 12;

    private static int chapter = 0;
    private static int page = 0;
    private List<FormattedCharSequence> lines = new ArrayList<>();
    private Button prev;
    private Button next;

    public GuideScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.guide.title"));
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - HEIGHT) / 2);
    }

    private int linesPerPage() {
        return (HEIGHT - 58) / 10;
    }

    private int pages() {
        return Math.max(1, (lines.size() + linesPerPage() - 1) / linesPerPage());
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        for (int i = 0; i < CHAPTERS.length; i++) {
            final int index = i;
            Button b = Button.builder(Component.translatable("guide.tokyoghoulrpg." + CHAPTERS[i] + ".title"), btn -> {
                chapter = index;
                page = 0;
                rebuildWidgets();
            }).bounds(left + 6, top + 22 + i * ROW, LIST_W - 8, ROW - 1).build();
            b.active = i != chapter;
            addRenderableWidget(b);
        }
        int textW = WIDTH - LIST_W - 20;
        lines = this.font.split(Component.translatable("guide.tokyoghoulrpg." + CHAPTERS[chapter] + ".text"), textW);
        page = Math.max(0, Math.min(page, pages() - 1));
        int tx = left + LIST_W + 8;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                .bounds(tx, top + HEIGHT - 24, 20, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                .bounds(left + WIDTH - 28, top + HEIGHT - 24, 20, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(tx + (textW - 80) / 2 + 6, top + HEIGHT - 24, 80, 16).build());
        updateArrows();
    }

    private void turn(int delta) {
        page = Math.max(0, Math.min(pages() - 1, page + delta));
        updateArrows();
    }

    private void updateArrows() {
        if (prev != null) {
            prev.active = page > 0;
            next.active = page < pages() - 1;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        turn(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xD0120405, 0xD0000000);
        int left = left();
        int top = top();
        gfx.fill(left, top, left + WIDTH, top + HEIGHT, 0xE01A1210);
        gfx.fill(left, top, left + 2, top + HEIGHT, 0xFFB01E1E);
        gfx.fill(left + LIST_W, top + 20, left + LIST_W + 1, top + HEIGHT - 6, 0x60B01E1E);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top + 7, 0xE05050);
        int tx = left + LIST_W + 10;
        gfx.drawString(this.font, Component.translatable("guide.tokyoghoulrpg." + CHAPTERS[chapter] + ".title"),
                tx, top + 24, 0xFFD27A, false);
        int per = linesPerPage();
        int from = page * per;
        int y = top + 38;
        for (int i = from; i < Math.min(lines.size(), from + per); i++) {
            gfx.drawString(this.font, lines.get(i), tx, y, 0xE0D8D0, false);
            y += 10;
        }
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.guide.page", page + 1, pages()),
                left + WIDTH - 70, top + 24, 0x8A8A8A, false);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
