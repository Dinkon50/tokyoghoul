package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CafeOwnerNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModItems;
import com.tokyoghoulrpg.rep.Standing;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Меню хозяина кофейни: кофе и мясо за очки крови и задания.
 * Гуль и полугуль раз в игровые сутки берут бесплатно одну чашку кофе и один кусок мяса
 * (каждое отдельно); дальше в тот же день — за обычную цену. Люди и агенты платят всегда.
 */
public final class CafeShop {
    public static final int COFFEE = 0;
    public static final int FLESH = 1;
    public static final int CONTRACTS = 2;
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private CafeShop() {
    }

    /** Игровые сутки (по времени суток: начинаются утром, вместе с открытием кофейни). */
    public static long day(Level level) {
        return level.getDayTime() / 24000L;
    }

    /** Может ли игрок сегодня взять это бесплатно. Работает и у клиента (для подписи в меню). */
    public static boolean isFree(PlayerData data, long day, int item) {
        if (data == null || !data.getPlayerClass().hasKagune()) {
            return false;
        }
        return item == COFFEE ? data.getCafeCoffeeDay() != day : data.getCafeFleshDay() != day;
    }

    public static int price(int item) {
        return item == COFFEE ? Balance.CAFE_COFFEE_PRICE : Balance.CAFE_FLESH_PRICE;
    }

    public static void action(ServerPlayer player, int ownerId, int action) {
        Entity entity = player.level().getEntity(ownerId);
        PlayerData data = PlayerData.get(player);
        if (!(entity instanceof CafeOwnerNpc) || !entity.isAlive() || entity.distanceToSqr(player) > MAX_DISTANCE_SQR
                || data == null) {
            return;
        }
        if (action == CONTRACTS) {
            NetworkHandler.openContracts(player, ownerId);
            return;
        }
        if (action != COFFEE && action != FLESH) {
            return;
        }
        long day = day(player.level());
        Item item = action == COFFEE ? ModItems.COFFEE.get() : ModItems.FLESH.get();
        ItemStack stack = new ItemStack(item);
        if (isFree(data, day, action)) {
            if (action == COFFEE) {
                data.setCafeCoffeeDay(day);
            } else {
                data.setCafeFleshDay(day);
            }
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cafe.free", stack.getHoverName()), true);
        } else {
            int price = price(action);
            if (data.getBloodPoints() < price) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.blood_not_enough"), true);
                return;
            }
            data.addBloodPoints(-price);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.blood_bought",
                    stack.getHoverName(), price), true);
            Standing.onTrade(player);
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        Effects.sound(player, SoundEvents.BOTTLE_FILL, 0.6F, 1.2F);
        NetworkHandler.syncTo(player);
    }
}
