package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.client.screen.BloodShopScreen;
import com.tokyoghoulrpg.client.screen.ClanScreen;
import com.tokyoghoulrpg.network.packet.ClanStateS2C;
import com.tokyoghoulrpg.client.screen.ClassSelectionScreen;
import com.tokyoghoulrpg.client.screen.ContractScreen;
import com.tokyoghoulrpg.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Клиентские действия, которые вызываются из пакетов. Класс загружается только на клиенте. */
public final class ClientHooks {
    private static boolean pendingClassSelection = false;
    private static boolean pendingVariantSelection = false;
    /** Выбор типа кагуне предлагается один раз за вход в мир (потом — из «Профиля»). */
    private static boolean typeAsked = false;
    private static int typeDelay = 0;

    private ClientHooks() {
    }

    public static void applySync(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || tag == null) {
            return;
        }
        mc.player.getCapability(PlayerData.CAP).ifPresent(data -> data.deserializeNBT(tag));
    }

    /** Меню откроется, как только закончится загрузка мира и не будет других экранов. */
    public static void requestClassSelection() {
        pendingClassSelection = true;
    }

    /** Лавка торговца открывается сразу: игрок уже в мире и сам нажал на торговца. */
    public static void openBloodShop(int traderId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.setScreen(new BloodShopScreen(traderId));
        }
    }

    /** Ранг игрока: запомнить и обновить ник над головой. */
    public static void rank(int entityId, int code) {
        com.tokyoghoulrpg.rank.Ranks.acceptClient(entityId, code);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(entityId) instanceof net.minecraft.world.entity.player.Player player) {
            player.refreshDisplayName();
        }
    }

    /** Лавка Скупщика (ПКМ по нему). */
    public static void openFence(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.setScreen(new com.tokyoghoulrpg.client.screen.FenceScreen(entityId));
        }
    }

    /** Меню мастерской масок (ПКМ по блоку). */
    public static void openMaskWorkshop(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.setScreen(new com.tokyoghoulrpg.client.screen.MaskWorkshopScreen(pos));
        }
    }

    /** Меню хозяина кофейни. */
    public static void openCafe(int ownerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.setScreen(new com.tokyoghoulrpg.client.screen.CafeScreen(ownerId));
        }
    }

    /** Пришло состояние клана: обновить меню клана. */
    public static void clanState(ClanStateS2C state) {
        ClanScreen.accept(state);
    }

    /** Окно информатора. */
    public static void openContracts(int informantId, String offer, long day) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.setScreen(new ContractScreen(informantId, offer, day));
        }
    }

    /** Выбор варианта кагуне (после инъекции гуля человеку). */
    public static void requestVariantSelection() {
        pendingVariantSelection = true;
    }

    public static void tickPendingScreens(Minecraft mc) {
        if (pendingClassSelection && mc.screen == null && mc.level != null) {
            pendingClassSelection = false;
            mc.setScreen(new ClassSelectionScreen());
        }
        if (pendingVariantSelection && mc.screen == null && mc.level != null && mc.player != null) {
            pendingVariantSelection = false;
            PlayerData data = PlayerData.get(mc.player);
            if (data != null && data.getPlayerClass().hasKagune()) {
                mc.setScreen(new ClassSelectionScreen(data.getPlayerClass()));
            }
        }
        // Гуль или полугуль без типа кагуне (новый персонаж или из старого сохранения): предложить выбрать
        PlayerData own = mc.player == null ? null : PlayerData.get(mc.player);
        boolean needType = own != null && own.getPlayerClass().hasKagune()
                && own.getKaguneVariant() != com.tokyoghoulrpg.data.KaguneVariant.NONE
                && own.getKaguneType() == com.tokyoghoulrpg.data.KaguneType.NONE;
        if (!typeAsked && needType && mc.screen == null && mc.level != null) {
            if (++typeDelay >= 40) {
                typeAsked = true;
                mc.setScreen(new com.tokyoghoulrpg.client.screen.KaguneTypeScreen());
            }
        } else if (!needType) {
            typeDelay = 0;
        }
        TutorialClient.tick(mc);
    }

    public static void reset() {
        pendingClassSelection = false;
        pendingVariantSelection = false;
        typeAsked = false;
        typeDelay = 0;
        TutorialClient.reset();
    }
}
