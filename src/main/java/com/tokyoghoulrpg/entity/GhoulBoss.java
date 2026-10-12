package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.goal.LeapSlamGoal;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.registry.ModItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
 * Босс-гуль «Пожиратель». Прыгает на цель и бьёт о землю (урон всем вокруг, пробивает блок),
 * на половине и четверти здоровья зовёт двух гулей, ниже 30% здоровья впадает в бешенство:
 * быстрее и сильнее. Нападает на тех же, что и обычные гули. Полоса здоровья наверху экрана.
 */
public class GhoulBoss extends Monster implements BossMob {
    private static final UUID ENRAGE_SPEED = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e20");
    private static final UUID ENRAGE_DAMAGE = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e21");

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int summonsDone = 0;
    private boolean wild = false;
    private boolean equipped = false;

    public GhoulBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    /** Свои: гули и сам Пожиратель. Их особые атаки не задевают. */
    private static boolean ally(LivingEntity entity) {
        return entity instanceof GhoulNpc || entity instanceof GhoulBoss;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapSlamGoal(this, GhoulBoss::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(GhoulNpc.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                GhoulNpc::isHumanPlayer));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CcgAgentNpc.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CcgBoss.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, TraderNpc.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        equip();
        if (reason == MobSpawnType.NATURAL) {
            this.wild = true;                                   // бродячий босс может исчезнуть вдали от игроков
        } else {
            this.setPersistenceRequired();                      // из постройки, яйца или команды — остаётся
        }
        return result;
    }

    /** Маска-трофей. Вызывается и при появлении, и на первом тике: босс из постройки тоже будет в маске. */
    private void equip() {
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MASK_ONI.get()));
        }
        this.setDropChance(EquipmentSlot.HEAD, 1.0F);
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
        // зовёт гулей на половине и четверти здоровья
        if ((summonsDone == 0 && fraction <= 0.5F) || (summonsDone == 1 && fraction <= 0.25F)) {
            summonsDone++;
            summon();
        }
        // бешенство ниже 30%
        if (fraction <= 0.3F) {
            enrage(this.getAttribute(Attributes.MOVEMENT_SPEED), ENRAGE_SPEED, 0.3);
            enrage(this.getAttribute(Attributes.ATTACK_DAMAGE), ENRAGE_DAMAGE, 0.2);
            if (this.tickCount % 5 == 0) {
                Effects.particles(this, Effects.RAGE, 6, 0.5, 0.02);
            }
        }
    }

    private static void enrage(AttributeInstance attribute, UUID id, double amount) {
        if (attribute != null && attribute.getModifier(id) == null) {
            attribute.addTransientModifier(new AttributeModifier(id, "tg_boss_enrage", amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private void summon() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Effects.sound(this, SoundEvents.RAVAGER_ROAR, 1.5F, 0.8F);
        for (int i = 0; i < 2; i++) {
            GhoulNpc ghoul = ModEntities.GHOUL.get().create(level);
            if (ghoul == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            ghoul.moveTo(this.getX() + Math.cos(angle) * 2.5, this.getY(), this.getZ() + Math.sin(angle) * 2.5,
                    this.getYRot(), 0.0F);
            ghoul.finalizeSpawn(level, level.getCurrentDifficultyAt(ghoul.blockPosition()), MobSpawnType.MOB_SUMMONED,
                    null, null);
            ghoul.setTarget(this.getTarget());
            level.addFreshEntity(ghoul);
            Effects.particles(ghoul, Effects.BLOOD, 20, 0.5, 0.05);
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
        tag.putInt("SummonsDone", summonsDone);
        tag.putBoolean("Wild", wild);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.summonsDone = tag.getInt("SummonsDone");
        this.wild = tag.getBoolean("Wild");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    /** Босс из постройки или яйца не исчезает. Бродячий ведёт себя как обычный монстр. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return wild;
    }

    /** Ночью в мире появляется редко: из восьми подходящих попыток проходит одна. */
    public static boolean checkBossSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level,
                                              MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                && (reason != MobSpawnType.NATURAL || random.nextInt(8) == 0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
