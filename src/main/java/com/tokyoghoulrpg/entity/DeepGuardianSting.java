package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.deep.DeepBalance;
import com.tokyoghoulrpg.entity.goal.ChargeStrikeGoal;
import com.tokyoghoulrpg.entity.goal.KaguneSpearGoal;
import com.tokyoghoulrpg.entity.goal.LeapSlamGoal;
import com.tokyoghoulrpg.entity.goal.SpinSweepGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * «Страж глубин: Жало» — ринкаку, ближний бой. Копьё кагуне, рывок, круговой удар, прыжок с ударом о землю.
 * Здоровье 1460, удар 22, броня 14 / 4, особые атаки x1.6 (сила ~270, tools/boss_power.py).
 */
public class DeepGuardianSting extends DeepGuardian {
    public DeepGuardianSting(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createStingAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DeepBalance.STING_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, DeepBalance.STING_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, DeepBalance.STING_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, DeepBalance.STING_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override
    protected void addSkills() {
        this.goalSelector.addGoal(1, new KaguneSpearGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new ChargeStrikeGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new SpinSweepGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new LeapSlamGoal(this, HalfGhoulBoss::ally));
    }

    @Override
    protected float specialMult() {
        return DeepBalance.STING_SPECIAL_MULT;
    }
}
