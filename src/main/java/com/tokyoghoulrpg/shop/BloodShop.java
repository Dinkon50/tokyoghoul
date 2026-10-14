package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.rep.Reputation;
import com.tokyoghoulrpg.rep.Standing;
import com.tokyoghoulrpg.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Лавка торговца за очки крови: инъекции и квинке.
 * Один и тот же список используют и клиент (показать), и сервер (проверить покупку).
 */
public final class BloodShop {
    /** Товар: что даёт и сколько стоит в очках крови. */
    public record Entry(Supplier<Item> item, int price, boolean fixedPrice) {
        public Entry(Supplier<Item> item, int price) {
            this(item, price, false);
        }

        public ItemStack stack() {
            return new ItemStack(item.get());
        }
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry(ModItems.HUMAN_INJECTION, 1000),
            new Entry(ModItems.GHOUL_INJECTION, 1000),
            new Entry(ModItems.CCG_INJECTION, 1000),
            // квинке: способности открываются только у агента CCG, другим это просто оружие.
            // Narukami и IXA не продаются: их отдаёт Старший следователь в финальном задании агента.
            new Entry(ModItems.QUINQUE_KURA, 300),
            new Entry(ModItems.QUINQUE_JASON, 1200),
            // печать сброса навыков: всегда ровно 5000, репутация на цену не влияет
            new Entry(ModItems.SKILL_RESET_SEAL, com.tokyoghoulrpg.combat.Balance.RESET_SEAL_PRICE, true));

    private BloodShop() {
    }

    /**
     * Цена для игрока: зависит от репутации у своей стороны (от -20% до +25%), а в «удачный день»
     * (высокая личная репутация) ещё скидка 15-25%. Печать перерождения всегда по своей цене.
     */
    public static int priceFor(Entry entry, PlayerData data) {
        if (data == null || entry.fixedPrice()) {
            return entry.price();
        }
        int price = Reputation.price(data, entry.price());
        int deal = Standing.dealPercent(data);
        return deal > 0 ? (int) Math.round(price * (100 - deal) / 100.0) : price;
    }

    public static Entry get(int index) {
        return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
    }
}
