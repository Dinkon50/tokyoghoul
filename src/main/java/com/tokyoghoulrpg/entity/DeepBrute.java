package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.deep.DeepBalance;
import com.tokyoghoulrpg.entity.goal.LeapSlamGoal;
import com.tokyoghoulrpg.entity.goal.SpinSweepGoal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * «Гуль-мутант»: огромный (в 1.35 раза больше игрока), медленный, в наростах кагуне. Бьёт очень сильно,
 * прыгает и бьёт о землю, раскручивается с круговым ударом. Почти не отлетает от ударов.
 */
public class DeepBrute extends DeepMonster {
    public DeepBrute(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DeepBalance.BRUTE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, DeepBalance.BRUTE_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, DeepBalance.BRUTE_SPEED)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override
    protected void addAttacks() {
        this.goalSelector.addGoal(1, new LeapSlamGoal(this, DeepDweller::ally));
        this.goalSelector.addGoal(1, new SpinSweepGoal(this, DeepDweller::ally));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            com.tokyoghoulrpg.combat.BossFx.slash(this, 2.4, 120.0, 7);
        }
        return hit;
    }
}
