package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Гуль, полугуль или агент CCG выбрал скин: свой (false) или скин класса из мода (true). Видят все игроки. */
public class SkinChoiceC2S {
    private final boolean modSkin;

    public SkinChoiceC2S(boolean modSkin) {
        this.modSkin = modSkin;
    }

    public static void encode(SkinChoiceC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.modSkin);
    }

    public static SkinChoiceC2S decode(FriendlyByteBuf buf) {
        return new SkinChoiceC2S(buf.readBoolean());
    }

    public static void handle(SkinChoiceC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            PlayerData data = player == null ? null : PlayerData.get(player);
            if (data == null || !PlayerData.hasModSkin(data.getPlayerClass()) || data.isModSkin() == msg.modSkin) {
                return;
            }
            data.setModSkin(msg.modSkin);
            // syncTo рассылает и состояние кагуне (в нём выбор скина) всем, кто видит игрока
            NetworkHandler.syncTo(player);
        });
        context.setPacketHandled(true);
    }
}
