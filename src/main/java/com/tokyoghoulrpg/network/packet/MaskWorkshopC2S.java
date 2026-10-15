package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.world.MaskWorkshop;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Заказ в мастерской масок: какая маска (0-3) и работа мастера или обычная. */
public class MaskWorkshopC2S {
    private final BlockPos pos;
    private final int mask;
    private final boolean master;

    public MaskWorkshopC2S(BlockPos pos, int mask, boolean master) {
        this.pos = pos;
        this.mask = mask;
        this.master = master;
    }

    public static void encode(MaskWorkshopC2S msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.mask);
        buf.writeBoolean(msg.master);
    }

    public static MaskWorkshopC2S decode(FriendlyByteBuf buf) {
        return new MaskWorkshopC2S(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(MaskWorkshopC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MaskWorkshop.order(player, msg.pos, msg.mask, msg.master);
            }
        });
        context.setPacketHandled(true);
    }
}
