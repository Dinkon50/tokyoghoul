package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.client.screen.BloodShopScreen;
import com.tokyoghoulrpg.client.screen.ClassSelectionScreen;
import com.tokyoghoulrpg.client.screen.ContractScreen;
import com.tokyoghoulrpg.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Клиентские действия, которые вызываются из пакетов. Класс загружается только на клиенте. */
public final class ClientHooks {
    private static boolean pendingClassSelection = false;
    private static boolean pendingVariantSelection = false;

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
    }

    public static void reset() {
        pendingClassSelection = false;
        pendingVariantSelection = false;
    }
}
