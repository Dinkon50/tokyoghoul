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
                    data.removeMember(clan, player.getUUID());
                    tell(player, "left", clan.name);
                    notifyMembers(server, clan, "member_left", player.getGameProfile().getName());
                    refreshClan(server, clan);
                }
            }
            case KICK -> kick(player, data, clan, target);
            case GIVE_POINTS, GIVE_BLOOD -> give(player, clan, target, amount, action == GIVE_BLOOD);
            default -> {
            }
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
        if (clan != null) {
            name = clan.name;
            leader = clan.leader;
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
                new ClanStateS2C(name, leader, members, candidates, invites));
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
        }
    }
}
