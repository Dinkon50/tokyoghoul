package com.tokyoghoulrpg.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Ульта гуля началась (targetId — кого схватили) или прервалась (targetId = -1). Видят все, кто рядом. */
public class KaguneUltimateS2C {
    private final int ownerId;
    private final int targetId;

    public KaguneUltimateS2C(int ownerId, int targetId) {
        this.ownerId = ownerId;
        this.targetId = targetId;
    }

    public static void encode(KaguneUltimateS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ownerId);
        buf.writeVarInt(msg.targetId + 1);
    }

    public static KaguneUltimateS2C decode(FriendlyByteBuf buf) {
        return new KaguneUltimateS2C(buf.readVarInt(), buf.readVarInt() - 1);
    }

    public static void handle(KaguneUltimateS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tokyoghoulrpg.client.kagune.KaguneUltimateFx.start(msg.ownerId, msg.targetId)));
        context.setPacketHandled(true);
    }
}
