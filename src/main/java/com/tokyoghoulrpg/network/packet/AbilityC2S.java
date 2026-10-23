package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.combat.Aim;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Клиент просит применить способность. Сервер сам проверяет класс, цель и перезарядку.
 * offhand: в VR способность сделана левой рукой (целится она).
 */
public class AbilityC2S {
    private final AbilityType type;
    private final boolean offhand;

    public AbilityC2S(AbilityType type) {
        this(type, false);
    }

    public AbilityC2S(AbilityType type, boolean offhand) {
        this.type = type;
        this.offhand = offhand;
    }

    public static void encode(AbilityC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeBoolean(msg.offhand);
    }

    public static AbilityC2S decode(FriendlyByteBuf buf) {
        AbilityType type = buf.readEnum(AbilityType.class);
        return new AbilityC2S(type, buf.readBoolean());
    }

    public static void handle(AbilityC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Aim.runWithHand(player, msg.offhand, () -> AbilityManager.useAbility(player, msg.type));
            }
        });
        context.setPacketHandled(true);
    }
}
