package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.FenceNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Лавка Скупщика. Продаёт аптечки и дымовые шашки за очки крови и покупает добычу.
 * Список и цены одинаковы на сервере и в меню (меню берёт их отсюда).
 */
public final class FenceShop {
    public static final int BUY = 0;
    public static final int SELL_ONE = 1;
    public static final int SELL_ALL = 2;

    /** Что продаёт Скупщик. */
    public record Offer(Supplier<Item> item, int price) {
    }

    public static final List<Offer> SELLS = List.of(
            new Offer(() -> ModItems.MEDKIT.get(), Balance.FENCE_MEDKIT_PRICE),
            new Offer(() -> ModItems.SMOKE_BOMB.get(), Balance.FENCE_SMOKE_PRICE));

    /** Что он покупает и сколько крови даёт за штуку. */
    public static final List<Offer> BUYS = List.of(
            new Offer(() -> ModItems.RC_STEEL_INGOT.get(), 150),
            new Offer(() -> ModItems.RAW_RC_STEEL.get(), 90),
            new Offer(() -> Items.DIAMOND, 60),
            new Offer(() -> Items.EMERALD, 20),
            new Offer(() -> Items.ENDER_PEARL, 10),
            new Offer(() -> Items.GOLD_INGOT, 8),
            new Offer(() -> ModItems.FLESH.get(), 4),
            new Offer(() -> Items.IRON_INGOT, 3));

    private FenceShop() {
    }

    public static void action(ServerPlayer player, int entityId, int action, int index) {
        PlayerData data = PlayerData.get(player);
        Entity entity = player.level().getEntity(entityId);
        if (data == null || !(entity instanceof FenceNpc) || !entity.isAlive() || player.distanceToSqr(entity) > 64.0) {
            return;
        }
        if (action == BUY) {
            if (index < 0 || index >= SELLS.size()) {
                return;
            }
            Offer offer = SELLS.get(index);
            if (data.getBloodPoints() < offer.price()) {
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.fence.no_blood", offer.price())
                        .withStyle(ChatFormatting.RED));
                return;
            }
            data.addBloodPoints(-offer.price());
            ItemStack stack = new ItemStack(offer.item().get());
            Component name = stack.getHoverName();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            NetworkHandler.syncTo(player);
            Effects.sound(player, SoundEvents.VILLAGER_YES, 0.7F, 0.8F);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.fence.bought", name, offer.price()), true);
            return;
        }
        if ((action != SELL_ONE && action != SELL_ALL) || index < 0 || index >= BUYS.size()) {
            return;
        }
        Offer offer = BUYS.get(index);
        Item item = offer.item().get();
        int have = player.getInventory().countItem(item);
        if (have <= 0) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.fence.none"), true);
            return;
        }
        int count = action == SELL_ALL ? have : 1;
        int removed = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && removed < count; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            // квинке и ядра сюда не попадают; предмет с особыми данными (переименованный и т. п.) тоже можно продать
            if (stack.is(item)) {
                int take = Math.min(count - removed, stack.getCount());
                stack.shrink(take);
                removed += take;
            }
        }
        int total = removed * offer.price();
        data.addBloodPoints(total);
        NetworkHandler.syncTo(player);
        Effects.sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 0.8F);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.fence.sold", removed,
                new ItemStack(item).getHoverName(), total), true);
    }
}
