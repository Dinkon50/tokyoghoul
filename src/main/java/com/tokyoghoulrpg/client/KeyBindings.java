package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Все клавиши мода. Любую можно переназначить: Настройки → Управление → Привязка клавиш → раздел «Tokyo Ghoul RPG».
 * Удары мышью, Shift и прыжок — это обычные клавиши Minecraft (Атака, Использовать, Красться, Прыжок),
 * они меняются там же, и мод сразу берёт новые.
 */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.tokyoghoulrpg";

    public static final KeyMapping TOGGLE_KAGUNE = key("toggle_kagune", GLFW.GLFW_KEY_G);
    public static final KeyMapping BITE = key("bite", GLFW.GLFW_KEY_V);
    public static final KeyMapping DASH = key("dash", GLFW.GLFW_KEY_H);
    public static final KeyMapping BLOCK = key("block", GLFW.GLFW_KEY_L);
    public static final KeyMapping BARRAGE = key("barrage", GLFW.GLFW_KEY_K);
    public static final KeyMapping RAGE = key("rage", GLFW.GLFW_KEY_J);
    public static final KeyMapping SKILL_TREE = key("skill_tree", GLFW.GLFW_KEY_B);
    public static final KeyMapping QUESTS = key("quests", GLFW.GLFW_KEY_N);
    public static final KeyMapping CLAN = key("clan", GLFW.GLFW_KEY_Y);
    /** Особые ауры (открываются в дереве навыков, вкладка «Особые»). */
    public static final KeyMapping RETRIBUTION = key("retribution", GLFW.GLFW_KEY_R);
    public static final KeyMapping CHARISMA = key("charisma", GLFW.GLFW_KEY_Z);
    public static final KeyMapping FORTUNE = key("fortune", GLFW.GLFW_KEY_I);
    /** Какудзя (гуль и полугуль, когда накопили очки). */
    public static final KeyMapping KAKUJA = key("kakuja", GLFW.GLFW_KEY_U);
    /** Меню «Профиль»: всё о персонаже на одном экране. */
    public static final KeyMapping PROFILE = key("profile", GLFW.GLFW_KEY_O);
    /** Торговая площадка: продажа вещей между кланами и внутри клана. */
    public static final KeyMapping MARKET = key("market", GLFW.GLFW_KEY_M);
    /**
     * Держать вместе с ЛКМ (притянуть цель) или ПКМ (притянуться самому): захваты кагуне.
     * По умолчанию Ctrl. На Mac Minecraft сам превращает Ctrl + ЛКМ в ПКМ, поэтому там по умолчанию Option (Alt).
     */
    public static final KeyMapping GRAB = key("grab", Minecraft.ON_OSX ? GLFW.GLFW_KEY_LEFT_ALT : GLFW.GLFW_KEY_LEFT_CONTROL);

    private KeyBindings() {
    }

    /** Зажата ли клавиша захвата. Проверяем саму клавишу: она может совпадать с бегом (тоже Ctrl). */
    public static boolean grabHeld() {
        if (GRAB.isUnbound()) {
            return false;
        }
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key key = GRAB.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping("key.tokyoghoulrpg." + name, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
    }
}
