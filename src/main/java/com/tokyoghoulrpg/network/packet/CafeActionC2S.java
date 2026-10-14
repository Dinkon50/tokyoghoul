package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.shop.CafeShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Действие в меню кофейни: купить кофе, мясо или открыть задания. */
public class CafeActionC2S {
    private final int entityId;
    private final int action;

    public CafeActionC2S(int entityId, int action) {
        this.entityId = entityId;
        this.action = action;
    }

    public static void encode(CafeActionC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.action);
    }

    public static CafeActionC2S decode(FriendlyByteBuf buf) {
        return new CafeActionC2S(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(CafeActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CafeShop.action(player, msg.entityId, msg.action);
            }
        });
        context.setPacketHandled(true);
    }
}
