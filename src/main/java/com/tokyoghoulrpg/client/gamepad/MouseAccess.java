package com.tokyoghoulrpg.client.gamepad;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Двигает курсор мыши, когда в меню им управляет геймпад.
 * Системный курсор двигаем через GLFW, но не на всех системах Minecraft об этом узнаёт
 * (на Linux и macOS событие движения от такого перемещения не приходит). Поэтому положение курсора
 * у Minecraft (MouseHandler.xpos / ypos — по нему меню подсвечивает кнопки) пишем и сами.
 * Поля закрытые и в собранной игре называются иначе, поэтому ищем их по виду: это первые два поля double,
 * и проверяем, что в них лежит то же, что возвращают xpos() и ypos().
 */
final class MouseAccess {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean searched;
    private static Field xField;
    private static Field yField;

    private MouseAccess() {
    }

    static void moveTo(Minecraft mc, double x, double y) {
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
        MouseHandler mouse = mc.mouseHandler;
        if (!searched) {
            searched = true;
            find(mouse);
        }
        if (xField != null) {
            try {
                xField.setDouble(mouse, x);
                yField.setDouble(mouse, y);
            } catch (IllegalAccessException | RuntimeException e) {
                xField = null;
                yField = null;
            }
        }
    }

    private static void find(MouseHandler mouse) {
        try {
            List<Field> doubles = new ArrayList<>();
            for (Field f : MouseHandler.class.getDeclaredFields()) {
                if (f.getType() == double.class && !Modifier.isStatic(f.getModifiers())) {
                    doubles.add(f);
                }
            }
            if (doubles.size() < 2) {
                return;
            }
            Field fx = doubles.get(0);
            Field fy = doubles.get(1);
            fx.setAccessible(true);
            fy.setAccessible(true);
            if (fx.getDouble(mouse) == mouse.xpos() && fy.getDouble(mouse) == mouse.ypos()) {
                xField = fx;
                yField = fy;
            } else {
                LOGGER.warn("Tokyo Ghoul RPG: не нашёл положение курсора, курсор геймпада может не подсвечивать кнопки");
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Tokyo Ghoul RPG: не нашёл положение курсора для геймпада", e);
        }
    }
}
