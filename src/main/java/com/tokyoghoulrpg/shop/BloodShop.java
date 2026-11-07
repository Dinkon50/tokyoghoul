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
            // инъекции: цена от сложности мира (injectionPrice), репутация и скидки на неё не действуют
            new Entry(ModItems.HUMAN_INJECTION, com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_HARD, true),
            new Entry(ModItems.GHOUL_INJECTION, com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_HARD, true),
            new Entry(ModItems.CCG_INJECTION, com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_HARD, true),
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
    public static int priceFor(Entry entry, PlayerData data, net.minecraft.world.Difficulty difficulty) {
        if (isInjection(entry)) {
            return injectionPrice(difficulty);
        }
        if (data == null || entry.fixedPrice()) {
            return entry.price();
        }
        int price = Reputation.price(data, entry.price());
        int deal = Standing.dealPercent(data);
        return deal > 0 ? (int) Math.round(price * (100 - deal) / 100.0) : price;
    }

    private static boolean isInjection(Entry entry) {
        net.minecraft.world.item.Item item = entry.item().get();
        return item == ModItems.HUMAN_INJECTION.get() || item == ModItems.GHOUL_INJECTION.get()
                || item == ModItems.CCG_INJECTION.get();
    }

    /** Инъекция: 100 000 на сложной, 80 000 на нормальной, 60 000 на лёгкой и мирной. Никогда не дешевле 60 000. */
    public static int injectionPrice(net.minecraft.world.Difficulty difficulty) {
        int price = difficulty == net.minecraft.world.Difficulty.HARD ? com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_HARD
                : difficulty == net.minecraft.world.Difficulty.NORMAL ? com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_NORMAL
                : com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_EASY;
        return Math.max(com.tokyoghoulrpg.combat.Balance.INJECTION_PRICE_MIN, price);
    }

    public static Entry get(int index) {
        return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
    }
}
