package com.tokyoghoulrpg.market;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Лоты торговой площадки и выручка продавцов, которые были не в сети. Файл мира data/tokyoghoulrpg_market.dat. */
public class MarketData extends SavedData {
    private static final String FILE = "tokyoghoulrpg_market";

    /** Лот: вещь, цена в очках крови, продавец и видимость (всем или только его клану). */
    public static final class Listing {
        public final int id;
        public final UUID seller;
        public final String sellerName;
        /** Клан продавца (null — без клана). */
        public final UUID clan;
        public final String clanName;
        public final boolean clanOnly;
        public final ItemStack stack;
        /** Цена (для аукциона — стартовая). */
        public final int price;
        /** Аукцион: до какого времени (мс), текущая ставка и кто её сделал (null — ставок нет). */
        public boolean auction;
        public long endMs;
        public int bid;
        public UUID bidder;
        public String bidderName = "";

        Listing(int id, UUID seller, String sellerName, UUID clan, String clanName, boolean clanOnly, ItemStack stack, int price) {
            this.id = id;
            this.seller = seller;
            this.sellerName = sellerName;
            this.clan = clan;
            this.clanName = clanName;
            this.clanOnly = clanOnly;
            this.stack = stack;
            this.price = price;
        }
    }

    final Map<Integer, Listing> listings = new LinkedHashMap<>();
    /** Продавец → выручка, которую он получит при входе. */
    final Map<UUID, Integer> earnings = new HashMap<>();
    /** Игрок → вещи, которые он получит при входе (выигранный аукцион, вернувшийся лот). */
    final Map<UUID, ListTag> pendingItems = new HashMap<>();
    /** Продавец → сколько продаж было, пока его не было (для достижений). */
    final Map<UUID, Integer> pendingSales = new HashMap<>();
    private int nextId = 1;

    public static MarketData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MarketData::load, MarketData::new, FILE);
    }

    Listing add(UUID seller, String sellerName, UUID clan, String clanName, boolean clanOnly, ItemStack stack, int price) {
        Listing listing = new Listing(nextId++, seller, sellerName, clan, clanName, clanOnly, stack, price);
        listings.put(listing.id, listing);
        setDirty();
        return listing;
    }

    Listing remove(int id) {
        Listing listing = listings.remove(id);
        if (listing != null) {
            setDirty();
        }
        return listing;
    }

    void addEarnings(UUID seller, int amount) {
        earnings.merge(seller, amount, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
        setDirty();
    }

    int takeEarnings(UUID seller) {
        Integer amount = earnings.remove(seller);
        if (amount != null) {
            setDirty();
        }
        return amount == null ? 0 : amount;
    }

    void addPendingItem(UUID player, ItemStack stack) {
        pendingItems.computeIfAbsent(player, k -> new ListTag()).add(stack.save(new CompoundTag()));
        setDirty();
    }

    void addPendingSale(UUID player) {
        pendingSales.merge(player, 1, Integer::sum);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Listing l : listings.values()) {
            CompoundTag c = new CompoundTag();
            c.putInt("id", l.id);
            c.putUUID("seller", l.seller);
            c.putString("seller_name", l.sellerName);
            if (l.clan != null) {
                c.putUUID("clan", l.clan);
            }
            c.putString("clan_name", l.clanName);
            c.putBoolean("clan_only", l.clanOnly);
            c.put("item", l.stack.save(new CompoundTag()));
            c.putInt("price", l.price);
            c.putBoolean("auction", l.auction);
            c.putLong("end", l.endMs);
            c.putInt("bid", l.bid);
            if (l.bidder != null) {
                c.putUUID("bidder", l.bidder);
            }
            c.putString("bidder_name", l.bidderName);
            list.add(c);
        }
        tag.put("listings", list);
        ListTag earn = new ListTag();
        for (Map.Entry<UUID, Integer> e : earnings.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("uuid", e.getKey());
            c.putInt("amount", e.getValue());
            earn.add(c);
        }
        tag.put("earnings", earn);
        ListTag items = new ListTag();
        for (Map.Entry<UUID, ListTag> e : pendingItems.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("uuid", e.getKey());
            c.put("items", e.getValue().copy());
            items.add(c);
        }
        tag.put("pending_items", items);
        ListTag sales = new ListTag();
        for (Map.Entry<UUID, Integer> e : pendingSales.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("uuid", e.getKey());
            c.putInt("count", e.getValue());
            sales.add(c);
        }
        tag.put("pending_sales", sales);
        tag.putInt("next_id", nextId);
        return tag;
    }

    public static MarketData load(CompoundTag tag) {
        MarketData data = new MarketData();
        ListTag list = tag.getList("listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            ItemStack stack = ItemStack.of(c.getCompound("item"));
            if (stack.isEmpty()) {
                continue;
            }
            Listing l = new Listing(c.getInt("id"), c.getUUID("seller"), c.getString("seller_name"),
                    c.hasUUID("clan") ? c.getUUID("clan") : null, c.getString("clan_name"), c.getBoolean("clan_only"),
                    stack, Math.max(1, c.getInt("price")));
            l.auction = c.getBoolean("auction");
            l.endMs = c.getLong("end");
            l.bid = c.getInt("bid");
            l.bidder = c.hasUUID("bidder") ? c.getUUID("bidder") : null;
            l.bidderName = c.getString("bidder_name");
            data.listings.put(l.id, l);
        }
        ListTag earn = tag.getList("earnings", Tag.TAG_COMPOUND);
        for (int i = 0; i < earn.size(); i++) {
            CompoundTag c = earn.getCompound(i);
            data.earnings.put(c.getUUID("uuid"), Math.max(0, c.getInt("amount")));
        }
        ListTag items = tag.getList("pending_items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag c = items.getCompound(i);
            data.pendingItems.put(c.getUUID("uuid"), c.getList("items", Tag.TAG_COMPOUND));
        }
        ListTag sales = tag.getList("pending_sales", Tag.TAG_COMPOUND);
        for (int i = 0; i < sales.size(); i++) {
            CompoundTag c = sales.getCompound(i);
            data.pendingSales.put(c.getUUID("uuid"), c.getInt("count"));
        }
        data.nextId = Math.max(1, tag.getInt("next_id"));
        for (int id : data.listings.keySet()) {
            data.nextId = Math.max(data.nextId, id + 1);
        }
        return data;
    }
}
