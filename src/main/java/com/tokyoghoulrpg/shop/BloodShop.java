package com.tokyoghoulrpg.shop;

import com.tokyoghoulrpg.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Лавка торговца за очки крови. Пока здесь только инъекции; позже добавим квинке.
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
            new Entry(ModItems.CCG_INJECTION, 1000));

    private BloodShop() {
    }

    public static Entry get(int index) {
        return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
    }
}
