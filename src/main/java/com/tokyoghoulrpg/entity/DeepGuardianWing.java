package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.deep.DeepBalance;
import com.tokyoghoulrpg.entity.goal.BloodNovaGoal;
import com.tokyoghoulrpg.entity.goal.ChargeStrikeGoal;
import com.tokyoghoulrpg.entity.goal.LeapSlamGoal;
import com.tokyoghoulrpg.entity.goal.ShardVolleyGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * «Страж глубин: Крыло» — укаку, дальний бой. Залп осколков, кровавая волна, рывок, прыжок с ударом о землю.
 * Здоровье 1660, удар 20, броня 12 / 3, особые атаки x1.8 (сила ~270, tools/boss_power.py).
 */
public class DeepGuardianWing extends DeepGuardian {
    public DeepGuardianWing(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createWingAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DeepBalance.WING_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, DeepBalance.WING_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, DeepBalance.WING_ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, DeepBalance.WING_TOUGHNESS)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected void addSkills() {
        this.goalSelector.addGoal(1, new ShardVolleyGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new BloodNovaGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new ChargeStrikeGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new LeapSlamGoal(this, HalfGhoulBoss::ally));
    }

    @Override
    protected float specialMult() {
        return DeepBalance.WING_SPECIAL_MULT;
    }
}
