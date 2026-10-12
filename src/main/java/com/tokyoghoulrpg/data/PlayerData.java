package com.tokyoghoulrpg.data;

import com.tokyoghoulrpg.combat.CombatState;
import java.util.Collections;
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
    private final Set<String> skills = new LinkedHashSet<>();

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

    public void addSkillPoints(int points) {
        setSkillPoints(skillPoints + points);
    }

    public boolean hasSkill(String id) {
        return skills.contains(id);
    }

    public void addSkill(String id) {
        skills.add(id);
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
