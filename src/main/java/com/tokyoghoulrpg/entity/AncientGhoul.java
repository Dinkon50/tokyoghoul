package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.deep.DeepBalance;
import com.tokyoghoulrpg.difficulty.DifficultyScaling;
import com.tokyoghoulrpg.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * «Древний гуль» — хозяин Глубин 24-го района, босс самого большого данжа мода. Сидит на троне в логове.
 * <ul>
 *   <li>Две полные полосы здоровья по 1420 (как у Алого Монарха: вторая — после превращения), удар 32,
 *       броня 16 / 6. Все умения Кровавого полугуля, особые атаки x2.2; во второй полосе урон +25%,
 *       атаки чаще и шипы из земли. Сила ~600 (tools/boss_power.py).</li>
 *   <li>Пока у врат живы два стража глубин, его закрывает их кровь: он получает только 10% урона.</li>
 *   <li>Зовёт монстров глубин: на половине каждой полосы и в начале второй.</li>
 *   <li>Сильнее с каждым игроком на сервере; 30 секунд без игроков — бой сбрасывается.</li>
 * </ul>
 */
public class AncientGhoul extends HalfGhoulBoss implements DeepDweller {
    private boolean shielded = false;
    private int shieldCheck = 0;
    private int lastShieldMessage = -100;
    /** Какие призывы уже были: 1 — половина первой полосы, 2 — начало второй, 4 — половина второй. */
    private int summoned = 0;
    private int lastPhase = 1;

    public AncientGhoul(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createAncientAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DeepBalance.ANCIENT_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, DeepBalance.ANCIENT_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, DeepBalance.ANCIENT_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, DeepBalance.ANCIENT_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0);
    }

    @Override
    protected int maxPhase() {
        return 2;
    }

    /** Каждая стадия — полная полоса: вторая начинается, когда кончилась первая. */
    @Override
    protected float phaseThreshold() {
        return 0.0F;
    }

    @Override
    protected String phaseKey() {
        return "ancient_ghoul";
    }

    @Override
    protected BossEvent.BossBarColor baseColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected double phaseDamage() {
        return 1.0 + DeepBalance.ANCIENT_PHASE_DAMAGE * (getPhase() - 1);
    }

    @Override
    protected double phaseSpeed() {
        return DeepBalance.ANCIENT_PHASE_SPEED * (getPhase() - 1);
    }

    @Override
    protected boolean summonsMinions() {
        return false;
    }

    @Override
    public float damageMultiplier() {
        int players = DifficultyScaling.players(this.level());
        return DeepBalance.ANCIENT_SPECIAL_MULT * (float) phaseDamage()
                * (float) (1.0 + Balance.HG_PER_PLAYER_DAMAGE * (players - 1));
    }

    @Override
    public double cooldownMultiplier() {
        return DeepBalance.ANCIENT_COOLDOWN[Math.max(0, Math.min(1, getPhase() - 1))];
    }

    @Override
    public boolean isRespawned() {
        return false;
    }

    public boolean isShielded() {
        return shielded;
    }

    // ------------------------------------------------------------------ щит стражей

    private boolean guardiansAlive() {
        double r = DeepBalance.GUARDIAN_LINK_RADIUS;
        return !this.level().getEntitiesOfClass(DeepGuardian.class, this.getBoundingBox().inflate(r, 24.0, r),
                DeepGuardian::isAlive).isEmpty();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (shielded && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= DeepBalance.ANCIENT_SHIELD;
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.6, this.getZ(), 10,
                        0.7, 0.9, 0.7, 0.1);
            }
            if (source.getEntity() instanceof ServerPlayer player && this.tickCount - lastShieldMessage > 60) {
                lastShieldMessage = this.tickCount;
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ancient_ghoul.shielded"), true);
                Effects.sound(this, SoundEvents.SHIELD_BLOCK, 1.2F, 0.5F);
            }
        }
        return super.hurt(source, amount);
    }

    private void updateShield() {
        boolean now = guardiansAlive();
        if (now == shielded) {
            return;
        }
        shielded = now;
        if (!shielded) {
            // стражи пали: щит рушится
            Effects.sound(this, SoundEvents.WITHER_BREAK_BLOCK, 1.5F, 0.6F);
            BossFx.shockwave(this, this.position(), 8.0, 16);
            BossFx.shake(this, this.position(), 24.0, 1.4F, 12);
            for (ServerPlayer player : ((ServerLevel) this.level()).getPlayers(p -> p.distanceToSqr(this) < 80 * 80)) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ancient_ghoul.shield_broken"), true);
            }
        }
        refreshName();
    }

    private void refreshName() {
        Component base = getPhase() >= 2
                ? Component.translatable("bossbar.tokyoghoulrpg.ancient_ghoul.phase2", this.getDisplayName())
                : this.getDisplayName();
        this.bossEvent.setName(shielded
                ? Component.translatable("bossbar.tokyoghoulrpg.ancient_ghoul.shielded", base) : base);
    }

    // ------------------------------------------------------------------ тик

    @Override
    protected void customServerAiStep() {
        if (!hasArena()) {
            setArena(this.blockPosition());      // трон: отсюда далеко не уходит
        }
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        if (++shieldCheck >= 20) {
            shieldCheck = 0;
            updateShield();
        }
        if (getPhase() != lastPhase) {
            lastPhase = getPhase();
            refreshName();
        }
        if (shielded && this.tickCount % 10 == 0) {
            Effects.particles(this, ParticleTypes.ENCHANTED_HIT, 3, 0.8, 0.0);
        }
        if (isTransforming() || this.getHealth() <= 0.0F) {
            return;
        }
        float fraction = this.getHealth() / this.getMaxHealth();
        if (getPhase() == 1 && (summoned & 1) == 0 && fraction <= 0.5F) {
            summoned |= 1;
            summonDeep(3, 0);
        } else if (getPhase() >= 2 && (summoned & 2) == 0) {
            summoned |= 2;
            summonDeep(3, 1);
        } else if (getPhase() >= 2 && (summoned & 4) == 0 && fraction <= 0.5F) {
            summoned |= 4;
            summonDeep(3, 2);
        }
        // полное восстановление (сброс боя) — призывы снова доступны
        if (getPhase() == 1 && fraction >= 0.999F && (summoned & 6) != 0) {
            summoned = 0;
        }
    }

    /** Монстры глубин из-под земли вокруг трона: гули, мутанты и плеватели. */
    private void summonDeep(int ghouls, int brutes) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Effects.sound(this, SoundEvents.WARDEN_ROAR, 1.6F, 0.7F);
        int players = Math.max(1, DifficultyScaling.players(level));
        for (int i = 0; i < ghouls + brutes + (players - 1); i++) {
            Mob mob;
            if (i < brutes) {
                mob = ModEntities.DEEP_BRUTE.get().create(level);
            } else if (i % 3 == 2) {
                mob = ModEntities.DEEP_SPITTER.get().create(level);
            } else {
                mob = ModEntities.DEEP_GHOUL.get().create(level);
            }
            if (mob == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = 4.0 + this.random.nextDouble() * 4.0;
            mob.moveTo(this.getX() + Math.cos(angle) * dist, this.getY(), this.getZ() + Math.sin(angle) * dist,
                    this.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) {
                mob.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            mob.setTarget(this.getTarget());
            level.addFreshEntity(mob);
            BossFx.debris(this, mob.position(), 1.0, 20);
            Effects.particles(mob, Effects.BLOOD, 20, 0.5, 0.05);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DeepSummoned", summoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        summoned = tag.getInt("DeepSummoned");
    }
}
