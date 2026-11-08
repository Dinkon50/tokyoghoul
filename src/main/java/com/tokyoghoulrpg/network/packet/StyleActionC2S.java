package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.style.StyleManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Действие в меню стилей: выбрать, убрать, прокачать ветку, бросить вызов боссу (см. StyleManager). */
public class StyleActionC2S {
    private final int action;
    private final int style;
    private final int branch;
    private final int mentorId;

    public StyleActionC2S(int action, int style, int branch, int mentorId) {
        this.action = action;
        this.style = style;
        this.branch = branch;
        this.mentorId = mentorId;
    }

    public static void encode(StyleActionC2S msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action);
        buf.writeByte(msg.style);
        buf.writeByte(msg.branch);
        buf.writeVarInt(msg.mentorId);
    }

    public static StyleActionC2S decode(FriendlyByteBuf buf) {
        return new StyleActionC2S(buf.readByte(), buf.readByte(), buf.readByte(), buf.readVarInt());
    }

    public static void handle(StyleActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                StyleManager.handle(player, msg.action, msg.style, msg.branch, msg.mentorId);
            }
        });
        context.setPacketHandled(true);
    }
}
