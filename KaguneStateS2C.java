package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import com.tokyoghoulrpg.data.KaguneVariant;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер сообщает всем, кто видит игрока, включено ли у него кагуне и действует ли ярость. */
public class KaguneStateS2C {
    private final int entityId;
    private final boolean active;
    private final KaguneVariant variant;
    private final boolean rage;

    public KaguneStateS2C(int entityId, boolean active, KaguneVariant variant, boolean rage) {
        this.entityId = entityId;
        this.active = active;
        this.variant = variant;
        this.rage = rage;
    }

    public static void encode(KaguneStateS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.active);
        buf.writeEnum(msg.variant);
        buf.writeBoolean(msg.rage);
    }

    public static KaguneStateS2C decode(FriendlyByteBuf buf) {
        return new KaguneStateS2C(buf.readVarInt(), buf.readBoolean(), buf.readEnum(KaguneVariant.class), buf.readBoolean());
    }

    public static void handle(KaguneStateS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                KaguneClientState.applyState(msg.entityId, msg.active, msg.variant, msg.rage)));
        context.setPacketHandled(true);
    }
}
