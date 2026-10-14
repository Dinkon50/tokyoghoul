package com.tokyoghoulrpg.clan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Все кланы мира. Хранятся в файле мира data/tokyoghoulrpg_clans.dat. */
public class ClanData extends SavedData {
    private static final String FILE = "tokyoghoulrpg_clans";

    /** Один клан. Участники в порядке вступления, первый — глава. */
    public static final class Clan {
        public final UUID id;
        public String name;
        public UUID leader;
        public final Map<UUID, String> members = new LinkedHashMap<>();
        /** Приглашённые: кто ещё не ответил. */
        public final Set<UUID> invited = new LinkedHashSet<>();
        /** Репутация клана (от -100 до 100). Меняется от дел всех участников. */
        public int reputation = 0;

        Clan(UUID id, String name, UUID leader) {
            this.id = id;
            this.name = name;
            this.leader = leader;
        }
    }

    private final Map<UUID, Clan> clans = new LinkedHashMap<>();
    /** Игрок → клан (для быстрого поиска). */
    private final Map<UUID, UUID> memberOf = new HashMap<>();

    public static ClanData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ClanData::load, ClanData::new, FILE);
    }

    public Clan clanOf(UUID player) {
        UUID id = memberOf.get(player);
        return id == null ? null : clans.get(id);
    }

    public Clan byId(UUID id) {
        return clans.get(id);
    }

    public boolean nameTaken(String name) {
        for (Clan clan : clans.values()) {
            if (clan.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public Clan create(String name, UUID leader, String leaderName) {
        Clan clan = new Clan(UUID.randomUUID(), name, leader);
        clans.put(clan.id, clan);
        addMember(clan, leader, leaderName);
        setDirty();
        return clan;
    }

    public void addMember(Clan clan, UUID player, String playerName) {
        clan.members.put(player, playerName);
        clan.invited.remove(player);
        memberOf.put(player, clan.id);
        // вступил в клан — все остальные приглашения больше не нужны
        for (Clan other : clans.values()) {
            other.invited.remove(player);
        }
        setDirty();
    }

    /** Убрать игрока из клана. Глава уходит — главой становится следующий; последний ушёл — клан распущен. */
    public void removeMember(Clan clan, UUID player) {
        clan.members.remove(player);
        memberOf.remove(player);
        if (clan.members.isEmpty()) {
            clans.remove(clan.id);
        } else if (player.equals(clan.leader)) {
            clan.leader = clan.members.keySet().iterator().next();
        }
        setDirty();
    }

    /** Изменить репутацию клана (в пределах от -100 до 100). Возвращает старое значение. */
    public int changeReputation(Clan clan, int delta) {
        int before = clan.reputation;
        clan.reputation = Math.max(-100, Math.min(100, before + delta));
        if (clan.reputation != before) {
            setDirty();
        }
        return before;
    }

    /** Кланы, которые пригласили игрока. */
    public List<Clan> invitesFor(UUID player) {
        List<Clan> list = new ArrayList<>();
        for (Clan clan : clans.values()) {
            if (clan.invited.contains(player)) {
                list.add(clan);
            }
        }
        return list;
    }

    /** Обновить имя игрока (мог сменить ник). */
    public void rename(UUID player, String name) {
        Clan clan = clanOf(player);
        if (clan != null && !name.equals(clan.members.get(player))) {
            clan.members.put(player, name);
            setDirty();
        }
    }

    // ------------------------------------------------------------------ сохранение

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Clan clan : clans.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", clan.id);
            c.putString("name", clan.name);
            c.putUUID("leader", clan.leader);
            c.putInt("rep", clan.reputation);
            ListTag members = new ListTag();
            for (Map.Entry<UUID, String> entry : clan.members.entrySet()) {
                CompoundTag m = new CompoundTag();
                m.putUUID("uuid", entry.getKey());
                m.putString("name", entry.getValue());
                members.add(m);
            }
            c.put("members", members);
            ListTag invited = new ListTag();
            for (UUID uuid : clan.invited) {
                CompoundTag i = new CompoundTag();
                i.putUUID("uuid", uuid);
                invited.add(i);
            }
            c.put("invited", invited);
            list.add(c);
        }
        tag.put("clans", list);
        return tag;
    }

    public static ClanData load(CompoundTag tag) {
        ClanData data = new ClanData();
        ListTag list = tag.getList("clans", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Clan clan = new Clan(c.getUUID("id"), c.getString("name"), c.getUUID("leader"));
            clan.reputation = Math.max(-100, Math.min(100, c.getInt("rep")));
            ListTag members = c.getList("members", Tag.TAG_COMPOUND);
            for (int j = 0; j < members.size(); j++) {
                CompoundTag m = members.getCompound(j);
                UUID uuid = m.getUUID("uuid");
                clan.members.put(uuid, m.getString("name"));
                data.memberOf.put(uuid, clan.id);
            }
            ListTag invited = c.getList("invited", Tag.TAG_COMPOUND);
            for (int j = 0; j < invited.size(); j++) {
                clan.invited.add(invited.getCompound(j).getUUID("uuid"));
            }
            if (!clan.members.isEmpty()) {
                if (!clan.members.containsKey(clan.leader)) {
                    clan.leader = clan.members.keySet().iterator().next();
                }
                data.clans.put(clan.id, clan);
            }
        }
        return data;
    }
}
