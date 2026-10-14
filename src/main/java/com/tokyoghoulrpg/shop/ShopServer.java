package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.RcCoreItem;
import com.tokyoghoulrpg.entity.TraderNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.rep.Standing;
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
        // покупка у торговца — тоже сделка: личная репутация и репутация клана
        Standing.onTrade(player);
    }

    /** Продать все ядра гулей (любые, чьи бы ни были). Купить ядро нельзя нигде. */
    public static void sellCores(ServerPlayer player, int entityId) {
        PlayerData data = PlayerData.get(player);
        if (data == null || nearbyTrader(player, entityId) == null) {
            return;
        }
        int total = 0;
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof RcCoreItem core) {
                total += Balance.CORE_PRICE[core.tier() - 1] * stack.getCount();
                count += stack.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        if (count == 0) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.core.nothing_to_sell"), true);
            return;
        }
        data.addBloodPoints(total);
        NetworkHandler.syncTo(player);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.core.sold", count, total), true);
        Standing.onTrade(player);
    }

    public static void openEmeraldTrade(ServerPlayer player, int entityId) {
        TraderNpc trader = nearbyTrader(player, entityId);
        if (trader != null) {
            trader.openEmeraldTrade(player);
        }
    }
}
