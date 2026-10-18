package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.clan.ClanManager;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Действие в меню клана: открыть, создать, пригласить, принять, отклонить, покинуть, исключить, передать. */
public class ClanActionC2S {
    private static final UUID NONE = new UUID(0L, 0L);

    private final int action;
    private final String text;
    private final UUID target;
    private final int amount;

    public ClanActionC2S(int action, String text, UUID target, int amount) {
        this.action = action;
        this.text = text == null ? "" : text;
        this.target = target;
        this.amount = amount;
    }

    public static void encode(ClanActionC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeUtf(msg.text, 80);
        buf.writeUUID(msg.target == null ? NONE : msg.target);
        buf.writeVarInt(msg.amount);
    }

    public static ClanActionC2S decode(FriendlyByteBuf buf) {
        int action = buf.readVarInt();
        String text = buf.readUtf(80);
        UUID target = buf.readUUID();
        int amount = buf.readVarInt();
        return new ClanActionC2S(action, text, NONE.equals(target) ? null : target, amount);
    }

    public static void handle(ClanActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ClanManager.action(player, msg.action, msg.text, msg.target, msg.amount);
            }
        });
        context.setPacketHandled(true);
    }
}
