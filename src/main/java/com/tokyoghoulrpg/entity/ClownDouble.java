package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.style.StyleBalance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Двойник Клоуна из Пьеро: выглядит как он, но хрупкий и слабый (здоровье 20, удар 4).
 * Живёт 20 секунд, исчезает вместе с Клоуном. Ничего не роняет и не сохраняется в мире.
 */
public class ClownDouble extends Monster {
    private int ownerId = -1;

    public ClownDouble(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, StyleBalance.CLOWN_DOUBLE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, StyleBalance.CLOWN_DOUBLE_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public void setOwner(ClownBoss owner) {
        this.ownerId = owner.getId();
    }

    public boolean isOwnedBy(Entity entity) {
        return entity != null && entity.getId() == ownerId;
    }

    /** Свои у Клоуна: он сам и его двойники. */
    public static boolean ally(LivingEntity entity) {
        return entity instanceof ClownBoss || entity instanceof ClownDouble;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        Entity owner = this.level().getEntity(ownerId);
        if (!(owner instanceof ClownBoss boss) || !boss.isAlive() || this.tickCount >= StyleBalance.CLOWN_DOUBLE_TICKS) {
            vanish();
            return;
        }
        if (this.getTarget() == null && boss.getTarget() != null) {
            this.setTarget(boss.getTarget());
        }
    }

    public void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 16,
                    0.3, 0.6, 0.3, 0.02);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker && ally(attacker)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 20,
                    0.3, 0.6, 0.3, 0.02);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_CELEBRATE;
    }
}
