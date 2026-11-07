package com.tokyoghoulrpg.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Заглушка для меню, которые появятся на следующих этапах (дерево навыков, задания). */
public class StubScreen extends ModScreen {
    private final Component text;

    public StubScreen(Component title, Component text) {
        super(title);
        this.text = text;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.close"), button -> onClose())
                .bounds(this.width / 2 - 50, this.height / 2 + 20, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        modBackground(gfx);
        int w = Math.max(220, this.font.width(this.text) + 40);
        modPanel(gfx, this.width / 2 - w / 2, this.height / 2 - 36, w, 84, 0xFFB01E1E);
        modTitle(gfx, this.title, this.width / 2, this.height / 2 - 24, 0xC21B1B);
        gfx.drawCenteredString(this.font, this.text, this.width / 2, this.height / 2 - 8, 0x9A9A9A);
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
