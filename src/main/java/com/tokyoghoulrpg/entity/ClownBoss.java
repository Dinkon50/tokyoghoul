package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.goal.KnifeFanGoal;
import com.tokyoghoulrpg.entity.goal.ShadowStrikeGoal;
import com.tokyoghoulrpg.entity.goal.SummonDoublesGoal;
import com.tokyoghoulrpg.style.FightStyle;
import com.tokyoghoulrpg.style.StyleBalance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
 * «Клоун из Пьеро» — босс стиля «Тень». Лёгкая броня, но его трудно поймать:
 * <ul>
 *   <li>уворачивается от каждого 4-го удара вблизи — отскок вбок в облаке дыма;</li>
 *   <li>«Удар из тени» — растворяется в дыму и через секунду появляется за спиной цели (там вспыхивает дым
 *       и слышен смех — успей обернуться или поставить блок), удар пробивает блок;</li>
 *   <li>«Веер ножей» — пять ножей веером, перед броском видны линии полёта;</li>
 *   <li>«Двойники» — двое хрупких двойников на 20 секунд.</li>
 *   <li>На половине здоровья — «Финальный акт»: быстрее, сильнее, атаки чаще.</li>
 * </ul>
 * Сила ~239 (tools/boss_power.py). Победа открывает стиль «Тень».
 */
public class ClownBoss extends StyleBossMob {
    /** Удары вблизи по нему подряд: каждый CLOWN_DODGE_EVERY-й — мимо. */
    private int closeHits = 0;

    public ClownBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, StyleBalance.CLOWN_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, StyleBalance.CLOWN_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, StyleBalance.CLOWN_ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public FightStyle style() {
        return FightStyle.SHADOW;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SummonDoublesGoal(this, ClownDouble::ally));
        this.goalSelector.addGoal(1, new ShadowStrikeGoal(this, ClownDouble::ally));
        this.goalSelector.addGoal(1, new KnifeFanGoal(this, ClownDouble::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ClownDouble.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    // ------------------------------------------------------------------ уворот

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && source.getDirectEntity() instanceof LivingEntity attacker && attacker.distanceTo(this) <= 5.0
                && BossCombat.canAct(this)) {
            if (ClownDouble.ally(attacker)) {
                return false;
            }
            closeHits++;
            if (closeHits >= StyleBalance.CLOWN_DODGE_EVERY && dodge(attacker)) {
                closeHits = 0;
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    /** Отскок вбок от удара (влево или вправо — где свободно). */
    private boolean dodge(LivingEntity attacker) {
        Vec3 to = this.position().subtract(attacker.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 side = new Vec3(-flat.z, 0.0, flat.x).normalize();
        Vec3 from = this.position();
        for (double sign : this.random.nextBoolean() ? new double[] {1.0, -1.0} : new double[] {-1.0, 1.0}) {
            Vec3 spot = from.add(side.scale(2.5 * sign)).add(flat.normalize().scale(0.8));
            this.setPos(spot.x, from.y, spot.z);
            if (this.level().noCollision(this) && !this.level().getBlockState(this.blockPosition().below()).isAir()) {
                this.teleportTo(spot.x, from.y, spot.z);
                smokePuff(from);
                Effects.sound(this, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
                Effects.sound(this, com.tokyoghoulrpg.registry.ModSounds.CLOWN_LAUGH.get(), 0.8F, 1.2F);
                return true;
            }
            this.setPos(from.x, from.y, from.z);
        }
        return false;
    }

    public void smokePuff(Vec3 at) {
        this.level().playSound(null, at.x, at.y, at.z, com.tokyoghoulrpg.registry.ModSounds.SMOKE.get(), this.getSoundSource(), 0.8F,
                0.9F + this.random.nextFloat() * 0.2F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.0, at.z, 24, 0.4, 0.7, 0.4, 0.02);
            level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1.0, at.z, 8, 0.3, 0.5, 0.3, 0.02);
        }
    }

    // ------------------------------------------------------------------ стадии и уход

    @Override
    protected void onSecondWind(ServerLevel level) {
        smokePuff(this.position());
        level.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.2, this.getZ(), 40, 0.8, 0.8, 0.8, 0.1);
        Effects.sound(this, com.tokyoghoulrpg.registry.ModSounds.CLOWN_LAUGH.get(), 1.6F, 0.8F);
    }

    @Override
    protected void onLeave() {
        removeDoubles();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide()) {
            removeDoubles();
        }
        super.remove(reason);
    }

    private void removeDoubles() {
        for (ClownDouble twin : this.level().getEntitiesOfClass(ClownDouble.class, this.getBoundingBox().inflate(48.0),
                d -> d.isOwnedBy(this))) {
            twin.vanish();
        }
    }

    /** Если мир сохранили посреди «Удара из тени» — не остаться невидимым навсегда. */
    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setInvisible(false);
    }

    // ------------------------------------------------------------------ звуки и удар

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.WITCH_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.85F;
    }

    /** Обычный удар: короткий нож, быстрый взмах. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            BossFx.slash(this, 2.0, 90.0, 5);
            Effects.sound(this, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
        }
        return hit;
    }
}
