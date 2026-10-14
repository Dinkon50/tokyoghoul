package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Открыть меню хозяина кофейни. */
public class OpenCafeS2C {
    private final int entityId;

    public OpenCafeS2C(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(OpenCafeS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static OpenCafeS2C decode(FriendlyByteBuf buf) {
        return new OpenCafeS2C(buf.readVarInt());
    }

    public static void handle(OpenCafeS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCafe(msg.entityId)));
        context.setPacketHandled(true);
    }
}
