package com.tokyoghoulrpg.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tokyoghoulrpg.client.screen.ScreenFx;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Панель персонажа в стиле RPG в левом верхнем углу (вместо сердец, еды и брони Minecraft):
 * портрет (лицо скина) в рамке цвета класса с уровнем, класс и броня, большая полоса здоровья с цифрами,
 * делениями и «следом» от урона, под ней сытость, у гуля и полугуля — кровь и ярость, у агента CCG — квинке.
 * Ниже — плашки с особыми состояниями (Какудзя, ярость гурмана, ауры, война кланов и т. д.).
 */
public final class HudOverlay {
    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PORTRAIT = 30;
    private static final int BAR_X = PORTRAIT + 10;
    private static final int BAR_W = 118;
    private static final int PANEL_W = BAR_X + BAR_W + 6;
    private static final int HP_H = 11;
    private static final int BAR_H = 8;
    private static final int GAP = 3;

    /** «След» здоровья: после урона светлая часть полосы плавно догоняет новое значение. */
    private static float ghostHealth = -1.0F;
    private static float lastHealth = -1.0F;
    private static long lastDropMillis;

    private HudOverlay() {
    }

    public static void render(ForgeGui gui, GuiGraphics gfx, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        Optional<PlayerData> optional = player.getCapability(PlayerData.CAP).resolve();
        if (optional.isEmpty()) {
            return;
        }
        PlayerData data = optional.get();
        if (!data.hasChosenClass()) {
            return;
        }
        Font font = mc.font;
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        float t = now + partialTick;
        boolean ghoul = data.getPlayerClass().hasKagune();
        boolean agent = data.getPlayerClass() == PlayerClass.CCG_AGENT;
        ScreenFx.Theme theme = ScreenFx.theme(data.getPlayerClass());
        int accent = theme.accent() & 0xFFFFFF;

        int rows = 1 + (ghoul ? 2 : (agent ? 1 : 0));
        int barsH = 15 + HP_H + rows * (BAR_H + GAP);
        int panelH = Math.max(PORTRAIT + 20, barsH + 6);
        int x = PANEL_X;
        int y = PANEL_Y;

        // панель: тёмный градиент, рамка и уголки цвета класса, полоса слева
        gfx.fill(x + 2, y + 3, x + PANEL_W + 2, y + panelH + 3, 0x50000000);
        gfx.fillGradient(x, y, x + PANEL_W, y + panelH, 0xC8160A0D, 0xC8060304);
        ScreenFx.frame(gfx, x, y, PANEL_W, panelH, ScreenFx.alpha(accent, 0x60));
        gfx.fillGradient(x, y, x + 2, y + panelH, 0xFF000000 | accent, ScreenFx.alpha(accent, 0x40));
        ScreenFx.corners(gfx, x, y, PANEL_W, panelH, 0xFF000000 | accent, 5);
        gfx.fillGradient(x + 1, y + 1, x + PANEL_W - 1, y + 12, ScreenFx.alpha(accent, 0x30), 0x00000000);

        // портрет: лицо скина в рамке, под ним уровень
        int px = x + 6;
        int py = y + 5;
        gfx.fill(px - 2, py - 2, px + PORTRAIT + 2, py + PORTRAIT + 2, 0xFF000000 | accent);
        gfx.fill(px - 1, py - 1, px + PORTRAIT + 1, py + PORTRAIT + 1, 0xFF0A0506);
        if (player instanceof AbstractClientPlayer client) {
            PlayerFaceRenderer.draw(gfx, client.getSkinTextureLocation(), px + 1, py + 1, PORTRAIT - 2);
        }
        if (player.hurtTime > 0) {
            gfx.fill(px, py, px + PORTRAIT, py + PORTRAIT, ScreenFx.alpha(0xFF0000, player.hurtTime * 12));
        }
        String lvl = String.valueOf(player.experienceLevel);
        int bw = Math.max(16, font.width(lvl) + 8);
        int bx = px + PORTRAIT / 2 - bw / 2;
        int by = py + PORTRAIT - 3;
        gfx.fill(bx - 1, by - 1, bx + bw + 1, by + 11, 0xFF000000 | accent);
        gfx.fillGradient(bx, by, bx + bw, by + 10, 0xFF2A1014, 0xFF0C0506);
        gfx.drawString(font, lvl, bx + (bw - font.width(lvl)) / 2, by + 1, 0xFFFFE6A0, true);
        // полоска опыта до следующего уровня под портретом
        int ex = px - 1;
        int ey = py + PORTRAIT + 10;
        gfx.fill(ex, ey, ex + PORTRAIT + 2, ey + 2, 0xFF1A0A0C);
        gfx.fill(ex, ey, ex + Math.round((PORTRAIT + 2) * Mth.clamp(player.experienceProgress, 0.0F, 1.0F)), ey + 2,
                0xFF000000 | ScreenFx.mix(0xE0303A, 0xFFD27A, 0.4F));

        // верхняя строка: класс и броня
        int cx = x + BAR_X;
        gfx.drawString(font, Component.translatable(data.getPlayerClass().translationKey()), cx, y + 4,
                0xFF000000 | ScreenFx.mix(0xFFFFFF, accent, 0.35F), true);
        int armor = player.getArmorValue();
        if (armor > 0) {
            Component a = Component.translatable("hud.tokyoghoulrpg.armor", armor);
            small(gfx, font, a, x + PANEL_W - 6 - Math.round(font.width(a) * 0.75F), y + 5, 0xFFB8B0B4);
        }

        // здоровье
        int hy = y + 15;
        float max = Math.max(1.0F, player.getMaxHealth());
        float hp = Mth.clamp(player.getHealth() / max, 0.0F, 1.0F);
        updateGhost(hp);
        int top = 0xFF4650;
        int bottom = 0x9A0E1C;
        if (player.hasEffect(MobEffects.POISON)) {
            top = 0x8CD04A;
            bottom = 0x3E6A14;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            top = 0x6A6066;
            bottom = 0x2A2226;
        } else if (player.isFullyFrozen()) {
            top = 0xA8E0FF;
            bottom = 0x3A7AA8;
        }
        float pulse = 1.0F;
        if (hp <= 0.25F) {
            pulse = 0.75F + 0.25F * Mth.sin(t * 0.35F);
        }
        Component hpText = Component.literal(Math.round(player.getHealth()) + " / " + Math.round(max));
        bar(gfx, font, cx, hy, BAR_W, HP_H, hp, ghostHealth, scale(top, pulse), scale(bottom, pulse),
                Component.translatable("hud.tokyoghoulrpg.bar.health"), hpText, 10);
        float absorb = player.getAbsorptionAmount();
        if (absorb > 0.0F) {
            int aw = Math.round(BAR_W * Mth.clamp(absorb / max, 0.0F, 1.0F));
            gfx.fill(cx, hy - 2, cx + aw, hy, 0xFFFFD040);
        }

        int ry = hy + HP_H + GAP;
        // сытость
        float food = player.getFoodData().getFoodLevel() / 20.0F;
        boolean hunger = player.hasEffect(MobEffects.HUNGER);
        bar(gfx, font, cx, ry, BAR_W, BAR_H, food, -1.0F, hunger ? 0x9CC44A : 0xFFB45A, hunger ? 0x4A6A18 : 0x9A4E12,
                Component.translatable("hud.tokyoghoulrpg.bar.food"),
                Component.literal(player.getFoodData().getFoodLevel() + " / 20"), 0);
        ry += BAR_H + GAP;

        if (ghoul) {
            // кровь (жажда крови): пустеет, когда гуль голоден; при малом запасе мигает
            int craving = data.getCraving();
            float p = craving <= 25 ? 0.6F + 0.4F * Mth.sin(t * 0.5F) : 1.0F;
            bar(gfx, font, cx, ry, BAR_W, BAR_H, craving / 100.0F, -1.0F, scale(0xD0203C, p), scale(0x5A0814, p),
                    Component.translatable("hud.tokyoghoulrpg.blood"), Component.literal(craving + " / 100"), 0);
            ry += BAR_H + GAP;
        }
        if (ghoul || agent) {
            // ярость гуля или «Освобождение квинке» агента
            int full = agent ? 0x6FF0E0 : 0xFF4A78;
            int low = agent ? 0x1E8478 : 0x9A1440;
            Component label = Component.translatable(agent ? "hud.tokyoghoulrpg.bar.release" : "hud.tokyoghoulrpg.bar.rage");
            if (data.isRageActive()) {
                long total = Math.max(1L, data.getRageEndTick() - data.getRageStartTick());
                long left = Math.max(0L, data.getRageEndTick() - now);
                float p = 0.8F + 0.2F * Mth.sin(t * 0.4F);
                bar(gfx, font, cx, ry, BAR_W, BAR_H, left / (float) total, -1.0F, scale(full, p), scale(low, p), label,
                        Component.translatable("hud.tokyoghoulrpg.bar.seconds", (int) Math.ceil(left / 20.0)), 0);
                glowEdge(gfx, cx, ry, BAR_W, BAR_H, full, p);
            } else {
                int rage = data.getRage();
                boolean ready = rage >= PlayerData.RAGE_MAX;
                float p = ready ? 0.7F + 0.3F * Mth.sin(t * 0.3F) : 1.0F;
                bar(gfx, font, cx, ry, BAR_W, BAR_H, rage / (float) PlayerData.RAGE_MAX, -1.0F, scale(full, p),
                        scale(low, p), label, ready ? Component.translatable("hud.tokyoghoulrpg.bar.ready")
                                : Component.literal(rage + " / " + PlayerData.RAGE_MAX), 0);
                if (ready) {
                    glowEdge(gfx, cx, ry, BAR_W, BAR_H, full, p);
                }
            }
        }

        // ниже панели: стадия Какудзя, ярость гурмана и плашки с особыми состояниями
        int ty = y + panelH + 4;
        if (ghoul && data.getKakujaPoints() > 0) {
            int points = data.getKakujaPoints();
            int stage = com.tokyoghoulrpg.combat.Kakuja.stage(points);
            int next = com.tokyoghoulrpg.combat.Kakuja.nextThreshold(stage);
            int prev = stage == 0 ? 0 : com.tokyoghoulrpg.combat.Balance.KAKUJA_STAGES[stage - 1];
            Component label = stage == 0
                    ? Component.translatable("hud.tokyoghoulrpg.kakuja_stage_none")
                    : Component.translatable("hud.tokyoghoulrpg.kakuja_stage", com.tokyoghoulrpg.skill.SkillTree.roman(stage));
            float fill = next < 0 ? 1.0F : (points - prev) / (float) Math.max(1, next - prev);
            float p = stage >= 3 ? 0.8F + 0.2F * Mth.sin(t * 0.12F) : 1.0F;
            ty = chipBar(gfx, font, label, ty, fill, scale(next < 0 ? 0xFF2040 : 0xC01828, p), 0xE0203A);
        }
        if (ghoul && data.getKaguneVariant() == com.tokyoghoulrpg.data.KaguneVariant.GOURMET
                && (data.getGourmetRage() > 0 || data.isGourmetRageActive(now))) {
            int maxRage = com.tokyoghoulrpg.combat.GourmetBalance.RAGE_MAX;
            boolean active = data.isGourmetRageActive(now);
            Component label;
            float fill;
            int color;
            if (active) {
                long left = Math.max(0L, data.getGourmetRageEndTick() - now);
                label = Component.translatable("hud.tokyoghoulrpg.gourmet_rage_active", (int) Math.ceil(left / 20.0));
                fill = left / (float) com.tokyoghoulrpg.combat.GourmetBalance.RAGE_TICKS;
                color = scale(0xC060FF, 0.8F + 0.2F * Mth.sin(t * 0.4F));
            } else {
                label = Component.translatable("hud.tokyoghoulrpg.gourmet_rage", data.getGourmetRage(), maxRage);
                fill = data.getGourmetRage() / (float) maxRage;
                color = data.getGourmetRage() >= maxRage ? scale(0xC060FF, 0.7F + 0.3F * Mth.sin(t * 0.3F)) : 0x8A3AD8;
            }
            ty = chipBar(gfx, font, label, ty, fill, color, 0xC48AF0);
        }
        // стиль боя: откаты способностей на C и Shift + C, стойка «Контратаки»
        com.tokyoghoulrpg.style.StyleData styleData = data.style();
        if (styleData.active() != null) {
            com.tokyoghoulrpg.style.FightStyle style = styleData.active();
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.style",
                    Component.translatable(style.key("name")), styleCooldown(styleData.ability1Ready(), now),
                    styleCooldown(styleData.ability2Ready(), now)), ty, style.accent());
            if (styleData.stanceUntil > now) {
                ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.style.stance"), ty,
                        scale(0xFFD070, 0.8F + 0.2F * Mth.sin(t * 0.6F)) & 0xFFFFFF);
            }
            if (styleData.smokeUntil > now) {
                ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.style.smoke",
                        secondsLeft(styleData.smokeUntil, now)), ty,
                        scale(0xA88AE8, 0.8F + 0.2F * Mth.sin(t * 0.4F)) & 0xFFFFFF);
            }
        }
        if (com.tokyoghoulrpg.item.MaskEffects.fullSet(player, data)) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.ghoul_set"), ty, 0xD04050);
        }
        if (com.tokyoghoulrpg.item.CcgSet.fullSet(player, data)) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.ccg_set"), ty, 0x3FD8C0);
        }
        if (!data.getWarEnemy().isEmpty() && data.getWarEndTick() > now) {
            int secs = secondsLeft(data.getWarEndTick(), now);
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.war", data.getWarEnemy(), data.getWarOur(),
                    data.getWarTheir(), data.getWarTarget(), secs / 60, String.format("%02d", secs % 60)), ty, 0xFF4040);
        }
        if (com.tokyoghoulrpg.world.HuntNight.isActive(mc.level)) {
            float p = 0.75F + 0.25F * Mth.sin(t * 0.15F);
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.hunt_night"), ty, scale(0xFF3030, p) & 0xFFFFFF);
        }
        if (data.isRetributionActive(now)) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.retribution",
                    secondsLeft(data.getRetributionEndTick(), now)), ty, 0xB070FF);
        }
        if (data.isCharismaActive(now)) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.charisma",
                    secondsLeft(data.getCharismaEndTick(), now)), ty, 0xFFD133);
        }
        double shield = data.fortuneShieldAt(now);
        if (shield > 0.0) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.fortune", Math.round(shield * 100),
                    secondsLeft(data.getFortuneShieldEndTick(), now)), ty, 0x3CC878);
        }
        if (data.isSilencedAt(now)) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.silenced",
                    secondsLeft(data.getSilenceEndTick(), now)), ty, 0xA0A0A0);
        }
        if (data.isKakujaActive(now)) {
            ty = chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.kakuja",
                    secondsLeft(data.getKakujaEndTick(), now)), ty, 0xE0203A);
        }
        if (data.getBondEndTick() > now) {
            double power = data.getBondPower();
            boolean gained = power >= 0.0;
            chip(gfx, font, Component.translatable(gained ? "hud.tokyoghoulrpg.bond_gained" : "hud.tokyoghoulrpg.bond_given",
                    Math.round(Math.abs(power) * 100), secondsLeft(data.getBondEndTick(), now)), ty,
                    gained ? 0xFFC040 : 0xC07858);
        } else if (data.getBondCooldownEnd() > now) {
            chip(gfx, font, Component.translatable("hud.tokyoghoulrpg.bond_cooldown",
                    secondsLeft(data.getBondCooldownEnd(), now)), ty, 0x8A8A8A);
        }
    }

    /** Отдельный слой поверх всего: подсказки для новичков. */
    public static void renderTutorial(ForgeGui gui, GuiGraphics gfx, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        com.tokyoghoulrpg.client.TutorialClient.render(gfx, screenWidth, screenHeight);
    }

    private static void updateGhost(float hp) {
        long ms = Util.getMillis();
        if (ghostHealth < 0.0F || hp >= ghostHealth) {
            ghostHealth = hp;
        } else if (ms - lastDropMillis > 450L) {
            ghostHealth = Math.max(hp, ghostHealth - Math.max(0.004F, (ghostHealth - hp) * 0.06F));
        }
        if (lastHealth >= 0.0F && hp < lastHealth - 1.0E-4F) {
            lastDropMillis = ms;
        }
        lastHealth = hp;
    }

    /**
     * Полоса: тёмная подложка с рамкой, «след» урона, заливка градиентом с бликом сверху и тенью снизу,
     * деления, слева название, справа значение (мелким шрифтом).
     */
    private static void bar(GuiGraphics gfx, Font font, int x, int y, int w, int h, float fraction, float ghost,
            int top, int bottom, Component label, Component value, int ticks) {
        float f = Mth.clamp(fraction, 0.0F, 1.0F);
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        gfx.fillGradient(x, y, x + w, y + h, 0xFF1A0E10, 0xFF0C0607);
        if (ghost > f) {
            int gw = Math.round(w * Mth.clamp(ghost, 0.0F, 1.0F));
            gfx.fill(x + Math.round(w * f), y, x + gw, y + h, 0xB0E07880);
        }
        int fw = Math.round(w * f);
        if (fw > 0) {
            gfx.fillGradient(x, y, x + fw, y + h, 0xFF000000 | top, 0xFF000000 | bottom);
            gfx.fill(x, y, x + fw, y + 1, 0x60FFFFFF);
            gfx.fill(x, y + h - 1, x + fw, y + h, 0x50000000);
            if (fw > 1) {
                gfx.fill(x + fw - 1, y, x + fw, y + h, 0x90FFFFFF);
            }
        }
        if (ticks > 1) {
            for (int i = 1; i < ticks; i++) {
                int tx = x + w * i / ticks;
                gfx.fill(tx, y + 1, tx + 1, y + h - 1, 0x40000000);
            }
        }
        float s = h >= 10 ? 0.75F : 0.62F;
        int ty = y + Math.round((h - 8 * s) / 2.0F);
        small(gfx, font, label, x + 3, ty, 0xFFF4E8E8, s);
        int vw = Math.round(font.width(value) * s);
        small(gfx, font, value, x + w - 3 - vw, ty, 0xFFFFFFFF, s);
    }

    /** Мягкое свечение по краю полосы (ярость готова или действует). */
    private static void glowEdge(GuiGraphics gfx, int x, int y, int w, int h, int rgb, float pulse) {
        int a = (int) (0x50 * pulse);
        ScreenFx.frame(gfx, x - 2, y - 2, w + 4, h + 4, ScreenFx.alpha(rgb, a));
        ScreenFx.frame(gfx, x - 3, y - 3, w + 6, h + 6, ScreenFx.alpha(rgb, a / 2));
    }

    private static Component styleCooldown(long ready, long now) {
        return ready <= now ? Component.translatable("hud.tokyoghoulrpg.style.ready")
                : Component.translatable("hud.tokyoghoulrpg.style.seconds", (ready - now + 19) / 20);
    }

    /** Плашка под панелью: тёмный фон, цветная полоса слева, текст. Возвращает y следующей плашки. */
    private static int chip(GuiGraphics gfx, Font font, Component text, int y, int color) {
        int w = font.width(text) + 10;
        int x = PANEL_X;
        gfx.fillGradient(x, y, x + w, y + 11, 0xB8140A0C, 0xB8060304);
        gfx.fill(x, y, x + 2, y + 11, 0xFF000000 | color);
        gfx.fill(x + 2, y + 10, x + w, y + 11, ScreenFx.alpha(color, 0x50));
        gfx.drawString(font, text, x + 6, y + 2, 0xFF000000 | color, true);
        return y + 13;
    }

    /** Плашка с полоской (стадия Какудзя, ярость гурмана). */
    private static int chipBar(GuiGraphics gfx, Font font, Component text, int y, float fill, int barColor, int textColor) {
        int tw = font.width(text);
        int w = Math.max(PANEL_W, tw + 70);
        int x = PANEL_X;
        gfx.fillGradient(x, y, x + w, y + 11, 0xB8140A0C, 0xB8060304);
        gfx.fill(x, y, x + 2, y + 11, 0xFF000000 | textColor);
        gfx.drawString(font, text, x + 6, y + 2, 0xFF000000 | textColor, true);
        int bx = x + tw + 12;
        int bw = x + w - 6 - bx;
        gfx.fill(bx - 1, y + 3, bx + bw + 1, y + 8, 0xFF000000);
        gfx.fill(bx, y + 4, bx + bw, y + 7, 0xFF1A0A0C);
        int fw = Math.round(bw * Mth.clamp(fill, 0.0F, 1.0F));
        if (fw > 0) {
            gfx.fillGradient(bx, y + 4, bx + fw, y + 7, 0xFF000000 | ScreenFx.mix(barColor & 0xFFFFFF, 0xFFFFFF, 0.25F),
                    0xFF000000 | barColor);
        }
        return y + 13;
    }

    private static void small(GuiGraphics gfx, Font font, Component text, int x, int y, int color) {
        small(gfx, font, text, x, y, color, 0.75F);
    }

    private static void small(GuiGraphics gfx, Font font, Component text, int x, int y, int color, float s) {
        PoseStack ps = gfx.pose();
        ps.pushPose();
        ps.translate(x, y, 0.0F);
        ps.scale(s, s, 1.0F);
        gfx.drawString(font, text, 0, 0, color, true);
        ps.popPose();
    }

    private static int secondsLeft(long end, long now) {
        return (int) Math.ceil(Math.max(0L, end - now) / 20.0);
    }

    /** Цвет 0xRRGGBB, затемнённый в factor раз (для мигания). */
    private static int scale(int rgb, float factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }
}
