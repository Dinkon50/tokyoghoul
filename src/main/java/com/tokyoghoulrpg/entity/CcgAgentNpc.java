package com.tokyoghoulrpg.entity;

import java.util.UUID;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Агент CCG (NPC). Нападает на гулей: NPC-гулей и игроков класса «Гуль».
 * Полугулей не трогает (как в твоей спеке). Людей и всех остальных атакует, только если они ударили
 * его самого или другого агента рядом. Носит форму CCG и меч (квинке появятся вместе с классом агента).
 */
public class CcgAgentNpc extends PathfinderMob implements SkinVariant {
    /** Внешности: в белом плаще, в сером плаще и очках, девушка-следователь. */
    public static final int VARIANTS = 3;
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(CcgAgentNpc.class, EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
    }

    @Override
    public int getSkinVariant() {
        return this.entityData.get(VARIANT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkinVariant", getSkinVariant());
        tag.putBoolean("Raider", raider);
        if (raidTarget != null) {
            tag.putUUID("RaidTarget", raidTarget);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, Mth.clamp(tag.getInt("SkinVariant"), 0, VARIANTS - 1));
        this.raider = tag.getBoolean("Raider");
        this.raidTarget = tag.hasUUID("RaidTarget") ? tag.getUUID("RaidTarget") : null;
    }

    public CcgAgentNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, 2.0);
    }

    /**
     * На кого из игроков агенты нападают сами: на гулей (полугулей не трогают). С репутацией у CCG
     * от 50 не трогают никого, а при -30 и ниже охотятся на любого игрока, даже на человека.
     */
    public static boolean isGhoulPlayer(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass()) {
            return false;
        }
        int rep = data.getRepCcg();
        if (rep >= Balance.REP_FRIENDLY) {
            return false;
        }
        if (rep <= Balance.REP_HOSTILE) {
            return true;
        }
        return data.getPlayerClass() == PlayerClass.GHOUL;
    }

    /** Агент из рейда: охотится на своего игрока и исчезает, когда тот далеко. */
    private boolean raider = false;
    private UUID raidTarget = null;

    public void startRaid(Player target) {
        this.raider = true;
        this.raidTarget = target.getUUID();
        this.setTarget(target);
    }

    private boolean isRaidTarget(LivingEntity entity) {
        return raider && raidTarget != null && entity.getUUID().equals(raidTarget);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Рейд: свою цель находят даже за стенами
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, 5, false, false,
                this::isRaidTarget));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CcgAgentNpc.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, GhoulNpc.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                CcgAgentNpc::isGhoulPlayer));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        this.entityData.set(VARIANT, level.getRandom().nextInt(VARIANTS));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CCG_COAT.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CCG_PANTS.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CCG_BOOTS.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.05F);
        }
        return result;
    }

    /** Агенты не исчезают, когда игрок уходит далеко. Кроме отрядов из рейда: те уходят. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return raider;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
