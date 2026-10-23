package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.client.ClientHooks;
import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.AbilityC2S;
import com.tokyoghoulrpg.network.packet.ClanActionC2S;
import com.tokyoghoulrpg.network.packet.ToggleKaguneC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Меню способностей и окон мода большими кнопками: всё, что обычно на клавишах, без клавиатуры.
 * Нужно в VR (открывается касанием запястья левой руки или из меню паузы) и на геймпаде (из меню паузы).
 * Способность срабатывает сразу после закрытия меню.
 */
public class AbilityMenuScreen extends Screen {
    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int STEP = 23;

    public AbilityMenuScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.abilities.title"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_W - 4;
        int right = this.width / 2 + 4;
        int top = top();

        // слева — способности
        ability(left, top, "key.tokyoghoulrpg.toggle_kagune", () -> NetworkHandler.CHANNEL.sendToServer(new ToggleKaguneC2S()));
        ability(left, top + STEP, "key.tokyoghoulrpg.bite", () -> send(AbilityType.BITE));
        ability(left, top + STEP * 2, "key.tokyoghoulrpg.dash", () -> send(AbilityType.DASH));
        ability(left, top + STEP * 3, "key.tokyoghoulrpg.rage", () -> send(AbilityType.RAGE));
        ability(left, top + STEP * 4, "key.tokyoghoulrpg.kakuja", () -> send(AbilityType.KAKUJA));
        ability(left, top + STEP * 5, "key.tokyoghoulrpg.retribution", () -> send(AbilityType.RETRIBUTION));
        ability(left, top + STEP * 6, "key.tokyoghoulrpg.charisma", () -> send(AbilityType.CHARISMA));
        ability(left, top + STEP * 7, "key.tokyoghoulrpg.fortune", () -> send(AbilityType.FORTUNE));

        // справа — окна мода
        open(right, top, "key.tokyoghoulrpg.skill_tree", () -> this.minecraft.setScreen(new SkillTreeScreen()));
        open(right, top + STEP, "key.tokyoghoulrpg.quests", () -> this.minecraft.setScreen(new QuestScreen()));
        open(right, top + STEP * 2, "key.tokyoghoulrpg.clan", () -> {
            this.minecraft.setScreen(new ClanScreen());
            NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.OPEN, "", null, 0));
        });
        open(right, top + STEP * 3, "key.tokyoghoulrpg.profile", () -> {
            this.minecraft.setScreen(new ProfileScreen());
            NetworkHandler.CHANNEL.sendToServer(new com.tokyoghoulrpg.network.packet.RequestSyncC2S());
        });
        open(right, top + STEP * 4, "key.tokyoghoulrpg.market", () -> {
            this.minecraft.setScreen(new MarketScreen());
            NetworkHandler.CHANNEL.sendToServer(new com.tokyoghoulrpg.network.packet.MarketActionC2S(
                    com.tokyoghoulrpg.market.Market.OPEN, 0, 0, false));
        });
        open(right, top + STEP * 5, "screen.tokyoghoulrpg.guide.title", ClientHooks::openGuide);

        // гуль, полугуль, агент CCG: сменить скин (свой / скин класса из мода)
        com.tokyoghoulrpg.data.PlayerData data = this.minecraft.player == null ? null
                : com.tokyoghoulrpg.data.PlayerData.get(this.minecraft.player);
        if (data != null && com.tokyoghoulrpg.data.PlayerData.hasModSkin(data.getPlayerClass())) {
            boolean wearing = data.isModSkin();
            addRenderableWidget(Button.builder(Component.translatable(wearing
                                    ? "screen.tokyoghoulrpg.skin.toggle_mod" : "screen.tokyoghoulrpg.skin.toggle_own"),
                            b -> {
                                NetworkHandler.CHANNEL.sendToServer(
                                        new com.tokyoghoulrpg.network.packet.SkinChoiceC2S(!wearing));
                                onClose();
                            })
                    .bounds(right, top + STEP * 6, BUTTON_W, BUTTON_H).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), b -> onClose())
                .bounds(right, top + STEP * 7, BUTTON_W, BUTTON_H).build());
    }

    private int top() {
        return Math.max(24, (this.height - 10 * STEP) / 2);
    }

    /** Кнопка способности: меню закрывается, способность срабатывает. */
    private void ability(int x, int y, String key, Runnable action) {
        addRenderableWidget(Button.builder(Component.translatable(key), b -> {
            this.minecraft.setScreen(null);
            action.run();
        }).bounds(x, y, BUTTON_W, BUTTON_H).build());
    }

    /** Кнопка окна мода: вместо этого меню открывается нужное окно. */
    private void open(int x, int y, String key, Runnable action) {
        addRenderableWidget(Button.builder(Component.translatable(key), b -> action.run())
                .bounds(x, y, BUTTON_W, BUTTON_H).build());
    }

    private static void send(AbilityType type) {
        NetworkHandler.CHANNEL.sendToServer(new AbilityC2S(type));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        int top = top();
        gfx.drawCenteredString(this.font, this.title, this.width / 2, top - 16, 0xE05050);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.abilities.hint"),
                this.width / 2, top + STEP * 9 + 6, 0x8A8A8A);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
