package com.tokyoghoulrpg.data;

import com.tokyoghoulrpg.combat.CombatState;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.INBTSerializable;

/** Данные RPG-персонажа, привязанные к игроку. Главный на сервере, клиент получает копию. */
public class PlayerData implements INBTSerializable<CompoundTag> {
    public static final Capability<PlayerData> CAP = CapabilityManager.get(new CapabilityToken<>() { });

    /** Временное значение: максимум шкалы ярости. */
    public static final int RAGE_MAX = 100;

    // Сохраняется в мире
    private PlayerClass playerClass = PlayerClass.NONE;
    private KaguneVariant kaguneVariant = KaguneVariant.NONE;
    private boolean kaguneActive = false;
    private int rage = 0;
    private int rageLevel = 0;
    private int skillPoints = 0;
    /** Самый высокий уровень Minecraft, за который уже выданы очки навыков. */
    private int maxLevelRewarded = 0;
    /** Очки крови: валюта для покупок у торговца. */
    private int bloodPoints = 0;
    /** Выданы ли стартовые очки крови (один раз на персонажа). */
    private boolean startBloodGiven = false;
    /** Активный контракт информатора (null — нет), прогресс и день, когда последний контракт сдан. */
    private String contractId = null;
    private int contractProgress = 0;
    private long contractDoneDay = -1L;
    /** Шкала крови гуля: 100 — полная, убывает при критическом голоде. */
    private int craving = 100;
    /** Репутация у CCG и у гулей: от -100 до 100. */
    private int repCcg = 0;
    private int repGhouls = 0;
    /** Когда (время мира) CCG придут с рейдом. 0 — ещё не назначено. */
    private long nextRaid = 0L;
    /** Личная репутация (от -100 до 100): торговля и боссы поднимают, убийство мирных и смерть снижают. */
    private int standing = 0;
    /** Сколько сделок уже засчитано в репутацию в игровые сутки tradeDay (не больше STANDING_TRADE_DAILY). */
    private long tradeDay = -1L;
    private int tradesToday = 0;
    /** Когда придёт следующая волна NPC (0 — волн нет). */
    private long nextWave = 0L;
    /** «Удачный день» у торговцев: в какие сутки бросали и какая скидка в процентах (0 — нет). */
    private long dealDay = -1L;
    private int dealPercent = 0;
    /** Привязанные квинке, которые не выпали при смерти: вернутся в инвентарь после возрождения. */
    private final ListTag keptItems = new ListTag();
    /** Кофейня: в какие игровые сутки гуль уже взял бесплатный кофе и бесплатное мясо. */
    private long cafeCoffeeDay = -1L;
    private long cafeFleshDay = -1L;
    /** Какудзя: накопленные очки (100 — форма открыта) и до какого тика форма действует. */
    private int kakujaPoints = 0;
    private long kakujaEndTick = 0L;
    /** Очки ранга: отдельно звание CCG и рейтинг гуля (при смене класса своё не теряется). */
    private int rankCcg = 0;
    private int rankGhoul = 0;
    /** Для меню «Профиль»: клан игрока (сервер заполняет перед отправкой) и откаты способностей. */
    private String clanNameCache = "";
    private int clanRepCache = 0;
    private final Map<String, Long> cooldownsCache = new HashMap<>();
    /** Агенту CCG уже выдана стартовая квинке Kura. */
    private boolean starterQuinque = false;
    /** Финальное задание агента: какие квинке уже получены (1 — Narukami, 2 — IXA). */
    private int finalQuinques = 0;
    /** Номер текущего задания в цепочке и прогресс по нему. */
    private int questIndex = 0;
    private int questProgress = 0;
    private final Set<String> skills = new LinkedHashSet<>();
    /** Сколько очков заплачено за навык (на сложной игре навыки дороже). Нужно, чтобы вернуть ровно столько. */
    private final Map<String, Integer> skillPaid = new HashMap<>();
    /** Прогресс заданий других цепочек: ключ — цепочка, значение — {номер задания, прогресс}. */
    private final Map<String, int[]> savedQuests = new HashMap<>();

    // Не сохраняется, но передаётся клиенту. Время в тиках мира.
    private boolean rageActive = false;
    private long rageStartTick = 0L;
    private long rageEndTick = 0L;
    private boolean blocking = false;
    private long lockEndTick = 0L;
    /** Особые ауры и «тишина» (время мира, до которого действуют). */
    private long retributionEndTick = 0L;
    private long charismaEndTick = 0L;
    private long silenceEndTick = 0L;
    /** «Бросок судьбы»: защита от урона (доля) и до какого времени. */
    private double fortuneShield = 0.0;
    private long fortuneShieldEndTick = 0L;
    /** Передача силы в клане (только для показа на экране): до какого тика, множитель силы, откат. */
    private long bondEndTick = 0L;
    private double bondPower = 0.0;
    private long bondCooldownEnd = 0L;

    // Только сервер
    private final CombatState combat = new CombatState();

    /** Данные игрока или null, если их нет. */
    public static PlayerData get(Player player) {
        return player.getCapability(CAP).resolve().orElse(null);
    }

    public CombatState combat() {
        return combat;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public KaguneVariant getKaguneVariant() {
        return kaguneVariant;
    }

    public void setKaguneVariant(KaguneVariant kaguneVariant) {
        this.kaguneVariant = kaguneVariant;
    }

    public boolean isKaguneActive() {
        return kaguneActive;
    }

    public void setKaguneActive(boolean kaguneActive) {
        this.kaguneActive = kaguneActive;
    }

    public int getRage() {
        return rage;
    }

    public void setRage(int rage) {
        this.rage = Mth.clamp(rage, 0, RAGE_MAX);
    }

    /** Добавляет очки ярости. Пока ярость действует, очки не копятся. Возвращает true, если шкала изменилась. */
    public boolean addRage(int points) {
        if (rageActive || points <= 0 || rage >= RAGE_MAX) {
            return false;
        }
        setRage(rage + points);
        return true;
    }

    public int getRageLevel() {
        return rageLevel;
    }

    public void setRageLevel(int rageLevel) {
        this.rageLevel = Mth.clamp(rageLevel, 0, com.tokyoghoulrpg.combat.Balance.RAGE_LEVEL_MAX);
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }

    public int getBloodPoints() {
        return bloodPoints;
    }

    public void setBloodPoints(int bloodPoints) {
        this.bloodPoints = Math.max(0, bloodPoints);
    }

    public void addBloodPoints(int points) {
        setBloodPoints(bloodPoints + points);
    }

    public boolean isStartBloodGiven() {
        return startBloodGiven;
    }

    public void setStartBloodGiven(boolean given) {
        this.startBloodGiven = given;
    }

    public String getContractId() {
        return contractId;
    }

    public int getContractProgress() {
        return contractProgress;
    }

    public void setContract(String id, int progress) {
        this.contractId = id;
        this.contractProgress = Math.max(0, progress);
    }

    public long getContractDoneDay() {
        return contractDoneDay;
    }

    public void setContractDoneDay(long day) {
        this.contractDoneDay = day;
    }

    public int getCraving() {
        return craving;
    }

    public void setCraving(int craving) {
        this.craving = Mth.clamp(craving, 0, 100);
    }

    public int getRepCcg() {
        return repCcg;
    }

    public void setRepCcg(int value) {
        this.repCcg = Mth.clamp(value, -100, 100);
    }

    public int getRepGhouls() {
        return repGhouls;
    }

    public void setRepGhouls(int value) {
        this.repGhouls = Mth.clamp(value, -100, 100);
    }

    /** Передача силы: power > 0 — получил силу, < 0 — отдал; endTick — когда закончится; cooldownEnd — откат. */
    public void setBond(long endTick, double power, long cooldownEnd) {
        this.bondEndTick = endTick;
        this.bondPower = power;
        this.bondCooldownEnd = cooldownEnd;
    }

    public long getBondEndTick() {
        return bondEndTick;
    }

    public double getBondPower() {
        return bondPower;
    }

    public long getBondCooldownEnd() {
        return bondCooldownEnd;
    }

    public int getStanding() {
        return standing;
    }

    public void setStanding(int value) {
        this.standing = Mth.clamp(value, -100, 100);
    }

    /** Засчитать сделку в репутацию: true, если в эти игровые сутки лимит ещё не исчерпан. */
    public boolean countTrade(long day, int dailyLimit) {
        if (day != tradeDay) {
            tradeDay = day;
            tradesToday = 0;
        }
        if (tradesToday >= dailyLimit) {
            return false;
        }
        tradesToday++;
        return true;
    }

    public void addKeptItem(CompoundTag item) {
        keptItems.add(item);
    }

    /** Забрать сохранённые при смерти предметы (список очищается). */
    public java.util.List<CompoundTag> takeKeptItems() {
        java.util.List<CompoundTag> list = new java.util.ArrayList<>();
        for (int i = 0; i < keptItems.size(); i++) {
            list.add(keptItems.getCompound(i));
        }
        keptItems.clear();
        return list;
    }

    public int getKakujaPoints() {
        return kakujaPoints;
    }

    public void setKakujaPoints(int points) {
        this.kakujaPoints = Math.max(0, Math.min(9999, points));
    }

    public long getKakujaEndTick() {
        return kakujaEndTick;
    }

    public void setKakujaEndTick(long tick) {
        this.kakujaEndTick = tick;
    }

    public boolean isKakujaActive(long now) {
        return kakujaEndTick > now;
    }

    public int getRankCcg() {
        return rankCcg;
    }

    public int getRankGhoul() {
        return rankGhoul;
    }

    public void setRankCcg(int points) {
        this.rankCcg = Math.max(0, points);
    }

    public void setRankGhoul(int points) {
        this.rankGhoul = Math.max(0, points);
    }

    public void setClanCache(String name, int reputation) {
        this.clanNameCache = name == null ? "" : name;
        this.clanRepCache = reputation;
    }

    public String getClanNameCache() {
        return clanNameCache;
    }

    public int getClanRepCache() {
        return clanRepCache;
    }

    /** Откаты способностей для меню «Профиль»: имя → тик, когда снова готово (только у клиента). */
    public Map<String, Long> getCooldownsCache() {
        return cooldownsCache;
    }

    public long getCafeCoffeeDay() {
        return cafeCoffeeDay;
    }

    public long getCafeFleshDay() {
        return cafeFleshDay;
    }

    public void setCafeCoffeeDay(long day) {
        this.cafeCoffeeDay = day;
    }

    public void setCafeFleshDay(long day) {
        this.cafeFleshDay = day;
    }

    public long getNextWave() {
        return nextWave;
    }

    public void setNextWave(long time) {
        this.nextWave = time;
    }

    public long getDealDay() {
        return dealDay;
    }

    /** Скидка «удачного дня» у торговца мода в процентах (0 — сегодня скидки нет). */
    public int getDealPercent() {
        return dealPercent;
    }

    public void setDeal(long day, int percent) {
        this.dealDay = day;
        this.dealPercent = Math.max(0, Math.min(90, percent));
    }

    public long getNextRaid() {
        return nextRaid;
    }

    public void setNextRaid(long time) {
        this.nextRaid = time;
    }

    public boolean isStarterQuinqueGiven() {
        return starterQuinque;
    }

    public void setStarterQuinqueGiven(boolean given) {
        this.starterQuinque = given;
    }

    public int getFinalQuinques() {
        return finalQuinques;
    }

    public void setFinalQuinques(int mask) {
        this.finalQuinques = mask & 3;
    }

    public int getQuestIndex() {
        return questIndex;
    }

    public void setQuestIndex(int questIndex) {
        this.questIndex = Math.max(0, questIndex);
    }

    public int getQuestProgress() {
        return questProgress;
    }

    public void setQuestProgress(int questProgress) {
        this.questProgress = Math.max(0, questProgress);
    }

    public int getMaxLevelRewarded() {
        return maxLevelRewarded;
    }

    public void setMaxLevelRewarded(int level) {
        this.maxLevelRewarded = Math.max(0, level);
    }

    public void addSkillPoints(int points) {
        setSkillPoints(skillPoints + points);
    }

    public boolean hasSkill(String id) {
        return skills.contains(id);
    }

    public void addSkill(String id) {
        skills.add(id);
    }

    /** Открыть навык, запомнив, сколько за него заплачено. */
    public void addSkill(String id, int paid) {
        skills.add(id);
        skillPaid.put(id, Math.max(0, paid));
    }

    /** Сколько вернуть за навык. Для навыков из старых версий мода — fallback (обычная цена). */
    public int paidFor(String id, int fallback) {
        return skillPaid.getOrDefault(id, fallback);
    }

    /** Уровень ветки навыков: номер последнего открытого навыка вида «ветка_номер». */
    public int skillLevel(String branch) {
        int level = 0;
        while (skills.contains(branch + "_" + (level + 1))) {
            level++;
        }
        return level;
    }

    public void removeSkill(String id) {
        skills.remove(id);
        skillPaid.remove(id);
    }

    /** Запомнить прогресс текущей цепочки заданий перед сменой класса. */
    public void stashQuest(String chain) {
        if (chain != null) {
            savedQuests.put(chain, new int[] {questIndex, questProgress});
        }
    }

    /** Продолжить цепочку нового класса с того места, где она была. */
    public void restoreQuest(String chain) {
        int[] saved = chain == null ? null : savedQuests.get(chain);
        questIndex = saved == null ? 0 : saved[0];
        questProgress = saved == null ? 0 : saved[1];
    }

    public Set<String> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public boolean isRageActive() {
        return rageActive;
    }

    public long getRageStartTick() {
        return rageStartTick;
    }

    public long getRageEndTick() {
        return rageEndTick;
    }

    public void setRageActive(boolean active, long startTick, long endTick) {
        this.rageActive = active;
        this.rageStartTick = startTick;
        this.rageEndTick = endTick;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public long getLockEndTick() {
        return lockEndTick;
    }

    public void setLockEndTick(long lockEndTick) {
        this.lockEndTick = lockEndTick;
    }

    public long getRetributionEndTick() {
        return retributionEndTick;
    }

    public void setRetributionEndTick(long tick) {
        this.retributionEndTick = tick;
    }

    public long getCharismaEndTick() {
        return charismaEndTick;
    }

    public void setCharismaEndTick(long tick) {
        this.charismaEndTick = tick;
    }

    public long getSilenceEndTick() {
        return silenceEndTick;
    }

    public void setSilenceEndTick(long tick) {
        this.silenceEndTick = tick;
    }

    public double getFortuneShield() {
        return fortuneShield;
    }

    public long getFortuneShieldEndTick() {
        return fortuneShieldEndTick;
    }

    public void setFortuneShield(double shield, long endTick) {
        this.fortuneShield = shield;
        this.fortuneShieldEndTick = endTick;
    }

    /** Защита «Броска судьбы» сейчас (0 — нет). */
    public double fortuneShieldAt(long now) {
        return fortuneShieldEndTick > now ? fortuneShield : 0.0;
    }

    /** Действует ли «Аура возмездия». */
    public boolean isRetributionActive(long now) {
        return retributionEndTick > now;
    }

    public boolean isCharismaActive(long now) {
        return charismaEndTick > now;
    }

    /** Под «тишиной»: способности недоступны, только обычные удары. */
    public boolean isSilencedAt(long now) {
        return silenceEndTick > now;
    }

    /** Оглушён или обездвижен в этот момент (для клиента: блокировать управление). */
    public boolean isLockedAt(long now) {
        return lockEndTick > now;
    }

    public boolean hasChosenClass() {
        return playerClass != PlayerClass.NONE;
    }

    public void reset() {
        playerClass = PlayerClass.NONE;
        kaguneVariant = KaguneVariant.NONE;
        kaguneActive = false;
        rage = 0;
        rageLevel = 0;
        skillPoints = 0;
        maxLevelRewarded = 0;
        bloodPoints = 0;
        startBloodGiven = false;
        contractId = null;
        contractProgress = 0;
        contractDoneDay = -1L;
        craving = 100;
        repCcg = 0;
        repGhouls = 0;
        nextRaid = 0L;
        standing = 0;
        tradeDay = -1L;
        tradesToday = 0;
        nextWave = 0L;
        dealDay = -1L;
        dealPercent = 0;
        keptItems.clear();
        cafeCoffeeDay = -1L;
        cafeFleshDay = -1L;
        kakujaPoints = 0;
        kakujaEndTick = 0L;
        rankCcg = 0;
        rankGhoul = 0;
        starterQuinque = false;
        finalQuinques = 0;
        questIndex = 0;
        questProgress = 0;
        savedQuests.clear();
        skills.clear();
        skillPaid.clear();
        rageActive = false;
        rageStartTick = 0L;
        rageEndTick = 0L;
        blocking = false;
        lockEndTick = 0L;
        retributionEndTick = 0L;
        charismaEndTick = 0L;
        silenceEndTick = 0L;
        fortuneShield = 0.0;
        fortuneShieldEndTick = 0L;
        bondEndTick = 0L;
        bondPower = 0.0;
        bondCooldownEnd = 0L;
        combat.reset();
    }

    /** Переносит только то, что сохраняется в мире (смерть, возвращение из Энда). */
    public void copyFrom(PlayerData other) {
        deserializeNBT(other.serializeNBT());
    }

    /** То, что сохраняется в мире. */
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("class", playerClass.id());
        tag.putString("kagune_variant", kaguneVariant.id());
        tag.putBoolean("kagune_active", kaguneActive);
        tag.putInt("rage", rage);
        tag.putInt("rage_level", rageLevel);
        tag.putInt("skill_points", skillPoints);
        tag.putInt("max_level_rewarded", maxLevelRewarded);
        tag.putInt("blood_points", bloodPoints);
        tag.putBoolean("start_blood_given", startBloodGiven);
        tag.putString("contract", contractId == null ? "" : contractId);
        tag.putInt("contract_progress", contractProgress);
        tag.putLong("contract_done_day", contractDoneDay);
        tag.putInt("craving", craving);
        tag.putInt("rep_ccg", repCcg);
        tag.putInt("rep_ghouls", repGhouls);
        tag.putLong("next_raid", nextRaid);
        tag.putInt("standing", standing);
        tag.putLong("trade_day", tradeDay);
        tag.putInt("trades_today", tradesToday);
        tag.putLong("next_wave", nextWave);
        tag.putLong("deal_day", dealDay);
        tag.putInt("deal_percent", dealPercent);
        tag.put("kept_items", keptItems.copy());
        tag.putLong("cafe_coffee_day", cafeCoffeeDay);
        tag.putLong("cafe_flesh_day", cafeFleshDay);
        tag.putInt("kakuja_points", kakujaPoints);
        tag.putInt("rank_ccg", rankCcg);
        tag.putInt("rank_ghoul", rankGhoul);
        tag.putBoolean("starter_quinque", starterQuinque);
        tag.putInt("final_quinques", finalQuinques);
        tag.putInt("quest_index", questIndex);
        tag.putInt("quest_progress", questProgress);
        CompoundTag saved = new CompoundTag();
        for (Map.Entry<String, int[]> entry : savedQuests.entrySet()) {
            saved.putIntArray(entry.getKey(), entry.getValue());
        }
        tag.put("saved_quests", saved);
        ListTag skillList = new ListTag();
        for (String skill : skills) {
            skillList.add(StringTag.valueOf(skill));
        }
        tag.put("skills", skillList);
        CompoundTag paid = new CompoundTag();
        for (Map.Entry<String, Integer> entry : skillPaid.entrySet()) {
            paid.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("skill_paid", paid);
        return tag;
    }

    /** То, что сохраняется, плюс текущее состояние боя: его сервер отправляет клиенту. */
    public CompoundTag toSyncTag() {
        CompoundTag tag = serializeNBT();
        tag.putBoolean("rage_active", rageActive);
        tag.putLong("rage_start", rageStartTick);
        tag.putLong("rage_end", rageEndTick);
        tag.putBoolean("blocking", blocking);
        tag.putLong("lock_end", lockEndTick);
        tag.putLong("retribution_end", retributionEndTick);
        tag.putLong("charisma_end", charismaEndTick);
        tag.putLong("silence_end", silenceEndTick);
        tag.putDouble("fortune_shield", fortuneShield);
        tag.putLong("fortune_end", fortuneShieldEndTick);
        tag.putLong("bond_end", bondEndTick);
        tag.putDouble("bond_power", bondPower);
        tag.putLong("bond_cooldown", bondCooldownEnd);
        tag.putLong("kakuja_end", kakujaEndTick);
        tag.putString("clan_name", clanNameCache);
        tag.putInt("clan_rep", clanRepCache);
        CompoundTag cd = new CompoundTag();
        cd.putLong("bite", combat.biteReady);
        cd.putLong("dash", combat.dashReady);
        cd.putLong("barrage", combat.barrageReady);
        cd.putLong("block", combat.blockReady);
        cd.putLong("grapple", combat.grappleReady);
        cd.putLong("pull", combat.pullReady);
        cd.putLong("strong", combat.strongReady);
        cd.putLong("human_strong", combat.humanStrongReady);
        cd.putLong("lunge", combat.lungeReady);
        cd.putLong("retribution", combat.retributionReady);
        cd.putLong("charisma", combat.charismaReady);
        cd.putLong("fortune", combat.fortuneReady);
        cd.putLong("kakuja", combat.kakujaReady);
        cd.putLong("bond", bondCooldownEnd);
        tag.put("cooldowns", cd);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        playerClass = PlayerClass.byId(tag.getString("class"));
        kaguneVariant = KaguneVariant.byId(tag.getString("kagune_variant"));
        kaguneActive = tag.getBoolean("kagune_active");
        setRage(tag.getInt("rage"));
        setRageLevel(tag.getInt("rage_level"));
        setSkillPoints(tag.getInt("skill_points"));
        setMaxLevelRewarded(tag.getInt("max_level_rewarded"));
        setBloodPoints(tag.getInt("blood_points"));
        startBloodGiven = tag.getBoolean("start_blood_given");
        String contract = tag.getString("contract");
        contractId = contract.isEmpty() ? null : contract;
        contractProgress = tag.getInt("contract_progress");
        contractDoneDay = tag.contains("contract_done_day") ? tag.getLong("contract_done_day") : -1L;
        setCraving(tag.contains("craving") ? tag.getInt("craving") : 100);
        setRepCcg(tag.getInt("rep_ccg"));
        setRepGhouls(tag.getInt("rep_ghouls"));
        nextRaid = tag.getLong("next_raid");
        setStanding(tag.getInt("standing"));
        tradeDay = tag.contains("trade_day") ? tag.getLong("trade_day") : -1L;
        tradesToday = tag.getInt("trades_today");
        nextWave = tag.getLong("next_wave");
        setDeal(tag.contains("deal_day") ? tag.getLong("deal_day") : -1L, tag.getInt("deal_percent"));
        keptItems.clear();
        ListTag kept = tag.getList("kept_items", Tag.TAG_COMPOUND);
        for (int i = 0; i < kept.size(); i++) {
            keptItems.add(kept.getCompound(i));
        }
        cafeCoffeeDay = tag.contains("cafe_coffee_day") ? tag.getLong("cafe_coffee_day") : -1L;
        cafeFleshDay = tag.contains("cafe_flesh_day") ? tag.getLong("cafe_flesh_day") : -1L;
        setKakujaPoints(tag.getInt("kakuja_points"));
        setRankCcg(tag.getInt("rank_ccg"));
        setRankGhoul(tag.getInt("rank_ghoul"));
        starterQuinque = tag.getBoolean("starter_quinque");
        setFinalQuinques(tag.getInt("final_quinques"));
        setQuestIndex(tag.getInt("quest_index"));
        setQuestProgress(tag.getInt("quest_progress"));
        savedQuests.clear();
        CompoundTag saved = tag.getCompound("saved_quests");
        for (String key : saved.getAllKeys()) {
            int[] value = saved.getIntArray(key);
            if (value.length == 2) {
                savedQuests.put(key, value);
            }
        }
        skills.clear();
        ListTag skillList = tag.getList("skills", Tag.TAG_STRING);
        for (int i = 0; i < skillList.size(); i++) {
            skills.add(skillList.getString(i));
        }
        skillPaid.clear();
        CompoundTag paid = tag.getCompound("skill_paid");
        for (String key : paid.getAllKeys()) {
            if (skills.contains(key)) {
                skillPaid.put(key, paid.getInt(key));
            }
        }
        rageActive = tag.getBoolean("rage_active");
        rageStartTick = tag.getLong("rage_start");
        rageEndTick = tag.getLong("rage_end");
        blocking = tag.getBoolean("blocking");
        lockEndTick = tag.getLong("lock_end");
        retributionEndTick = tag.getLong("retribution_end");
        charismaEndTick = tag.getLong("charisma_end");
        silenceEndTick = tag.getLong("silence_end");
        fortuneShield = tag.getDouble("fortune_shield");
        fortuneShieldEndTick = tag.getLong("fortune_end");
        bondEndTick = tag.getLong("bond_end");
        bondPower = tag.getDouble("bond_power");
        bondCooldownEnd = tag.getLong("bond_cooldown");
        kakujaEndTick = tag.getLong("kakuja_end");
        clanNameCache = tag.getString("clan_name");
        clanRepCache = tag.getInt("clan_rep");
        cooldownsCache.clear();
        CompoundTag cd = tag.getCompound("cooldowns");
        for (String key : cd.getAllKeys()) {
            cooldownsCache.put(key, cd.getLong(key));
        }
    }
}
