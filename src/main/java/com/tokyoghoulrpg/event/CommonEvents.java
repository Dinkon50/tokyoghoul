package com.tokyoghoulrpg.event;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.clan.ClanBond;
import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.data.PlayerDataProvider;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.rep.Standing;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
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
                event.getEntity().getCapability(PlayerData.CAP).ifPresent(newData -> {
                    newData.copyFrom(oldData);
                    // После смерти ярость сбрасывается: шкала пустая, действующая ярость закончилась.
                    // При возвращении из Энда (это не смерть) шкала сохраняется.
                    if (event.isWasDeath()) {
                        newData.setRage(0);
                        newData.setRageActive(false, 0L, 0L);
                        // после возрождения жажда крови утолена
                        newData.setCraving(100);
                    }
                }));
        event.getOriginal().invalidateCaps();
    }

    /** Сделка с жителем, странствующим торговцем или торговцем мода (за изумруды): репутация растёт. */
    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Standing.onTrade(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerData.CAP).ifPresent(data -> {
                // Персонажи, созданные до появления очков крови, тоже получают стартовые 100
                if (data.hasChosenClass()) {
                    SelectClassC2S.giveStartBlood(data);
                }
            });
            NetworkHandler.syncTo(player);
            ClanManager.onLogin(player);
            player.getCapability(PlayerData.CAP).ifPresent(data -> {
                if (!data.hasChosenClass()) {
                    NetworkHandler.openClassSelection(player);
                } else if (data.getPlayerClass().hasKagune() && data.getKaguneVariant() == KaguneVariant.NONE) {
                    NetworkHandler.openVariantSelection(player);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && !event.isEndConquered()) {
                AbilityManager.applyClassStats(player, data, true);
            }
            ClanBond.refreshDisplay(player);
            // после возрождения у игрока новый номер сущности: ранг разослать заново
            com.tokyoghoulrpg.rank.Ranks.forget(player);
            NetworkHandler.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            NetworkHandler.sendKaguneTo(tracker, target);
            NetworkHandler.sendRankTo(tracker, target);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.syncTo(player);
        }
    }
}
