package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.AbilityType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент просит применить способность. Сервер сам проверяет класс, цель и перезарядку. */
public class AbilityC2S {
    private final AbilityType type;

    public AbilityC2S(AbilityType type) {
        this.type = type;
    }

    public static void encode(AbilityC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
    }

    public static AbilityC2S decode(FriendlyByteBuf buf) {
        return new AbilityC2S(buf.readEnum(AbilityType.class));
    }

    public static void handle(AbilityC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AbilityManager.useAbility(player, msg.type);
            }
        });
        context.setPacketHandled(true);
    }
}
