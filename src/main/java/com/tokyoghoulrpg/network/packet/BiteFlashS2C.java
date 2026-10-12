package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.hud.BiteVignette;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Укус удался: у укусившего края экрана на полторы секунды краснеют. */
public class BiteFlashS2C {
    public static void encode(BiteFlashS2C msg, FriendlyByteBuf buf) {
    }

    public static BiteFlashS2C decode(FriendlyByteBuf buf) {
        return new BiteFlashS2C();
    }

    public static void handle(BiteFlashS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BiteVignette::start));
        context.setPacketHandled(true);
    }
}
