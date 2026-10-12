package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.contract.ContractManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Действие у информатора: взять, сдать или отказаться от контракта. */
public class ContractActionC2S {
    private final int entityId;
    private final int action;

    public ContractActionC2S(int entityId, int action) {
        this.entityId = entityId;
        this.action = action;
    }

    public static void encode(ContractActionC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.action);
    }

    public static ContractActionC2S decode(FriendlyByteBuf buf) {
        return new ContractActionC2S(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ContractActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ContractManager.action(player, msg.entityId, msg.action);
            }
        });
        context.setPacketHandled(true);
    }
}
