package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер просит клиент открыть выбор варианта кагуне. */
public class OpenVariantSelectionS2C {
    public static void encode(OpenVariantSelectionS2C msg, FriendlyByteBuf buf) {
    }

    public static OpenVariantSelectionS2C decode(FriendlyByteBuf buf) {
        return new OpenVariantSelectionS2C();
    }

    public static void handle(OpenVariantSelectionS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::requestVariantSelection));
        context.setPacketHandled(true);
    }
}
