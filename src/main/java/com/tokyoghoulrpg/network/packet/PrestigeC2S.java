package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.rank.Ranks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Игрок подтвердил престиж в «Профиле». */
public class PrestigeC2S {
    public PrestigeC2S() {
    }

    public static void encode(PrestigeC2S msg, FriendlyByteBuf buf) {
    }

    public static PrestigeC2S decode(FriendlyByteBuf buf) {
        return new PrestigeC2S();
    }

    public static void handle(PrestigeC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Ranks.prestigeUp(player);
            }
        });
        context.setPacketHandled(true);
    }
}
