package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Подопытный — неудачный эксперимент из заброшенной лаборатории RC (оригинальный персонаж).
 * Разум потерян: нападает на любого игрока (любой класс, репутация не помогает) и на агентов CCG.
 * Быстрый, прыгает на цель, удар иногда вызывает «RC-заражение» (слабость и голод на 4 с).
 * Считается гулем: по нему работают навыки CCG против гулей. Живёт в лабораториях, сам в мире не появляется.
 * Иногда роняет записи лаборатории (очки навыков).
 */
public class TestSubjectNpc extends Monster implements SkinVariant {
    /** Внешности: больничная рубашка с ремнями и смирительная куртка. */
    public static final int VARIANTS = 2;
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(TestSubjectNpc.class, EntityDataSerializers.INT);

    public TestSubjectNpc(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Balance.SUBJECT_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Balance.SUBJECT_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, Balance.SUBJECT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, 2.0);
    }

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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(TestSubjectNpc.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CcgAgentNpc.class, 10, true, false, e -> true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        this.entityData.set(VARIANT, level.getRandom().nextInt(VARIANTS));
        return result;
    }

    /** Удар: иногда «RC-заражение». */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && this.random.nextFloat() < Balance.SUBJECT_INFECT_CHANCE) {
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Balance.SUBJECT_INFECT_TICKS, 0));
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, Balance.SUBJECT_INFECT_TICKS, 1));
            Effects.particles(living, Effects.RC, 12, 0.4, 0.02);
        }
        return hit;
    }

    /** Время от времени из ран сочится RC-клетки (бирюзовые искры). */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 30 == 0 && this.level() instanceof ServerLevel level) {
            level.sendParticles(Effects.RC, this.getX(), this.getY() + 1.2, this.getZ(), 2, 0.25, 0.4, 0.25, 0.0);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit && this.random.nextFloat() < Balance.SUBJECT_NOTES_CHANCE + 0.02F * looting) {
            this.spawnAtLocation(ModItems.LAB_NOTES.get());
        }
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

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.75F + this.random.nextFloat() * 0.2F;
    }
}
