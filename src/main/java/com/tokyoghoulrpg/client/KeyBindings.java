package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

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

    private KeyBindings() {
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping("key.tokyoghoulrpg." + name, InputConstants.Type.KEYSYM, keyCode, CATEGORY);
    }
}
