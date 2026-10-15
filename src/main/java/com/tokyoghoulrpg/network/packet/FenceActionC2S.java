package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.shop.FenceShop;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Действие в лавке Скупщика: купить товар или продать одну / все штуки. */
public class FenceActionC2S {
    private final int entityId;
    private final int action;
    private final int index;

    public FenceActionC2S(int entityId, int action, int index) {
        this.entityId = entityId;
        this.action = action;
        this.index = index;
    }

    public static void encode(FenceActionC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.index);
    }

    public static FenceActionC2S decode(FriendlyByteBuf buf) {
        return new FenceActionC2S(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FenceActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                FenceShop.action(player, msg.entityId, msg.action, msg.index);
            }
        });
        context.setPacketHandled(true);
    }
}
