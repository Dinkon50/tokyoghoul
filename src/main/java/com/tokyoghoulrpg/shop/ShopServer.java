package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.TraderNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Серверная часть лавки: проверка, что торговец рядом, списание очков крови и выдача предмета. */
public final class ShopServer {
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private ShopServer() {
    }

    /** Торговец с этим номером, если он жив и стоит рядом с игроком. */
    public static TraderNpc nearbyTrader(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof TraderNpc trader && trader.isAlive() && trader.distanceToSqr(player) <= MAX_DISTANCE_SQR) {
            return trader;
        }
        return null;
    }

    public static void buy(ServerPlayer player, int entityId, int index) {
        BloodShop.Entry entry = BloodShop.get(index);
        PlayerData data = PlayerData.get(player);
        if (entry == null || data == null || nearbyTrader(player, entityId) == null) {
            return;
        }
        int price = BloodShop.priceFor(entry, data);
        if (data.getBloodPoints() < price) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.blood_not_enough"), true);
            return;
        }
        data.addBloodPoints(-price);
        ItemStack stack = entry.stack();
        Component name = stack.getHoverName();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        NetworkHandler.syncTo(player);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.blood_bought", name, price), true);
    }

    public static void openEmeraldTrade(ServerPlayer player, int entityId) {
        TraderNpc trader = nearbyTrader(player, entityId);
        if (trader != null) {
            trader.openEmeraldTrade(player);
        }
    }
}
