package com.tokyoghoulrpg.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Заглушка для меню, которые появятся на следующих этапах (дерево навыков, задания). */
public class StubScreen extends Screen {
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
        gfx.fillGradient(0, 0, this.width, this.height, 0xE0120405, 0xE0000000);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 24, 0xC21B1B);
        gfx.drawCenteredString(this.font, this.text, this.width / 2, this.height / 2 - 8, 0x9A9A9A);
        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
