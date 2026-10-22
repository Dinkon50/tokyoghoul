package com.tokyoghoulrpg;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Настройки клиента: файл config/tokyoghoulrpg-client.toml (появляется после первого запуска).
 * Там настройки геймпада, вида от первого лица и VR.
 */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue GAMEPAD_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GAMEPAD_LOOK_SPEED;
    public static final ForgeConfigSpec.BooleanValue GAMEPAD_INVERT_Y;
    public static final ForgeConfigSpec.DoubleValue GAMEPAD_DEADZONE;
    public static final ForgeConfigSpec.DoubleValue GAMEPAD_CURSOR_SPEED;

    public static final ForgeConfigSpec.BooleanValue KAGUNE_FIRST_PERSON_ALWAYS;
    public static final ForgeConfigSpec.DoubleValue VR_QUINQUE_SCALE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Геймпад (Xbox, PlayStation и другие, которые понимает GLFW)").push("gamepad");
        GAMEPAD_ENABLED = b.comment("Играть на геймпаде. false — мод не трогает геймпад")
                .define("enabled", true);
        GAMEPAD_LOOK_SPEED = b.comment("Скорость поворота камеры правым стиком (1.0 — обычная)")
                .defineInRange("lookSpeed", 1.0, 0.1, 4.0);
        GAMEPAD_INVERT_Y = b.comment("Инвертировать правый стик по вертикали")
                .define("invertY", false);
        GAMEPAD_DEADZONE = b.comment("Мёртвая зона стиков: чем больше, тем меньше дрожание от старого стика")
                .defineInRange("deadzone", 0.15, 0.0, 0.6);
        GAMEPAD_CURSOR_SPEED = b.comment("Скорость курсора в меню (1.0 — обычная)")
                .defineInRange("cursorSpeed", 1.0, 0.2, 4.0);
        b.pop();

        b.comment("Вид от первого лица и VR").push("view");
        KAGUNE_FIRST_PERSON_ALWAYS = b.comment("Своё кагуне от первого лица видно всегда. false — только в действии "
                        + "(удары, барраж, захваты, рывок, блок), чтобы не отвлекало")
                .define("kaguneFirstPersonAlways", false);
        VR_QUINQUE_SCALE = b.comment("Размер квинке в руке в VR (1.0 — настоящий размер, как у модели игрока)")
                .defineInRange("vrQuinqueScale", 1.0, 0.3, 3.0);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
