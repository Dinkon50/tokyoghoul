package com.tokyoghoulrpg.entity;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Гуль (NPC). Сам нападает на людей: игроков-людей и игроков-агентов CCG, на NPC-агентов и торговцев.
 * Игроков-гулей и полугулей не трогает, пока они его не ударят; тогда отвечает и зовёт гулей рядом.
 * Появляется в темноте, как обычные монстры, но не горит на солнце. Носит одну из масок мода,
 * иногда плащ. Маска и одежда иногда выпадают.
 */
public class GhoulNpc extends Monster implements SkinVariant {
    /** Внешности: толстовка с капюшоном, кожаная куртка, девушка в чёрном пальто. */
    public static final int VARIANTS = 3;
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(GhoulNpc.class, EntityDataSerializers.INT);

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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, Mth.clamp(tag.getInt("SkinVariant"), 0, VARIANTS - 1));
    }

    public GhoulNpc(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Ударили: отвечает и зовёт гулей рядом. Так гули дерутся с гулями и полугулями,
        // только если те напали первыми.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(GhoulNpc.class));
        // Сами нападают на людей: игроков-людей и агентов CCG, NPC-агентов и торговцев
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                GhoulNpc::isHumanPlayer));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CcgAgentNpc.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, TraderNpc.class, true));
    }

    /**
     * На кого из игроков гули нападают сами: на людей и агентов CCG. С репутацией у гулей от 50
     * не трогают никого, а при репутации -30 и ниже нападают и на гулей с полугулями.
     */
    public static boolean isHumanPlayer(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass()) {
            return false;
        }
        int rep = data.getRepGhouls();
        if (rep >= Balance.REP_FRIENDLY) {
            return false;
        }
        if (rep <= Balance.REP_HOSTILE) {
            return true;
        }
        PlayerClass cls = data.getPlayerClass();
        return cls == PlayerClass.HUMAN || cls == PlayerClass.CCG_AGENT;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        RandomSource random = level.getRandom();
        this.entityData.set(VARIANT, random.nextInt(VARIANTS));
        ItemStack mask = new ItemStack(ModItems.MASKS.get(random.nextInt(ModItems.MASKS.size())).get());
        this.setItemSlot(EquipmentSlot.HEAD, mask);
        this.setDropChance(EquipmentSlot.HEAD, 0.15F);
        if (random.nextFloat() < 0.5F) {
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.GHOUL_COAT.get()));
            this.setDropChance(EquipmentSlot.CHEST, 0.08F);
        }
        return result;
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
