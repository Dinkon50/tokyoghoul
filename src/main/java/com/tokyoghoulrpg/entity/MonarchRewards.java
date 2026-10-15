package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * Награда за Алого Монарха: только очки крови — 25 000 каждому, кто нанёс ему урон (и был в игре).
 * Весь сервер узнаёт, кто победил финального босса.
 */
public final class MonarchRewards {
    private MonarchRewards() {
    }

    public static void give(CrimsonMonarch boss) {
        MinecraftServer server = boss.getServer();
        if (server == null) {
            return;
        }
        List<ServerPlayer> winners = new ArrayList<>();
        for (Map.Entry<UUID, Float> entry : boss.damageRanking()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && entry.getValue() > 0.0F) {
                winners.add(player);
            }
        }
        StringBuilder names = new StringBuilder();
        for (ServerPlayer player : winners) {
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                continue;
            }
            data.addBloodPoints(Balance.MONARCH_BLOOD);
            NetworkHandler.syncTo(player);
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.crimson_monarch.reward", Balance.MONARCH_BLOOD));
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.crimson_monarch.all_bosses"));
            Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(player.getGameProfile().getName());
        }
        if (names.length() > 0) {
            Component broadcast = Component.translatable("message.tokyoghoulrpg.crimson_monarch.defeated", names.toString());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(broadcast);
            }
        }
    }
}
