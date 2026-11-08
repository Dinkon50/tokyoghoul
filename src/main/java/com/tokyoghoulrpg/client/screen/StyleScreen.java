package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.StyleActionC2S;
import com.tokyoghoulrpg.style.FightStyle;
import com.tokyoghoulrpg.style.StyleBalance;
import com.tokyoghoulrpg.style.StyleData;
import com.tokyoghoulrpg.style.StyleManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Меню стилей боя. Слева три стиля (Боец, Тень, Жнец) с состоянием, справа выбранный: описание, две способности
 * на клавише C, три ветки по три уровня за очки мастерства. Снизу — вызов босса стиля и выбор стиля
 * (только у наставника в додзё; прокачивать ветки можно где угодно).
 */
public class StyleScreen extends ModScreen {
    private static final int W = 360;
    private static final int H = 258;
    private static final int CARD_W = 112;
    private static final int CARD_H = 44;

    private final int mentorId;
    private FightStyle selected;
    private Button challengeButton;
    private Button selectButton;
    private final Button[] upgradeButtons = new Button[3];

    public StyleScreen(int mentorId) {
        super(Component.translatable("screen.tokyoghoulrpg.style.title"));
        this.mentorId = mentorId;
        StyleData sd = sd();
        this.selected = sd != null && sd.active() != null ? sd.active() : FightStyle.FIGHTER;
    }

    private static StyleData sd() {
        Minecraft mc = Minecraft.getInstance();
        PlayerData data = mc.player == null ? null : PlayerData.get(mc.player);
        return data == null ? null : data.style();
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return Math.max(4, (this.height - H) / 2);
    }

    private int rightX() {
        return left() + CARD_W + 16;
    }

    private int branchX(int i) {
        int rw = W - CARD_W - 24;
        return rightX() + i * (rw / 3);
    }

    private int branchW() {
        return (W - CARD_W - 24) / 3 - 6;
    }

    private int branchY() {
        return top() + 156;
    }

    private void send(int action, int branch) {
        NetworkHandler.CHANNEL.sendToServer(new StyleActionC2S(action, selected.ordinal(), branch, mentorId));
    }

    @Override
    protected void init() {
        int left = left();
        int by = top() + H - 26;
        challengeButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(StyleManager.CHALLENGE, 0))
                .bounds(left + 8, by, 150, 20).build());
        selectButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            StyleData sd = sd();
            send(sd != null && sd.isActive(selected) ? StyleManager.CLEAR : StyleManager.SELECT, 0);
        }).bounds(left + 164, by, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(left + W - 70, by, 62, 20).build());
        for (int i = 0; i < 3; i++) {
            final int branch = i;
            upgradeButtons[i] = addRenderableWidget(Button.builder(Component.literal("+"), b -> send(StyleManager.UPGRADE, branch))
                    .bounds(branchX(i) + (branchW() - 44) / 2, branchY() + 40, 44, 14).build());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = left() + 8;
        for (FightStyle style : FightStyle.values()) {
            int y = top() + 40 + style.ordinal() * (CARD_H + 6);
            if (mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H) {
                selected = style;
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                        .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        modBackground(gfx);
        StyleData sd = sd();
        int left = left();
        int top = top();
        int accent = selected.accent();
        modPanel(gfx, left, top, W, H, 0xFF000000 | accent);
        modTitle(gfx, this.title, this.width / 2, top + 10, accent);

        int mastery = sd == null ? 0 : sd.mastery();
        int left2 = sd == null ? StyleBalance.KILLS_PER_POINT : StyleBalance.KILLS_PER_POINT - sd.killProgress();
        Component info = sd != null && sd.anyUnlocked()
                ? Component.translatable("screen.tokyoghoulrpg.style.points", mastery, left2)
                : Component.translatable("screen.tokyoghoulrpg.style.points_locked");
        gfx.drawCenteredString(this.font, info, this.width / 2, top + 25, 0xE8C890);

        // ---------- стили слева
        for (FightStyle style : FightStyle.values()) {
            int x = left + 8;
            int y = top + 40 + style.ordinal() * (CARD_H + 6);
            boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
            float hl = style == selected ? 1.0F : hover ? 0.5F : 0.0F;
            modCard(gfx, x, y, CARD_W, CARD_H, style.ready() ? style.accent() : 0x606060, hl);
            int nameColor = style.ready() ? ScreenFx.mix(0xFFFFFF, style.accent(), 0.45F) : 0x8A8A8A;
            gfx.drawString(this.font, Component.translatable(style.key("name")), x + 8, y + 6, nameColor, true);
            Component state;
            int stateColor;
            if (!style.ready()) {
                state = Component.translatable("screen.tokyoghoulrpg.style.soon");
                stateColor = 0x7A7A7A;
            } else if (sd != null && sd.isActive(style)) {
                state = Component.translatable("screen.tokyoghoulrpg.style.state_active");
                stateColor = 0x7EE08A;
            } else if (sd != null && sd.isUnlocked(style)) {
                state = Component.translatable("screen.tokyoghoulrpg.style.state_open");
                stateColor = 0xE8D8B0;
            } else {
                state = Component.translatable("screen.tokyoghoulrpg.style.state_locked");
                stateColor = 0xC07070;
            }
            gfx.drawString(this.font, state, x + 8, y + 18, stateColor, false);
            if (style.ready() && sd != null && sd.isUnlocked(style)) {
                gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.style.spent", sd.spent(style), 18),
                        x + 8, y + 30, 0x9A8A78, false);
            } else {
                gfx.drawString(this.font, Component.translatable(style.key("boss")), x + 8, y + 30, 0x8A7A70, false);
            }
        }

        // ---------- выбранный стиль справа
        int rx = rightX();
        int rw = W - CARD_W - 24;
        int y = top + 40;
        ScreenFx.bigText(gfx, this.font, Component.translatable(selected.key("name")), rx + rw / 2, y, 1.15F,
                0xFF000000 | ScreenFx.mix(0xFFFFFF, accent, 0.5F), 0xC0000000);
        y += 14;
        for (FormattedCharSequence line : this.font.split(Component.translatable(selected.key("desc")), rw)) {
            gfx.drawString(this.font, line, rx, y, 0xD8D0C8, false);
            y += 10;
        }
        y = Math.max(y + 4, top + 82);
        gfx.drawString(this.font, Component.translatable("screen.tokyoghoulrpg.style.abilities"), rx, y,
                ScreenFx.mix(0xFFFFFF, accent, 0.6F), false);
        y += 11;
        for (int n = 1; n <= 2; n++) {
            Component keyName = Component.translatable(n == 1 ? "screen.tokyoghoulrpg.style.key1"
                    : "screen.tokyoghoulrpg.style.key2");
            Component line = Component.translatable("screen.tokyoghoulrpg.style.ability_line", keyName,
                    Component.translatable(selected.key("ability" + n)), Component.translatable(selected.key("ability" + n + ".desc")));
            for (FormattedCharSequence part : this.font.split(line, rw)) {
                gfx.drawString(this.font, part, rx, y, 0xC8C0B8, false);
                y += 10;
            }
        }

        // ---------- ветки
        boolean unlocked = sd != null && sd.isUnlocked(selected) && selected.ready();
        int bY = branchY();
        List<FormattedCharSequence> tip = null;
        for (int i = 0; i < 3; i++) {
            int bx = branchX(i);
            int bw = branchW();
            int level = sd == null ? 0 : sd.level(selected, i);
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= bY && mouseY < bY + 38;
            modCard(gfx, bx, bY, bw, 38, unlocked ? accent : 0x606060, hover ? 0.6F : 0.15F);
            Component name = Component.translatable(selected.branchKey(i, "name"));
            int nw = this.font.width(name);
            float ns = Math.min(1.0F, (bw - 6) / (float) Math.max(1, nw));
            gfx.pose().pushPose();
            gfx.pose().translate(bx + bw / 2.0F - nw * ns / 2.0F, bY + 6 + (1.0F - ns) * 4.0F, 0.0F);
            gfx.pose().scale(ns, ns, 1.0F);
            gfx.drawString(this.font, name, 0, 0, unlocked ? 0xF0E6D8 : 0x8A8A8A, true);
            gfx.pose().popPose();
            int pipW = 14;
            int px = bx + (bw - (pipW * 3 + 8)) / 2;
            for (int l = 0; l < 3; l++) {
                int x0 = px + l * (pipW + 4);
                gfx.fill(x0, bY + 22, x0 + pipW, bY + 28, 0xFF000000);
                gfx.fill(x0 + 1, bY + 23, x0 + pipW - 1, bY + 27, l < level ? 0xFF000000 | accent : 0xFF2A2224);
            }
            Button b = upgradeButtons[i];
            b.visible = unlocked;
            if (unlocked) {
                b.active = level < 3 && sd.mastery() >= StyleBalance.COSTS[Math.min(2, level)];
                b.setMessage(level < 3 ? Component.literal("+" + StyleBalance.COSTS[level])
                        : Component.translatable("screen.tokyoghoulrpg.style.max"));
            }
            if (hover) {
                tip = branchTip(i, level);
            }
        }
        if (!unlocked) {
            gfx.drawCenteredString(this.font, Component.translatable(selected.ready()
                    ? "screen.tokyoghoulrpg.style.how_unlock" : "screen.tokyoghoulrpg.style.soon_hint",
                    Component.translatable(selected.key("boss"))), rx + rw / 2, bY + 44, 0xB09080);
        }

        // ---------- кнопки
        boolean atMentor = mentorId >= 0;
        challengeButton.setMessage(Component.translatable("screen.tokyoghoulrpg.style.challenge",
                Component.translatable(selected.key("boss"))));
        long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        challengeButton.active = atMentor && selected.ready() && sd != null && now >= sd.challengeReady();
        boolean isActive = sd != null && sd.isActive(selected);
        selectButton.setMessage(Component.translatable(isActive ? "screen.tokyoghoulrpg.style.clear"
                : sd != null && sd.active() != null ? "screen.tokyoghoulrpg.style.switch"
                : "screen.tokyoghoulrpg.style.select", StyleBalance.SWITCH_COST));
        selectButton.active = atMentor && (isActive || unlocked);
        if (!atMentor) {
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.style.only_mentor"),
                    left + 160, top + H - 36, 0x9A8A78);
        } else if (sd != null && now < sd.challengeReady()) {
            long minutes = Math.max(1L, (sd.challengeReady() - now) / 1200L);
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.style.challenge_wait", minutes),
                    left + 83, top + H - 36, 0x9A8A78);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
        if (tip != null) {
            gfx.renderTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    private List<FormattedCharSequence> branchTip(int branch, int level) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(selected.branchKey(branch, "name")).withStyle(s -> s.withColor(selected.accent()))
                .getVisualOrderText());
        lines.addAll(this.font.split(Component.translatable(selected.branchKey(branch, "desc")), 200));
        lines.add(Component.translatable("screen.tokyoghoulrpg.style.level", level).getVisualOrderText());
        if (level < 3) {
            lines.add(Component.translatable("screen.tokyoghoulrpg.style.next_cost", StyleBalance.COSTS[level])
                    .withStyle(s -> s.withColor(0xE8C890)).getVisualOrderText());
        }
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
