package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.difficulty.DifficultyScaling;
import com.tokyoghoulrpg.entity.goal.BloodNovaGoal;
import com.tokyoghoulrpg.entity.goal.ChargeStrikeGoal;
import com.tokyoghoulrpg.entity.goal.KaguneSpearGoal;
import com.tokyoghoulrpg.entity.goal.LeapSlamGoal;
import com.tokyoghoulrpg.entity.goal.ShardVolleyGoal;
import com.tokyoghoulrpg.entity.goal.SpikeRainGoal;
import com.tokyoghoulrpg.entity.goal.SpinSweepGoal;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.world.ArenaAltarBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * «Кровавый полугуль» — самый сильный босс мода. Живёт на своей арене.
 * <ul>
 *   <li>Умеет всё, что Пожиратель (прыжок с ударом о землю) и Старший следователь (рывок, круговой удар),
 *       но втрое сильнее; плюс дальние атаки (залп осколков, копьё-кагуне) и атаки по области
 *       (кровавая волна, шипы из земли во второй стадии).</li>
 *   <li>Зовёт полугулей: на 75% здоровья, в начале второй стадии и на 66% и 33% во второй.</li>
 *   <li>Две стадии: когда снесли половину здоровья, 3 секунды неуязвим, восстанавливает всё здоровье
 *       и становится сильнее, быстрее и чаще делает особые атаки.</li>
 *   <li>Сильнее с каждым игроком на сервере (до пяти) и на нормальной и сложной сложности.</li>
 *   <li>Если 30 секунд рядом нет игроков, бой сбрасывается: босс снова целый и в первой стадии.</li>
 *   <li>Урон каждого игрока записывается: награду получают двое, нанёсшие больше всего (BossRewards).</li>
 * </ul>
 */
public class HalfGhoulBoss extends Monster implements BossMob {
    private static final UUID PLAYERS_HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e50");
    private static final UUID DAMAGE_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e51");
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e52");
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(HalfGhoulBoss.class, EntityDataSerializers.INT);

    protected final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    /** Урон по боссу от каждого игрока за весь бой (обе стадии). */
    private final Map<UUID, Float> damageBy = new HashMap<>();
    private BlockPos arena = null;
    private boolean respawned = false;
    private int summonStage = 0;
    private int transformTicks = 0;
    private int aloneTicks = 0;
    private int players = 1;

    public HalfGhoulBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Balance.HG_BOSS_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Balance.HG_BOSS_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE, 1);
    }

    /** 1 — первая стадия, 2 — вторая (после восстановления), у Алого Монарха есть и третья. */
    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    // ------------------------------------------------------------------ то, что меняет Алый Монарх

    /** Сколько стадий у босса. */
    protected int maxPhase() {
        return 2;
    }

    /** При какой доле здоровья переходит в следующую стадию (0 — только когда здоровье кончилось). */
    protected float phaseThreshold() {
        return 0.5F;
    }

    /** Часть ключей перевода: bossbar.tokyoghoulrpg.<ключ>.phase2 и message.tokyoghoulrpg.<ключ>.phase2. */
    protected String phaseKey() {
        return "half_ghoul_boss";
    }

    protected BossEvent.BossBarColor baseColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    /** Во сколько раз сильнее обычный удар в этой стадии. */
    protected double phaseDamage() {
        return getPhase() >= 2 ? Balance.HG_PHASE2_DAMAGE : 1.0;
    }

    /** Прибавка к скорости в этой стадии. */
    protected double phaseSpeed() {
        return getPhase() >= 2 ? Balance.HG_PHASE2_SPEED : 0.0;
    }

    /** Ключ строки полосы здоровья для стадии (2, 3 ...). */
    private Component phaseBarName() {
        return Component.translatable("bossbar.tokyoghoulrpg." + phaseKey() + ".phase" + getPhase(), this.getDisplayName());
    }

    /** Смертельный удар: если есть ещё стадии — не умирает, а переходит в следующую. */
    public boolean preventDeath() {
        if (getPhase() >= maxPhase()) {
            return false;
        }
        this.setHealth(1.0F);
        nextPhase();
        return true;
    }

    public boolean isTransforming() {
        return transformTicks > 0;
    }

    /** Свои: полугули и сам босс. Его атаки их не задевают. */
    public static boolean ally(LivingEntity entity) {
        return entity instanceof HalfGhoulNpc || entity instanceof HalfGhoulBoss;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // умения Пожирателя и Старшего следователя
        this.goalSelector.addGoal(1, new LeapSlamGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new ChargeStrikeGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new SpinSweepGoal(this, HalfGhoulBoss::ally));
        // свои: дальние атаки и атаки по области
        this.goalSelector.addGoal(1, new ShardVolleyGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new KaguneSpearGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new BloodNovaGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new SpikeRainGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    // ------------------------------------------------------------------ сила

    @Override
    public float damageMultiplier() {
        return Balance.HG_BOSS_SPECIAL_MULT * (float) phaseDamage()
                * (float) (1.0 + Balance.HG_PER_PLAYER_DAMAGE * (players - 1));
    }

    @Override
    public double cooldownMultiplier() {
        return getPhase() >= 2 ? Balance.HG_PHASE2_COOLDOWN : 1.0;
    }

    @Override
    public boolean canUseSpecials() {
        return transformTicks <= 0;
    }

    /** Пересчитать силу от числа игроков и стадии. Здоровье сохраняет долю: раненый остаётся раненым. */
    private void refreshScaling() {
        players = DifficultyScaling.players(this.level());
        float oldMax = this.getMaxHealth();
        float health = this.getHealth();
        set(Attributes.MAX_HEALTH, PLAYERS_HEALTH_ID, Balance.HG_PER_PLAYER_HEALTH * (players - 1));
        float newMax = this.getMaxHealth();
        if (Math.abs(newMax - oldMax) > 1.0E-3F && oldMax > 0.0F && this.isAlive()) {
            this.setHealth(Math.max(1.0F, health * newMax / oldMax));
        }
        double damage = phaseDamage() * (1.0 + Balance.HG_PER_PLAYER_DAMAGE * (players - 1));
        set(Attributes.ATTACK_DAMAGE, DAMAGE_ID, damage - 1.0);
        set(Attributes.MOVEMENT_SPEED, SPEED_ID, phaseSpeed());
    }

    private void set(net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, double amount) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 1.0E-4) {
                return;
            }
            instance.removeModifier(id);
        }
        if (Math.abs(amount) >= 1.0E-4) {
            instance.addPermanentModifier(new AttributeModifier(id, "tg_half_ghoul_boss", amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ------------------------------------------------------------------ тик

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        this.setPersistenceRequired();
        refreshScaling();
        this.setHealth(this.getMaxHealth());
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (transformTicks > 0) {
            transform();
        }
        if (this.tickCount % 100 == 0) {
            refreshScaling();
        }
        if (this.tickCount % 20 == 0) {
            keepToArena();
        }
        float fraction = this.getHealth() / this.getMaxHealth();
        this.bossEvent.setProgress(transformTicks > 0 ? 1.0F - (float) transformTicks / Balance.HG_TRANSFORM_TICKS : fraction);
        if (transformTicks > 0) {
            return;
        }
        if (getPhase() < maxPhase() && phaseThreshold() > 0.0F && fraction <= phaseThreshold()) {
            nextPhase();
            return;
        }
        // свита: 75% в первой стадии, 66% и 33% во второй (начало второй стадии — отдельно)
        if (getPhase() == 1 && summonStage == 0 && fraction <= 0.75F) {
            summonStage = 1;
            summon();
        } else if (getPhase() >= 2 && summonStage == 2 && fraction <= 0.66F) {
            summonStage = 3;
            summon();
        } else if (getPhase() >= 2 && summonStage == 3 && fraction <= 0.33F) {
            summonStage = 4;
            summon();
        }
        if (getPhase() >= 2 && this.tickCount % 4 == 0) {
            Effects.particles(this, Effects.RAGE, 5, 0.6, 0.02);
        }
    }

    /** Следующая стадия: превращение, полное восстановление, сильнее. */
    public void nextPhase() {
        if (getPhase() >= maxPhase()) {
            return;
        }
        this.entityData.set(PHASE, getPhase() + 1);
        transformTicks = Balance.HG_TRANSFORM_TICKS;
        summonStage = 2;                    // во второй и следующих стадиях снова зовёт свиту на 66% и 33%
        this.getNavigation().stop();
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setName(phaseBarName());
        refreshScaling();
        Effects.sound(this, SoundEvents.WITHER_SPAWN, 1.5F, 0.8F);
        for (Player player : nearbyPlayers(48.0)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg." + phaseKey() + ".phase" + getPhase()), true);
        }
        summon();
    }

    /** Совместимость: старое имя перехода во вторую стадию. */
    public void startPhase2() {
        nextPhase();
    }

    /** Превращение: стоит на месте, неуязвим, здоровье восполняется до конца. */
    private void transform() {
        transformTicks--;
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
        float step = this.getMaxHealth() / Balance.HG_TRANSFORM_TICKS;
        // напрямую, а не heal(): «Подавитель RC» не должен мешать превращению
        this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + step));
        if (this.level() instanceof ServerLevel level) {
            double angle = transformTicks * 0.6;
            for (int i = 0; i < 3; i++) {
                double a = angle + i * Math.PI * 2 / 3;
                level.sendParticles(Effects.RAGE, this.getX() + Math.cos(a) * 1.4, this.getY() + 0.2 + (Balance.HG_TRANSFORM_TICKS - transformTicks) * 0.05,
                        this.getZ() + Math.sin(a) * 1.4, 2, 0.05, 0.05, 0.05, 0.0);
            }
        }
        if (transformTicks == 0) {
            this.setHealth(this.getMaxHealth());
            Effects.sound(this, SoundEvents.WARDEN_ROAR, 1.5F, 0.9F);
            Effects.particles(this, Effects.RAGE, 80, 1.2, 0.2);
        }
    }

    /** Арена: далеко ушёл — вернуться. Нет игроков 30 секунд — бой сбрасывается. */
    private void keepToArena() {
        if (arena != null) {
            Vec3 center = Vec3.atBottomCenterOf(arena).add(0.0, 1.0, 0.0);
            this.restrictTo(arena, (int) Balance.HG_ARENA_RADIUS - 2);
            if (this.position().distanceToSqr(center) > 40.0 * 40.0) {
                this.teleportTo(center.x, center.y, center.z);
                this.setTarget(null);
            }
        }
        // игроки рядом (творческий режим тоже считается, чтобы бой не сбрасывался при проверке)
        if (this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0),
                p -> p.isAlive() && !p.isSpectator()).isEmpty()) {
            aloneTicks += 20;
            if (aloneTicks >= Balance.HG_RESET_TICKS) {
                resetFight();
            }
        } else {
            aloneTicks = 0;
        }
    }

    private void resetFight() {
        aloneTicks = 0;
        boolean changed = getPhase() != 1 || this.getHealth() < this.getMaxHealth() || !damageBy.isEmpty();
        if (!changed) {
            return;
        }
        this.entityData.set(PHASE, 1);
        transformTicks = 0;
        summonStage = 0;
        damageBy.clear();
        this.bossEvent.setColor(baseColor());
        this.bossEvent.setDarkenScreen(false);
        this.bossEvent.setName(this.getDisplayName());
        refreshScaling();
        this.setHealth(this.getMaxHealth());
        this.setTarget(null);
        if (arena != null) {
            Vec3 center = Vec3.atBottomCenterOf(arena).add(0.0, 1.0, 0.0);
            this.teleportTo(center.x, center.y, center.z);
        }
    }

    public List<Player> nearbyPlayers(double radius) {
        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
    }

    /** Позвать полугулей: двое плюс по одному за каждого игрока сверх первого, всего не больше 8. */
    private void summon() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int alive = level.getEntitiesOfClass(HalfGhoulNpc.class, this.getBoundingBox().inflate(48.0),
                m -> this.getUUID().equals(m.getOwnerId())).size();
        int count = Math.min(2 + (players - 1), Balance.HG_MINION_LIMIT - alive);
        if (count <= 0) {
            return;
        }
        Effects.sound(this, SoundEvents.RAVAGER_ROAR, 1.5F, 0.6F);
        for (int i = 0; i < count; i++) {
            HalfGhoulNpc minion = ModEntities.HALF_GHOUL.get().create(level);
            if (minion == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = 2.5 + this.random.nextDouble() * 2.0;
            minion.moveTo(this.getX() + Math.cos(angle) * dist, this.getY(), this.getZ() + Math.sin(angle) * dist,
                    this.getYRot(), 0.0F);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(minion.blockPosition()), MobSpawnType.MOB_SUMMONED,
                    null, null);
            minion.setOwner(this.getUUID());
            minion.setTarget(this.getTarget());
            level.addFreshEntity(minion);
            Effects.particles(minion, Effects.BLOOD, 25, 0.5, 0.05);
        }
    }

    // ------------------------------------------------------------------ урон

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // во время превращения неуязвим (кроме /kill и пустоты)
        if (transformTicks > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float before = this.getHealth();
        super.actuallyHurt(source, amount);
        float dealt = before - Math.max(0.0F, this.getHealth());
        if (dealt > 0.0F && source.getEntity() instanceof ServerPlayer player) {
            damageBy.merge(player.getUUID(), dealt, Float::sum);
        }
    }

    /** Кто сколько нанёс, от большего к меньшему. В списке только те, кто нанёс хоть немного урона. */
    public List<Map.Entry<UUID, Float>> damageRanking() {
        List<Map.Entry<UUID, Float>> list = new ArrayList<>(damageBy.entrySet());
        list.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        return list;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ арена

    public void setArena(BlockPos pos) {
        this.arena = pos;
        if (pos != null) {
            this.restrictTo(pos, (int) Balance.HG_ARENA_RADIUS - 2);
        }
    }

    /** Возрождённый на арене босс (второй и следующие): за него дают печать сброса навыков. */
    public boolean isRespawned() {
        return respawned;
    }

    public void setRespawned(boolean respawned) {
        this.respawned = respawned;
    }

    /** Босс убит: сообщить арене, чтобы пошёл таймер возрождения. */
    public void onDefeated() {
        if (arena != null && this.level().isLoaded(arena)) {
            BlockEntity be = this.level().getBlockEntity(arena);
            if (be instanceof ArenaAltarBlockEntity altar) {
                altar.onBossKilled(this);
            }
        }
    }

    // ------------------------------------------------------------------ полоса здоровья и сохранение

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
        tag.putInt("Phase", getPhase());
        tag.putInt("SummonStage", summonStage);
        tag.putBoolean("Respawned", respawned);
        if (arena != null) {
            tag.putIntArray("Arena", new int[] {arena.getX(), arena.getY(), arena.getZ()});
        }
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Float> entry : damageBy.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putUUID("Player", entry.getKey());
            e.putFloat("Damage", entry.getValue());
            list.add(e);
        }
        tag.put("DamageBy", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(PHASE, Math.max(1, Math.min(maxPhase(), tag.getInt("Phase"))));
        this.summonStage = tag.getInt("SummonStage");
        this.respawned = tag.getBoolean("Respawned");
        int[] a = tag.getIntArray("Arena");
        setArena(a.length == 3 ? new BlockPos(a[0], a[1], a[2]) : null);
        damageBy.clear();
        ListTag list = tag.getList("DamageBy", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            damageBy.put(e.getUUID("Player"), e.getFloat("Damage"));
        }
        if (getPhase() >= 2) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.bossEvent.setDarkenScreen(true);
            this.bossEvent.setName(phaseBarName());
        } else if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
