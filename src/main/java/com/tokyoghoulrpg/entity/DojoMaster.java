package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.goal.MasterFlurryGoal;
import com.tokyoghoulrpg.entity.goal.MasterGuardGoal;
import com.tokyoghoulrpg.entity.goal.PalmStrikeGoal;
import com.tokyoghoulrpg.style.FightStyle;
import com.tokyoghoulrpg.style.StyleBalance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * «Мастер додзё» — босс стиля «Боец». Появляется, когда игрок бросает вызов у наставника в додзё.
 * Дерётся только руками и ногами:
 * <ul>
 *   <li>«Шквал» — пять быстрых ударов перед собой, последний пробивает блок;</li>
 *   <li>«Удар ладонью» — рывок к цели издалека и толчок, отбрасывает далеко;</li>
 *   <li>«Стойка» — 2 секунды держит блок: удары спереди слабее на 80%, а кто ударит вблизи — получит ответ.
 *       Бить его в стойке лучше со спины или переждать.</li>
 *   <li>На половине здоровья — «Второе дыхание»: быстрее, сильнее, атаки чаще.</li>
 * </ul>
 * Сила посчитана tools/boss_power.py (~239, чуть выше середины боссов мода). Если 30 секунд рядом нет игроков,
 * уходит. Победа открывает стиль «Боец» всем, кто рядом.
 */
public class DojoMaster extends StyleBossMob {
    private static final EntityDataAccessor<Boolean> GUARDING =
            SynchedEntityData.defineId(DojoMaster.class, EntityDataSerializers.BOOLEAN);
    /** Кто ударил его в стойке вблизи: ему ответ. */
    private LivingEntity counterTarget = null;

    public DojoMaster(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, StyleBalance.MASTER_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, StyleBalance.MASTER_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, StyleBalance.MASTER_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, StyleBalance.MASTER_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, 0.6);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GUARDING, false);
    }

    /** Свои: только он сам (дуэль). */
    public static boolean ally(LivingEntity entity) {
        return entity instanceof DojoMaster;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MasterGuardGoal(this, DojoMaster::ally));
        this.goalSelector.addGoal(1, new MasterFlurryGoal(this, DojoMaster::ally));
        this.goalSelector.addGoal(1, new PalmStrikeGoal(this, DojoMaster::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    // ------------------------------------------------------------------ стойка

    public boolean isGuarding() {
        return this.entityData.get(GUARDING);
    }

    public void setGuarding(boolean guarding) {
        this.entityData.set(GUARDING, guarding);
        if (!guarding) {
            counterTarget = null;
        }
    }

    /** Кого бить в ответ (или null). Забирает цель: ответ один на стойку. */
    public LivingEntity takeCounterTarget() {
        LivingEntity target = counterTarget;
        counterTarget = null;
        return target;
    }

    public boolean hasCounterTarget() {
        return counterTarget != null;
    }

    /** Удар спереди (в пределах 65 градусов от взгляда). */
    private boolean fromFront(Vec3 from) {
        Vec3 to = from.subtract(this.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return true;
        }
        Vec3 look = Vec3.directionFromRotation(0.0F, this.getYRot());
        return flat.normalize().dot(look) > Math.cos(Math.toRadians(65.0));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isGuarding() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && source.getSourcePosition() != null
                && fromFront(source.getSourcePosition())) {
            amount *= (float) (1.0 - StyleBalance.MASTER_GUARD_REDUCTION);
            Effects.sound(this, SoundEvents.SHIELD_BLOCK, 1.0F, 0.6F + this.random.nextFloat() * 0.2F);
            if (this.level() instanceof ServerLevel level) {
                Vec3 at = this.position().add(this.getLookAngle().scale(0.6)).add(0.0, 1.6, 0.0);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.15);
            }
            if (counterTarget == null && source.getEntity() instanceof LivingEntity attacker
                    && attacker != this && attacker.distanceTo(this) <= 4.5) {
                counterTarget = attacker;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public FightStyle style() {
        return FightStyle.FIGHTER;
    }

    @Override
    protected void onSecondWind(ServerLevel level) {
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 40, 1.2, 0.1, 1.2, 0.08);
        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.4, this.getZ(), 30, 0.6, 0.8, 0.6, 0.3);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    /** Обычный удар: кулак, короткий взмах перед ним; удары чередуют руки. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.swing(this.random.nextBoolean() ? net.minecraft.world.InteractionHand.MAIN_HAND
                    : net.minecraft.world.InteractionHand.OFF_HAND);
            com.tokyoghoulrpg.combat.BossFx.slash(this, 2.2, 110.0, 6);
            Effects.sound(this, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
        }
        return hit;
    }
}
