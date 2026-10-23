package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import com.tokyoghoulrpg.data.KaguneVariant;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер сообщает всем, кто видит игрока, включено ли у него кагуне, действует ли ярость и держит ли он блок. */
public class KaguneStateS2C {
    private final int entityId;
    private final boolean active;
    private final KaguneVariant variant;
    private final boolean rage;
    private final boolean blocking;
    /** Какой скин мода носит игрок (его видят все): 0 — свой, 1 — гуль, 2 — CCG. */
    private final int modSkin;

    public KaguneStateS2C(int entityId, boolean active, KaguneVariant variant, boolean rage, boolean blocking,
                          int modSkin) {
        this.entityId = entityId;
        this.active = active;
        this.variant = variant;
        this.rage = rage;
        this.blocking = blocking;
        this.modSkin = modSkin;
    }

    public static void encode(KaguneStateS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.active);
        buf.writeEnum(msg.variant);
        buf.writeBoolean(msg.rage);
        buf.writeBoolean(msg.blocking);
        buf.writeByte(msg.modSkin);
    }

    public static KaguneStateS2C decode(FriendlyByteBuf buf) {
        return new KaguneStateS2C(buf.readVarInt(), buf.readBoolean(), buf.readEnum(KaguneVariant.class), buf.readBoolean(),
                buf.readBoolean(), buf.readByte());
    }

    public static void handle(KaguneStateS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            KaguneClientState.applyState(msg.entityId, msg.active, msg.variant, msg.rage, msg.blocking);
            com.tokyoghoulrpg.client.ModSkins.setWearing(msg.entityId, msg.modSkin);
        }));
        context.setPacketHandled(true);
    }
}
