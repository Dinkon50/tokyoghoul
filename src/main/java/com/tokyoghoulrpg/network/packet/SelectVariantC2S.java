package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Игрок выбрал вариант кагуне после того, как стал полугулем. Сработает, только если варианта ещё нет. */
public class SelectVariantC2S {
    private final KaguneVariant variant;

    public SelectVariantC2S(KaguneVariant variant) {
        this.variant = variant;
    }

    public static void encode(SelectVariantC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.variant);
    }

    public static SelectVariantC2S decode(FriendlyByteBuf buf) {
        return new SelectVariantC2S(buf.readEnum(KaguneVariant.class));
    }

    public static void handle(SelectVariantC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            PlayerData data = player == null ? null : PlayerData.get(player);
            if (data == null || !data.getPlayerClass().hasKagune() || data.getKaguneVariant() != KaguneVariant.NONE) {
                return;
            }
            if (data.getPlayerClass().variants().contains(msg.variant)) {
                data.setKaguneVariant(msg.variant);
                NetworkHandler.syncTo(player);
            }
        });
        context.setPacketHandled(true);
    }
}
