package com.tokyoghoulrpg.market;

import com.tokyoghoulrpg.clan.ClanData;
import com.tokyoghoulrpg.clan.ClanQuests;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.QuinqueUpgrade;
import com.tokyoghoulrpg.item.RcCoreItem;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.MarketStateS2C;
import com.tokyoghoulrpg.rep.Achievements;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * Торговая площадка (клавиша M). Игрок выставляет вещь из главной руки за очки крови:
 * <ul>
 *   <li>«Всем кланам» — лот видят и покупают все игроки на сервере (продажа между кланами);</li>
 *   <li>«Только клану» — лот видят и покупают только участники клана продавца.</li>
 * </ul>
 * Два вида лота: с ценой (купил — сразу твоё) и аукцион на 10 / 30 / 60 минут: ставки растут минимум на 10%,
 * очки крови списываются при ставке, перебили — возвращаются. Время вышло: вещь — у победителя, выручка — у продавца;
 * ставок не было — вещь возвращается продавцу. Кто не в сети — получит при входе.
 * Комиссия: с продажи всем кланам 15% цены, внутри клана 10% — в банк клана продавца
 * (у продавца без клана комиссия просто удерживается). Нельзя выставить квинке под замком и ядра RC.
 */
public final class Market {
    public static final int OPEN = 0;
    public static final int SELL = 1;
    public static final int BUY = 2;
    public static final int CANCEL = 3;
    public static final int BID = 4;

    public static final int MAX_LISTINGS = 10;
    public static final int MAX_PRICE = 1_000_000;
    /** Комиссия: продажа всем кланам — 15%, внутри своего клана — 10%. Уходит в банк клана продавца. */
    public static final double PUBLIC_FEE = 0.15;
    public static final double CLAN_FEE = 0.10;
    public static final int[] AUCTION_MINUTES = {0, 10, 30, 60};
    public static final double BID_STEP = 0.10;

    /** Продавец «Чёрный рынок» (системные аукционы: выручка сгорает, без ставок вещь исчезает). */
    public static final UUID SYSTEM = new UUID(0L, 0L);

    private Market() {
    }

    /** Выставить системный аукцион (видят все). */
    public static void systemAuction(MinecraftServer server, ItemStack stack, int start, int minutes, String sellerName) {
        MarketData data = MarketData.get(server);
        MarketData.Listing listing = data.add(SYSTEM, sellerName, null, "", false, stack, start);
        listing.auction = true;
        listing.endMs = System.currentTimeMillis() + minutes * 60_000L;
        data.setDirty();
    }

    private static ClanData.Clan clanOf(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server == null ? null : ClanData.get(server).clanOf(player.getUUID());
    }

    /** Виден ли лот этому игроку. */
    private static boolean visible(MarketData.Listing listing, ServerPlayer player, ClanData.Clan clan) {
        if (listing.seller.equals(player.getUUID())) {
            return true;
        }
        return !listing.clanOnly || (clan != null && clan.id.equals(listing.clan));
    }

    /** Следующая минимальная ставка. */
    public static int nextBid(int start, int current, boolean hasBid) {
        if (!hasBid) {
            return start;
        }
        return (int) Math.min(MAX_PRICE * 10L, Math.max(current + 1L, (long) Math.ceil(current * (1.0 + BID_STEP))));
    }

    public static void action(ServerPlayer player, int action, int value, int price, boolean clanOnly) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        MarketData data = MarketData.get(server);
        switch (action) {
            case SELL -> sell(player, data, price, clanOnly, value);
            case BUY -> buy(player, data, value);
            case BID -> bid(player, data, value);
            case CANCEL -> cancel(player, data, value);
            default -> {
            }
        }
        sendState(player);
    }

    private static void sell(ServerPlayer player, MarketData data, int price, boolean clanOnly, int mode) {
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            tell(player, "empty_hand");
            return;
        }
        if (price <= 0 || price > MAX_PRICE) {
            tell(player, "bad_price");
            return;
        }
        if (hand.getItem() instanceof RcCoreItem || QuinqueUpgrade.isLocked(hand, player.level().getGameTime())) {
            tell(player, "forbidden");
            return;
        }
        ClanData.Clan clan = clanOf(player);
        if (clanOnly && clan == null) {
            tell(player, "no_clan");
            return;
        }
        long mine = data.listings.values().stream().filter(l -> l.seller.equals(player.getUUID())).count();
        if (mine >= MAX_LISTINGS) {
            tell(player, "too_many", MAX_LISTINGS);
            return;
        }
        int minutes = AUCTION_MINUTES[Math.max(0, Math.min(AUCTION_MINUTES.length - 1, mode))];
        ItemStack stack = hand.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        MarketData.Listing listing = data.add(player.getUUID(), player.getGameProfile().getName(), clan == null ? null : clan.id,
                clan == null ? "" : clan.name, clanOnly, stack, price);
        if (minutes > 0) {
            listing.auction = true;
            listing.endMs = System.currentTimeMillis() + minutes * 60_000L;
            data.setDirty();
        }
        Effects.sound(player, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 0.7F, 1.0F);
        player.sendSystemMessage(minutes > 0
                ? Component.translatable(clanOnly ? "message.tokyoghoulrpg.market.auction_clan"
                : "message.tokyoghoulrpg.market.auction", stack.getHoverName(), stack.getCount(), price, minutes)
                : Component.translatable(clanOnly ? "message.tokyoghoulrpg.market.listed_clan"
                : "message.tokyoghoulrpg.market.listed", stack.getHoverName(), stack.getCount(), price));
    }

    private static void buy(ServerPlayer player, MarketData data, int id) {
        MarketData.Listing listing = data.listings.get(id);
        ClanData.Clan clan = clanOf(player);
        if (listing == null) {
            tell(player, "gone");
            return;
        }
        if (listing.auction || listing.seller.equals(player.getUUID()) || !visible(listing, player, clan)) {
            return;
        }
        PlayerData pd = PlayerData.get(player);
        if (pd == null || pd.getBloodPoints() < listing.price) {
            tell(player, "no_blood", listing.price);
            return;
        }
        data.remove(id);
        pd.addBloodPoints(-listing.price);
        give(player, listing.stack.copy());
        NetworkHandler.syncTo(player);
        Effects.sound(player, SoundEvents.VILLAGER_YES, 0.7F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.bought",
                listing.stack.getHoverName(), listing.stack.getCount(), listing.price, listing.sellerName));
        paySeller(player.getServer(), data, listing, listing.price, player.getGameProfile().getName());
    }

    private static void bid(ServerPlayer player, MarketData data, int id) {
        MarketData.Listing listing = data.listings.get(id);
        ClanData.Clan clan = clanOf(player);
        if (listing == null || !listing.auction || System.currentTimeMillis() >= listing.endMs) {
            tell(player, "gone");
            return;
        }
        if (listing.seller.equals(player.getUUID()) || !visible(listing, player, clan)) {
            return;
        }
        if (player.getUUID().equals(listing.bidder)) {
            tell(player, "already_top");
            return;
        }
        int amount = nextBid(listing.price, listing.bid, listing.bidder != null);
        PlayerData pd = PlayerData.get(player);
        if (pd == null || pd.getBloodPoints() < amount) {
            tell(player, "no_blood", amount);
            return;
        }
        MinecraftServer server = player.getServer();
        // вернуть ставку прежнему лидеру
        if (listing.bidder != null) {
            refund(server, data, listing.bidder, listing.bid, listing);
        }
        pd.addBloodPoints(-amount);
        listing.bid = amount;
        listing.bidder = player.getUUID();
        listing.bidderName = player.getGameProfile().getName();
        // ставка в последнюю минуту — аукцион продлевается на минуту
        long left = listing.endMs - System.currentTimeMillis();
        if (left < 60_000L) {
            listing.endMs += 60_000L - left;
        }
        data.setDirty();
        NetworkHandler.syncTo(player);
        Effects.sound(player, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7F, 1.4F);
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.bid_done",
                listing.stack.getHoverName(), amount));
        ServerPlayer seller = server.getPlayerList().getPlayer(listing.seller);
        if (seller != null) {
            seller.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.bid_seller",
                    listing.stack.getHoverName(), listing.bidderName, amount).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void refund(MinecraftServer server, MarketData data, UUID who, int amount, MarketData.Listing listing) {
        ServerPlayer player = server.getPlayerList().getPlayer(who);
        if (player != null) {
            PlayerData pd = PlayerData.get(player);
            if (pd != null) {
                pd.addBloodPoints(amount);
                NetworkHandler.syncTo(player);
            }
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.outbid",
                    listing.stack.getHoverName(), amount).withStyle(ChatFormatting.YELLOW));
        } else {
            data.addEarnings(who, amount);
        }
    }

    private static void cancel(ServerPlayer player, MarketData data, int id) {
        MarketData.Listing listing = data.listings.get(id);
        if (listing == null || !listing.seller.equals(player.getUUID())) {
            return;
        }
        if (listing.auction && listing.bidder != null) {
            tell(player, "has_bids");
            return;
        }
        data.remove(id);
        give(player, listing.stack.copy());
        tell(player, "cancelled");
    }

    /** Выручка продавцу минус комиссия (15% всем кланам / 10% внутри клана) — в банк клана продавца. */
    private static void paySeller(MinecraftServer server, MarketData data, MarketData.Listing listing, int amount, String buyer) {
        if (SYSTEM.equals(listing.seller)) {
            Component news = Component.translatable("message.tokyoghoulrpg.kagune_auction.sold", listing.stack.getHoverName(),
                    buyer, amount).withStyle(ChatFormatting.DARK_RED);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(news);
            }
            return;
        }
        int fee = (int) Math.round(amount * (listing.clanOnly ? CLAN_FEE : PUBLIC_FEE));
        ClanData clans = ClanData.get(server);
        ClanData.Clan sellerClan = listing.clan == null ? null : clans.byId(listing.clan);
        if (sellerClan != null) {
            if (fee > 0) {
                sellerClan.treasury = (int) Math.min(Integer.MAX_VALUE, (long) sellerClan.treasury + fee);
                clans.setDirty();
            }
            ClanQuests.progress(server, sellerClan, ClanQuests.Kind.MARKET_SALE, 1);
        }
        int earned = amount - fee;
        ServerPlayer seller = server.getPlayerList().getPlayer(listing.seller);
        if (seller != null) {
            PlayerData sd = PlayerData.get(seller);
            if (sd != null) {
                sd.addBloodPoints(earned);
                NetworkHandler.syncTo(seller);
            }
            Achievements.add(seller, "market_sales", 1);
            seller.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.sold",
                    listing.stack.getHoverName(), listing.stack.getCount(), buyer, earned).withStyle(ChatFormatting.GREEN));
        } else {
            data.addEarnings(listing.seller, earned);
            data.addPendingSale(listing.seller);
        }
    }

    /** Отдать вещь игроку в сети: в инвентарь, не влезло — под ноги. */
    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void deliver(MinecraftServer server, MarketData data, UUID who, ItemStack stack) {
        ServerPlayer player = server.getPlayerList().getPlayer(who);
        if (player != null) {
            give(player, stack);
        } else {
            data.addPendingItem(who, stack);
        }
    }

    /** Раз в секунду: закончившиеся аукционы. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 5) {
            return;
        }
        MarketData data = MarketData.get(server);
        long now = System.currentTimeMillis();
        for (MarketData.Listing listing : new ArrayList<>(data.listings.values())) {
            if (!listing.auction || now < listing.endMs) {
                continue;
            }
            data.remove(listing.id);
            if (listing.bidder == null && SYSTEM.equals(listing.seller)) {
                continue;        // никто не купил — кагуне ушёл другому покупателю
            }
            if (listing.bidder == null) {
                deliver(server, data, listing.seller, listing.stack.copy());
                ServerPlayer seller = server.getPlayerList().getPlayer(listing.seller);
                if (seller != null) {
                    seller.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.auction_none",
                            listing.stack.getHoverName()));
                }
                continue;
            }
            deliver(server, data, listing.bidder, listing.stack.copy());
            ServerPlayer winner = server.getPlayerList().getPlayer(listing.bidder);
            if (winner != null) {
                winner.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.auction_won",
                        listing.stack.getHoverName(), listing.bid).withStyle(ChatFormatting.GREEN));
            }
            paySeller(server, data, listing, listing.bid, listing.bidderName);
        }
    }

    /** При входе: выручка, возвраты ставок и вещи, которые ждали игрока. */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        MarketData data = MarketData.get(server);
        int earned = data.takeEarnings(player.getUUID());
        PlayerData pd = PlayerData.get(player);
        if (earned > 0 && pd != null) {
            pd.addBloodPoints(earned);
            NetworkHandler.syncTo(player);
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.offline_sales", earned)
                    .withStyle(ChatFormatting.GREEN));
        }
        ListTag items = data.pendingItems.remove(player.getUUID());
        if (items != null) {
            data.setDirty();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = ItemStack.of(items.getCompound(i));
                if (!stack.isEmpty()) {
                    give(player, stack);
                }
            }
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market.offline_items", items.size())
                    .withStyle(ChatFormatting.GREEN));
        }
        Integer sales = data.pendingSales.remove(player.getUUID());
        if (sales != null) {
            data.setDirty();
            Achievements.add(player, "market_sales", sales);
        }
    }

    /** Отправить игроку лоты, которые ему видны. */
    public static void sendState(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        MarketData data = MarketData.get(server);
        ClanData.Clan clan = clanOf(player);
        long now = System.currentTimeMillis();
        List<MarketStateS2C.Entry> entries = new ArrayList<>();
        for (MarketData.Listing l : data.listings.values()) {
            if (!visible(l, player, clan)) {
                continue;
            }
            boolean mine = l.seller.equals(player.getUUID());
            boolean ownClan = clan != null && clan.id.equals(l.clan);
            int shownPrice = l.auction ? nextBid(l.price, l.bid, l.bidder != null) : l.price;
            int secondsLeft = l.auction ? (int) Math.max(0L, (l.endMs - now) / 1000L) : -1;
            entries.add(new MarketStateS2C.Entry(l.id, l.stack, shownPrice, l.sellerName, l.clanName, l.clanOnly, mine,
                    ownClan, secondsLeft, l.bidder == null ? "" : l.bidderName, player.getUUID().equals(l.bidder)));
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MarketStateS2C(clan != null, entries));
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.market." + key, args));
    }
}
