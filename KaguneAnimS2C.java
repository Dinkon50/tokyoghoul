package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер просит всех, кто видит игрока, проиграть анимацию кагуне. Пустое имя прерывает текущую. */
public class KaguneAnimS2C {
    private final int entityId;
    private final String animation;

    public KaguneAnimS2C(int entityId, String animation) {
        this.entityId = entityId;
        this.animation = animation;
    }

    public static void encode(KaguneAnimS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.animation);
    }

    public static KaguneAnimS2C decode(FriendlyByteBuf buf) {
        return new KaguneAnimS2C(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(KaguneAnimS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                KaguneClientState.playAnimation(msg.entityId, msg.animation)));
        context.setPacketHandled(true);
    }
}
