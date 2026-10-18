package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.clan.ClanAlliances;
import com.tokyoghoulrpg.clan.ClanData;
import com.tokyoghoulrpg.clan.ClanGuards;
import com.tokyoghoulrpg.clan.ClanTerritory;
import com.tokyoghoulrpg.clan.ClanWars;
import com.tokyoghoulrpg.combat.Balance;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Страж территории клана (оригинальный персонаж). Нанимается главой за кровь из казны.
 * <ul>
 *   <li>живёт на своей территории и не уходит с неё: забрёл за край — возвращается к знамени;</li>
 *   <li>бьёт врагов клана во время войны (и врагов союзников по войне), тех, кто недавно ранил
 *       участника клана или стража, и враждебных мобов на территории;</li>
 *   <li>свой клан и союзников не трогает, они не могут его ранить;</li>
 *   <li>территорию потеряли (захват, продажа, знамя сломано) — страж уходит.</li>
 * </ul>
 */
public class GuardNpc extends PathfinderMob implements SkinVariant {
    private UUID clanId;
    private String key = "";

    public GuardNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Balance.GUARD_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Balance.GUARD_DAMAGE)
                .add(Attributes.ARMOR, Balance.GUARD_ARMOR)
                .add(Attributes.MOVEMENT_SPEED, Balance.GUARD_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public void setup(UUID clanId, String key, BlockPos home, int radius) {
        this.clanId = clanId;
        this.key = key;
        this.restrictTo(home, Math.max(4, radius));
    }

    public UUID clanId() {
        return clanId;
    }

    public String territoryKey() {
        return key;
    }

    @Override
    public int getSkinVariant() {
        return 0;
    }

    private ClanData.Clan clan() {
        if (clanId == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        return ClanData.get(level.getServer()).byId(clanId);
    }

    /** Свой: участник клана или союзника, страж своего или союзного клана. */
    public boolean friendly(LivingEntity entity) {
        if (entity == null || clanId == null || !(this.level() instanceof ServerLevel level)) {
            return false;
        }
        ClanData data = ClanData.get(level.getServer());
        UUID other = null;
        if (entity instanceof GuardNpc guard) {
            other = guard.clanId;
        } else if (entity instanceof ServerPlayer player) {
            ClanData.Clan c = data.clanOf(player.getUUID());
            other = c == null ? null : c.id;
        }
        if (other == null) {
            return false;
        }
        return other.equals(clanId) || ClanAlliances.allied(data.byId(clanId), data.byId(other));
    }

    /** Враг ли это сейчас для стража. */
    private boolean hostile(ClanData data, ClanData.Clan clan, LivingEntity entity) {
        if (!entity.isAlive() || friendly(entity)) {
            return false;
        }
        if (entity == this.getLastHurtByMob() && this.tickCount - this.getLastHurtByMobTimestamp() < 200) {
            return !(entity instanceof Player p) || !p.isCreative() && !p.isSpectator();
        }
        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            if (ClanGuards.grudge(clan.id, player.getUUID())) {
                return true;
            }
            ClanData.Clan theirs = data.clanOf(player.getUUID());
            return theirs != null && ClanWars.enemies(data, clan.id, theirs.id);
        }
        if (entity instanceof GuardNpc guard) {
            return guard.clanId != null && ClanWars.enemies(data, clan.id, guard.clanId);
        }
        return entity instanceof Enemy;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
    }

    /** Раз в полсекунды: на месте ли, кого бить, лечиться. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 10 != 0 || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        ClanData data = ClanData.get(level.getServer());
        ClanData.Clan clan = clan();
        if (clan == null || !ClanTerritory.flags(clan).contains(key) || !ClanTerritory.dim(level).equals(ClanTerritory.dimOf(key))) {
            leave(level);
            return;
        }
        BlockPos home = BlockPos.of(ClanTerritory.posOf(key));
        int size = ClanTerritory.size(clan, key);
        if (!this.hasRestriction() || !this.getRestrictCenter().equals(home)) {
            this.restrictTo(home, Math.max(4, size / 2));
        }
        boolean insideNow = ClanTerritory.insideKey(clan, key, level, this.getX(), this.getZ());
        LivingEntity target = this.getTarget();
        if (target != null && (!hostile(data, clan, target)
                || !ClanTerritory.insideKey(clan, key, level, target.getX(), target.getZ()))) {
            this.setTarget(null);
            target = null;
        }
        if (!insideNow) {
            // ушёл с территории (погоня, отбросило) — назад к знамени
            this.setTarget(null);
            if (this.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > (size + 24.0) * (size + 24.0)) {
                this.teleportTo(home.getX() + 0.5, home.getY() + 1.0, home.getZ() + 0.5);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            } else {
                this.getNavigation().moveTo(home.getX() + 0.5, home.getY() + 1.0, home.getZ() + 0.5, 1.2);
            }
            return;
        }
        if (target == null) {
            LivingEntity best = null;
            double bestDist = 24.0 * 24.0;
            AABB box = this.getBoundingBox().inflate(24.0, 8.0, 24.0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this)) {
                if (!ClanTerritory.insideKey(clan, key, level, e.getX(), e.getZ()) || !hostile(data, clan, e)) {
                    continue;
                }
                double d = this.distanceToSqr(e);
                if (d < bestDist && this.hasLineOfSight(e)) {
                    best = e;
                    bestDist = d;
                }
            }
            if (best != null) {
                this.setTarget(best);
            }
        }
        if (this.getTarget() == null && this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(Balance.GUARD_REGEN);
        }
    }

    private void leave(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity living && friendly(living)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !friendly(target) && super.canAttack(target);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            ClanGuards.onGuardDied(this);
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (clanId != null) {
            tag.putUUID("GuardClan", clanId);
        }
        tag.putString("GuardKey", key);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        clanId = tag.hasUUID("GuardClan") ? tag.getUUID("GuardClan") : null;
        key = tag.getString("GuardKey");
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
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
