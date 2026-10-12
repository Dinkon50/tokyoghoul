package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Открыть лавку торговца (номер торговца в мире). */
public class OpenBloodShopS2C {
    private final int entityId;

    public OpenBloodShopS2C(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(OpenBloodShopS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static OpenBloodShopS2C decode(FriendlyByteBuf buf) {
        return new OpenBloodShopS2C(buf.readVarInt());
    }

    public static void handle(OpenBloodShopS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientHooks.openBloodShop(msg.entityId)));
        context.setPacketHandled(true);
    }
}
