package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер просит клиент открыть меню выбора класса. */
public class OpenClassSelectionS2C {
    public static void encode(OpenClassSelectionS2C msg, FriendlyByteBuf buf) {
    }

    public static OpenClassSelectionS2C decode(FriendlyByteBuf buf) {
        return new OpenClassSelectionS2C();
    }

    public static void handle(OpenClassSelectionS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::requestClassSelection));
        context.setPacketHandled(true);
    }
}
