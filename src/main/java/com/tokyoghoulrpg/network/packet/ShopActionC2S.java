package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.shop.ShopServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Действие в лавке: купить товар номер index или (index = -1) перейти к торговле за изумруды. */
public class ShopActionC2S {
    public static final int EMERALD_TRADE = -1;
    /** Продать торговцу все ядра гулей из инвентаря. */
    public static final int SELL_CORES = -2;

    private final int entityId;
    private final int index;

    public ShopActionC2S(int entityId, int index) {
        this.entityId = entityId;
        this.index = index;
    }

    public static void encode(ShopActionC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeInt(msg.index);
    }

    public static ShopActionC2S decode(FriendlyByteBuf buf) {
        return new ShopActionC2S(buf.readVarInt(), buf.readInt());
    }

    public static void handle(ShopActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (msg.index == EMERALD_TRADE) {
                ShopServer.openEmeraldTrade(player, msg.entityId);
            } else if (msg.index == SELL_CORES) {
                ShopServer.sellCores(player, msg.entityId);
            } else {
                ShopServer.buy(player, msg.entityId, msg.index);
            }
        });
        context.setPacketHandled(true);
    }
}
