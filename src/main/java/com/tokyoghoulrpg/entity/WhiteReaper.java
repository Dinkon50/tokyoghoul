package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.BossFx;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.goal.ReaperExecuteGoal;
import com.tokyoghoulrpg.entity.goal.ReaperLungeGoal;
import com.tokyoghoulrpg.style.DeathMark;
import com.tokyoghoulrpg.style.FightStyle;
import com.tokyoghoulrpg.style.StyleBalance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

/**
 * «Белый жнец» — босс стиля «Жнец». Тяжёлая броня, сильный удар длинным клинком:
 * <ul>
 *   <li>«Выпад» — рывок насквозь по полосе, пробивает блок;</li>
 *   <li>«Казнь» — удар сверху в круг под целью, по цели слабее половины здоровья вдвое сильнее;</li>
 *   <li>«Метка смерти» — раз в 15 секунд метит цель на 6 секунд: она светится и получает на 20% больше урона.</li>
 *   <li>На половине здоровья — «Последний урожай»: быстрее, сильнее, атаки чаще.</li>
 * </ul>
 * Сила ~239 (tools/boss_power.py). Победа открывает стиль «Жнец».
 */
public class WhiteReaper extends StyleBossMob {
    private long nextMark = 0L;

    public WhiteReaper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, StyleBalance.REAPER_HEALTH)
                // клинок в руке (железный меч) добавляет +5: вместе выходит REAPER_ATTACK
                .add(Attributes.ATTACK_DAMAGE, StyleBalance.REAPER_ATTACK - 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, StyleBalance.REAPER_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, StyleBalance.REAPER_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4);
    }

    @Override
    public FightStyle style() {
        return FightStyle.REAPER;
    }

    public static boolean ally(LivingEntity entity) {
        return entity instanceof WhiteReaper;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ReaperExecuteGoal(this, WhiteReaper::ally));
        this.goalSelector.addGoal(1, new ReaperLungeGoal(this, WhiteReaper::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data,
            @javax.annotation.Nullable net.minecraft.nbt.CompoundTag tag) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long now = this.level().getGameTime();
        if (nextMark == 0L) {
            nextMark = now + 100;
        }
        LivingEntity target = this.getTarget();
        if (now >= nextMark && target != null && target.isAlive() && BossCombat.canAct(this)
                && this.distanceTo(target) <= 20.0 && this.hasLineOfSight(target)) {
            DeathMark.apply(target, StyleBalance.REAPER_MARK_TICKS);
            nextMark = now + BossCombat.cooldown(this, StyleBalance.REAPER_MARK_COOLDOWN);
            if (target instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.white_reaper.marked"), true);
            }
        }
    }

    @Override
    protected void onSecondWind(ServerLevel level) {
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.2, this.getZ(), 40, 0.8, 1.0, 0.8, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2, this.getZ(), 30, 0.6, 1.0, 0.6, 0.08);
        Effects.sound(this, SoundEvents.BELL_BLOCK, 1.6F, 0.5F);
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

    /** Обычный удар: широкий взмах длинным клинком. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            BossFx.slash(this, 2.8, 140.0, 6);
            Effects.sound(this, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        }
        return hit;
    }
}
