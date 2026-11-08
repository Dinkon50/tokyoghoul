package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Приманка стиля «Тень» («Подмена»): тёмная фигура на месте игрока. Враги рядом бросаются на неё.
 * Лопается от первого удара или когда выйдет время: облако дыма ослепляет и замедляет врагов вокруг.
 * Не сохраняется в мире.
 */
public class ShadowDecoy extends PathfinderMob {
    private UUID owner = null;
    private int life = StyleBalance.DECOY_TICKS;
    private boolean popped = false;

    public ShadowDecoy(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setup(Player owner, int life) {
        this.owner = owner.getUUID();
        this.life = life;
    }

    private ServerPlayer owner() {
        return owner != null && this.level().getPlayerByUUID(owner) instanceof ServerPlayer p ? p : null;
    }

    /** Враги, что охотятся за хозяином, переключаются на приманку. */
    public void lure() {
        ServerPlayer player = owner();
        if (player == null) {
            return;
        }
        double r = StyleBalance.DECOY_RADIUS;
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(r),
                m -> m != this && m.getTarget() == player)) {
            mob.setTarget(this);
        }
    }

    @Override
    protected void registerGoals() {
        // стоит на месте
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6),
                        0.0, 0.02, 0.0);
            }
            return;
        }
        if (owner() == null) {
            this.discard();
            return;
        }
        if (this.tickCount % 10 == 0) {
            lure();
        }
        if (this.tickCount >= life) {
            pop();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || source.getEntity() != null && source.getEntity().getUUID().equals(owner)) {
            return false;
        }
        if (this.tickCount > 2) {
            pop();
        }
        return false;
    }

    /** Лопнула: дым, слепота и замедление врагам рядом. */
    private void pop() {
        if (popped || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        popped = true;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.9, 0.8, 0.9, 0.03);
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.7, 0.6, 0.7, 0.03);
        Effects.sound(this, com.tokyoghoulrpg.registry.ModSounds.SMOKE.get(), 1.0F, 0.85F);
        ServerPlayer player = owner();
        double r = StyleBalance.DECOY_BLAST;
        for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r),
                e -> e != this && e != player && e.isAlive() && !(e instanceof Player)
                        && (e instanceof Mob mob && (mob.getTarget() == this || mob.getTarget() == player)))) {
            enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, StyleBalance.DECOY_BLIND, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, StyleBalance.DECOY_BLIND, 1));
            if (enemy instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
        this.discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }
}
