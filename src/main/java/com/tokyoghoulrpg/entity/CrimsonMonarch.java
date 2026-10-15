package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.entity.goal.QuinqueThrustGoal;
import com.tokyoghoulrpg.entity.goal.ThunderStrikeGoal;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * «Алый Монарх» — финальный босс мода (оригинальный персонаж). Сильнее Кровавого полугуля:
 * <ul>
 *   <li>три стадии, каждая — полное здоровье (1000 до усилений): когда здоровье кончается, он 3 секунды
 *       неуязвим, восстанавливается и становится сильнее (+25% урона и +12% скорости за стадию, особые атаки чаще);</li>
 *   <li>удар 40, особые атаки x4: всё, что умеют Кровавый полугуль, Пожиратель и Старший следователь
 *       (прыжок, рывок, круговой удар, осколки, копьё-кагуне, кровавая волна, шипы, разряд квинке, молнии);</li>
 *   <li>зовёт полугулей, растёт с числом игроков (как Кровавый полугуль) и со сложностью;</li>
 *   <li>награда — только очки крови (MonarchRewards). Призывается Алой короной.</li>
 * </ul>
 */
public class CrimsonMonarch extends HalfGhoulBoss {
    public CrimsonMonarch(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;                       // ничего, кроме очков крови
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder createMonarchAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Balance.MONARCH_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, Balance.MONARCH_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 56.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // и умения Старшего следователя
        this.goalSelector.addGoal(1, new QuinqueThrustGoal(this, HalfGhoulBoss::ally));
        this.goalSelector.addGoal(1, new ThunderStrikeGoal(this, HalfGhoulBoss::ally));
    }

    @Override
    protected int maxPhase() {
        return 3;
    }

    /** Стадии меняются только когда здоровье кончилось: каждая стадия — полная полоса. */
    @Override
    protected float phaseThreshold() {
        return 0.0F;
    }

    @Override
    protected String phaseKey() {
        return "crimson_monarch";
    }

    @Override
    protected BossEvent.BossBarColor baseColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected double phaseDamage() {
        return 1.0 + Balance.MONARCH_PHASE_DAMAGE * (getPhase() - 1);
    }

    @Override
    protected double phaseSpeed() {
        return Balance.MONARCH_PHASE_SPEED * (getPhase() - 1);
    }

    @Override
    public float damageMultiplier() {
        int players = com.tokyoghoulrpg.difficulty.DifficultyScaling.players(this.level());
        return Balance.MONARCH_SPECIAL_MULT * (float) phaseDamage()
                * (float) (1.0 + Balance.HG_PER_PLAYER_DAMAGE * (players - 1));
    }

    @Override
    public double cooldownMultiplier() {
        return Balance.MONARCH_COOLDOWN[Math.max(0, Math.min(2, getPhase() - 1))];
    }

    @Override
    public boolean isRespawned() {
        return false;
    }
}
