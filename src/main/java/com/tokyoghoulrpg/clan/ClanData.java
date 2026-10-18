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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
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
        /** Уровень клана (1-5) и казна в очках крови, из которой платят за улучшение. */
        public int level = 1;
        public int treasury = 0;
        /** Склад клана: всегда 54 ячейки, сколько рядов открыто — зависит от уровня. */
        public final SimpleContainer storage = new SimpleContainer(ClanPerks.STORAGE_SLOTS);
        /** Банк клана: очки навыков (очки крови банка — это treasury). */
        public int bankPoints = 0;
        /** Просьбы участников забрать из банка (ждут решения главы). */
        public final List<ClanBank.Request> requests = new ArrayList<>();
        public int nextRequestId = 1;
        /** Одобрено, но игрок был не в сети: выдать при входе {очки, кровь}. */
        public final Map<UUID, int[]> payouts = new HashMap<>();
        /** Войны: победы, поражения; до какого времени (мс) клан защищён от новых войн и сам не может объявлять. */
        public int warWins = 0;
        public int warLosses = 0;
        public long protectedUntil = 0L;
        public long declareCooldownUntil = 0L;
        /** Территория: знамя (измерение и позиция; пусто — знамени нет), когда последний раз начислен доход (мс). */
        public String flagDim = "";
        public long flagPos = 0L;
        public long lastIncome = 0L;
        /** Захваченные территории (знамёна побеждённых кланов): «измерение;позиция». */
        public final List<String> captured = new ArrayList<>();
        /** Территории на продаже: «измерение;позиция» → цена в очках крови. */
        public final Map<String, Integer> forSale = new LinkedHashMap<>();
        /** Улучшения каждой территории: «измерение;позиция» → сколько раз улучшена. */
        public final Map<String, Integer> tiers = new HashMap<>();
        /** Клановый босс: сколько раз побеждён и когда можно призвать снова (мс). */
        public int bossKills = 0;
        /** Стражи на территориях: ключ знамени → сколько нанято (живых). */
        public final Map<String, Integer> guards = new HashMap<>();
        /** Союзные кланы (взаимно: у союзника записан и этот клан). */
        public final Set<UUID> allies = new LinkedHashSet<>();
        public long bossCooldownUntil = 0L;
        /** Задания клана: день и неделя (по календарю), номера заданий, прогресс, выполнено ли. */
        public long questDay = -1L;
        public long questWeek = -1L;
        public int[] quests = new int[0];
        public int[] questProgress = new int[0];
        public int questsDone = 0;
        /** Умения клана: уровень каждого (0-3), по порядку ClanPerks.Skill. */
        public final int[] skills = new int[ClanPerks.Skill.values().length];

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

    /** Склад: любое изменение помечает файл кланов для сохранения. */
    private void watch(Clan clan) {
        clan.storage.addListener(container -> setDirty());
    }

    public Clan create(String name, UUID leader, String leaderName) {
        Clan clan = new Clan(UUID.randomUUID(), name, leader);
        watch(clan);
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
            for (Clan other : clans.values()) {
                other.allies.remove(clan.id);
            }
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

    /** Все кланы (для войн, рейтинга, территорий). */
    public java.util.Collection<Clan> all() {
        return clans.values();
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
            c.putInt("level", clan.level);
            c.putInt("treasury", clan.treasury);
            ListTag items = new ListTag();
            for (int slot = 0; slot < clan.storage.getContainerSize(); slot++) {
                ItemStack stack = clan.storage.getItem(slot);
                if (!stack.isEmpty()) {
                    CompoundTag it = stack.save(new CompoundTag());
                    it.putByte("Slot", (byte) slot);
                    items.add(it);
                }
            }
            c.put("storage", items);
            c.putIntArray("skills", clan.skills);
            c.putInt("bank_points", clan.bankPoints);
            c.putInt("war_wins", clan.warWins);
            c.putInt("war_losses", clan.warLosses);
            c.putLong("protected_until", clan.protectedUntil);
            c.putLong("declare_cd", clan.declareCooldownUntil);
            c.putString("flag_dim", clan.flagDim);
            c.putLong("flag_pos", clan.flagPos);
            c.putLong("last_income", clan.lastIncome);
            ListTag cap = new ListTag();
            for (String t : clan.captured) {
                cap.add(net.minecraft.nbt.StringTag.valueOf(t));
            }
            c.put("captured", cap);
            CompoundTag sale = new CompoundTag();
            for (Map.Entry<String, Integer> e : clan.forSale.entrySet()) {
                sale.putInt(e.getKey(), e.getValue());
            }
            c.put("for_sale", sale);
            CompoundTag tierTag = new CompoundTag();
            for (Map.Entry<String, Integer> e : clan.tiers.entrySet()) {
                tierTag.putInt(e.getKey(), e.getValue());
            }
            c.put("tiers", tierTag);
            c.putInt("boss_kills", clan.bossKills);
            ListTag allyTag = new ListTag();
            for (UUID ally : clan.allies) {
                allyTag.add(net.minecraft.nbt.StringTag.valueOf(ally.toString()));
            }
            c.put("allies", allyTag);
            CompoundTag guardTag = new CompoundTag();
            for (Map.Entry<String, Integer> e : clan.guards.entrySet()) {
                guardTag.putInt(e.getKey(), e.getValue());
            }
            c.put("guards", guardTag);
            c.putLong("boss_cd", clan.bossCooldownUntil);
            c.putLong("quest_day", clan.questDay);
            c.putLong("quest_week", clan.questWeek);
            c.putIntArray("quests", clan.quests);
            c.putIntArray("quest_progress", clan.questProgress);
            c.putInt("quests_done", clan.questsDone);
            c.putInt("next_request", clan.nextRequestId);
            ListTag reqs = new ListTag();
            for (ClanBank.Request r : clan.requests) {
                reqs.add(r.save());
            }
            c.put("requests", reqs);
            ListTag pays = new ListTag();
            for (Map.Entry<UUID, int[]> e : clan.payouts.entrySet()) {
                CompoundTag pt = new CompoundTag();
                pt.putUUID("uuid", e.getKey());
                pt.putIntArray("v", e.getValue());
                pays.add(pt);
            }
            c.put("payouts", pays);
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
            clan.level = Math.max(1, Math.min(ClanPerks.MAX_LEVEL, c.contains("level") ? c.getInt("level") : 1));
            clan.treasury = Math.max(0, c.getInt("treasury"));
            ListTag items = c.getList("storage", Tag.TAG_COMPOUND);
            for (int j = 0; j < items.size(); j++) {
                CompoundTag it = items.getCompound(j);
                int slot = it.getByte("Slot") & 255;
                if (slot < clan.storage.getContainerSize()) {
                    clan.storage.setItem(slot, ItemStack.of(it));
                }
            }
            int[] skills = c.getIntArray("skills");
            for (int j = 0; j < Math.min(skills.length, clan.skills.length); j++) {
                clan.skills[j] = Math.max(0, Math.min(ClanPerks.SKILL_MAX, skills[j]));
            }
            clan.bankPoints = Math.max(0, c.getInt("bank_points"));
            clan.warWins = c.getInt("war_wins");
            clan.warLosses = c.getInt("war_losses");
            clan.protectedUntil = c.getLong("protected_until");
            clan.declareCooldownUntil = c.getLong("declare_cd");
            clan.flagDim = c.getString("flag_dim");
            clan.flagPos = c.getLong("flag_pos");
            clan.lastIncome = c.getLong("last_income");
            ListTag cap = c.getList("captured", Tag.TAG_STRING);
            for (int j = 0; j < cap.size(); j++) {
                clan.captured.add(cap.getString(j));
            }
            CompoundTag tierTag = c.getCompound("tiers");
            for (String k : tierTag.getAllKeys()) {
                clan.tiers.put(k, Math.max(0, tierTag.getInt(k)));
            }
            CompoundTag sale = c.getCompound("for_sale");
            for (String k : sale.getAllKeys()) {
                if (clan.captured.contains(k)) {
                    clan.forSale.put(k, Math.max(1, sale.getInt(k)));
                }
            }
            clan.bossKills = c.getInt("boss_kills");
            CompoundTag guardTag = c.getCompound("guards");
            for (String k : guardTag.getAllKeys()) {
                clan.guards.put(k, Math.max(0, guardTag.getInt(k)));
            }
            ListTag allyTag = c.getList("allies", Tag.TAG_STRING);
            for (int j = 0; j < allyTag.size(); j++) {
                try {
                    clan.allies.add(UUID.fromString(allyTag.getString(j)));
                } catch (IllegalArgumentException ignored) {
                    // битая запись — пропускаем
                }
            }
            clan.bossCooldownUntil = c.getLong("boss_cd");
            clan.questDay = c.contains("quest_day") ? c.getLong("quest_day") : -1L;
            clan.questWeek = c.contains("quest_week") ? c.getLong("quest_week") : -1L;
            clan.quests = c.getIntArray("quests");
            clan.questProgress = c.getIntArray("quest_progress");
            if (clan.questProgress.length != clan.quests.length) {
                clan.questProgress = new int[clan.quests.length];
            }
            clan.questsDone = c.getInt("quests_done");
            clan.nextRequestId = Math.max(1, c.getInt("next_request"));
            ListTag reqs = c.getList("requests", Tag.TAG_COMPOUND);
            for (int j = 0; j < reqs.size(); j++) {
                clan.requests.add(ClanBank.Request.load(reqs.getCompound(j)));
            }
            ListTag pays = c.getList("payouts", Tag.TAG_COMPOUND);
            for (int j = 0; j < pays.size(); j++) {
                int[] v = pays.getCompound(j).getIntArray("v");
                if (v.length == 2) {
                    clan.payouts.put(pays.getCompound(j).getUUID("uuid"), v);
                }
            }
            data.watch(clan);
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
