package com.tokyoghoulrpg.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.data.PlayerDataProvider;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(TokyoGhoulRPG.MOD_ID, "player_data"), new PlayerDataProvider());
        }
    }

    /** Переносит данные персонажа в новую копию игрока (смерть, возвращение из Энда). */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PlayerData.CAP).ifPresent(oldData ->
                event.getEntity().getCapability(PlayerData.CAP).ifPresent(newData -> newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncTo(player);
            player.getCapability(PlayerData.CAP).ifPresent(data -> {
                if (!data.hasChosenClass()) {
                    NetworkHandler.openClassSelection(player);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            NetworkHandler.sendKaguneTo(tracker, target);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncTo(player);
        }
    }

    /**
     * Команды для проверки (нужны права оператора):
     * /tgrpg reset            сбросить выбор класса
     * /tgrpg rage <0-100>     задать очки ярости
     * /tgrpg ragelevel <0-5>  задать уровень прокачки ярости
     * /tgrpg points <0-1000>  задать очки навыков
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tgrpg")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reset").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            AbilityManager.resetAll(player);
                            player.getCapability(PlayerData.CAP).ifPresent(PlayerData::reset);
                            NetworkHandler.syncTo(player);
                            NetworkHandler.openClassSelection(player);
                            context.getSource().sendSuccess(
                                    () -> Component.translatable("message.tokyoghoulrpg.reset"), false);
                            return 1;
                        }))
                        .then(Commands.literal("rage")
                                .then(Commands.argument("points", IntegerArgumentType.integer(0, PlayerData.RAGE_MAX))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int points = IntegerArgumentType.getInteger(context, "points");
                                            player.getCapability(PlayerData.CAP).ifPresent(data -> data.setRage(points));
                                            NetworkHandler.syncTo(player);
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("message.tokyoghoulrpg.rage_set", points), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("ragelevel")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, Balance.RAGE_LEVEL_MAX))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            player.getCapability(PlayerData.CAP).ifPresent(data -> data.setRageLevel(level));
                                            NetworkHandler.syncTo(player);
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("message.tokyoghoulrpg.ragelevel_set", level), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("points")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1000))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            player.getCapability(PlayerData.CAP).ifPresent(data -> data.setSkillPoints(amount));
                                            NetworkHandler.syncTo(player);
                                            context.getSource().sendSuccess(
                                                    () -> Component.translatable("message.tokyoghoulrpg.points_set", amount), false);
                                            return 1;
                                        }))));
    }
}
