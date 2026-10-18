package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.rep.Achievements;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Игрок выбрал титул из полученных достижений (пустой — без титула). */
public class TitleC2S {
    private final String id;

    public TitleC2S(String id) {
        this.id = id == null ? "" : id;
    }

    public static void encode(TitleC2S msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.id, 32);
    }

    public static TitleC2S decode(FriendlyByteBuf buf) {
        return new TitleC2S(buf.readUtf(32));
    }

    public static void handle(TitleC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Achievements.chooseTitle(player, msg.id);
            }
        });
        context.setPacketHandled(true);
    }
}
