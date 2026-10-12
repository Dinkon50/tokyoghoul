package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.client.screen.ClassSelectionScreen;
import com.tokyoghoulrpg.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Клиентские действия, которые вызываются из пакетов. Класс загружается только на клиенте. */
public final class ClientHooks {
    private static boolean pendingClassSelection = false;

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

    public static void tickPendingScreens(Minecraft mc) {
        if (pendingClassSelection && mc.screen == null && mc.level != null) {
            pendingClassSelection = false;
            mc.setScreen(new ClassSelectionScreen());
        }
    }

    public static void reset() {
        pendingClassSelection = false;
    }
}
