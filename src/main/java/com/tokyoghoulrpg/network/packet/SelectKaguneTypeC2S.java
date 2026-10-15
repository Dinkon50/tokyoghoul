package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.KaguneTypeBonus;
import com.tokyoghoulrpg.data.KaguneType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Игрок выбрал (или сменил) тип кагуне в меню. Проверки и цена — на сервере. */
public class SelectKaguneTypeC2S {
    private final KaguneType type;

    public SelectKaguneTypeC2S(KaguneType type) {
        this.type = type;
    }

    public static void encode(SelectKaguneTypeC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
    }

    public static SelectKaguneTypeC2S decode(FriendlyByteBuf buf) {
        return new SelectKaguneTypeC2S(buf.readEnum(KaguneType.class));
    }

    public static void handle(SelectKaguneTypeC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                KaguneTypeBonus.choose(player, msg.type);
            }
        });
        context.setPacketHandled(true);
    }
}
