package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.style.FightStyle;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Общее у боссов стилей боя (Мастер додзё, Клоун из Пьеро, Белый жнец): полоса здоровья,
 * «второе дыхание» на половине здоровья (урон +15%, особые атаки чаще, скорость +20%),
 * уход, если 30 секунд рядом нет игроков. Победа открывает стиль style() всем, кто рядом.
 * Тексты второй стадии — по имени существа: bossbar.tokyoghoulrpg.&lt;id&gt;.phase2 и message.tokyoghoulrpg.&lt;id&gt;.phase2.
 */
public abstract class StyleBossMob extends Monster implements BossMob {
    private static final UUID PHASE_DAMAGE_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e70");
    private static final UUID PHASE_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e71");
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(StyleBossMob.class, EntityDataSerializers.INT);
    private static final int LEAVE_TICKS = 30 * 20;

    protected final ServerBossEvent bossEvent;
    private int aloneTicks = 0;

    protected StyleBossMob(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 200;
        this.setPersistenceRequired();
    }

    /** Какой стиль открывает победа. */
    public abstract FightStyle style();

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE, 1);
    }

    protected String bossId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
    }

    // ------------------------------------------------------------------ стадии

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    @Override
    public float damageMultiplier() {
        return getPhase() >= 2 ? StyleBalance.BOSS_PHASE2_DAMAGE : 1.0F;
    }

    @Override
    public double cooldownMultiplier() {
        return getPhase() >= 2 ? StyleBalance.BOSS_PHASE2_COOLDOWN : 1.0;
    }

    /** Свой эффект перехода во вторую стадию (частицы, звук). */
    protected void onSecondWind(ServerLevel level) {
    }

    private void secondWind() {
        this.entityData.set(PHASE, 2);
        modifier(Attributes.ATTACK_DAMAGE, PHASE_DAMAGE_ID, StyleBalance.BOSS_PHASE2_DAMAGE - 1.0);
        modifier(Attributes.MOVEMENT_SPEED, PHASE_SPEED_ID, StyleBalance.BOSS_PHASE2_SPEED);
        this.bossEvent.setName(barName());
        Effects.sound(this, SoundEvents.RAVAGER_ROAR, 1.6F, 1.2F);
        if (this.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(this) < 48 * 48)) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg." + bossId() + ".phase2"), true);
            }
            onSecondWind(level);
        }
        BossFx.shockwave(this, this.position(), 6.0, 14);
        BossFx.shake(this, this.position(), 14.0, 1.2F, 10);
    }

    private void modifier(Attribute attribute, UUID id, double amount) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, "tg_style_boss_phase", amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    protected Component barName() {
        return getPhase() >= 2
                ? Component.translatable("bossbar.tokyoghoulrpg." + bossId() + ".phase2", this.getDisplayName())
                : this.getDisplayName();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        float fraction = this.getHealth() / this.getMaxHealth();
        this.bossEvent.setProgress(fraction);
        if (getPhase() == 1 && fraction <= 0.5F) {
            secondWind();
        }
        // поединок без соперника: через 30 секунд босс уходит
        if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel level) {
            boolean anyone = !level.getPlayers(p -> !p.isSpectator() && !p.isCreative()
                    && p.distanceToSqr(this) < 40 * 40).isEmpty();
            aloneTicks = anyone ? 0 : aloneTicks + 20;
            if (aloneTicks >= LEAVE_TICKS) {
                level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
                onLeave();
                this.discard();
            }
        }
    }

    /** Перед уходом (например, убрать своих двойников). */
    protected void onLeave() {
    }

    // ------------------------------------------------------------------ полоса и сохранение

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
        this.bossEvent.setName(barName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(PHASE, Math.max(1, tag.getInt("Phase")));
        this.bossEvent.setName(barName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
