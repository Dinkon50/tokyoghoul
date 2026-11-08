package com.tokyoghoulrpg.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Открыть меню стилей боя у наставника (номер наставника: выбор, смена и вызов — только у него). */
public class OpenStylesS2C {
    private final int mentorId;

    public OpenStylesS2C(int mentorId) {
        this.mentorId = mentorId;
    }

    public static void encode(OpenStylesS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.mentorId);
    }

    public static OpenStylesS2C decode(FriendlyByteBuf buf) {
        return new OpenStylesS2C(buf.readVarInt());
    }

    public static void handle(OpenStylesS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tokyoghoulrpg.client.ClientHooks.openStyles(msg.mentorId)));
        context.setPacketHandled(true);
    }
}
