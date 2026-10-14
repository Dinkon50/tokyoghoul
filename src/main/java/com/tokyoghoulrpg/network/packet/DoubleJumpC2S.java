package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

/** Клиент сделал двойной прыжок: обнуляем высоту падения, звук и облачко под ногами. Раз за прыжок. */
public class DoubleJumpC2S {
    public static void encode(DoubleJumpC2S msg, FriendlyByteBuf buf) {
    }

    public static DoubleJumpC2S decode(FriendlyByteBuf buf) {
        return new DoubleJumpC2S();
    }

    public static void handle(DoubleJumpC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.onGround()) {
                return;
            }
            PlayerData data = PlayerData.get(player);
            if (data == null || !data.hasChosenClass() || data.combat().doubleJumpUsed
                    || com.tokyoghoulrpg.combat.Silence.isSilenced(player)) {
                return;
            }
            data.combat().doubleJumpUsed = true;
            player.fallDistance = 0.0F;
            Effects.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.3F, 1.7F);
            Effects.particles(player, ParticleTypes.CLOUD, 6, 0.25, 0.02);
        });
        context.setPacketHandled(true);
    }
}
