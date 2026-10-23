package com.tokyoghoulrpg.client.gamepad;

import com.mojang.blaze3d.platform.Window;
import com.tokyoghoulrpg.ClientConfig;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.client.vr.VrClient;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

/**
 * Игра на геймпаде: Xbox, PlayStation (DualShock 4, DualSense) и другие, которые GLFW знает как «gamepad».
 * Minecraft Java сам геймпад не поддерживает, поэтому мод читает его через GLFW (он уже есть в игре)
 * и нажимает обычные привязки Minecraft и мода: всё, что работает с клавиатуры, работает и с геймпада.
 * <p>
 * Раскладка (Xbox / PlayStation):
 * <pre>
 *   Левый стик — ходьба, нажать (L3) — бег          Правый стик — камера, нажать (R3) — какудзя
 *   A / ✕ — прыжок (дважды — двойной прыжок)        B / ○ — присесть (держать)
 *   X / □ — рывок                                   Y / △ — кагуне вкл/выкл
 *   RT / R2 — атака (ЛКМ)                           LT / L2 — использовать (ПКМ), дважды — сильная атака
 *   RB / R1 — блок (держать)                        LB / L1 — держать: второй слой кнопок
 *   Крестовина: ↑ ярость, ↓ укус, ← → предмет в руке
 *   View / Share — инвентарь                        Menu / Options — пауза
 * Второй слой (держать LB / L1):
 *   RT / LT — притянуть цель / притянуться самому (как Ctrl + ЛКМ / ПКМ)
 *   RB — барраж (держать), A — «Харизма», B — «Бросок судьбы», X — «Аура возмездия», Y — выбросить предмет
 *   Крестовина: ↑ навыки, ↓ задания, ← клан, → профиль
 *   View / Share — торговая площадка, Menu / Options — книга новичка, L3 — вид (F5), R3 — ульта гуля
 * В меню: левый стик — курсор, A / ✕ — нажать, X / □ — правая кнопка мыши, B / ○ — назад,
 *   правый стик или LB / RB — прокрутка (в книге новичка — листать страницы).
 * </pre>
 * Не работает в VR (там свои контроллеры Vivecraft) и если стоит другой мод для геймпадов (Controllable, Controlify).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class Gamepad {
    // Кнопки GLFW 0..14, дальше два курка как кнопки
    private static final int A = GLFW.GLFW_GAMEPAD_BUTTON_A;
    private static final int B = GLFW.GLFW_GAMEPAD_BUTTON_B;
    private static final int X = GLFW.GLFW_GAMEPAD_BUTTON_X;
    private static final int Y = GLFW.GLFW_GAMEPAD_BUTTON_Y;
    private static final int LB = GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
    private static final int RB = GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
    private static final int BACK = GLFW.GLFW_GAMEPAD_BUTTON_BACK;
    private static final int START = GLFW.GLFW_GAMEPAD_BUTTON_START;
    private static final int L3 = GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
    private static final int R3 = GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
    private static final int UP = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP;
    private static final int RIGHT = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
    private static final int DOWN = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
    private static final int LEFT = GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
    private static final int LT = 15;
    private static final int RT = 16;
    private static final int COUNT = 17;

    /** Курок считается нажатым, если выжат больше чем на 40%. */
    private static final float TRIGGER_PRESSED = 0.4F;
    /** Поворот камеры при полностью отклонённом стике, градусов в секунду. */
    private static final double LOOK_DEGREES_PER_SECOND = 300.0;
    /** Курсор в меню при полностью отклонённом стике: доля высоты окна в секунду. */
    private static final double CURSOR_SCREENS_PER_SECOND = 0.9;

    private static GLFWGamepadState state;
    private static int joystick = -1;
    private static int scanCooldown;
    private static String connectedName = "";
    private static boolean playStation;
    private static boolean announcePending;
    private static Boolean otherModPresent;

    private static final boolean[] down = new boolean[COUNT];
    private static final boolean[] was = new boolean[COUNT];
    /** Какую привязку нажала каждая кнопка: отпускаем именно её, даже если слой уже сменился. */
    private static final KeyMapping[] pressedMapping = new KeyMapping[COUNT];
    private static float lx;
    private static float ly;
    private static float rx;
    private static float ry;
    private static long lastFrameNanos;
    private static double scrollAccumulator;
    /** Геймпад сейчас управляет игрой (не в меню). */
    private static boolean inGame;

    private Gamepad() {
    }

    // ------------------------------------------------------------------ для других частей мода

    /** Зажат второй слой (LB / L1): с RT / LT это захваты кагуне, как с клавишей захвата. */
    public static boolean grabHeld() {
        return inGame && down[LB];
    }

    /** Подключён ли геймпад сейчас (для подсказок). */
    public static boolean connected() {
        return joystick >= 0;
    }

    public static boolean isPlayStation() {
        return playStation;
    }

    // ------------------------------------------------------------------ опрос

    private static boolean enabled() {
        if (otherModPresent == null) {
            otherModPresent = ModList.get().isLoaded("controllable") || ModList.get().isLoaded("controlify");
        }
        if (otherModPresent) {
            return false;
        }
        try {
            return ClientConfig.GAMEPAD_ENABLED.get();
        } catch (IllegalStateException e) {
            // настройки ещё не загружены
            return false;
        }
    }

    private static double config(net.minecraftforge.common.ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    /** Ищет геймпад и читает его состояние. false — геймпада нет. */
    private static boolean poll() {
        if (state == null) {
            state = GLFWGamepadState.calloc();
        }
        if (joystick < 0 || !GLFW.glfwJoystickIsGamepad(joystick)) {
            if (joystick >= 0) {
                joystick = -1;
                connectedName = "";
            }
            if (scanCooldown-- > 0) {
                return false;
            }
            scanCooldown = 60;
            for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
                if (GLFW.glfwJoystickIsGamepad(jid)) {
                    joystick = jid;
                    String name = GLFW.glfwGetGamepadName(jid);
                    connectedName = name == null ? "" : name;
                    String n = connectedName.toLowerCase(Locale.ROOT);
                    playStation = n.contains("ps3") || n.contains("ps4") || n.contains("ps5") || n.contains("dualshock")
                            || n.contains("dualsense") || n.contains("playstation") || n.contains("sony");
                    announcePending = true;
                    break;
                }
            }
            if (joystick < 0) {
                return false;
            }
        }
        if (!GLFW.glfwGetGamepadState(joystick, state)) {
            return false;
        }
        for (int i = 0; i <= GLFW.GLFW_GAMEPAD_BUTTON_LAST; i++) {
            down[i] = state.buttons(i) == GLFW.GLFW_PRESS;
        }
        // курки: от -1 (отпущен) до 1 (выжат до конца)
        down[LT] = (state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER) + 1.0F) / 2.0F > TRIGGER_PRESSED;
        down[RT] = (state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) + 1.0F) / 2.0F > TRIGGER_PRESSED;
        lx = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
        ly = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
        rx = state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X);
        ry = state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
        return true;
    }

    /** Стик с мёртвой зоной и плавной кривой: {x, y}, каждая от -1 до 1. */
    private static double[] stick(float x, float y) {
        double deadzone = config(ClientConfig.GAMEPAD_DEADZONE, 0.15);
        double mag = Math.sqrt(x * x + y * y);
        if (mag <= deadzone || mag < 1.0E-6) {
            return new double[] {0.0, 0.0};
        }
        double scaled = Math.min(1.0, (mag - deadzone) / (1.0 - deadzone));
        double curved = scaled * scaled;
        return new double[] {x / mag * curved, y / mag * curved};
    }

    private static boolean pressed(int button) {
        return down[button] && !was[button];
    }

    private static boolean released(int button) {
        return !down[button] && was[button];
    }

    // ------------------------------------------------------------------ каждый кадр

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (now - lastFrameNanos) / 1.0E9F);
        lastFrameNanos = now;

        if (!enabled() || VrClient.active() || !poll()) {
            releaseAll();
            return;
        }
        if (announcePending && mc.player != null) {
            announcePending = false;
            mc.player.displayClientMessage(Component.translatable(
                    playStation ? "message.tokyoghoulrpg.gamepad.connected_ps" : "message.tokyoghoulrpg.gamepad.connected",
                    connectedName).withStyle(ChatFormatting.GRAY), false);
        }

        if (mc.screen != null) {
            if (inGame) {
                releaseAll();
            }
            handleScreen(mc, mc.screen, dt);
        } else if (mc.player != null && mc.getOverlay() == null) {
            inGame = true;
            handleGame(mc, mc.player, dt);
        } else {
            releaseAll();
        }
        System.arraycopy(down, 0, was, 0, COUNT);
    }

    /** Отпускает всё, что нажал геймпад (геймпад отключили, открылось меню и т. п.). */
    private static void releaseAll() {
        for (int i = 0; i < COUNT; i++) {
            if (pressedMapping[i] != null) {
                pressedMapping[i].setDown(false);
                pressedMapping[i] = null;
            }
        }
        inGame = false;
        // нажатия, начатые до этого, не должны сработать при возврате
        System.arraycopy(down, 0, was, 0, COUNT);
    }

    // ------------------------------------------------------------------ в игре

    private static void handleGame(Minecraft mc, LocalPlayer player, float dt) {
        Options o = mc.options;
        boolean layer2 = down[LB];

        // Камера
        double[] look = stick(rx, ry);
        if (look[0] != 0.0 || look[1] != 0.0) {
            double speed = LOOK_DEGREES_PER_SECOND * config(ClientConfig.GAMEPAD_LOOK_SPEED, 1.0) * dt;
            boolean invert;
            try {
                invert = ClientConfig.GAMEPAD_INVERT_Y.get();
            } catch (IllegalStateException e) {
                invert = false;
            }
            double yaw = look[0] * speed;
            double pitch = look[1] * speed * 0.75 * (invert ? -1.0 : 1.0);
            // turn() умножает на 0,15 (как для мыши)
            player.turn(yaw / 0.15, pitch / 0.15);
        }

        // Кнопки, которые держат привязку (нажал — нажата, отпустил — отпущена)
        bind(A, layer2 ? KeyBindings.CHARISMA : o.keyJump);
        bind(B, layer2 ? KeyBindings.FORTUNE : o.keyShift);
        bind(X, layer2 ? KeyBindings.RETRIBUTION : KeyBindings.DASH);
        bind(Y, layer2 ? o.keyDrop : KeyBindings.TOGGLE_KAGUNE);
        bind(RB, layer2 ? KeyBindings.BARRAGE : KeyBindings.BLOCK);
        bind(RT, o.keyAttack);
        bind(LT, o.keyUse);
        bind(L3, layer2 ? o.keyTogglePerspective : o.keySprint);
        if (layer2) {
            // LB + R3 — ульта гуля
            if (pressed(R3)) {
                releaseButton(R3);
                com.tokyoghoulrpg.network.NetworkHandler.CHANNEL.sendToServer(
                        new com.tokyoghoulrpg.network.packet.AbilityC2S(com.tokyoghoulrpg.combat.AbilityType.ULTIMATE));
            } else if (released(R3)) {
                releaseButton(R3);
            }
        } else {
            bind(R3, KeyBindings.KAKUJA);
        }
        bind(UP, layer2 ? KeyBindings.SKILL_TREE : KeyBindings.RAGE);
        bind(DOWN, layer2 ? KeyBindings.QUESTS : KeyBindings.BITE);
        if (layer2) {
            bind(LEFT, KeyBindings.CLAN);
            bind(RIGHT, KeyBindings.PROFILE);
            bind(BACK, KeyBindings.MARKET);
        } else {
            releaseButton(LEFT);
            releaseButton(RIGHT);
            bind(BACK, o.keyInventory);
            if (pressed(LEFT)) {
                player.getInventory().swapPaint(1.0);
            }
            if (pressed(RIGHT)) {
                player.getInventory().swapPaint(-1.0);
            }
        }
        if (pressed(START)) {
            if (layer2) {
                com.tokyoghoulrpg.client.ClientHooks.openGuide();
            } else {
                mc.pauseGame(false);
            }
        }
    }

    /** Кнопка держит привязку: в момент нажатия нажимает её (и засчитывает клик), при отпускании отпускает. */
    private static void bind(int button, KeyMapping mapping) {
        if (pressed(button)) {
            releaseButton(button);
            pressedMapping[button] = mapping;
            mapping.setDown(true);
            if (!mapping.isUnbound()) {
                // как настоящее нажатие клавиши: засчитывается клик (consumeClick)
                KeyMapping.click(mapping.getKey());
            }
        } else if (released(button)) {
            releaseButton(button);
        }
    }

    private static void releaseButton(int button) {
        if (pressedMapping[button] != null) {
            pressedMapping[button].setDown(false);
            pressedMapping[button] = null;
        }
    }

    /** Ходьба левым стиком. Раньше обычного обработчика: оглушение потом всё равно обнулит движение. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!inGame || joystick < 0) {
            return;
        }
        Input input = event.getInput();
        // клавиатура важнее: если нажаты клавиши ходьбы, стик не трогаем
        if (Math.abs(input.forwardImpulse) > 1.0E-4F || Math.abs(input.leftImpulse) > 1.0E-4F) {
            return;
        }
        double[] move = stick(lx, ly);
        if (move[0] == 0.0 && move[1] == 0.0) {
            return;
        }
        float forward = (float) -move[1];
        float strafe = (float) -move[0];
        input.up = forward > 0.3F;
        input.down = forward < -0.3F;
        input.left = strafe > 0.3F;
        input.right = strafe < -0.3F;
        if (event.getEntity() instanceof LocalPlayer player && player.isMovingSlowly()) {
            forward *= 0.3F;
            strafe *= 0.3F;
        }
        input.forwardImpulse = forward;
        input.leftImpulse = strafe;
    }

    // ------------------------------------------------------------------ в меню

    private static void handleScreen(Minecraft mc, Screen screen, float dt) {
        Window window = mc.getWindow();
        double sw = window.getScreenWidth();
        double sh = window.getScreenHeight();
        if (sw <= 0 || sh <= 0) {
            return;
        }
        double scaleX = window.getGuiScaledWidth() / sw;
        double scaleY = window.getGuiScaledHeight() / sh;
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();

        // Курсор
        double[] move = stick(lx, ly);
        if (move[0] != 0.0 || move[1] != 0.0) {
            double speed = CURSOR_SCREENS_PER_SECOND * config(ClientConfig.GAMEPAD_CURSOR_SPEED, 1.0) * sh * dt;
            double nx = Math.max(0.0, Math.min(sw - 1.0, x + move[0] * speed));
            double ny = Math.max(0.0, Math.min(sh - 1.0, y + move[1] * speed));
            if (nx != x || ny != y) {
                MouseAccess.moveTo(mc, nx, ny);
                screen.mouseMoved(nx * scaleX, ny * scaleY);
                if (down[A]) {
                    screen.mouseDragged(nx * scaleX, ny * scaleY, 0, (nx - x) * scaleX, (ny - y) * scaleY);
                }
                x = nx;
                y = ny;
            }
        }
        double gx = x * scaleX;
        double gy = y * scaleY;

        // Нажатия
        if (pressed(A)) {
            screen.mouseClicked(gx, gy, 0);
        } else if (released(A) && mc.screen == screen) {
            screen.mouseReleased(gx, gy, 0);
        }
        if (pressed(X)) {
            screen.mouseClicked(gx, gy, 1);
        } else if (released(X) && mc.screen == screen) {
            screen.mouseReleased(gx, gy, 1);
        }
        if (pressed(B) || pressed(START)) {
            screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
            return;
        }

        // Прокрутка: правым стиком плавно, бамперами по одному шагу
        double[] scroll = stick(rx, ry);
        scrollAccumulator += -scroll[1] * 8.0 * dt;
        if (scroll[1] == 0.0) {
            scrollAccumulator = 0.0;
        }
        while (scrollAccumulator >= 1.0) {
            scrollAccumulator -= 1.0;
            screen.mouseScrolled(gx, gy, 1.0);
        }
        while (scrollAccumulator <= -1.0) {
            scrollAccumulator += 1.0;
            screen.mouseScrolled(gx, gy, -1.0);
        }
        if (pressed(LB)) {
            screen.mouseScrolled(gx, gy, 1.0);
        }
        if (pressed(RB)) {
            screen.mouseScrolled(gx, gy, -1.0);
        }
    }
}
