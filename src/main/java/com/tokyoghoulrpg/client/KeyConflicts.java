package com.tokyoghoulrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loader.FMLPaths;
import org.lwjgl.glfw.GLFW;

/**
 * Клавиши в сборке с другими модами. У мода много своих клавиш (G, V, H, L, K, J, B, N, Y, R, Z, I, U, P, O, M, C),
 * и другие моды часто занимают те же (карта на J или M, рюкзак на B и т. п.). При первом заходе в главное меню
 * клавиша другого мода, которая совпала с нашей, переносится на свободную (цифровой блок, F6-F10, Insert/Home...).
 * Каждую клавишу трогаем один раз: если игрок потом сам поставит её обратно — больше не меняем
 * (список уже перенесённых — config/tokyoghoulrpg-keys.txt). Клавиши самого Minecraft не трогаем, кроме одной:
 * «Достижения» (L) совпадает с блоком мода и тоже переносится (достижения всегда открываются и из меню Esc).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class KeyConflicts {
    private static final Pattern VANILLA = Pattern.compile("^key\\.([a-zA-Z]+|hotbar\\.\\d)$");
    /** Клавиши Minecraft, которые всё же можно переносить (у них есть кнопка в меню Esc). */
    private static final Set<String> MOVABLE_VANILLA = Set.of("key.advancements");
    private static final int[] FREE = {
        GLFW.GLFW_KEY_KP_1, GLFW.GLFW_KEY_KP_2, GLFW.GLFW_KEY_KP_3, GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_5,
        GLFW.GLFW_KEY_KP_6, GLFW.GLFW_KEY_KP_7, GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_9, GLFW.GLFW_KEY_KP_0,
        GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_F9, GLFW.GLFW_KEY_F10,
        GLFW.GLFW_KEY_INSERT, GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_END, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN,
        GLFW.GLFW_KEY_KP_ADD, GLFW.GLFW_KEY_KP_SUBTRACT, GLFW.GLFW_KEY_KP_MULTIPLY, GLFW.GLFW_KEY_KP_DIVIDE,
        GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET,
        GLFW.GLFW_KEY_BACKSLASH, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_EQUAL,
    };
    private static boolean done = false;

    private KeyConflicts() {
    }

    @SubscribeEvent
    public static void onTitle(ScreenEvent.Init.Post event) {
        if (done || !(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        done = true;
        try {
            resolve();
        } catch (RuntimeException e) {
            TokyoGhoulRPG.LOGGER.warn("Не удалось разобрать совпадения клавиш", e);
        }
    }

    private static boolean isOurs(KeyMapping key) {
        return KeyBindings.CATEGORY.equals(key.getCategory());
    }

    private static void resolve() {
        Minecraft mc = Minecraft.getInstance();
        Path file = FMLPaths.CONFIGDIR.get().resolve("tokyoghoulrpg-keys.txt");
        Set<String> handled = new LinkedHashSet<>();
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String name = line.split(" ", 2)[0].trim();
                    if (!name.isEmpty() && !name.startsWith("#")) {
                        handled.add(name);
                    }
                }
            }
        } catch (IOException ignored) {
            // нет файла — ничего страшного
        }
        KeyMapping[] all = mc.options.keyMappings;
        List<String> changed = new ArrayList<>();
        for (KeyMapping ours : all) {
            if (!isOurs(ours) || ours.isUnbound()) {
                continue;
            }
            for (KeyMapping other : all) {
                if (other == ours || isOurs(other)
                        || VANILLA.matcher(other.getName()).matches() && !MOVABLE_VANILLA.contains(other.getName())
                        || handled.contains(other.getName()) || other.isUnbound()
                        || !other.getKey().equals(ours.getKey()) || other.getKeyModifier() != ours.getKeyModifier()) {
                    continue;
                }
                if (!ours.getKeyConflictContext().conflicts(other.getKeyConflictContext())
                        && !other.getKeyConflictContext().conflicts(ours.getKeyConflictContext())) {
                    continue;
                }
                InputConstants.Key free = freeKey(all);
                other.setKeyModifierAndCode(KeyModifier.NONE, free);
                handled.add(other.getName());
                changed.add(other.getName() + " " + ours.getKey().getName() + " -> " + free.getName());
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        KeyMapping.resetMapping();
        mc.options.save();
        List<String> lines = new ArrayList<>();
        lines.add("# Клавиши других модов, которые Tokyo Ghoul RPG перенёс с совпавших клавиш (каждую — один раз)");
        for (String name : handled) {
            String change = changed.stream().filter(c -> c.startsWith(name + " ")).findFirst().orElse(name);
            lines.add(change);
        }
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            TokyoGhoulRPG.LOGGER.warn("Не удалось записать {}", file, e);
        }
        for (String c : changed) {
            TokyoGhoulRPG.LOGGER.info("Клавиша другого мода перенесена: {}", c);
        }
        SystemToast.add(mc.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                Component.translatable("toast.tokyoghoulrpg.keys.title"),
                Component.translatable("toast.tokyoghoulrpg.keys.text", changed.size()));
    }

    private static InputConstants.Key freeKey(KeyMapping[] all) {
        for (int code : FREE) {
            InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(code);
            boolean used = false;
            for (KeyMapping m : all) {
                if (m.getKey().equals(key)) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                return key;
            }
        }
        return InputConstants.UNKNOWN;
    }
}
