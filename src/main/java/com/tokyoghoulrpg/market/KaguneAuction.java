package com.tokyoghoulrpg.market;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.KaguneType;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Аукцион кагуне: раз в 45 минут (пока кто-то в игре) чёрный рынок выставляет на торговую площадку
 * «Редкий кагуне» случайного типа — аукцион на 10 минут со стартом 1000 очков крови. Видят все.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class KaguneAuction {
    private static long nextMs = 0L;

    private KaguneAuction() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 400 != 17 || server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (nextMs == 0L) {
            nextMs = now + Balance.KAGUNE_AUCTION_EVERY_MS / 3;   // первый — через 15 минут после старта
            return;
        }
        if (now < nextMs) {
            return;
        }
        nextMs = now + Balance.KAGUNE_AUCTION_EVERY_MS;
        start(server);
    }

    /** Выставить редкий кагуне прямо сейчас. */
    public static void start(MinecraftServer server) {
        KaguneType type = KaguneType.CHOICES[server.overworld().random.nextInt(KaguneType.CHOICES.length)];
        ItemStack stack = new ItemStack(ModItems.RARE_KAGUNE.get());
        stack.setHoverName(Component.translatable("item.tokyoghoulrpg.rare_kagune.named",
                Component.translatable(type.translationKey())).withStyle(ChatFormatting.DARK_RED));
        Market.systemAuction(server, stack, Balance.KAGUNE_AUCTION_START, Balance.KAGUNE_AUCTION_MINUTES,
                Component.translatable("message.tokyoghoulrpg.kagune_auction.seller").getString());
        Component msg = Component.translatable("message.tokyoghoulrpg.kagune_auction.start",
                Component.translatable(type.translationKey()), Balance.KAGUNE_AUCTION_MINUTES, Balance.KAGUNE_AUCTION_START)
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
            p.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 1.0F, 0.5F);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        nextMs = 0L;
    }
}
