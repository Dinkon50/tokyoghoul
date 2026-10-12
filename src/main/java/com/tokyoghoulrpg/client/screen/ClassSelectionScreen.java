package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.network.packet.SelectVariantC2S;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Меню выбора стороны при первом входе. Закрыть его без выбора нельзя. */
public class ClassSelectionScreen extends Screen {
    private static final List<PlayerClass> CHOICES =
            List.of(PlayerClass.HUMAN, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL, PlayerClass.CCG_AGENT);

    /** Выбранный класс, если у него есть второй шаг (вариант кагуне). */
    private PlayerClass selected = null;
    /** Класс уже есть (например, стал полугулем после инъекции): выбираем только вариант кагуне. */
    private final boolean variantOnly;

    public ClassSelectionScreen() {
        super(Component.translatable("screen.tokyoghoulrpg.class_select.title"));
        this.variantOnly = false;
    }

    public ClassSelectionScreen(PlayerClass currentClass) {
        super(Component.translatable("screen.tokyoghoulrpg.variant_select.title"));
        this.selected = currentClass;
        this.variantOnly = true;
    }

    @Override
    protected void init() {
        buildButtons();
    }

    private void buildButtons() {
        clearWidgets();
        int centerX = this.width / 2;

        if (selected == null) {
            int y = this.height / 2 - 40;
            for (PlayerClass cls : CHOICES) {
                addRenderableWidget(Button.builder(Component.translatable(cls.translationKey()), button -> onClassClicked(cls))
                        .bounds(centerX - 90, y, 180, 20)
                        .tooltip(Tooltip.create(Component.translatable(cls.descriptionKey())))
                        .build());
                y += 26;
            }
        } else {
            int y = this.height / 2 - 20;
            for (KaguneVariant variant : selected.variants()) {
                addRenderableWidget(Button.builder(Component.translatable(variant.translationKey()),
                                button -> choose(selected, variant))
                        .bounds(centerX - 90, y, 180, 20)
                        .tooltip(Tooltip.create(Component.translatable(variant.descriptionKey())))
                        .build());
                y += 26;
            }
            if (!variantOnly) {
                addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.back"), button -> {
                            selected = null;
                            buildButtons();
                        })
                        .bounds(centerX - 90, y + 8, 180, 20)
                        .build());
            }
        }
    }

    private void onClassClicked(PlayerClass cls) {
        if (cls.hasKagune()) {
            selected = cls;
            buildButtons();
        } else {
            choose(cls, KaguneVariant.NONE);
        }
    }

    private void choose(PlayerClass cls, KaguneVariant variant) {
        if (variantOnly) {
            NetworkHandler.CHANNEL.sendToServer(new SelectVariantC2S(variant));
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SelectClassC2S(cls, variant));
        }
        onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xF0120405, 0xF0000000);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 76, 0xC21B1B);

        Component subtitle = selected == null
                ? Component.translatable("screen.tokyoghoulrpg.class_select.subtitle")
                : Component.translatable("screen.tokyoghoulrpg.variant_select.subtitle",
                        Component.translatable(selected.translationKey()));
        gfx.drawCenteredString(this.font, subtitle, this.width / 2, this.height / 2 - 62, 0x9A9A9A);

        super.render(gfx, mouseX, mouseY, partialTick);
    }
}
