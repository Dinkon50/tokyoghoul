package com.tokyoghoulrpg.clan;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Продажа территорий кланов. Продаёт и покупает только глава (создатель) клана.
 * <ul>
 *   <li>Продать можно только захваченные территории — основная (своё знамя) не продаётся,
 *       и последнюю территорию клана продать нельзя: всю землю клан не отдаст.</li>
 *   <li>Цену глава назначает сам (очки крови). Лот видят главы всех кланов во вкладке «Земли».</li>
 *   <li>Купить может глава другого клана, если в казне хватает очков и у клана есть место в лимите территорий.
 *       Оплата — из казны покупателя в казну продавца, знамя остаётся на месте, территория переходит покупателю.</li>
 *   <li>Территорию, вокруг которой идёт война (у любого из кланов), продать и купить нельзя.</li>
 * </ul>
 */
public final class LandSale {
    public static final int MAX_PRICE = 1_000_000;

    private LandSale() {
    }

    public static void sell(ServerPlayer leader, ClanData data, ClanData.Clan clan, String key, int price) {
        if (clan == null || !leader.getUUID().equals(clan.leader) || key == null) {
            return;
        }
        if (!clan.captured.contains(key)) {
            tell(leader, "main");
            return;
        }
        if (ClanTerritory.territoryCount(clan) <= 1) {
            tell(leader, "last");
            return;
        }
        if (price <= 0 || price > MAX_PRICE) {
            tell(leader, "price");
            return;
        }
        clan.forSale.put(key, price);
        data.setDirty();
        Component news = Component.translatable("message.tokyoghoulrpg.land.listed", clan.name, place(key), price)
                .withStyle(ChatFormatting.YELLOW);
        for (ClanData.Clan other : data.all()) {
            ServerPlayer otherLeader = leader.getServer().getPlayerList().getPlayer(other.leader);
            if (otherLeader != null) {
                otherLeader.sendSystemMessage(news);
            }
        }
    }

    public static void cancel(ServerPlayer leader, ClanData data, ClanData.Clan clan, String key) {
        if (clan != null && leader.getUUID().equals(clan.leader) && clan.forSale.remove(key) != null) {
            data.setDirty();
            tell(leader, "cancelled");
        }
    }

    public static void buy(ServerPlayer leader, ClanData data, ClanData.Clan buyer, UUID sellerId, String key) {
        ClanData.Clan seller = sellerId == null ? null : data.byId(sellerId);
        if (buyer == null || seller == null || buyer == seller || !leader.getUUID().equals(buyer.leader)) {
            return;
        }
        Integer price = seller.forSale.get(key);
        if (price == null || !seller.captured.contains(key)) {
            tell(leader, "gone");
            return;
        }
        if (ClanWars.warOf(buyer.id) != null || ClanWars.warOf(seller.id) != null) {
            tell(leader, "war");
            return;
        }
        if (ClanTerritory.territoryCount(buyer) >= ClanTerritory.maxTerritories(buyer)) {
            leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.limit_capture",
                    ClanTerritory.maxTerritories(buyer)).withStyle(ChatFormatting.RED));
            return;
        }
        if (buyer.treasury < price) {
            tell(leader, "no_blood", price);
            return;
        }
        buyer.treasury -= price;
        seller.treasury = (int) Math.min(Integer.MAX_VALUE, (long) seller.treasury + price);
        Integer tier = seller.tiers.remove(key);   // улучшения переходят вместе с территорией
        seller.captured.remove(key);
        seller.forSale.remove(key);
        buyer.captured.add(key);
        if (tier != null) {
            buyer.tiers.put(key, tier);
        }
        data.setDirty();
        Component news = Component.translatable("message.tokyoghoulrpg.land.sold", seller.name, buyer.name, place(key), price)
                .withStyle(ChatFormatting.GOLD);
        MinecraftServer server = leader.getServer();
        for (ClanData.Clan c : new ClanData.Clan[] {buyer, seller}) {
            for (UUID id : c.members.keySet()) {
                ServerPlayer member = server.getPlayerList().getPlayer(id);
                if (member != null) {
                    member.sendSystemMessage(news);
                }
            }
        }
    }

    /** «x y z» из ключа «измерение;позиция». */
    public static String place(String key) {
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(ClanTerritory.posOf(key));
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.land." + key, args).withStyle(ChatFormatting.RED));
    }
}
