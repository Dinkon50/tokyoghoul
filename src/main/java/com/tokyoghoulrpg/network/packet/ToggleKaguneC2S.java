package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.LockManager;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клавиша G: включить или убрать кагуне. */
public class ToggleKaguneC2S {
    public static void encode(ToggleKaguneC2S msg, FriendlyByteBuf buf) {
    }

    public static ToggleKaguneC2S decode(FriendlyByteBuf buf) {
        return new ToggleKaguneC2S();
    }

    public static void handle(ToggleKaguneC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            player.getCapability(PlayerData.CAP).ifPresent(data -> {
                if (!data.getPlayerClass().hasKagune()) {
                    player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.no_kagune"), true);
                    return;
                }
                // Во время баррага и оглушения кагуне не убрать
                if (LockManager.isLocked(player) || data.combat().isBarraging()) {
                    return;
                }
                data.setKaguneActive(!data.isKaguneActive());
                NetworkHandler.syncTo(player);
                player.displayClientMessage(Component.translatable(
                        data.isKaguneActive() ? "message.tokyoghoulrpg.kagune_on" : "message.tokyoghoulrpg.kagune_off"), true);
            });
        });
        context.setPacketHandled(true);
    }
}
