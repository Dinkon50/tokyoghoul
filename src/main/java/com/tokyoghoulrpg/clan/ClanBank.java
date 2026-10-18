package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;

/**
 * Банк клана: общие очки навыков и очки крови (очки крови банка — это и есть казна, из неё же платят за прокачку).
 * <ul>
 *   <li>Положить может любой участник.</li>
 *   <li>Забрать: глава — сразу; остальные — только с разрешения главы. Просьба ждёт, пока глава её одобрит
 *       или отклонит (если он не в сети — пока не зайдёт). Одобрил, а просивший не в сети — получит при входе.</li>
 *   <li>Налог клана: 10% очков крови и очков навыков, которые участник получает за убийства, боссов, задания
 *       и контракты, сразу уходят в банк.</li>
 *   <li>Убийство людей участником (игроки-люди и агенты CCG, NPC-агенты, жители, Старший следователь)
 *       приносит банку ещё и отдельную награду — у игрока ничего не отнимается.</li>
 * </ul>
 */
public final class ClanBank {
    public static final double TAX = 0.10;
    /** За убийство людей — в банк сверху: {очки навыков, очки крови}. */
    public static final int[] KILL_PLAYER = {1, 50};
    public static final int[] KILL_NPC = {0, 10};
    public static final int[] KILL_CCG_BOSS = {2, 150};
    public static final int MAX_REQUESTS = 20;

    /** Просьба участника забрать из банка. */
    public record Request(int id, UUID player, String name, int points, int blood) {
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("id", id);
            tag.putUUID("uuid", player);
            tag.putString("name", name);
            tag.putInt("points", points);
            tag.putInt("blood", blood);
            return tag;
        }

        static Request load(CompoundTag tag) {
            return new Request(tag.getInt("id"), tag.getUUID("uuid"), tag.getString("name"),
                    Math.max(0, tag.getInt("points")), Math.max(0, tag.getInt("blood")));
        }
    }

    private ClanBank() {
    }

    private static ClanData data(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        return server == null ? null : ClanData.get(server);
    }

    private static void add(ClanData data, ClanData.Clan clan, int points, int blood) {
        clan.bankPoints = (int) Math.min(Integer.MAX_VALUE, (long) clan.bankPoints + Math.max(0, points));
        clan.treasury = (int) Math.min(Integer.MAX_VALUE, (long) clan.treasury + Math.max(0, blood));
        data.setDirty();
    }

    /**
     * Налог клана с награды, которую игрок только что получил (кровь и очки уже начислены):
     * 10% уходит в банк. Очки навыков — целые, поэтому дробная часть берётся с шансом.
     */
    public static void tax(ServerPlayer player, int blood, int points) {
        ClanData data = data(player);
        ClanData.Clan clan = data == null ? null : data.clanOf(player.getUUID());
        PlayerData pd = PlayerData.get(player);
        if (clan == null || pd == null) {
            return;
        }
        int bloodTax = (int) Math.round(Math.max(0, blood) * TAX);
        double exact = Math.max(0, points) * TAX;
        int pointsTax = (int) Math.floor(exact) + (player.getRandom().nextDouble() < exact - Math.floor(exact) ? 1 : 0);
        bloodTax = Math.min(bloodTax, pd.getBloodPoints());
        pointsTax = Math.min(pointsTax, pd.getSkillPoints());
        if (bloodTax <= 0 && pointsTax <= 0) {
            return;
        }
        pd.addBloodPoints(-bloodTax);
        pd.setSkillPoints(pd.getSkillPoints() - pointsTax);
        add(data, clan, pointsTax, bloodTax);
        NetworkHandler.syncTo(player);
    }

    /** Участник убил человека: банку отдельная награда. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        ClanData data = data(killer);
        ClanData.Clan clan = data == null ? null : data.clanOf(killer.getUUID());
        if (clan == null) {
            return;
        }
        int[] reward = null;
        if (victim instanceof ServerPlayer other && other != killer) {
            PlayerData od = PlayerData.get(other);
            if (od != null && (od.getPlayerClass() == PlayerClass.HUMAN || od.getPlayerClass() == PlayerClass.CCG_AGENT)
                    && !clan.members.containsKey(other.getUUID())) {
                reward = KILL_PLAYER;
            }
        } else if (victim instanceof CcgBoss) {
            reward = KILL_CCG_BOSS;
        } else if (victim instanceof CcgAgentNpc || victim instanceof AbstractVillager) {
            reward = KILL_NPC;
        }
        if (reward != null) {
            add(data, clan, reward[0], reward[1]);
        }
    }

    // ------------------------------------------------------------------ действия из меню

    public static void deposit(ServerPlayer player, ClanData data, ClanData.Clan clan, int amount, boolean points) {
        PlayerData pd = PlayerData.get(player);
        if (clan == null || pd == null || amount <= 0) {
            return;
        }
        int have = points ? pd.getSkillPoints() : pd.getBloodPoints();
        if (amount > have) {
            tell(player, "not_enough");
            return;
        }
        if (points) {
            pd.setSkillPoints(have - amount);
        } else {
            pd.addBloodPoints(-amount);
        }
        add(data, clan, points ? amount : 0, points ? 0 : amount);
        NetworkHandler.syncTo(player);
        notifyAll(player.getServer(), clan, Component.translatable(points ? "message.tokyoghoulrpg.clan.bank_put_points"
                : "message.tokyoghoulrpg.clan.bank_put_blood", player.getGameProfile().getName(), amount));
    }

    /** Забрать: глава сразу, остальные — просьба главе. */
    public static void request(ServerPlayer player, ClanData data, ClanData.Clan clan, int amount, boolean points) {
        if (clan == null || amount <= 0) {
            return;
        }
        int inBank = points ? clan.bankPoints : clan.treasury;
        if (amount > inBank) {
            tell(player, "bank_low");
            return;
        }
        if (player.getUUID().equals(clan.leader)) {
            payOut(data, clan, player.getServer(), player.getUUID(), points ? amount : 0, points ? 0 : amount);
            return;
        }
        if (clan.requests.size() >= MAX_REQUESTS) {
            tell(player, "bank_queue_full");
            return;
        }
        Request request = new Request(clan.nextRequestId++, player.getUUID(), player.getGameProfile().getName(),
                points ? amount : 0, points ? 0 : amount);
        clan.requests.add(request);
        data.setDirty();
        tell(player, "bank_requested");
        ServerPlayer leader = player.getServer().getPlayerList().getPlayer(clan.leader);
        if (leader != null) {
            leader.sendSystemMessage(describe("message.tokyoghoulrpg.clan.bank_new_request", request)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** Глава одобрил или отклонил просьбу. */
    public static void decide(ServerPlayer leader, ClanData data, ClanData.Clan clan, int id, boolean approve) {
        if (clan == null || !leader.getUUID().equals(clan.leader)) {
            return;
        }
        Iterator<Request> it = clan.requests.iterator();
        while (it.hasNext()) {
            Request r = it.next();
            if (r.id() != id) {
                continue;
            }
            if (approve && (r.points() > clan.bankPoints || r.blood() > clan.treasury)) {
                tell(leader, "bank_low");
                return;
            }
            it.remove();
            data.setDirty();
            ServerPlayer asker = leader.getServer().getPlayerList().getPlayer(r.player());
            if (approve && clan.members.containsKey(r.player())) {
                payOut(data, clan, leader.getServer(), r.player(), r.points(), r.blood());
            } else if (asker != null) {
                asker.sendSystemMessage(describe("message.tokyoghoulrpg.clan.bank_declined", r).withStyle(ChatFormatting.RED));
            }
            return;
        }
    }

    /** Списать из банка и выдать (или отложить до входа игрока). */
    private static void payOut(ClanData data, ClanData.Clan clan, MinecraftServer server, UUID who, int points, int blood) {
        clan.bankPoints -= points;
        clan.treasury -= blood;
        data.setDirty();
        ServerPlayer player = server.getPlayerList().getPlayer(who);
        if (player != null) {
            give(player, points, blood);
        } else {
            int[] pending = clan.payouts.computeIfAbsent(who, k -> new int[2]);
            pending[0] += points;
            pending[1] += blood;
        }
    }

    private static void give(ServerPlayer player, int points, int blood) {
        PlayerData pd = PlayerData.get(player);
        if (pd == null) {
            return;
        }
        pd.addSkillPoints(points);
        pd.addBloodPoints(blood);
        NetworkHandler.syncTo(player);
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan.bank_got", points, blood)
                .withStyle(ChatFormatting.GREEN));
    }

    /** При входе: выдать одобренное, а главе напомнить о просьбах. */
    public static void onLogin(ServerPlayer player) {
        ClanData data = data(player);
        ClanData.Clan clan = data == null ? null : data.clanOf(player.getUUID());
        if (clan == null) {
            return;
        }
        int[] pending = clan.payouts.remove(player.getUUID());
        if (pending != null) {
            data.setDirty();
            give(player, pending[0], pending[1]);
        }
        if (player.getUUID().equals(clan.leader) && !clan.requests.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan.bank_waiting", clan.requests.size())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static net.minecraft.network.chat.MutableComponent describe(String key, Request r) {
        return Component.translatable(key, r.name(), r.points(), r.blood());
    }

    private static void notifyAll(MinecraftServer server, ClanData.Clan clan, Component message) {
        for (UUID uuid : clan.members.keySet()) {
            ServerPlayer member = server.getPlayerList().getPlayer(uuid);
            if (member != null) {
                member.sendSystemMessage(message);
            }
        }
    }

    private static void tell(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan." + key));
    }
}
