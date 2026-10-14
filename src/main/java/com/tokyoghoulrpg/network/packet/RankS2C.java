package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Ранг игрока для показа над головой: код = ладья * 10 + ранг (0 — нет ранга). */
public class RankS2C {
    private final int entityId;
    private final int code;

    public RankS2C(int entityId, int code) {
        this.entityId = entityId;
        this.code = code;
    }

    public static void encode(RankS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.code);
    }

    public static RankS2C decode(FriendlyByteBuf buf) {
        return new RankS2C(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(RankS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.rank(msg.entityId, msg.code)));
        context.setPacketHandled(true);
    }
}
