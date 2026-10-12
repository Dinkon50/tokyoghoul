package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Открыть окно информатора: номер информатора, контракт на сегодня и номер дня. */
public class OpenContractsS2C {
    private final int entityId;
    private final String offer;
    private final long day;

    public OpenContractsS2C(int entityId, String offer, long day) {
        this.entityId = entityId;
        this.offer = offer;
        this.day = day;
    }

    public static void encode(OpenContractsS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.offer, 64);
        buf.writeLong(msg.day);
    }

    public static OpenContractsS2C decode(FriendlyByteBuf buf) {
        return new OpenContractsS2C(buf.readVarInt(), buf.readUtf(64), buf.readLong());
    }

    public static void handle(OpenContractsS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHooks.openContracts(msg.entityId, msg.offer, msg.day)));
        context.setPacketHandled(true);
    }
}
