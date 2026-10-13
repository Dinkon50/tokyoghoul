package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.rep.Reputation;
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
    public record Entry(Supplier<Item> item, int price) {
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
            new Entry(ModItems.QUINQUE_JASON, 1200));

    private BloodShop() {
    }

    /** Цена для игрока: зависит от репутации у своей стороны (от -20% до +25%). */
    public static int priceFor(Entry entry, PlayerData data) {
        return data == null ? entry.price() : Reputation.price(data, entry.price());
    }

    public static Entry get(int index) {
        return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
    }
}
