package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.clan.ClanData;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.rep.Standing;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Наёмник (оригинальный персонаж): нанимается контрактом у Скупщика и 10 минут служит хозяину.
 * <ul>
 *   <li>ходит за хозяином, отстал дальше 20 блоков — догоняет телепортом;</li>
 *   <li>бьёт тех, кто напал на хозяина, и тех, кого хозяин бьёт сам; ещё отвечает тому, кто ударил его;</li>
 *   <li>хозяина и его клан не трогает, хозяин не может его ранить;</li>
 *   <li>вне боя понемногу лечится; уходит, когда время вышло, хозяин вышел из игры или ушёл в другое измерение.</li>
 * </ul>
 * У игрока одновременно только один наёмник: новый контракт отпускает прежнего.
 */
public class MercenaryNpc extends PathfinderMob implements SkinVariant {
    /** Хозяин → его наёмник (только пока сервер работает). */
    private static final Map<UUID, UUID> ACTIVE = new HashMap<>();

    private UUID owner;
    private long expireTick;

    public MercenaryNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Balance.MERC_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Balance.MERC_DAMAGE)
                .add(Attributes.ARMOR, Balance.MERC_ARMOR)
                .add(Attributes.MOVEMENT_SPEED, Balance.MERC_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    public int getSkinVariant() {
        return 0;
    }

    /** Нанять: поставить рядом с игроком. Прежний наёмник этого игрока уходит. */
    public static MercenaryNpc hire(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        dismiss(player);
        MercenaryNpc merc = com.tokyoghoulrpg.registry.ModEntities.MERCENARY.get().create(level);
        if (merc == null) {
            return null;
        }
        merc.owner = player.getUUID();
        merc.expireTick = level.getGameTime() + Balance.MERC_TICKS;
        merc.moveTo(player.getX() + player.getLookAngle().x * 1.5, player.getY(), player.getZ() + player.getLookAngle().z * 1.5,
                player.getYRot(), 0.0F);
        merc.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        merc.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        merc.setCustomName(Component.translatable("entity.tokyoghoulrpg.mercenary.of", player.getGameProfile().getName())
                .withStyle(ChatFormatting.GOLD));
        merc.setCustomNameVisible(true);
        level.addFreshEntity(merc);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, merc.getX(), merc.getY() + 1.0, merc.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        ACTIVE.put(player.getUUID(), merc.getUUID());
        return merc;
    }

    /** Отпустить наёмника игрока (если он есть в загруженном мире). */
    public static void dismiss(ServerPlayer player) {
        UUID id = ACTIVE.remove(player.getUUID());
        if (id == null || player.getServer() == null) {
            return;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof MercenaryNpc merc) {
                merc.leave();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private ServerPlayer ownerPlayer() {
        if (owner == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    /** Свой: хозяин или игрок из его клана. Их наёмник не бьёт. */
    private boolean friendly(LivingEntity entity) {
        if (entity == null || owner == null) {
            return false;
        }
        if (entity instanceof MercenaryNpc other && owner.equals(other.owner)) {
            return true;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        if (owner.equals(player.getUUID())) {
            return true;
        }
        ServerPlayer master = ownerPlayer();
        if (master == null) {
            return false;
        }
        ClanData.Clan clan = Standing.clanOf(master);
        return clan != null && clan == Standing.clanOf(player);
    }

    private void leave() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
        this.discard();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Раз в полсекунды: кого бить, идти за хозяином, уходить ли. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 10 != 0 || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer master = ownerPlayer();
        if (master == null || !master.isAlive() || master.level() != level || level.getGameTime() > expireTick) {
            if (master != null && level.getGameTime() > expireTick) {
                master.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.mercenary.left"));
            }
            if (owner != null && this.getUUID().equals(ACTIVE.get(owner))) {
                ACTIVE.remove(owner);
            }
            leave();
            return;
        }
        ACTIVE.putIfAbsent(owner, this.getUUID());
        // своих не бьём
        LivingEntity target = this.getTarget();
        if (target != null && (friendly(target) || !target.isAlive())) {
            this.setTarget(null);
            target = null;
        }
        // кто напал на хозяина, а если никто — кого бьёт хозяин
        if (target == null) {
            LivingEntity attacker = master.getLastHurtByMob();
            if (attacker != null && attacker.isAlive() && !friendly(attacker)
                    && master.tickCount - master.getLastHurtByMobTimestamp() < 200 && attacker.distanceToSqr(this) < 24 * 24) {
                this.setTarget(attacker);
            } else {
                LivingEntity victim = master.getLastHurtMob();
                if (victim != null && victim.isAlive() && !friendly(victim)
                        && master.tickCount - master.getLastHurtMobTimestamp() < 200 && victim.distanceToSqr(this) < 24 * 24) {
                    this.setTarget(victim);
                }
            }
        }
        double dist = this.distanceToSqr(master);
        if (dist > Balance.MERC_TELEPORT * Balance.MERC_TELEPORT) {
            this.getNavigation().stop();
            this.teleportTo(master.getX() - master.getLookAngle().x * 1.5, master.getY(), master.getZ() - master.getLookAngle().z * 1.5);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        } else if (this.getTarget() == null && dist > 16.0) {
            this.getNavigation().moveTo(master, 1.2);
        } else if (this.getTarget() == null && dist < 6.0) {
            this.getNavigation().stop();
        }
        // вне боя лечится
        if (this.getTarget() == null && this.tickCount % 60 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(Balance.MERC_REGEN);
        }
    }

    /** Хозяин (и его клан) ранить наёмника не может. */
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
        ServerPlayer master = ownerPlayer();
        if (master != null) {
            master.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.mercenary.died").withStyle(ChatFormatting.RED));
            if (this.getUUID().equals(ACTIVE.get(owner))) {
                ACTIVE.remove(owner);
            }
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) {
            tag.putUUID("MercOwner", owner);
        }
        tag.putLong("MercExpire", expireTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("MercOwner") ? tag.getUUID("MercOwner") : null;
        expireTick = tag.getLong("MercExpire");
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
