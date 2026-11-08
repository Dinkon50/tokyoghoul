package com.tokyoghoulrpg.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Книга новичка: слева разделы (список прокручивается колесом мыши или стрелками ▲ ▼),
 * справа текст раздела постранично (стрелки или колесо мыши над текстом).
 * Тексты — в языковых файлах: guide.tokyoghoulrpg.&lt;раздел&gt;.title / .text («\n» — новая строка).
 */
public class GuideScreen extends ModScreen {
    public static final String[] CHAPTERS = {
            "start", "classes", "keys", "gamepad", "vr", "ghoul", "kagune_types", "kakuja", "ccg", "human", "blood", "skills",
            "combos", "prestige", "styles", "deep_ward",
            "quests", "masks", "places", "events", "market", "clans", "territory", "tips"};
    private static final int WIDTH = 380;
    private static final int HEIGHT = 314;
    private static final int LIST_W = 112;
    private static final int ROW = 12;

    private static int chapter = 0;
    private static int page = 0;
    /** Первый видимый раздел в списке слева. */
    private static int listScroll = 0;
    private List<FormattedCharSequence> lines = new ArrayList<>();
    private Button prev;
    private Button next;

    public GuideScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.guide.title"));
        // открытый раздел виден в списке, когда книга открывается
        if (chapter < listScroll || chapter >= listScroll + visibleRows()) {
            listScroll = Math.max(0, Math.min(maxListScroll(), chapter - visibleRows() / 2));
        }
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

    /** Сколько разделов помещается в списке (место под стрелки ▲ ▼ сверху и снизу). */
    private int visibleRows() {
        return Math.max(1, (HEIGHT - 22 - 2 * ROW - 10) / ROW);
    }

    private int maxListScroll() {
        return Math.max(0, CHAPTERS.length - visibleRows());
    }

    private void scrollList(int delta) {
        int next = Math.max(0, Math.min(maxListScroll(), listScroll + delta));
        if (next != listScroll) {
            listScroll = next;
            rebuildWidgets();
        }
    }

    private int pages() {
        return Math.max(1, (lines.size() + linesPerPage() - 1) / linesPerPage());
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        listScroll = Math.max(0, Math.min(maxListScroll(), listScroll));
        Button up = addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollList(-visibleRows() + 1))
                .bounds(left + 6, top + 22, LIST_W - 8, ROW - 1).build());
        up.active = listScroll > 0;
        int rows = visibleRows();
        for (int r = 0; r < rows && listScroll + r < CHAPTERS.length; r++) {
            final int index = listScroll + r;
            Button b = Button.builder(Component.translatable("guide.tokyoghoulrpg." + CHAPTERS[index] + ".title"), btn -> {
                chapter = index;
                page = 0;
                rebuildWidgets();
            }).bounds(left + 6, top + 22 + (r + 1) * ROW, LIST_W - 8, ROW - 1).build();
            b.active = index != chapter;
            addRenderableWidget(b);
        }
        Button down = addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollList(visibleRows() - 1))
                .bounds(left + 6, top + 22 + (rows + 1) * ROW, LIST_W - 8, ROW - 1).build());
        down.active = listScroll < maxListScroll();
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
        if (mouseX < left() + LIST_W) {
            scrollList(delta > 0 ? -1 : 1);       // над списком разделов — листать список
        } else {
            turn(delta > 0 ? -1 : 1);             // над текстом — страницы
        }
        return true;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        modBackground(gfx);
        int left = left();
        int top = top();
        modPanel(gfx, left, top, WIDTH, (top + HEIGHT) - (top), 0xFFB01E1E);
        gfx.fill(left + LIST_W, top + 20, left + LIST_W + 1, top + HEIGHT - 6, 0x60B01E1E);
        modTitle(gfx, this.title, this.width / 2, top + 7, 0xE05050);
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
