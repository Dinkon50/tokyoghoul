package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.kagune.KaguneReach;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Захват кагуне: щупальце вытягивается от спины игрока до цели (существа или точки на блоке).
 * targetId = -1, если цель — блок. animation — какая анимация захвата (от неё берутся тайминги).
 */
public class KaguneReachS2C {
    private final int ownerId;
    private final int targetId;
    private final double x;
    private final double y;
    private final double z;
    private final String animation;

    public KaguneReachS2C(int ownerId, int targetId, double x, double y, double z, String animation) {
        this.ownerId = ownerId;
        this.targetId = targetId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.animation = animation;
    }

    public static void encode(KaguneReachS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ownerId);
        buf.writeInt(msg.targetId);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeUtf(msg.animation, 64);
    }

    public static KaguneReachS2C decode(FriendlyByteBuf buf) {
        return new KaguneReachS2C(buf.readVarInt(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(64));
    }

    public static void handle(KaguneReachS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                KaguneReach.start(msg.ownerId, msg.targetId, msg.x, msg.y, msg.z, msg.animation)));
        context.setPacketHandled(true);
    }
}
