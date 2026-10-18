package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Союзы кланов. Глава предлагает союз, глава другого клана принимает (тоже кнопкой «Союз»).
 * Союзники:
 * <ul>
 *   <li>не могут ранить друг друга;</li>
 *   <li>воюют вместе: если один из союзников в войне, второй на его стороне (их убийства идут в счёт);</li>
 *   <li>не могут объявить войну друг другу;</li>
 *   <li>стражи территории их не трогают.</li>
 * </ul>
 * Союз взаимный и хранится в сохранении. Предложения живут, пока сервер не перезапущен.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class ClanAlliances {
    public static final int MAX_ALLIES = 2;

    /** Состояние для меню: нет связи / союзники / они нам предложили / мы им предложили. */
    public static final int NONE = 0;
    public static final int ALLIED = 1;
    public static final int THEY_INVITED = 2;
    public static final int WE_INVITED = 3;

    /** Кто кому предложил союз: клан → кланы, которым он предложил. */
    private static final Map<UUID, Set<UUID>> PROPOSALS = new HashMap<>();

    private ClanAlliances() {
    }

    public static boolean allied(ClanData.Clan a, ClanData.Clan b) {
        return a != null && b != null && a != b && a.allies.contains(b.id) && b.allies.contains(a.id);
    }

    public static boolean allied(MinecraftServer server, UUID a, UUID b) {
        if (server == null || a == null || b == null || a.equals(b)) {
            return false;
        }
        ClanData data = ClanData.get(server);
        return allied(data.byId(a), data.byId(b));
    }

    /** Игроки из союзных кланов. */
    public static boolean alliedPlayers(ServerPlayer x, ServerPlayer y) {
        MinecraftServer server = x.getServer();
        if (server == null) {
            return false;
        }
        ClanData data = ClanData.get(server);
        return allied(data.clanOf(x.getUUID()), data.clanOf(y.getUUID()));
    }

    private static boolean proposed(UUID from, UUID to) {
        Set<UUID> set = PROPOSALS.get(from);
        return set != null && set.contains(to);
    }

    private static void unpropose(UUID from, UUID to) {
        Set<UUID> set = PROPOSALS.get(from);
        if (set != null) {
            set.remove(to);
            if (set.isEmpty()) {
                PROPOSALS.remove(from);
            }
        }
    }

    public static int state(ClanData.Clan own, ClanData.Clan other) {
        if (allied(own, other)) {
            return ALLIED;
        }
        if (proposed(other.id, own.id)) {
            return THEY_INVITED;
        }
        if (proposed(own.id, other.id)) {
            return WE_INVITED;
        }
        return NONE;
    }

    public static void clear() {
        PROPOSALS.clear();
    }

    /** Кнопка «Союз»: предложить или принять (если тот клан уже предложил нам). */
    public static void propose(ServerPlayer leader, ClanData data, ClanData.Clan own, ClanData.Clan other) {
        if (own == null || other == null || own == other || !leader.getUUID().equals(own.leader)) {
            return;
        }
        if (allied(own, other)) {
            return;
        }
        if (own.allies.size() >= MAX_ALLIES) {
            fail(leader, "ally.full_own", MAX_ALLIES);
            return;
        }
        if (other.allies.size() >= MAX_ALLIES) {
            fail(leader, "ally.full_other", other.name, MAX_ALLIES);
            return;
        }
        if (ClanWars.enemies(own.id, other.id)) {
            fail(leader, "ally.at_war", other.name);
            return;
        }
        MinecraftServer server = leader.getServer();
        if (proposed(other.id, own.id)) {
            unpropose(other.id, own.id);
            unpropose(own.id, other.id);
            own.allies.add(other.id);
            other.allies.add(own.id);
            data.setDirty();
            Component msg = Component.translatable("message.tokyoghoulrpg.ally.formed", own.name, other.name)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            tellClan(server, own, msg);
            tellClan(server, other, msg);
            return;
        }
        if (proposed(own.id, other.id)) {
            fail(leader, "ally.already_sent", other.name);
            return;
        }
        PROPOSALS.computeIfAbsent(own.id, k -> new HashSet<>()).add(other.id);
        leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ally.sent", other.name)
                .withStyle(ChatFormatting.AQUA));
        ServerPlayer otherLeader = server.getPlayerList().getPlayer(other.leader);
        if (otherLeader != null) {
            otherLeader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ally.incoming", own.name)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    /** Кнопка «Разорвать» / «Отклонить» / «Отозвать». */
    public static void breakAlliance(ServerPlayer leader, ClanData data, ClanData.Clan own, ClanData.Clan other) {
        if (own == null || other == null || own == other || !leader.getUUID().equals(own.leader)) {
            return;
        }
        MinecraftServer server = leader.getServer();
        if (allied(own, other)) {
            own.allies.remove(other.id);
            other.allies.remove(own.id);
            data.setDirty();
            Component msg = Component.translatable("message.tokyoghoulrpg.ally.broken", own.name, other.name)
                    .withStyle(ChatFormatting.GRAY);
            tellClan(server, own, msg);
            tellClan(server, other, msg);
        } else if (proposed(other.id, own.id)) {
            unpropose(other.id, own.id);
            leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ally.declined", other.name)
                    .withStyle(ChatFormatting.GRAY));
            ServerPlayer otherLeader = server.getPlayerList().getPlayer(other.leader);
            if (otherLeader != null) {
                otherLeader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ally.declined_by", own.name)
                        .withStyle(ChatFormatting.GRAY));
            }
        } else if (proposed(own.id, other.id)) {
            unpropose(own.id, other.id);
            leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ally.withdrawn", other.name)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void fail(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg." + key, args).withStyle(ChatFormatting.RED));
    }

    private static void tellClan(MinecraftServer server, ClanData.Clan clan, Component msg) {
        for (UUID id : clan.members.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.sendSystemMessage(msg);
            }
        }
    }

    /** Союзники не ранят друг друга (и стрелами тоже: источник — стрелявший). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer victim)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == victim) {
            return;
        }
        if (alliedPlayers(attacker, victim)) {
            event.setCanceled(true);
        }
    }
}
