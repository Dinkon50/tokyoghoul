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
    /** Номер текущего задания в цепочке и прогресс по нему. */
    private int questIndex = 0;
    private int questProgress = 0;
    private final Set<String> skills = new LinkedHashSet<>();
    /** Прогресс заданий других цепочек: ключ — цепочка, значение — {номер задания, прогресс}. */
    private final Map<String, int[]> savedQuests = new HashMap<>();

    // Не сохраняется, но передаётся клиенту. Время в тиках мира.
    private boolean rageActive = false;
    private long rageStartTick = 0L;
    private long rageEndTick = 0L;
    private boolean blocking = false;
    private long lockEndTick = 0L;

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
        questIndex = 0;
        questProgress = 0;
        savedQuests.clear();
        skills.clear();
        rageActive = false;
        rageStartTick = 0L;
        rageEndTick = 0L;
        blocking = false;
        lockEndTick = 0L;
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
        rageActive = tag.getBoolean("rage_active");
        rageStartTick = tag.getLong("rage_start");
        rageEndTick = tag.getLong("rage_end");
        blocking = tag.getBoolean("blocking");
        lockEndTick = tag.getLong("lock_end");
    }
}
