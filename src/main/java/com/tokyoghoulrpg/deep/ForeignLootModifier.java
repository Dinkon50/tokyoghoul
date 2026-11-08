package com.tokyoghoulrpg.deep;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Добыча из других модов в сундуках мода (во всех постройках, в данже — больше), rolls раз с шансом chance.
 * Редкость — та, что задал сам мод (обычный, необычный, редкий, эпический — цвет названия предмета):
 * <ul>
 *   <li>чаще берётся сундук другого мода (его автор уже решил, что выпадает часто, а что редко), из него —
 *       только вещи модов, и каждая проходит с шансом по редкости (см. KEEP);</li>
 *   <li>иначе — случайный предмет модов по весам (см. WEIGHT).</li>
 *   <li>Вещи без заданной редкости (обычная по умолчанию) — шанс маленький: 25% материалы, 8% снаряжение.</li>
 * </ul>
 * luck — щедрость сундука: в данже за троном редкие и эпические попадаются в 4 раза чаще.
 * Если других модов нет — ничего не добавляется.
 * Подключение: data/forge/loot_modifiers/global_loot_modifiers.json и data/tokyoghoulrpg/loot_modifiers/foreign_*.json.
 */
public class ForeignLootModifier extends LootModifier {
    public static final Supplier<Codec<ForeignLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(instance -> codecStart(instance)
                    .and(Codec.INT.fieldOf("rolls").forGetter(m -> m.rolls))
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
                    .and(Codec.BOOL.optionalFieldOf("all_mod_chests", false).forGetter(m -> m.allModChests))
                    .and(Codec.FLOAT.optionalFieldOf("luck", 1.0F).forGetter(m -> m.luck))
                    .apply(instance, ForeignLootModifier::new)));

    /** Не «другие» моды: сам Minecraft, Forge и наш мод. */
    private static final Set<String> OWN = Set.of("minecraft", "forge", "tokyoghoulrpg");
    private static List<Item> foreignItems = null;

    private final int rolls;
    private final float chance;
    /**
     * true — работает для всех сундуков мода (tokyoghoulrpg:chests/...), кроме сундуков данжа: у них свои,
     * более щедрые модификаторы. Так новые постройки мода получают добычу других модов сами.
     */
    private final boolean allModChests;
    /** Щедрость сундука: во сколько раз чаще редкие и эпические вещи (1 — обычный сундук, 4 — за троном). */
    private final float luck;

    public ForeignLootModifier(LootItemCondition[] conditions, int rolls, float chance, boolean allModChests, float luck) {
        super(conditions);
        this.rolls = rolls;
        this.chance = chance;
        this.allModChests = allModChests;
        this.luck = luck;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (allModChests) {
            ResourceLocation id = context.getQueriedLootTableId();
            if (id == null || !"tokyoghoulrpg".equals(id.getNamespace()) || !id.getPath().startsWith("chests/")
                    || id.getPath().startsWith("chests/deep_")) {
                return generatedLoot;
            }
        }
        MinecraftServer server = context.getLevel().getServer();
        if (server == null) {
            return generatedLoot;
        }
        LootDataManager data = server.getLootData();
        List<ResourceLocation> tables = foreignChestTables(data);
        RandomSource random = context.getRandom();
        for (int i = 0; i < rolls; i++) {
            if (random.nextFloat() >= chance) {
                continue;
            }
            List<ItemStack> found = new ArrayList<>();
            // чаще — из сундука другого мода (там его автор уже решил, что редкое, а что нет)
            if (!tables.isEmpty() && random.nextFloat() < 0.7F) {
                LootTable table = data.getLootTable(tables.get(random.nextInt(tables.size())));
                if (table != LootTable.EMPTY) {
                    table.getRandomItemsRaw(context, stack -> {
                        if (isForeign(stack) && random.nextFloat() < keepChance(stack, luck)) {
                            found.add(stack);
                        }
                    });
                }
            }
            // иначе (или если из сундука ничего не прошло) — случайный предмет модов с учётом его редкости
            if (found.isEmpty()) {
                ItemStack stack = pickByRarity(random, luck);
                if (!stack.isEmpty()) {
                    found.add(stack);
                }
            }
            // не больше 4 стопок за раз, чтобы сундук не переполнился
            for (int k = 0; k < found.size() && k < 4; k++) {
                generatedLoot.add(found.get(k));
            }
        }
        return generatedLoot;
    }

    // ------------------------------------------------------------------ редкость

    /**
     * Ступень редкости: 1 необычный, 2 редкий, 3 эпический — как задал мод. Обычная редкость в Minecraft —
     * это и «мод сказал: обычный», и «мод ничего не задал», их не различить. Поэтому обычные вещи других модов
     * считаем вещами неизвестной ценности: 0 — складываются в стопку (скорее материал), 4 — по одной штуке,
     * с прочностью или блеском (оружие, броня, инструменты, артефакты — могут быть сильными).
     */
    private static int tier(ItemStack stack) {
        Rarity rarity = stack.getRarity();
        if (rarity == Rarity.UNCOMMON) {
            return 1;
        }
        if (rarity == Rarity.RARE) {
            return 2;
        }
        if (rarity == Rarity.EPIC) {
            return 3;
        }
        if (rarity != Rarity.COMMON) {
            return 2;                   // особые редкости модов
        }
        boolean gear = stack.getMaxStackSize() == 1 || stack.isDamageableItem() || stack.hasFoil();
        return gear ? 4 : 0;
    }

    /** Вес при случайном выборе предмета: материалы чаще, эпические и вещи без редкости реже. */
    private static final float[] WEIGHT = {40.0F, 18.0F, 5.0F, 1.2F, 3.0F};
    /**
     * Шанс, что выбранная вещь вообще попадёт в сундук. У вещей без заданной редкости он маленький:
     * материалы — 25%, снаряжение и одиночные вещи — 8%. У необычных/редких/эпических — 70% / 35% / 12%.
     */
    private static final float[] KEEP = {0.25F, 0.7F, 0.35F, 0.12F, 0.08F};

    private static float keepChance(ItemStack stack, float luck) {
        int t = tier(stack);
        return Math.min(1.0F, KEEP[t] * (t == 0 ? 1.0F : luck));
    }

    private static ItemStack pickByRarity(RandomSource random, float luck) {
        List<Item> items = foreignItems();
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        float total = 0.0F;
        float[] w = new float[items.size()];
        for (int i = 0; i < items.size(); i++) {
            int t = foreignTiers.get(i);
            w[i] = t == 0 ? WEIGHT[0] : WEIGHT[t] * luck;
            total += w[i];
        }
        float r = random.nextFloat() * total;
        int index = items.size() - 1;
        for (int i = 0; i < items.size(); i++) {
            r -= w[i];
            if (r <= 0.0F) {
                index = i;
                break;
            }
        }
        Item item = items.get(index);
        ItemStack stack = new ItemStack(item);
        int t = foreignTiers.get(index);
        // вещь без заданной редкости: шанс маленький (редкость задана — её уже учёл вес)
        if ((t == 0 || t == 4) && random.nextFloat() >= Math.min(1.0F, KEEP[t] * (t == 0 ? 1.0F : luck))) {
            return ItemStack.EMPTY;
        }
        // материалов — горсть (до 4), необычных — до 3, остальные — по одной
        int max = Math.min(stack.getMaxStackSize(), t == 0 ? 4 : t == 1 ? 3 : 1);
        stack.setCount(1 + (max > 1 ? random.nextInt(max) : 0));
        return stack;
    }

    private static boolean isForeign(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return !stack.isEmpty() && id != null && !OWN.contains(id.getNamespace());
    }

    /** Таблицы сундуков других модов. */
    private static List<ResourceLocation> foreignChestTables(LootDataManager data) {
        List<ResourceLocation> list = new ArrayList<>();
        for (ResourceLocation id : data.getKeys(LootDataType.TABLE)) {
            if (!OWN.contains(id.getNamespace()) && id.getPath().contains("chests/")) {
                list.add(id);
            }
        }
        list.sort(ResourceLocation::compareTo);
        return list;
    }

    private static List<Integer> foreignTiers = null;

    /** Предметы других модов (без яиц призыва, воздуха и творческих/отладочных вещей) и их редкость. */
    private static List<Item> foreignItems() {
        if (foreignItems == null) {
            List<Item> list = new ArrayList<>();
            List<Integer> tiers = new ArrayList<>();
            for (var entry : ForgeRegistries.ITEMS.getEntries()) {
                ResourceLocation id = entry.getKey().location();
                Item item = entry.getValue();
                String path = id.getPath();
                if (OWN.contains(id.getNamespace()) || item == Items.AIR || item instanceof SpawnEggItem
                        || path.contains("creative") || path.contains("debug") || path.contains("spawn_egg")) {
                    continue;
                }
                list.add(item);
                tiers.add(tier(new ItemStack(item)));
            }
            foreignTiers = tiers;
            foreignItems = list;
        }
        return foreignItems;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
