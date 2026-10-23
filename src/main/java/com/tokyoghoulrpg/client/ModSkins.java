package com.tokyoghoulrpg.client;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.logging.LogUtils;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Скины классов из мода: гуль и полугуль — скин гуля, агент CCG — скин CCG.
 * При выборе класса игрок решает: оставить свой скин или взять скин класса.
 * <p>
 * Скин игрока Minecraft берёт из его PlayerInfo (там лежат скачанные текстуры и вид модели — обычные
 * или тонкие руки). Мы подменяем там текстуру скина на нашу и вид модели на обычный, пока игрок носит скин мода,
 * и возвращаем как было, когда перестал. Так скин мода видно везде, где Minecraft рисует скин: модель
 * игрока от третьего лица, руки от первого лица (и в VR), голова в списке игроков.
 * Скин свой у каждого клиента: сервер сообщает всем, кто носит скин мода (в состоянии кагуне).
 * Скачанный скин приходит не сразу и может записаться поверх нашего: поэтому подмену проверяем каждый тик
 * и перед отрисовкой игрока.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class ModSkins {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Скин мода: текстура и модель («default» — руки 4 пикселя, «slim» — 3 пикселя). */
    public record Skin(ResourceLocation texture, String model) {
    }

    /** 1 — гуль (обычные руки), 2 — CCG (тонкие руки). Номер совпадает с PlayerData.modSkinId(). */
    public static final Skin GHOUL = new Skin(new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/skin/ghoul.png"), "default");
    public static final Skin CCG = new Skin(new ResourceLocation(TokyoGhoulRPG.MOD_ID, "textures/skin/ccg.png"), "slim");

    public static Skin byId(int id) {
        return switch (id) {
            case 1 -> GHOUL;
            case 2 -> CCG;
            default -> null;
        };
    }

    private static boolean isModTexture(ResourceLocation texture) {
        return GHOUL.texture().equals(texture) || CCG.texture().equals(texture);
    }

    /** Какой скин мода носит игрок: номер сущности → скин. */
    private static final Map<Integer, Skin> WEARING = new HashMap<>();
    /** Что было у игрока до подмены: вернём, когда снимет скин мода. */
    private static final Map<UUID, ResourceLocation> ORIGINAL_SKIN = new HashMap<>();
    private static final Map<UUID, String> ORIGINAL_MODEL = new HashMap<>();

    private static boolean searched;
    private static Field texturesField;
    private static Field modelField;

    private ModSkins() {
    }

    /** Сервер сообщил, какой скин мода носит игрок (0 — свой). */
    public static void setWearing(int entityId, int skinId) {
        Skin skin = byId(skinId);
        if (skin != null) {
            WEARING.put(entityId, skin);
        } else {
            WEARING.remove(entityId);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            apply(player);
        }
    }

    /** Носит ли игрок сейчас скин мода (для превью и меню). */
    public static boolean isWearing(Player player) {
        return WEARING.containsKey(player.getId());
    }

    /** Собственный скин игрока (даже если сейчас на нём скин мода). */
    public static ResourceLocation ownSkin(AbstractClientPlayer player) {
        ResourceLocation original = ORIGINAL_SKIN.get(player.getUUID());
        if (original != null) {
            return original;
        }
        ResourceLocation current = player.getSkinTextureLocation();
        return isModTexture(current) ? net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(player.getUUID())
                : current;
    }

    private static boolean findFields() {
        if (!searched) {
            searched = true;
            try {
                for (Field f : PlayerInfo.class.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    if (f.getType() == Map.class && texturesField == null) {
                        f.setAccessible(true);
                        texturesField = f;
                    } else if (f.getType() == String.class && modelField == null) {
                        f.setAccessible(true);
                        modelField = f;
                    }
                }
                if (texturesField == null) {
                    LOGGER.warn("Tokyo Ghoul RPG: не нашёл скины игроков, скин гуля из мода не будет виден");
                }
            } catch (RuntimeException e) {
                texturesField = null;
                modelField = null;
                LOGGER.warn("Tokyo Ghoul RPG: не нашёл скины игроков, скин гуля из мода не будет виден", e);
            }
        }
        return texturesField != null;
    }

    @SuppressWarnings("unchecked")
    private static void apply(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || !findFields()) {
            return;
        }
        PlayerInfo info = connection.getPlayerInfo(player.getUUID());
        if (info == null) {
            return;
        }
        UUID id = player.getUUID();
        Skin wear = WEARING.get(player.getId());
        try {
            // скачивание скина запускается при первом обращении: пусть начнётся раньше нашей подмены
            info.getSkinLocation();
            Map<MinecraftProfileTexture.Type, ResourceLocation> textures =
                    (Map<MinecraftProfileTexture.Type, ResourceLocation>) texturesField.get(info);
            ResourceLocation current = textures.get(MinecraftProfileTexture.Type.SKIN);
            if (wear != null) {
                if (!wear.texture().equals(current)) {
                    // первая подмена или пришёл скачанный скин поверх нашего: запоминаем его и ставим наш
                    // (при смене одного скина мода на другой родной скин уже запомнен)
                    if (!isModTexture(current)) {
                        ORIGINAL_SKIN.put(id, current);
                        ORIGINAL_MODEL.put(id, modelField == null ? null : (String) modelField.get(info));
                    }
                    textures.put(MinecraftProfileTexture.Type.SKIN, wear.texture());
                }
                if (modelField != null && !wear.model().equals(modelField.get(info))) {
                    modelField.set(info, wear.model());
                }
            } else if (ORIGINAL_SKIN.containsKey(id) || ORIGINAL_MODEL.containsKey(id)) {
                if (isModTexture(current)) {
                    ResourceLocation original = ORIGINAL_SKIN.get(id);
                    if (original == null) {
                        textures.remove(MinecraftProfileTexture.Type.SKIN);
                    } else {
                        textures.put(MinecraftProfileTexture.Type.SKIN, original);
                    }
                    if (modelField != null) {
                        modelField.set(info, ORIGINAL_MODEL.get(id));
                    }
                }
                ORIGINAL_SKIN.remove(id);
                ORIGINAL_MODEL.remove(id);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Tokyo Ghoul RPG: не удалось сменить скин игрока", e);
            texturesField = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null) {
            return;
        }
        if (WEARING.isEmpty() && ORIGINAL_SKIN.isEmpty() && ORIGINAL_MODEL.isEmpty()) {
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            apply(player);
        }
    }

    /** Перед отрисовкой игрока: модель (обычная или тонкие руки) выбирается по скину прямо в этот момент. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player
                && (WEARING.containsKey(player.getId()) || ORIGINAL_SKIN.containsKey(player.getUUID()))) {
            apply(player);
        }
    }

    /** Руки от первого лица. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (WEARING.containsKey(player.getId()) || ORIGINAL_SKIN.containsKey(player.getUUID())) {
            apply(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        WEARING.clear();
        ORIGINAL_SKIN.clear();
        ORIGINAL_MODEL.clear();
    }
}
