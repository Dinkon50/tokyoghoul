package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.AbilityManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клавиша K: true, когда её зажали (начать барраж), false, когда отпустили (закончить). */
public class BarrageC2S {
    private final boolean held;

    public BarrageC2S(boolean held) {
        this.held = held;
    }

    public static void encode(BarrageC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.held);
    }

    public static BarrageC2S decode(FriendlyByteBuf buf) {
        return new BarrageC2S(buf.readBoolean());
    }

    public static void handle(BarrageC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AbilityManager.setBarrage(player, msg.held);
            }
        });
        context.setPacketHandled(true);
    }
}
