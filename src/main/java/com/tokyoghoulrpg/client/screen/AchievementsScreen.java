package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.TitleC2S;
import com.tokyoghoulrpg.rep.Achievements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Достижения: все с прогрессом и наградой; у полученных — кнопка «Титул», чтобы носить его. */
public class AchievementsScreen extends ModScreen {
    private static final int WIDTH = 340;
    private static final int ROW = 15;

    public AchievementsScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.achievements.title"));
    }

    private int height() {
        return 40 + Achievements.A.values().length * ROW + 30;
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(2, (this.height - height()) / 2);
    }

    private static PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PlayerData.get(mc.player);
    }

    @Override
    protected void init() {
        PlayerData data = data();
        int y = top() + 30;
        for (Achievements.A a : Achievements.A.values()) {
            if (data != null && data.hasAchievement(a.id)) {
                boolean worn = a.id.equals(data.getTitle());
                Button b = Button.builder(Component.translatable(worn ? "screen.tokyoghoulrpg.achievements.worn"
                                : "screen.tokyoghoulrpg.achievements.wear"), btn -> {
                    NetworkHandler.CHANNEL.sendToServer(new TitleC2S(worn ? "" : a.id));
                    onClose();
                }).bounds(left() + WIDTH - 66, y, 58, 14).build();
                addRenderableWidget(b);
            }
            y += ROW;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.back"),
                b -> Minecraft.getInstance().setScreen(new ProfileScreen())).bounds(this.width / 2 - 50, top() + height() - 24, 100, 18).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        modBackground(gfx);
        int left = left();
        int top = top();
        modPanel(gfx, left, top, WIDTH, (top + height()) - (top), 0xFFFFD700);
        PlayerData data = data();
        int got = data == null ? 0 : data.achievementCount();
        modTitle(gfx, Component.translatable("screen.tokyoghoulrpg.achievements.header", got,
                Achievements.A.values().length), this.width / 2, top + 8, 0xFFD700);
        int y = top + 30;
        for (Achievements.A a : Achievements.A.values()) {
            boolean has = data != null && data.hasAchievement(a.id);
            int progress = data == null ? 0 : Math.min(a.goal, data.getStat(a.stat));
            gfx.drawString(this.font, Component.translatable(a.titleKey()), left + 10, y, has ? 0xFFD700 : 0xB0B0B0, false);
            Component desc = Component.translatable(a.descKey(), a.goal);
            gfx.drawString(this.font, desc, left + 100, y, has ? 0xC9C9C9 : 0x8A8A8A, false);
            if (!has) {
                Component p = Component.literal(progress + "/" + a.goal);
                gfx.drawString(this.font, p, left + WIDTH - 10 - this.font.width(p), y, 0x7A7A7A, false);
            }
            y += ROW;
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
