package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.tutorial.Tutorial;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Подсказки для новичков: показана подсказка step (перейти к следующей), начать заново или пропустить все. */
public class TutorialC2S {
    public static final int NEXT = 0;
    public static final int RESTART = 1;
    public static final int SKIP = 2;

    private final int action;
    private final int step;

    public TutorialC2S(int action, int step) {
        this.action = action;
        this.step = step;
    }

    public static void encode(TutorialC2S msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action);
        buf.writeVarInt(msg.step);
    }

    public static TutorialC2S decode(FriendlyByteBuf buf) {
        return new TutorialC2S(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(TutorialC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Tutorial.action(player, msg.action, msg.step);
            }
        });
        context.setPacketHandled(true);
    }
}
