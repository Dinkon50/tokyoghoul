package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.market.Market;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Торговая площадка: открыть, выставить вещь из руки, купить или снять лот. */
public class MarketActionC2S {
    private final int action;
    private final int value;
    private final int price;
    private final boolean clanOnly;

    public MarketActionC2S(int action, int value, int price, boolean clanOnly) {
        this.action = action;
        this.value = value;
        this.price = price;
        this.clanOnly = clanOnly;
    }

    public static void encode(MarketActionC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.value);
        buf.writeVarInt(msg.price);
        buf.writeBoolean(msg.clanOnly);
    }

    public static MarketActionC2S decode(FriendlyByteBuf buf) {
        return new MarketActionC2S(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(MarketActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Market.action(player, msg.action, msg.value, msg.price, msg.clanOnly);
            }
        });
        context.setPacketHandled(true);
    }
}
