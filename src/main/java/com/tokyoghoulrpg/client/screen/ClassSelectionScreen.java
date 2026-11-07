package com.tokyoghoulrpg.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.ModSkins;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.network.packet.SelectVariantC2S;
import com.tokyoghoulrpg.network.packet.SkinChoiceC2S;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Меню выбора стороны при первом входе (закрыть без выбора нельзя). Три шага в одном стиле:
 * <ol>
 *   <li>Сторона: экран разделён пополам — слева гуль (тёмно-красный, искры), справа человек (сталь, пыль).
 *       В каждой половине живая 3D-модель (гуль в скине гуля, человек — Стив), она смотрит на мышь;
 *       под ней крупная надпись и чем класс отличается. Половина под мышью ярче, модель крупнее.</li>
 *   <li>Кагуне (гуль, а после инъекции полугуль): карточки с картинкой персонажа с выпущенным кагуне.</li>
 *   <li>Облик: карточки со своим скином и скинами из мода (тоже живые 3D-модели).</li>
 * </ol>
 */
public class ClassSelectionScreen extends Screen {
    /** Человек в меню — скин Стива (textures/skin/human.png). */
    private static final ResourceLocation STEVE = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/skin/human.png");
    /** Кагуне Канеки за спиной гуля на первом шаге (нарисовано заранее, 512 x 512, ноги игрока в точке AURA_FOOT). */
    private static final ResourceLocation GHOUL_AURA = new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/gui/class/ghoul_aura.png");
    private static final float AURA_FOOT_X = 257.0F;
    private static final float AURA_FOOT_Y = 370.0F;
    private static final float AURA_PLAYER_H = 218.0F;

    /** Выбранный класс, если у него есть второй шаг (вариант кагуне). */
    private PlayerClass selected = null;
    /** Выбранный вариант кагуне: дальше выбор скина. */
    private KaguneVariant chosenVariant = null;
    /** Класс уже есть (например, стал полугулем после инъекции): выбираем только вариант кагуне и скин. */
    private final boolean variantOnly;

    /** Плавная подсветка половин и карточек (0..1). */
    private float hoverLeft;
    private float hoverRight;
    private final float[] hoverCard = new float[4];
    private long lastFrame = 0L;

    /** Где что лежит на экране (считается в render, нажатия проверяются по ним). */
    private final List<int[]> cardRects = new ArrayList<>();
    private int[] backRect = null;

    private PlayerModel<AbstractClientPlayer> wideModel;
    private PlayerModel<AbstractClientPlayer> slimModel;

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
        if (wideModel == null) {
            wideModel = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            slimModel = new PlayerModel<>(this.minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            wideModel.young = false;
            slimModel.young = false;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean skinStep() {
        return selected != null && chosenVariant != null;
    }

    // ------------------------------------------------------------------ логика выбора (как раньше)

    private void onClassClicked(PlayerClass cls) {
        if (cls.hasKagune()) {
            selected = cls;
            chosenVariant = null;
        } else if (PlayerData.hasModSkin(cls)) {
            selected = cls;
            chosenVariant = KaguneVariant.NONE;
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SelectClassC2S(cls, KaguneVariant.NONE, 0));
            onClose();
        }
        java.util.Arrays.fill(hoverCard, 0.0F);
    }

    private void choose(int skin) {
        if (variantOnly) {
            NetworkHandler.CHANNEL.sendToServer(new SelectVariantC2S(chosenVariant));
            NetworkHandler.CHANNEL.sendToServer(new SkinChoiceC2S(skin));
        } else {
            NetworkHandler.CHANNEL.sendToServer(new SelectClassC2S(selected, chosenVariant, skin));
        }
        onClose();
    }

    private void back() {
        if (skinStep()) {
            chosenVariant = null;
            if (!selected.hasKagune()) {
                selected = null;
            }
        } else if (selected != null && !variantOnly) {
            selected = null;
        }
        java.util.Arrays.fill(hoverCard, 0.0F);
    }

    private boolean canGoBack() {
        if (skinStep()) {
            return !variantOnly || selected.hasKagune();
        }
        return selected != null && !variantOnly;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (backRect != null && inside(backRect, mouseX, mouseY)) {
            back();
            click();
            return true;
        }
        if (selected == null) {
            onClassClicked(mouseX < this.width / 2.0 ? PlayerClass.GHOUL : PlayerClass.HUMAN);
            click();
            return true;
        }
        for (int i = 0; i < cardRects.size(); i++) {
            if (inside(cardRects.get(i), mouseX, mouseY)) {
                click();
                if (skinStep()) {
                    choose(i);
                } else {
                    chosenVariant = selected.variants().get(i);
                    java.util.Arrays.fill(hoverCard, 0.0F);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean inside(int[] r, double x, double y) {
        return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        float dt = lastFrame == 0L ? 0.0F : Math.min(0.1F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        float time = (now % 1_000_000L) / 1000.0F;
        cardRects.clear();
        backRect = null;

        if (selected == null) {
            renderSides(gfx, mouseX, mouseY, time, dt);
        } else if (!skinStep()) {
            renderVariants(gfx, mouseX, mouseY, time, dt);
        } else {
            renderSkins(gfx, mouseX, mouseY, time, dt);
        }
        if (canGoBack()) {
            renderBack(gfx, mouseX, mouseY);
        }
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    /** Шаг 1: экран пополам, гуль и человек. */
    private void renderSides(GuiGraphics gfx, int mouseX, int mouseY, float time, float dt) {
        int w = this.width;
        int h = this.height;
        int half = w / 2;
        boolean overLeft = mouseX < half;
        hoverLeft = approach(hoverLeft, overLeft ? 1.0F : 0.0F, dt);
        hoverRight = approach(hoverRight, overLeft ? 0.0F : 1.0F, dt);

        // фоны половин
        gfx.fillGradient(0, 0, half, h, mix(0xFF1E0306, 0xFF3A070C, hoverLeft), 0xFF050102);
        gfx.fillGradient(half, 0, w, h, mix(0xFF101820, 0xFF1F3242, hoverRight), 0xFF020406);
        // свет из-за спины персонажа
        glow(gfx, half / 2, (int) (h * 0.42F), (int) (h * 0.36F), mix(0x30FF2030, 0x60FF2030, hoverLeft));
        glow(gfx, half + half / 2, (int) (h * 0.42F), (int) (h * 0.36F), mix(0x2080B0FF, 0x4080B0FF, hoverRight));
        // искры у гуля летят вверх, у человека медленно оседает пыль
        embers(gfx, 0, half, h, time, 46, true, 0.5F + 0.5F * hoverLeft);
        embers(gfx, half, w, h, time, 34, false, 0.5F + 0.5F * hoverRight);
        // разделитель
        gfx.fillGradient(half - 1, 0, half + 1, h, 0xFFB0202A, 0xFF3A6E9A);
        gfx.fill(half - 3, 0, half - 1, h, 0x30B0202A);
        gfx.fill(half + 1, 0, half + 3, h, 0x303A6E9A);

        // модели (за гулем — его кагуне, слегка покачивается)
        float size = h * 0.2F;
        int footY = (int) (h * 0.62F);
        float ghoulSize = size * (1.0F + 0.08F * hoverLeft);
        float k = 2.03F * ghoulSize / AURA_PLAYER_H * (1.0F + 0.015F * Mth.sin(time * 1.4F));
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F + 0.25F * hoverLeft);
        gfx.blit(GHOUL_AURA, Math.round(half / 2.0F - AURA_FOOT_X * k), Math.round(footY - AURA_FOOT_Y * k),
                Math.round(512 * k), Math.round(512 * k), 0, 0, 512, 512, 512, 512);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        renderPlayer(gfx, ModSkins.GHOUL.texture(), "slim".equals(ModSkins.GHOUL.model()), half / 2, footY,
                size * (1.0F + 0.08F * hoverLeft), 18.0F, mouseX, mouseY, time, true);
        renderPlayer(gfx, STEVE, false, half + half / 2, footY,
                size * (1.0F + 0.08F * hoverRight), -18.0F, mouseX, mouseY, time, false);

        // надписи
        boolean big = h >= 300;
        sideText(gfx, half / 2, footY + 10, half, "ghoul", 0xFFE0303A, 0xFF5A0A10, hoverLeft, big, time);
        sideText(gfx, half + half / 2, footY + 10, half, "human", 0xFFE6EEF6, 0xFF203448, hoverRight, big, time);

        // заголовок сверху
        gfx.fillGradient(0, 0, w, 28, 0xC0000000, 0x00000000);
        bigText(gfx, Component.translatable("screen.tokyoghoulrpg.class_select.title"), w / 2, 6, 1.4F, 0xFFEFEFEF, 0xFF000000);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.class_select.subtitle"),
                w / 2, 20, 0xFF9A9A9A);
        // подсказка снизу
        gfx.fillGradient(0, h - 18, w, h, 0x00000000, 0xC0000000);
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.class_select.later"),
                w / 2, h - 12, 0xFF8A8A8A);
    }

    private void sideText(GuiGraphics gfx, int cx, int y, int colW, String key, int color, int shadow, float hover,
            boolean big, float time) {
        float scale = (big ? 3.0F : 2.0F) + 0.25F * hover;
        Component name = Component.translatable("screen.tokyoghoulrpg.class_select." + key + ".name");
        bigText(gfx, name, cx, y, scale, color, shadow);
        int ty = y + (int) (9 * scale) + 4;
        gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.class_select." + key + ".tagline"),
                cx, ty, mix(0xFF8A8A8A, 0xFFD0D0D0, hover));
        ty += 14;
        int maxW = Math.min(colW - 30, 240);
        for (int i = 1; i <= 3; i++) {
            if (ty > this.height - 34) {
                break;
            }
            Component line = Component.literal("◆ ").withStyle(s -> s.withColor(color & 0xFFFFFF))
                    .append(Component.translatable("screen.tokyoghoulrpg.class_select." + key + ".f" + i)
                            .withStyle(s -> s.withColor(0xB8B8B8)));
            for (FormattedCharSequence part : this.font.split(line, maxW)) {
                gfx.drawCenteredString(this.font, part, cx, ty, 0xFFFFFFFF);
                ty += 10;
            }
        }
        if (hover > 0.05F && ty < this.height - 22) {
            int alpha = (int) (255 * hover * (0.6F + 0.4F * Mth.sin(time * 4.0F)));
            gfx.drawCenteredString(this.font, Component.translatable("screen.tokyoghoulrpg.class_select.pick"),
                    cx, Math.max(ty + 4, this.height - 32), (Math.max(8, alpha) << 24) | (color & 0xFFFFFF));
        }
    }

    /** Шаг 2: карточки вариантов кагуне. */
    private void renderVariants(GuiGraphics gfx, int mouseX, int mouseY, float time, float dt) {
        int w = this.width;
        int h = this.height;
        gfx.fillGradient(0, 0, w, h, 0xFF22050A, 0xFF040102);
        glow(gfx, w / 2, h / 2, (int) (h * 0.5F), 0x30FF2030);
        embers(gfx, 0, w, h, time, 70, true, 0.6F);
        header(gfx, Component.translatable("screen.tokyoghoulrpg.variant_select.title"),
                Component.translatable("screen.tokyoghoulrpg.variant_select.subtitle",
                        Component.translatable(selected.translationKey())), 0xFFE0303A);

        List<KaguneVariant> variants = selected.variants();
        int n = variants.size();
        int gap = 10;
        int cardW = Math.min(150, (w - 40 - gap * (n - 1)) / n);
        int cardH = Math.min(h - 80, cardW + 60);
        int x0 = (w - (cardW * n + gap * (n - 1))) / 2;
        int y0 = Math.max(46, (h - cardH) / 2 + 6);
        for (int i = 0; i < n; i++) {
            KaguneVariant v = variants.get(i);
            int x = x0 + i * (cardW + gap);
            boolean over = mouseX >= x && mouseX < x + cardW && mouseY >= y0 && mouseY < y0 + cardH;
            hoverCard[i] = approach(hoverCard[i], over ? 1.0F : 0.0F, dt);
            int lift = (int) (4 * hoverCard[i]);
            int y = y0 - lift;
            cardRects.add(new int[] {x, y0 - 4, cardW, cardH + 4});
            int accent = variantColor(v);
            card(gfx, x, y, cardW, cardH, accent, hoverCard[i]);
            ResourceLocation image = variantImage(v);
            int img = cardW - 12;
            if (image != null) {
                RenderSystem.enableBlend();
                gfx.blit(image, x + 6, y + 6, img, img, 0, 0, 256, 256, 256, 256);
                RenderSystem.disableBlend();
            }
            int ty = y + 8 + img;
            bigText(gfx, Component.translatable(v.translationKey()), x + cardW / 2, ty, 1.35F, accent | 0xFF000000, 0xFF000000);
            ty += 16;
            for (FormattedCharSequence line : this.font.split(
                    Component.translatable("screen.tokyoghoulrpg.variant_card." + v.id()), cardW - 12)) {
                if (ty > y + cardH - 10) {
                    break;
                }
                gfx.drawCenteredString(this.font, line, x + cardW / 2, ty, 0xFFB8B8B8);
                ty += 10;
            }
        }
    }

    /** Шаг 3: облик — свой скин или скины из мода, живые модели на карточках. */
    private void renderSkins(GuiGraphics gfx, int mouseX, int mouseY, float time, float dt) {
        int w = this.width;
        int h = this.height;
        boolean ghoul = selected.hasKagune();
        gfx.fillGradient(0, 0, w, h, ghoul ? 0xFF1E0408 : 0xFF0E1620, 0xFF030102);
        glow(gfx, w / 2, h / 2, (int) (h * 0.5F), ghoul ? 0x28FF2030 : 0x2880B0FF);
        embers(gfx, 0, w, h, time, 50, ghoul, 0.5F);
        header(gfx, Component.translatable("screen.tokyoghoulrpg.skin.title"),
                Component.translatable("screen.tokyoghoulrpg.skin.subtitle"), ghoul ? 0xFFE0303A : 0xFFE6EEF6);

        int count = PlayerData.skinCount(selected);
        String[] keys = ghoul
                ? new String[] {"screen.tokyoghoulrpg.skin.own", "screen.tokyoghoulrpg.skin.ghoul", "screen.tokyoghoulrpg.skin.ghoul_alt"}
                : new String[] {"screen.tokyoghoulrpg.skin.own", "screen.tokyoghoulrpg.skin.ccg"};
        ResourceLocation own = this.minecraft.player != null ? ModSkins.ownSkin(this.minecraft.player) : STEVE;
        boolean ownSlim = this.minecraft.player != null && "slim".equals(this.minecraft.player.getModelName())
                && !ModSkins.isWearing(this.minecraft.player);
        ModSkins.Skin first = ghoul ? ModSkins.GHOUL : ModSkins.CCG;
        ResourceLocation[] skins = {own, first.texture(), ModSkins.GHOUL_ALT.texture()};
        boolean[] slim = {ownSlim, "slim".equals(first.model()), "slim".equals(ModSkins.GHOUL_ALT.model())};

        int gap = 12;
        int cardW = Math.min(130, (w - 40 - gap * (count - 1)) / count);
        int cardH = Math.min(h - 80, 190);
        int x0 = (w - (cardW * count + gap * (count - 1))) / 2;
        int y0 = Math.max(46, (h - cardH) / 2 + 6);
        int accent = ghoul ? 0xE0303A : 0x8CC4FF;
        for (int i = 0; i < count; i++) {
            int x = x0 + i * (cardW + gap);
            boolean over = mouseX >= x && mouseX < x + cardW && mouseY >= y0 && mouseY < y0 + cardH;
            hoverCard[i] = approach(hoverCard[i], over ? 1.0F : 0.0F, dt);
            int y = y0 - (int) (4 * hoverCard[i]);
            cardRects.add(new int[] {x, y0 - 4, cardW, cardH + 4});
            card(gfx, x, y, cardW, cardH, accent, hoverCard[i]);
            int footY = y + cardH - 44;
            float size = Math.min(cardW * 0.42F, (cardH - 60) / 2.1F) * (1.0F + 0.06F * hoverCard[i]);
            renderPlayer(gfx, skins[i], slim[i], x + cardW / 2, footY, size, 22.0F, mouseX, mouseY, time, ghoul && i > 0);
            int ty = footY + 8;
            for (FormattedCharSequence line : this.font.split(Component.translatable(keys[i]), cardW - 10)) {
                gfx.drawCenteredString(this.font, line, x + cardW / 2, ty, 0xFF000000 | (hoverCard[i] > 0.5F ? accent : 0xD0D0D0));
                ty += 10;
            }
            if (over) {
                gfx.renderTooltip(this.font, this.font.split(Component.translatable(keys[i] + ".tip"), 200), mouseX, mouseY);
            }
        }
    }

    private void header(GuiGraphics gfx, Component title, Component subtitle, int color) {
        gfx.fillGradient(0, 0, this.width, 34, 0xC0000000, 0x00000000);
        bigText(gfx, title, this.width / 2, 8, 1.6F, color, 0xFF000000);
        gfx.drawCenteredString(this.font, subtitle, this.width / 2, 25, 0xFF9A9A9A);
    }

    private void renderBack(GuiGraphics gfx, int mouseX, int mouseY) {
        Component label = Component.translatable("screen.tokyoghoulrpg.class_select.back");
        int bw = this.font.width(label) + 16;
        int x = 10;
        int y = this.height - 26;
        backRect = new int[] {x, y, bw, 18};
        boolean over = inside(backRect, mouseX, mouseY);
        gfx.fill(x, y, x + bw, y + 18, over ? 0xA0401016 : 0x80000000);
        gfx.fill(x, y + 17, x + bw, y + 18, over ? 0xFFE0303A : 0x60FFFFFF);
        gfx.drawCenteredString(this.font, label, x + bw / 2, y + 5, over ? 0xFFFFFFFF : 0xFFB0B0B0);
    }

    // ------------------------------------------------------------------ детали оформления

    /** Карточка: тёмная плашка, рамка цвета варианта, при наведении светится. */
    private static void card(GuiGraphics gfx, int x, int y, int w, int h, int accent, float hover) {
        int rgb = accent & 0xFFFFFF;
        // мягкая тень и свечение
        gfx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
        if (hover > 0.02F) {
            gfx.fill(x - 3, y - 3, x + w + 3, y + h + 3, ((int) (0x40 * hover) << 24) | rgb);
        }
        gfx.fillGradient(x, y, x + w, y + h, 0xF0140A0C, 0xF0060304);
        int border = ((int) (0x50 + 0xAF * hover) << 24) | rgb;
        gfx.fill(x, y, x + w, y + 1, border);
        gfx.fill(x, y + h - 1, x + w, y + h, border);
        gfx.fill(x, y, x + 1, y + h, border);
        gfx.fill(x + w - 1, y, x + w, y + h, border);
        gfx.fillGradient(x + 1, y + 1, x + w - 1, y + 20, ((int) (0x30 + 0x30 * hover) << 24) | rgb, 0x00000000);
    }

    /** Мягкое круглое свечение из колец (без текстуры). */
    private static void glow(GuiGraphics gfx, int cx, int cy, int radius, int argb) {
        int a = argb >>> 24;
        int rgb = argb & 0xFFFFFF;
        int rings = 10;
        for (int i = rings; i >= 1; i--) {
            int r = radius * i / rings;
            int alpha = a * (rings - i + 1) / (rings * 3);
            int col = (alpha << 24) | rgb;
            gfx.fill(cx - r, cy - r / 2 - r / 4, cx + r, cy + r / 2 + r / 4, col);
        }
    }

    /** Частицы: у гуля красные искры поднимаются, у человека бледная пыль медленно опускается. */
    private static void embers(GuiGraphics gfx, int x0, int x1, int h, float time, int count, boolean rising, float strength) {
        int span = Math.max(1, x1 - x0);
        for (int i = 0; i < count; i++) {
            float seed = i * 12.9898F;
            float rx = fract(Mth.sin(seed) * 43758.545F);
            float speed = 0.04F + 0.08F * fract(Mth.sin(seed * 1.7F) * 2345.17F);
            float phase = fract(Mth.sin(seed * 3.1F) * 9821.3F);
            float t = fract(phase + time * speed * (rising ? 1.0F : 0.45F));
            float y = rising ? h * (1.0F - t) : h * t;
            float sway = Mth.sin(time * 1.3F + i) * 6.0F;
            int px = x0 + (int) (rx * span + sway);
            if (px < x0 || px >= x1) {
                continue;
            }
            float life = Mth.sin(t * Mth.PI);
            int alpha = (int) (200 * life * strength);
            int size = 1 + (i % 3 == 0 ? 1 : 0);
            int rgb = rising ? (i % 4 == 0 ? 0xFFB040 : 0xE0303A) : 0xC8D8E8;
            gfx.fill(px, (int) y, px + size, (int) y + size, (alpha << 24) | rgb);
        }
    }

    private static float fract(float v) {
        return v - Mth.floor(v);
    }

    /** Крупный текст с тенью (масштаб — во сколько раз больше обычного). */
    private void bigText(GuiGraphics gfx, Component text, int cx, int y, float scale, int color, int shadow) {
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(cx, y, 400.0F);
        ps.scale(scale, scale, 1.0F);
        int tw = this.font.width(text);
        gfx.drawString(this.font, text, -tw / 2 + 1, 1, shadow, false);
        gfx.drawString(this.font, text, -tw / 2, 0, color, false);
        ps.popPose();
    }

    private static float approach(float v, float target, float dt) {
        return v + (target - v) * Math.min(1.0F, dt * 9.0F);
    }

    private static int mix(int a, int b, float t) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static int variantColor(KaguneVariant v) {
        return switch (v) {
            case WINGS -> 0xFF8A2A;
            case GOURMET -> 0xA860F0;
            default -> 0xE0303A;
        };
    }

    private static ResourceLocation variantImage(KaguneVariant v) {
        String name = switch (v) {
            case KANEKI, AUTHOR -> "kaneki";
            case WINGS -> "wings";
            case GOURMET -> "gourmet";
            default -> null;
        };
        return name == null ? null
                : new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/gui/class/variant_" + name + ".png");
    }

    // ------------------------------------------------------------------ живая модель игрока

    /**
     * Модель игрока в меню: стоит на footY, высотой примерно 2 * size, повёрнута на yaw и смотрит на мышь.
     * Гуль чуть сутулится и держит руки «когтями», человек стоит ровно. Дышит и чуть качает руками.
     */
    private void renderPlayer(GuiGraphics gfx, ResourceLocation skin, boolean slim, int x, int footY, float size,
            float yaw, int mouseX, int mouseY, float time, boolean ghoulPose) {
        PlayerModel<AbstractClientPlayer> model = slim ? slimModel : wideModel;
        if (model == null) {
            return;
        }
        ModelPart[] parts = {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        for (ModelPart p : parts) {
            p.xRot = 0.0F;
            p.yRot = 0.0F;
            p.zRot = 0.0F;
        }
        float breathe = Mth.sin(time * 1.6F);
        // голова смотрит на мышь
        float dx = (mouseX - x) / Math.max(40.0F, size * 2.0F);
        float dy = (mouseY - (footY - size * 1.75F)) / Math.max(40.0F, size * 2.0F);
        model.head.yRot = Mth.clamp(dx, -0.9F, 0.9F) - yaw * Mth.DEG_TO_RAD;
        model.head.xRot = Mth.clamp(dy * 0.6F, -0.5F, 0.5F);
        model.rightArm.zRot = 0.05F + 0.035F * breathe;
        model.leftArm.zRot = -0.05F - 0.035F * breathe;
        if (ghoulPose) {
            model.head.xRot += 0.12F;
            model.body.xRot = 0.08F;
            model.rightArm.xRot = -0.35F + 0.05F * breathe;
            model.leftArm.xRot = -0.25F - 0.05F * breathe;
            model.rightArm.zRot += 0.18F;
            model.leftArm.zRot -= 0.18F;
        } else {
            model.rightArm.xRot = 0.04F * breathe;
            model.leftArm.xRot = -0.04F * breathe;
        }
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);

        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(x, footY, 200.0F);
        ps.mulPoseMatrix(new Matrix4f().scaling(size, size, -size));
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.translate(0.0F, -1.501F, 0.0F);
        Lighting.setupForEntityInInventory();
        VertexConsumer vc = gfx.bufferSource().getBuffer(RenderType.entityTranslucent(skin));
        model.renderToBuffer(ps, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        gfx.flush();
        Lighting.setupFor3DItems();
        ps.popPose();
    }
}
