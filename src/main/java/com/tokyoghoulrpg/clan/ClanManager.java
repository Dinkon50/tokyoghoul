package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ClanStateS2C;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Кланы: создать, пригласить игроков с сервера, принять или отклонить приглашение, покинуть,
 * исключить (глава), передать участнику очки навыков или очки крови. В клане могут быть любые классы.
 * Передать можно только участнику своего клана, который сейчас в игре.
 */
public final class ClanManager {
    public static final int OPEN = 0;
    public static final int CREATE = 1;
    public static final int INVITE = 2;
    public static final int ACCEPT = 3;
    public static final int DECLINE = 4;
    public static final int LEAVE = 5;
    public static final int KICK = 6;
    public static final int GIVE_POINTS = 7;
    public static final int GIVE_BLOOD = 8;
    public static final int GIVE_POWER = 9;
    public static final int DONATE = 10;
    public static final int UPGRADE = 11;
    public static final int STORAGE = 12;
    public static final int BUY_SKILL = 13;
    public static final int BANK_PUT_POINTS = 14;
    public static final int BANK_PUT_BLOOD = 15;
    public static final int BANK_TAKE_POINTS = 16;
    public static final int BANK_TAKE_BLOOD = 17;
    public static final int BANK_APPROVE = 18;
    public static final int BANK_DECLINE = 19;
    public static final int DECLARE_WAR = 20;
    public static final int LAND_SELL = 21;
    public static final int LAND_CANCEL = 22;
    public static final int LAND_BUY = 23;
    public static final int LAND_UPGRADE = 24;
    public static final int HOME = 25;
    public static final int ALLY = 26;
    public static final int ALLY_BREAK = 27;
    public static final int HIRE_GUARD = 28;

    public static final int NAME_MIN = 3;
    public static final int NAME_MAX = 16;
    public static final int MAX_MEMBERS = 20;

    private ClanManager() {
    }

    public static void action(ServerPlayer player, int action, String text, UUID target, int amount) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.clanOf(player.getUUID());
        switch (action) {
            case CREATE -> create(player, data, clan, text == null ? "" : text.trim());
            case INVITE -> invite(player, data, clan, target);
            case ACCEPT -> accept(player, data, clan, target);
            case DECLINE -> {
                ClanData.Clan other = target == null ? null : data.byId(target);
                if (other != null && other.invited.remove(player.getUUID())) {
                    data.setDirty();
                    refreshClan(server, other);
                }
            }
            case LEAVE -> {
                if (clan != null) {
                    if (clan.members.size() <= 1) {
                        // последний уходит — клан распущен, вещи со склада достаются ему
                        returnStorage(player, clan);
                    }
                    data.removeMember(clan, player.getUUID());
                    tell(player, "left", clan.name);
                    notifyMembers(server, clan, "member_left", player.getGameProfile().getName());
                    refreshClan(server, clan);
                }
            }
            case KICK -> kick(player, data, clan, target);
            case DONATE -> donate(player, data, clan, amount);
            case BUY_SKILL -> buySkill(player, data, clan, amount);
            case LAND_SELL -> LandSale.sell(player, data, clan, text, amount);
            case LAND_CANCEL -> LandSale.cancel(player, data, clan, text);
            case LAND_UPGRADE -> ClanTerritory.upgrade(player, data, clan, text);
            case HOME -> {
                ClanTerritory.home(player);
                return;
            }
            case ALLY, ALLY_BREAK -> {
                ClanData.Clan other = target == null ? null : data.byId(target);
                if (action == ALLY) {
                    ClanAlliances.propose(player, data, clan, other);
                } else {
                    ClanAlliances.breakAlliance(player, data, clan, other);
                }
                if (other != null) {
                    refreshClan(server, other);
                }
            }
            case HIRE_GUARD -> ClanGuards.hire(player, data, clan);
            case LAND_BUY -> {
                ClanData.Clan seller = target == null ? null : data.byId(target);
                LandSale.buy(player, data, clan, target, text);
                if (seller != null) {
                    refreshClan(server, seller);
                }
            }
            case DECLARE_WAR -> {
                ClanData.Clan target2 = target == null ? null : data.byId(target);
                if (clan != null && player.getUUID().equals(clan.leader) && target2 != null) {
                    ClanWars.declare(player, data, clan, target2);
                    refreshClan(server, clan);
                    refreshClan(server, target2);
                    return;
                }
            }
            case BANK_PUT_POINTS, BANK_PUT_BLOOD -> ClanBank.deposit(player, data, clan, amount, action == BANK_PUT_POINTS);
            case BANK_TAKE_POINTS, BANK_TAKE_BLOOD -> ClanBank.request(player, data, clan, amount, action == BANK_TAKE_POINTS);
            case BANK_APPROVE, BANK_DECLINE -> ClanBank.decide(player, data, clan, amount, action == BANK_APPROVE);
            case UPGRADE -> upgrade(player, data, clan);
            case STORAGE -> {
                if (clan != null) {
                    openStorage(player, data, clan);
                    return;     // открылся склад: меню клана закрылось само
                }
            }
            case GIVE_POINTS, GIVE_BLOOD -> give(player, clan, target, amount, action == GIVE_BLOOD);
            case GIVE_POWER -> {
                // передать силу и половину здоровья участнику своего клана, который в игре
                ServerPlayer receiver = target == null ? null : server.getPlayerList().getPlayer(target);
                if (clan != null && receiver != null && clan.members.containsKey(target)
                        && !target.equals(player.getUUID())) {
                    ClanBond.give(player, receiver);
                } else if (clan != null && target != null && clan.members.containsKey(target)) {
                    tell(player, "offline", clan.members.get(target));
                }
            }
            default -> {
            }
        }
        if (clan != null && action >= BANK_PUT_POINTS && action <= BANK_DECLINE) {
            refreshClan(server, clan);
            return;
        }
        sendState(player);
    }

    private static void create(ServerPlayer player, ClanData data, ClanData.Clan clan, String name) {
        if (clan != null) {
            return;
        }
        if (name.length() < NAME_MIN || name.length() > NAME_MAX || !name.matches("[\\p{L}\\p{N} _\\-]+")) {
            tell(player, "bad_name", NAME_MIN, NAME_MAX);
            return;
        }
        if (data.nameTaken(name)) {
            tell(player, "name_taken", name);
            return;
        }
        data.create(name, player.getUUID(), player.getGameProfile().getName());
        tell(player, "created", name);
        // знамя клана — сразу главе: поставь его, и вокруг будет территория (приват) клана
        net.minecraft.world.item.ItemStack banner = new net.minecraft.world.item.ItemStack(
                com.tokyoghoulrpg.registry.ModItems.CLAN_BANNER.get());
        if (!player.getInventory().add(banner)) {
            player.drop(banner, false);
        }
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.banner_given")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        com.tokyoghoulrpg.rep.Achievements.add(player, "clan_founded", 1);
    }

    private static void invite(ServerPlayer player, ClanData data, ClanData.Clan clan, UUID target) {
        if (clan == null || !player.getUUID().equals(clan.leader) || target == null) {
            return;
        }
        ServerPlayer invitee = player.getServer().getPlayerList().getPlayer(target);
        if (invitee == null || data.clanOf(target) != null || clan.invited.contains(target)) {
            return;
        }
        if (clan.members.size() >= MAX_MEMBERS) {
            tell(player, "full", MAX_MEMBERS);
            return;
        }
        clan.invited.add(target);
        data.setDirty();
        tell(player, "invited", invitee.getGameProfile().getName());
        invitee.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan.invite",
                player.getGameProfile().getName(), clan.name));
        sendState(invitee);
    }

    private static void accept(ServerPlayer player, ClanData data, ClanData.Clan clan, UUID clanId) {
        ClanData.Clan other = clanId == null ? null : data.byId(clanId);
        if (clan != null || other == null || !other.invited.contains(player.getUUID())) {
            return;
        }
        if (other.members.size() >= MAX_MEMBERS) {
            tell(player, "full", MAX_MEMBERS);
            return;
        }
        data.addMember(other, player.getUUID(), player.getGameProfile().getName());
        tell(player, "joined", other.name);
        notifyMembers(player.getServer(), other, "member_joined", player.getGameProfile().getName());
        refreshClan(player.getServer(), other);
    }

    private static void kick(ServerPlayer player, ClanData data, ClanData.Clan clan, UUID target) {
        if (clan == null || !player.getUUID().equals(clan.leader) || target == null
                || target.equals(player.getUUID()) || !clan.members.containsKey(target)) {
            return;
        }
        String name = clan.members.get(target);
        data.removeMember(clan, target);
        notifyMembers(player.getServer(), clan, "member_kicked", name);
        ServerPlayer kicked = player.getServer().getPlayerList().getPlayer(target);
        if (kicked != null) {
            tell(kicked, "kicked", clan.name);
            sendState(kicked);
        }
        refreshClan(player.getServer(), clan);
    }

    /** Вложить очки крови в казну клана. */
    private static void donate(ServerPlayer player, ClanData data, ClanData.Clan clan, int amount) {
        PlayerData pd = PlayerData.get(player);
        if (clan == null || pd == null || amount <= 0) {
            return;
        }
        if (amount > pd.getBloodPoints()) {
            tell(player, "not_enough");
            return;
        }
        pd.addBloodPoints(-amount);
        clan.treasury = (int) Math.min(Integer.MAX_VALUE, (long) clan.treasury + amount);
        data.setDirty();
        NetworkHandler.syncTo(player);
        notifyMembers(player.getServer(), clan, "donated", player.getGameProfile().getName() + " (+" + amount + ")");
        refreshClan(player.getServer(), clan);
    }

    /** Глава: новый уровень клана за очки из казны. */
    private static void upgrade(ServerPlayer player, ClanData data, ClanData.Clan clan) {
        if (clan == null || !player.getUUID().equals(clan.leader) || clan.level >= ClanPerks.MAX_LEVEL) {
            return;
        }
        int cost = ClanPerks.COST[clan.level + 1];
        if (clan.treasury < cost) {
            tell(player, "treasury_low", cost);
            return;
        }
        clan.treasury -= cost;
        clan.level++;
        data.setDirty();
        for (UUID uuid : clan.members.keySet()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(uuid);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan.level_up", clan.name, clan.level)
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
                com.tokyoghoulrpg.combat.Effects.sound(member, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
                ClanPerks.tick(member);
            }
        }
        // максимальный уровень — узнаёт весь сервер
        if (clan.level >= ClanPerks.MAX_LEVEL) {
            Component news = Component.translatable("message.tokyoghoulrpg.clan.max_level", clan.name, clan.level)
                    .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
            for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(news);
                com.tokyoghoulrpg.combat.Effects.sound(p, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
            }
        }
        refreshClan(player.getServer(), clan);
    }

    /** Глава: следующий уровень умения клана (номер умения в amount) за очки из казны. */
    private static void buySkill(ServerPlayer player, ClanData data, ClanData.Clan clan, int index) {
        ClanPerks.Skill[] all = ClanPerks.Skill.values();
        if (clan == null || !player.getUUID().equals(clan.leader) || index < 0 || index >= all.length) {
            return;
        }
        ClanPerks.Skill skill = all[index];
        int next = clan.skills[index] + 1;
        if (next > ClanPerks.SKILL_MAX) {
            return;
        }
        if (clan.level < next + 1) {
            tell(player, "skill_needs_level", next + 1);
            return;
        }
        int cost = skill.cost(next);
        if (clan.treasury < cost) {
            tell(player, "treasury_low", cost);
            return;
        }
        clan.treasury -= cost;
        clan.skills[index] = next;
        data.setDirty();
        for (UUID uuid : clan.members.keySet()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(uuid);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan.skill_up",
                        Component.translatable("clan_skill.tokyoghoulrpg." + skill.id()),
                        com.tokyoghoulrpg.skill.SkillTree.roman(next)).withStyle(net.minecraft.ChatFormatting.GOLD));
            }
        }
        refreshClan(player.getServer(), clan);
    }

    /** Склад клана: сундук на 3-6 рядов (по уровню), общий для всех участников. */
    private static void openStorage(ServerPlayer player, ClanData data, ClanData.Clan clan) {
        int rows = ClanPerks.STORAGE_ROWS[Math.max(1, Math.min(ClanPerks.MAX_LEVEL, clan.level))];
        net.minecraft.world.inventory.MenuType<?> type = switch (rows) {
            case 3 -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
            default -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
        };
        UUID clanId = clan.id;
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) ->
                new net.minecraft.world.inventory.ChestMenu(type, id, inventory, clan.storage, rows) {
                    @Override
                    public boolean stillValid(net.minecraft.world.entity.player.Player who) {
                        // выгнали из клана или клан распущен — склад закрывается
                        ClanData.Clan now = data.clanOf(who.getUUID());
                        return now != null && now.id.equals(clanId);
                    }
                }, Component.translatable("screen.tokyoghoulrpg.clan.storage_title", clan.name)));
    }

    /** Клан распускается: вещи со склада — последнему участнику (что не влезло, падает рядом). */
    private static void returnStorage(ServerPlayer player, ClanData.Clan clan) {
        for (int slot = 0; slot < clan.storage.getContainerSize(); slot++) {
            net.minecraft.world.item.ItemStack stack = clan.storage.removeItemNoUpdate(slot);
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    /** Передать участнику своего клана очки навыков или очки крови. */
    private static void give(ServerPlayer player, ClanData.Clan clan, UUID target, int amount, boolean blood) {
        if (clan == null || target == null || target.equals(player.getUUID()) || !clan.members.containsKey(target)
                || amount <= 0) {
            return;
        }
        ServerPlayer receiver = player.getServer().getPlayerList().getPlayer(target);
        if (receiver == null) {
            tell(player, "offline", clan.members.get(target));
            return;
        }
        PlayerData from = PlayerData.get(player);
        PlayerData to = PlayerData.get(receiver);
        if (from == null || to == null) {
            return;
        }
        int have = blood ? from.getBloodPoints() : from.getSkillPoints();
        if (amount > have) {
            tell(player, "not_enough");
            return;
        }
        if (blood) {
            from.addBloodPoints(-amount);
            to.addBloodPoints(amount);
        } else {
            from.setSkillPoints(from.getSkillPoints() - amount);
            to.addSkillPoints(amount);
        }
        NetworkHandler.syncTo(player);
        NetworkHandler.syncTo(receiver);
        String kind = blood ? "blood" : "points";
        tell(player, "gave_" + kind, amount, receiver.getGameProfile().getName());
        tell(receiver, "got_" + kind, amount, player.getGameProfile().getName());
    }

    // ------------------------------------------------------------------ отправка состояния

    /** Отправить игроку всё, что нужно меню клана. */
    public static void sendState(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.clanOf(player.getUUID());
        List<ClanStateS2C.Member> members = new ArrayList<>();
        List<ClanStateS2C.Member> candidates = new ArrayList<>();
        List<ClanStateS2C.Member> invites = new ArrayList<>();
        String name = "";
        UUID leader = null;
        int reputation = 0;
        int level = 0;
        int treasury = 0;
        int[] skills = new int[ClanPerks.Skill.values().length];
        int bankPoints = 0;
        List<ClanBank.Request> requests = new ArrayList<>();
        if (clan != null) {
            skills = clan.skills.clone();
            bankPoints = clan.bankPoints;
            requests.addAll(clan.requests);
            name = clan.name;
            leader = clan.leader;
            reputation = clan.reputation;
            level = clan.level;
            treasury = clan.treasury;
            for (Map.Entry<UUID, String> entry : clan.members.entrySet()) {
                boolean online = server.getPlayerList().getPlayer(entry.getKey()) != null;
                members.add(new ClanStateS2C.Member(entry.getKey(), entry.getValue(), online));
            }
            // кого можно пригласить: игроки на сервере без клана
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (data.clanOf(other.getUUID()) == null) {
                    candidates.add(new ClanStateS2C.Member(other.getUUID(), other.getGameProfile().getName(),
                            clan.invited.contains(other.getUUID())));
                }
            }
        } else {
            for (ClanData.Clan other : data.invitesFor(player.getUUID())) {
                String leaderName = other.members.getOrDefault(other.leader, "?");
                invites.add(new ClanStateS2C.Member(other.id, other.name + " (" + leaderName + ")", true));
            }
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ClanStateS2C(name, leader, reputation, level, treasury, skills, bankPoints, requests, members, candidates,
                        invites));
        if (clan != null) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), info(server, data, clan));
        }
    }

    /** Задания, войны, рейтинг, территория — для меню развития клана. */
    private static com.tokyoghoulrpg.network.packet.ClanInfoS2C info(MinecraftServer server, ClanData data, ClanData.Clan clan) {
        ClanQuests.ensure(clan);
        data.setDirty();
        List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.Quest> quests = new ArrayList<>();
        for (int i = 0; i < clan.quests.length; i++) {
            ClanQuests.Kind kind = ClanQuests.kind(clan.quests[i]);
            quests.add(new com.tokyoghoulrpg.network.packet.ClanInfoS2C.Quest(kind.ordinal(), clan.questProgress[i],
                    ClanQuests.target(kind, clan.members.size()), kind.daily()));
        }
        List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.WarTarget> targets = new ArrayList<>();
        java.util.Map<UUID, String> reasons = ClanWars.reasons(server, clan);
        for (ClanData.Clan other : data.all()) {
            if (other == clan) {
                continue;
            }
            String reason = reasons.get(other.id);
            targets.add(new com.tokyoghoulrpg.network.packet.ClanInfoS2C.WarTarget(other.id, other.name, other.level,
                    other.members.size(), reason == null ? "" : reason, ClanWars.protectedMinutes(other),
                    ClanAlliances.state(clan, other)));
        }
        List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.Rating> rating = new ArrayList<>();
        for (ClanData.Clan c : ClanRating.ranked(server)) {
            if (rating.size() >= 10) {
                break;
            }
            rating.add(new com.tokyoghoulrpg.network.packet.ClanInfoS2C.Rating(c.name, c.level, c.members.size(), c.warWins,
                    ClanRating.score(c), c == clan));
        }
        ClanWars.War war = ClanWars.warFor(data, clan.id);
        String enemy = "";
        int our = 0;
        int their = 0;
        if (war != null) {
            boolean isA = war.a.equals(ClanWars.sideOf(data, war, clan.id));
            enemy = isA ? war.nameB : war.nameA;
            our = isA ? war.scoreA : war.scoreB;
            their = isA ? war.scoreB : war.scoreA;
        }
        // «x y z|сколько территорий|максимум» (пусто — территорий нет)
        String territory = "-|0|" + ClanTerritory.maxTerritories(clan) + "|" + ClanTerritory.mainSize(clan);
        int count = ClanTerritory.territoryCount(clan);
        if (count > 0) {
            String own = "-";
            if (ClanTerritory.hasFlag(clan)) {
                net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(clan.flagPos);
                own = pos.getX() + " " + pos.getY() + " " + pos.getZ();
            }
            territory = own + "|" + count + "|" + ClanTerritory.maxTerritories(clan) + "|" + ClanTerritory.mainSize(clan);
        }
        int income = (ClanTerritory.hasFlag(clan) ? ClanTerritory.INCOME_PER_LEVEL * clan.level : 0)
                + clan.captured.size() * ClanTerritory.INCOME_PER_LEVEL * clan.level / 2;
        long bossMinutes = Math.max(0L, (clan.bossCooldownUntil - System.currentTimeMillis() + 59_999) / 60_000);
        return new com.tokyoghoulrpg.network.packet.ClanInfoS2C(quests, targets, rating, enemy, our, their, clan.warWins,
                clan.warLosses, ClanWars.protectedMinutes(clan), territory, income,
                bossMinutes, lands(server, clan), offers(data, clan));
    }

    private static List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.Land> lands(MinecraftServer server, ClanData.Clan clan) {
        List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.Land> list = new ArrayList<>();
        for (String key : ClanTerritory.flags(clan)) {
            boolean main = !clan.captured.contains(key);
            list.add(new com.tokyoghoulrpg.network.packet.ClanInfoS2C.Land(key, LandSale.place(key), main,
                    clan.forSale.getOrDefault(key, 0), ClanTerritory.size(clan, key), ClanTerritory.upgradeCost(clan, key),
                    ClanGuards.count(server, clan, key)));
        }
        return list;
    }

    private static List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.LandOffer> offers(ClanData data, ClanData.Clan clan) {
        List<com.tokyoghoulrpg.network.packet.ClanInfoS2C.LandOffer> list = new ArrayList<>();
        for (ClanData.Clan other : data.all()) {
            if (other == clan) {
                continue;
            }
            for (java.util.Map.Entry<String, Integer> e : other.forSale.entrySet()) {
                list.add(new com.tokyoghoulrpg.network.packet.ClanInfoS2C.LandOffer(other.id, other.name, e.getKey(),
                        LandSale.place(e.getKey()), e.getValue()));
            }
        }
        return list;
    }

    /** Обновить меню у всех участников клана, кто в игре (например, когда изменилась репутация клана). */
    public static void refresh(MinecraftServer server, ClanData.Clan clan) {
        refreshClan(server, clan);
    }

    /** Обновить меню у всех участников клана, кто в игре. */
    private static void refreshClan(MinecraftServer server, ClanData.Clan clan) {
        for (UUID uuid : new ArrayList<>(clan.members.keySet())) {
            ServerPlayer member = server.getPlayerList().getPlayer(uuid);
            if (member != null) {
                sendState(member);
            }
        }
    }

    private static void notifyMembers(MinecraftServer server, ClanData.Clan clan, String key, String who) {
        for (UUID uuid : clan.members.keySet()) {
            ServerPlayer member = server.getPlayerList().getPlayer(uuid);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan." + key, who));
            }
        }
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan." + key, args));
    }

    /** При входе в игру: обновить ник в клане. */
    public static void onLogin(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            ClanData.get(server).rename(player.getUUID(), player.getGameProfile().getName());
            ClanBank.onLogin(player);
            com.tokyoghoulrpg.market.Market.onLogin(player);
            ClanWars.onLogin(player);
        }
    }
}
