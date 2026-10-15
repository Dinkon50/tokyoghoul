package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.goal.ChargeStrikeGoal;
import com.tokyoghoulrpg.entity.goal.SpinSweepGoal;
import com.tokyoghoulrpg.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
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
 * Босс CCG «Старший следователь». Делает рывок с ударом и круговой удар (оба пробивают блок),
 * на половине здоровья зовёт двух агентов. Охотится на гулей так же, как агенты: полугулей не трогает,
 * остальных атакует, только если ударили его. Полоса здоровья наверху экрана.
 */
public class CcgBoss extends PathfinderMob implements BossMob {
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean summoned = false;
    private boolean equipped = false;

    public CcgBoss(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)          // второй по силе: больше Пожирателя (250)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    /** Свои: агенты CCG и сам босс. */
    private static boolean ally(LivingEntity entity) {
        return entity instanceof CcgAgentNpc || entity instanceof CcgBoss;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ChargeStrikeGoal(this, CcgBoss::ally));
        this.goalSelector.addGoal(1, new SpinSweepGoal(this, CcgBoss::ally));
        // больше атак: прыжок с ударом, электрический выпад квинке, удар молнии
        this.goalSelector.addGoal(1, new com.tokyoghoulrpg.entity.goal.LeapSlamGoal(this, CcgBoss::ally));
        this.goalSelector.addGoal(1, new com.tokyoghoulrpg.entity.goal.QuinqueThrustGoal(this, CcgBoss::ally));
        this.goalSelector.addGoal(1, new com.tokyoghoulrpg.entity.goal.ThunderStrikeGoal(this, CcgBoss::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CcgAgentNpc.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, GhoulBoss.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, GhoulNpc.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                CcgAgentNpc::isGhoulPlayer));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        equip();
        this.setPersistenceRequired();
        return result;
    }

    /** Квинке IXA в руках и форма. Вызывается и при появлении, и на первом тике (старые боссы с мечом тоже получат квинке). */
    private void equip() {
        if (!(this.getItemBySlot(EquipmentSlot.MAINHAND).getItem() instanceof com.tokyoghoulrpg.item.QuinqueItem)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.tokyoghoulrpg.registry.ModItems.QUINQUE_IXA.get()));
        }
        // квинке с него не падает: Narukami и IXA — награда за финальное задание агента
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.equipped = true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!equipped) {
            equip();
        }
        float fraction = this.getHealth() / this.getMaxHealth();
        this.bossEvent.setProgress(fraction);
        if (!summoned && fraction <= 0.5F) {
            summoned = true;
            summon();
        }
    }

    private void summon() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Effects.sound(this, SoundEvents.PLAYER_LEVELUP, 1.0F, 0.6F);
        for (int i = 0; i < 2; i++) {
            CcgAgentNpc agent = ModEntities.CCG_AGENT.get().create(level);
            if (agent == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            agent.moveTo(this.getX() + Math.cos(angle) * 2.5, this.getY(), this.getZ() + Math.sin(angle) * 2.5,
                    this.getYRot(), 0.0F);
            agent.finalizeSpawn(level, level.getCurrentDifficultyAt(agent.blockPosition()), MobSpawnType.MOB_SUMMONED,
                    null, null);
            agent.setTarget(this.getTarget());
            level.addFreshEntity(agent);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Summoned", summoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.summoned = tag.getBoolean("Summoned");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
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
