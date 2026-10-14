package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент открыл «Профиль»: прислать свежие данные (откаты, клан). */
public class RequestSyncC2S {
    public RequestSyncC2S() {
    }

    public static void encode(RequestSyncC2S msg, FriendlyByteBuf buf) {
    }

    public static RequestSyncC2S decode(FriendlyByteBuf buf) {
        return new RequestSyncC2S();
    }

    public static void handle(RequestSyncC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NetworkHandler.syncTo(player);
            }
        });
        context.setPacketHandled(true);
    }
}
