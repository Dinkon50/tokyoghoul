package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Гуль, полугуль или агент CCG выбрал скин: 0 — свой, 1 — скин класса из мода, 2 — второй скин гуля. Видят все игроки. */
public class SkinChoiceC2S {
    private final int choice;

    public SkinChoiceC2S(int choice) {
        this.choice = choice;
    }

    public static void encode(SkinChoiceC2S msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.choice);
    }

    public static SkinChoiceC2S decode(FriendlyByteBuf buf) {
        return new SkinChoiceC2S(buf.readByte());
    }

    public static void handle(SkinChoiceC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            PlayerData data = player == null ? null : PlayerData.get(player);
            if (data == null || !PlayerData.hasModSkin(data.getPlayerClass()) || msg.choice < 0
                    || msg.choice >= PlayerData.skinCount(data.getPlayerClass()) || data.skinChoice() == msg.choice) {
                return;
            }
            data.setSkinChoice(msg.choice);
            // syncTo рассылает и состояние кагуне (в нём выбор скина) всем, кто видит игрока
            NetworkHandler.syncTo(player);
        });
        context.setPacketHandled(true);
    }
}
