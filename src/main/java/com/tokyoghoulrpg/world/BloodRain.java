package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * «Кровавый дождь» — редкое событие верхнего мира. Каждый новый игровой день с шансом 5% начинается
 * дождь с кровавым туманом на 5 минут:
 * <ul>
 *   <li>гули и полугули регенерируют на 50% быстрее;</li>
 *   <li>у остальных под открытым небом темнеет в глазах (обзор падает).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class BloodRain {
    private static final DustParticleOptions MIST = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.04F), 1.6F);
    private static long endTick = 0L;
    private static long lastDay = Long.MIN_VALUE;

    private BloodRain() {
    }

    public static boolean isActive(MinecraftServer server) {
        return server != null && endTick > 0L && server.overworld().getGameTime() < endTick;
    }

    /** Множитель регенерации гуля (на сервере). */
    public static double regenFactor(Player player, PlayerData data) {
        if (data == null || !data.getPlayerClass().hasKagune()) {
            return 1.0;
        }
        if (!(player instanceof ServerPlayer sp) || !isActive(sp.getServer()) || sp.level() != sp.getServer().overworld()) {
            return 1.0;
        }
        return 1.0 + Balance.BLOOD_RAIN_REGEN;
    }

    /** Начать прямо сейчас (команда или случай). */
    public static void start(MinecraftServer server) {
        ServerLevel level = server.overworld();
        endTick = level.getGameTime() + Balance.BLOOD_RAIN_TICKS;
        level.setWeatherParameters(0, Balance.BLOOD_RAIN_TICKS, true, false);
        Component msg = Component.translatable("message.tokyoghoulrpg.blood_rain.start", Balance.BLOOD_RAIN_TICKS / 1200,
                Math.round(Balance.BLOOD_RAIN_REGEN * 100)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
            p.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
        }
    }

    private static void stop(MinecraftServer server) {
        endTick = 0L;
        server.overworld().setWeatherParameters(6000, 0, false, false);
        Component msg = Component.translatable("message.tokyoghoulrpg.blood_rain.end").withStyle(ChatFormatting.GRAY);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        long time = level.getGameTime();
        if (time % 5 != 0) {
            return;
        }
        if (time % 20 == 0) {
            long day = Math.floorDiv(level.getDayTime(), 24000L);
            if (lastDay == Long.MIN_VALUE) {
                lastDay = day;
            } else if (day != lastDay) {
                lastDay = day;
                if (endTick == 0L && !server.getPlayerList().getPlayers().isEmpty()
                        && level.random.nextDouble() < Balance.BLOOD_RAIN_CHANCE) {
                    start(server);
                }
            }
            if (endTick > 0L && time >= endTick) {
                stop(server);
                return;
            }
        }
        if (endTick == 0L) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!level.canSeeSky(player.blockPosition().above())) {
                continue;
            }
            level.sendParticles(player, MIST, false, player.getX(), player.getY() + 3.0, player.getZ(), 18, 8.0, 3.0, 8.0, 0.0);
            if (time % 20 == 0 && !player.isCreative() && !player.isSpectator()) {
                PlayerData data = PlayerData.get(player);
                if (data != null && data.hasChosenClass() && !data.getPlayerClass().hasKagune()) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, true));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        endTick = 0L;
        lastDay = Long.MIN_VALUE;
    }
}
