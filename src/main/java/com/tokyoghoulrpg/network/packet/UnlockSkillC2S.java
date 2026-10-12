package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.skill.SkillManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Игрок нажал на навык в дереве. Сервер сам проверяет очки и порядок открытия. */
public class UnlockSkillC2S {
    private final String skillId;

    public UnlockSkillC2S(String skillId) {
        this.skillId = skillId;
    }

    public static void encode(UnlockSkillC2S msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.skillId, 64);
    }

    public static UnlockSkillC2S decode(FriendlyByteBuf buf) {
        return new UnlockSkillC2S(buf.readUtf(64));
    }

    public static void handle(UnlockSkillC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                SkillManager.unlock(player, msg.skillId);
            }
        });
        context.setPacketHandled(true);
    }
}
