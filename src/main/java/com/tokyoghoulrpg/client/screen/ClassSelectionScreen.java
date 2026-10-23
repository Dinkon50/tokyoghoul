package com.tokyoghoulrpg.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tokyoghoulrpg.client.ModSkins;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.network.packet.SelectVariantC2S;
import com.tokyoghoulrpg.network.packet.SkinChoiceC2S;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Меню выбора стороны при первом входе. Закрыть его без выбора нельзя.
 * Гуль и полугуль выбирают ещё вариант кагуне. Потом гуль, полугуль и агент CCG выбирают скин:
 * оставить свой или взять скин класса из мода (скин гуля или скин CCG).
 */
public class ClassSelectionScreen extends Screen {
    private static final List<PlayerClass> CHOICES =
            List.of(PlayerClass.HUMAN, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL, PlayerClass.CCG_AGENT);
    /** Размер пикселя скина в превью. */
    private static final int DOLL_SCALE = 3;

    /** Выбранный класс, если у него есть второй шаг (вариант кагуне). */
    private PlayerClass selected = null;
    /** Выбранный вариант кагуне: дальше выбор скина. */
    private KaguneVariant chosenVariant = null;
    /** Класс уже есть (например, стал полугулем после инъекции): выбираем только вариант кагуне и скин. */
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
        } else if (chosenVariant == null) {
            int y = this.height / 2 - 20;
            for (KaguneVariant variant : selected.variants()) {
                addRenderableWidget(Button.builder(Component.translatable(variant.translationKey()),
                                button -> {
                                    chosenVariant = variant;
                                    buildButtons();
                                })
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
        } else {
            // скин: под каждым превью своя кнопка
            int y = dollTop() + 32 * DOLL_SCALE + 10;
            addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.skin.own"),
                            button -> choose(false))
                    .bounds(centerX - 150, y, 140, 20)
                    .tooltip(Tooltip.create(Component.translatable("screen.tokyoghoulrpg.skin.own.tip")))
                    .build());
            String skinKey = selected.hasKagune() ? "screen.tokyoghoulrpg.skin.ghoul" : "screen.tokyoghoulrpg.skin.ccg";
            addRenderableWidget(Button.builder(Component.translatable(skinKey),
                            button -> choose(true))
                    .bounds(centerX + 10, y, 140, 20)
                    .tooltip(Tooltip.create(Component.translatable(skinKey + ".tip")))
                    .build());
            if (!variantOnly || selected.hasKagune()) {
                addRenderableWidget(Button.builder(Component.translatable("screen.tokyoghoulrpg.back"), button -> {
                            chosenVariant = null;
                            if (!selected.hasKagune()) {
                                selected = null;
                            }
                            buildButtons();
                        })
                        .bounds(centerX - 90, y + 28, 180, 20)
                        .build());
            }
        }
    }

    private int dollTop() {
        return this.height / 2 - 48;
    }

    private void onClassClicked(PlayerClass cls) {
        if (cls.hasKagune()) {
            selected = cls;
            chosenVariant = null;
            buildButtons();
        } else if (PlayerData.hasModSkin(cls)) {
            // агент CCG: варианта кагуне нет, сразу выбор скина
            selected = cls;
            chosenVariant = KaguneVariant.NONE;
            buildButtons();
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SelectClassC2S(cls, KaguneVariant.NONE, false));
            onClose();
        }
    }

    private void choose(boolean modSkin) {
        if (variantOnly) {
            NetworkHandler.CHANNEL.sendToServer(new SelectVariantC2S(chosenVariant));
            NetworkHandler.CHANNEL.sendToServer(new SkinChoiceC2S(modSkin));
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SelectClassC2S(selected, chosenVariant, modSkin));
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
        boolean skinStep = selected != null && chosenVariant != null;
        int titleY = skinStep ? dollTop() - 34 : this.height / 2 - 76;
        gfx.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xC21B1B);

        Component subtitle;
        if (selected == null) {
            subtitle = Component.translatable("screen.tokyoghoulrpg.class_select.subtitle");
        } else if (!skinStep) {
            subtitle = Component.translatable("screen.tokyoghoulrpg.variant_select.subtitle",
                    Component.translatable(selected.translationKey()));
        } else {
            subtitle = Component.translatable("screen.tokyoghoulrpg.skin.subtitle");
        }
        gfx.drawCenteredString(this.font, subtitle, this.width / 2, titleY + 14, 0x9A9A9A);

        if (skinStep) {
            int dollW = 16 * DOLL_SCALE;
            ResourceLocation own = this.minecraft.player != null
                    ? ModSkins.ownSkin(this.minecraft.player)
                    : DefaultPlayerSkin.getDefaultSkin();
            boolean ownSlim = this.minecraft.player != null && "slim".equals(this.minecraft.player.getModelName())
                    && !ModSkins.isWearing(this.minecraft.player);
            drawDoll(gfx, own, this.width / 2 - 80 - dollW / 2, dollTop(), ownSlim);
            ModSkins.Skin classSkin = selected.hasKagune() ? ModSkins.GHOUL : ModSkins.CCG;
            drawDoll(gfx, classSkin.texture(), this.width / 2 + 80 - dollW / 2, dollTop(), "slim".equals(classSkin.model()));
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    /** Скин спереди, как бумажная кукла: голова, тело, руки, ноги и второй слой поверх. */
    private static void drawDoll(GuiGraphics gfx, ResourceLocation skin, int x, int y, boolean slim) {
        int s = DOLL_SCALE;
        // тонкие руки: 3 пикселя, прижаты к телу
        int arm = slim ? 3 : 4;
        int rightArmX = x + (4 - arm) * s;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // первый слой
        part(gfx, skin, x + 4 * s, y, 8, 8, 8, 8, s);            // голова
        part(gfx, skin, x + 4 * s, y + 8 * s, 20, 20, 8, 12, s);  // тело
        part(gfx, skin, rightArmX, y + 8 * s, 44, 20, arm, 12, s); // правая рука
        part(gfx, skin, x + 12 * s, y + 8 * s, 36, 52, arm, 12, s); // левая рука
        part(gfx, skin, x + 4 * s, y + 20 * s, 4, 20, 4, 12, s);  // правая нога
        part(gfx, skin, x + 8 * s, y + 20 * s, 20, 52, 4, 12, s); // левая нога
        // второй слой (шапка, куртка, рукава, штаны)
        part(gfx, skin, x + 4 * s, y, 40, 8, 8, 8, s);
        part(gfx, skin, x + 4 * s, y + 8 * s, 20, 36, 8, 12, s);
        part(gfx, skin, rightArmX, y + 8 * s, 44, 36, arm, 12, s);
        part(gfx, skin, x + 12 * s, y + 8 * s, 52, 52, arm, 12, s);
        part(gfx, skin, x + 4 * s, y + 20 * s, 4, 36, 4, 12, s);
        part(gfx, skin, x + 8 * s, y + 20 * s, 4, 52, 4, 12, s);
        RenderSystem.disableBlend();
    }

    private static void part(GuiGraphics gfx, ResourceLocation skin, int x, int y, int u, int v, int w, int h, int s) {
        gfx.blit(skin, x, y, w * s, h * s, u, v, w, h, 64, 64);
    }
}
