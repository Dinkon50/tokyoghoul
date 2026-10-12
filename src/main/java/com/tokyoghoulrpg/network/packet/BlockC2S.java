package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.AbilityManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клавиша L: true, пока кнопка удерживается, false, когда её отпустили. */
public class BlockC2S {
    private final boolean held;

    public BlockC2S(boolean held) {
        this.held = held;
    }

    public static void encode(BlockC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.held);
    }

    public static BlockC2S decode(FriendlyByteBuf buf) {
        return new BlockC2S(buf.readBoolean());
    }

    public static void handle(BlockC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AbilityManager.setBlock(player, msg.held);
            }
        });
        context.setPacketHandled(true);
    }
}
