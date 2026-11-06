package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Ульта гуля с клавиатуры: зажать Alt (левый или правый) и нажать клавишу ярости (J).
 * Без Alt клавиша J — обычная ярость, без задержки. Если клавишу ярости переназначили — работает новая.
 */
public final class UltimateCombo {
    private UltimateCombo() {
    }

    /** Зажат ли Alt (на Mac — Option). */
    public static boolean altHeld() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static void reset() {
    }
}
