package com.tokyoghoulrpg.client.hud;

import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Компактная панель в левом верхнем углу: класс, уровень, здоровье, голод, у гуля ещё кровь и ярость. */
public final class HudOverlay {
    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PANEL_W = 122;
    private static final int BAR_W = 108;
    private static final int BAR_H = 5;

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

        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        boolean ghoul = data.getPlayerClass().hasKagune();
        boolean agent = data.getPlayerClass() == PlayerClass.CCG_AGENT;
        boolean showRage = ghoul || agent;            // у агента это шкала «Освобождения квинке»
        boolean rageActive = showRage && data.isRageActive();
        int rows = ghoul ? 4 : (agent ? 3 : 2);
        int panelH = 18 + rows * 10 + 2 + (rageActive ? 10 : 0);

        // Тёмная панель с красной полосой слева
        gfx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + panelH, 0xB0100608);
        gfx.fill(PANEL_X, PANEL_Y, PANEL_X + 2, PANEL_Y + panelH, 0xFFB01E1E);

        Font font = mc.font;
        gfx.drawString(font, Component.translatable(data.getPlayerClass().translationKey()),
                PANEL_X + 8, PANEL_Y + 4, 0xE6E6E6, false);
        Component level = Component.translatable("hud.tokyoghoulrpg.level", player.experienceLevel);
        gfx.drawString(font, level, PANEL_X + PANEL_W - 6 - font.width(level), PANEL_Y + 4, 0x9A9A9A, false);

        int bx = PANEL_X + 8;
        int by = PANEL_Y + 17;
        drawBar(gfx, bx, by, player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0xFFB01E1E, 0xFF2E0A0A);
        by += 10;
        drawBar(gfx, bx, by, player.getFoodData().getFoodLevel() / 20.0F, 0xFFC9822B, 0xFF33200C);

        if (ghoul) {
            // Шкала «Кровь» (жажда крови): пустеет, когда гуль голоден; при малом запасе мигает
            by += 10;
            Component label = Component.translatable("hud.tokyoghoulrpg.blood");
            int labelW = font.width(label) + 4;
            gfx.drawString(font, label, bx, by - 2, 0xC04050, false);
            int craving = data.getCraving();
            int cravingColor = 0xFF8E1426;
            if (craving <= 25) {
                float pulse = 0.6F + 0.4F * Mth.sin((now + partialTick) * 0.5F);
                cravingColor = scale(0xFFE0203A, pulse);
            }
            drawBar(gfx, bx + labelW, by, BAR_W - labelW, craving / 100.0F, cravingColor, 0xFF240810);
        }
        if (showRage) {
            by += 10;
            int full = agent ? 0xFF3FD8C0 : 0xFFFF2A55;
            int base = agent ? 0xFF2AA898 : 0xFFD01F4A;
            int back = agent ? 0xFF0A2420 : 0xFF2A0A14;
            if (rageActive) {
                // Ярость действует: шкала убывает вместе со временем, под ней оставшиеся секунды
                long total = Math.max(1L, data.getRageEndTick() - data.getRageStartTick());
                long left = Math.max(0L, data.getRageEndTick() - now);
                float pulse = 0.8F + 0.2F * Mth.sin((now + partialTick) * 0.4F);
                drawBar(gfx, bx, by, left / (float) total, scale(full, pulse), back);
                int seconds = (int) Math.ceil(left / 20.0);
                gfx.drawString(font, Component.translatable(agent ? "hud.tokyoghoulrpg.release_active"
                        : "hud.tokyoghoulrpg.rage_active", seconds), bx, by + 7, agent ? 0x6FE8D8 : 0xFF5A78, false);
            } else {
                float fraction = data.getRage() / (float) PlayerData.RAGE_MAX;
                int color = base;
                if (data.getRage() >= PlayerData.RAGE_MAX) {
                    // Полная шкала пульсирует: ультимейт готов
                    float pulse = 0.7F + 0.3F * Mth.sin((now + partialTick) * 0.3F);
                    color = scale(full, pulse);
                }
                drawBar(gfx, bx, by, fraction, color, back);
            }
        }

        // Под панелью: особые ауры и «тишина», сколько секунд осталось
        int ty = PANEL_Y + panelH + 3;
        // Стадия Какудзя: название и полоска до следующей стадии (гуль и полугуль)
        if (ghoul && data.getKakujaPoints() > 0) {
            int points = data.getKakujaPoints();
            int stage = com.tokyoghoulrpg.combat.Kakuja.stage(points);
            int next = com.tokyoghoulrpg.combat.Kakuja.nextThreshold(stage);
            int prev = stage == 0 ? 0 : com.tokyoghoulrpg.combat.Balance.KAKUJA_STAGES[stage - 1];
            Component label = stage == 0
                    ? Component.translatable("hud.tokyoghoulrpg.kakuja_stage_none")
                    : Component.translatable("hud.tokyoghoulrpg.kakuja_stage", com.tokyoghoulrpg.skill.SkillTree.roman(stage));
            float pulse = stage >= 3 ? 0.8F + 0.2F * Mth.sin((now + partialTick) * 0.12F) : 1.0F;
            gfx.drawString(font, label, PANEL_X + 2, ty, scale(0xFFE0203A, pulse) & 0xFFFFFF, true);
            int barX = PANEL_X + 4 + font.width(label) + 4;
            int barW = Math.max(30, PANEL_X + PANEL_W - barX);
            float fill = next < 0 ? 1.0F : (points - prev) / (float) Math.max(1, next - prev);
            drawBar(gfx, barX, ty + 2, barW, fill, next < 0 ? 0xFFFF2040 : 0xFF9A1020, 0xFF240810);
            ty += 10;
        }
        // «Облик гуля»: полный комплект одежды гуля и маска
        if (com.tokyoghoulrpg.item.MaskEffects.fullSet(player, data)) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.ghoul_set"), PANEL_X + 2, ty, 0xD04050, true);
            ty += 10;
        }
        // «Защита CCG»: полный комплект брони CCG на агенте
        if (com.tokyoghoulrpg.item.CcgSet.fullSet(player, data)) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.ccg_set"), PANEL_X + 2, ty, 0x3FD8C0, true);
            ty += 10;
        }
        // война кланов: враг, счёт, сколько осталось
        if (!data.getWarEnemy().isEmpty() && data.getWarEndTick() > now) {
            int secs = secondsLeft(data.getWarEndTick(), now);
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.war", data.getWarEnemy(), data.getWarOur(),
                    data.getWarTheir(), data.getWarTarget(), secs / 60, String.format("%02d", secs % 60)),
                    PANEL_X + 2, ty, 0xFF4040, true);
            ty += 10;
        }
        // «Ночь охоты»: красная надпись, пока идёт
        if (com.tokyoghoulrpg.world.HuntNight.isActive(mc.level)) {
            float pulse = 0.75F + 0.25F * Mth.sin((now + partialTick) * 0.15F);
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.hunt_night"), PANEL_X + 2, ty,
                    scale(0xFFFF3030, pulse) & 0xFFFFFF, true);
            ty += 10;
        }
        if (data.isRetributionActive(now)) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.retribution", secondsLeft(data.getRetributionEndTick(), now)),
                    PANEL_X + 2, ty, 0xB070FF, true);
            ty += 10;
        }
        if (data.isCharismaActive(now)) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.charisma", secondsLeft(data.getCharismaEndTick(), now)),
                    PANEL_X + 2, ty, 0xFFD133, true);
            ty += 10;
        }
        double shield = data.fortuneShieldAt(now);
        if (shield > 0.0) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.fortune", Math.round(shield * 100),
                    secondsLeft(data.getFortuneShieldEndTick(), now)), PANEL_X + 2, ty, 0x3CC878, true);
            ty += 10;
        }
        if (data.isSilencedAt(now)) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.silenced", secondsLeft(data.getSilenceEndTick(), now)),
                    PANEL_X + 2, ty, 0xA0A0A0, true);
            ty += 10;
        }
        // Какудзя
        if (data.isKakujaActive(now)) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.kakuja", secondsLeft(data.getKakujaEndTick(), now)),
                    PANEL_X + 2, ty, 0xE0203A, true);
            ty += 10;
        }
        // передача силы в клане: получил (+) или отдал (-), потом откат
        if (data.getBondEndTick() > now) {
            double power = data.getBondPower();
            boolean gained = power >= 0.0;
            gfx.drawString(font, Component.translatable(gained ? "hud.tokyoghoulrpg.bond_gained" : "hud.tokyoghoulrpg.bond_given",
                            Math.round(Math.abs(power) * 100), secondsLeft(data.getBondEndTick(), now)),
                    PANEL_X + 2, ty, gained ? 0xFFC040 : 0xC07858, true);
        } else if (data.getBondCooldownEnd() > now) {
            gfx.drawString(font, Component.translatable("hud.tokyoghoulrpg.bond_cooldown",
                    secondsLeft(data.getBondCooldownEnd(), now)), PANEL_X + 2, ty, 0x8A8A8A, true);
        }
    }

    /** Отдельный слой поверх всего: подсказки для новичков. */
    public static void renderTutorial(ForgeGui gui, GuiGraphics gfx, float partialTick, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().options.hideGui) {
            return;
        }
        com.tokyoghoulrpg.client.TutorialClient.render(gfx, screenWidth, screenHeight);
    }

    private static int secondsLeft(long end, long now) {
        return (int) Math.ceil(Math.max(0L, end - now) / 20.0);
    }

    private static void drawBar(GuiGraphics gfx, int x, int y, float fraction, int fill, int back) {
        drawBar(gfx, x, y, BAR_W, fraction, fill, back);
    }

    private static void drawBar(GuiGraphics gfx, int x, int y, int barW, float fraction, int fill, int back) {
        float clamped = Mth.clamp(fraction, 0.0F, 1.0F);
        gfx.fill(x, y, x + barW, y + BAR_H, back);
        int width = Math.round(barW * clamped);
        if (width > 0) {
            gfx.fill(x, y, x + width, y + BAR_H, fill);
        }
    }

    private static int scale(int argb, float factor) {
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
