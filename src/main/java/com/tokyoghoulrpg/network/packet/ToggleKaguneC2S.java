package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.CombatState;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.LockManager;
import java.util.Locale;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

/** Клавиша G: включить или убрать кагуне. */
public class ToggleKaguneC2S {
    public static void encode(ToggleKaguneC2S msg, FriendlyByteBuf buf) {
    }

    public static ToggleKaguneC2S decode(FriendlyByteBuf buf) {
        return new ToggleKaguneC2S();
    }

    public static void handle(ToggleKaguneC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            player.getCapability(PlayerData.CAP).ifPresent(data -> {
                if (!data.getPlayerClass().hasKagune()) {
                    player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.no_kagune"), true);
                    return;
                }
                // Во время баррага и оглушения кагуне не убрать
                if (LockManager.isLocked(player) || data.combat().isBarraging()) {
                    return;
                }
                // Небольшой откат, чтобы G нельзя было спамить
                CombatState state = data.combat();
                long now = player.level().getGameTime();
                if (now < state.kaguneToggleReady) {
                    String seconds = String.format(Locale.ROOT, "%.1f", (state.kaguneToggleReady - now) / 20.0);
                    player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown",
                            Component.translatable("ability.tokyoghoulrpg.kagune"), seconds), true);
                    return;
                }
                state.kaguneToggleReady = now + Balance.KAGUNE_TOGGLE_COOLDOWN;
                data.setKaguneActive(!data.isKaguneActive());
                NetworkHandler.syncTo(player);
                if (data.isKaguneActive()) {
                    // кагуне прорывается из спины: влажный звук и брызги крови
                    Effects.sound(player, SoundEvents.SLIME_BLOCK_PLACE, 1.0F, 0.6F);
                    Effects.blood(player, 12);
                } else {
                    Effects.sound(player, SoundEvents.SLIME_BLOCK_BREAK, 0.8F, 0.8F);
                }
                player.displayClientMessage(Component.translatable(
                        data.isKaguneActive() ? "message.tokyoghoulrpg.kagune_on" : "message.tokyoghoulrpg.kagune_off"), true);
            });
        });
        context.setPacketHandled(true);
    }
}
