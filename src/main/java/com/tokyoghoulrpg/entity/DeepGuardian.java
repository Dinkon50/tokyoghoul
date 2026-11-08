package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.difficulty.DifficultyScaling;
import com.tokyoghoulrpg.combat.Balance;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Страж глубин: один из двух, что стоят у врат логова Древнего гуля. Не уходит от врат дальше 16 блоков,
 * пока он жив — Древний гуль получает только 10% урона. Одна полоса здоровья, без превращения и свиты.
 * Сильнее с каждым игроком на сервере (как Кровавый полугуль). 30 секунд без игроков — снова целый.
 */
public abstract class DeepGuardian extends HalfGhoulBoss implements DeepDweller {
    protected DeepGuardian(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 300;
        this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        this.bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
    }

    /** Свои особые атаки (приоритет 1). */
    protected abstract void addSkills();

    /** Во сколько раз сильнее базового урона особые атаки. */
    protected abstract float specialMult();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        addSkills();
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
    }

    @Override
    protected int maxPhase() {
        return 1;
    }

    @Override
    protected String phaseKey() {
        return "deep_guardian";
    }

    @Override
    protected BossEvent.BossBarColor baseColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    protected double phaseDamage() {
        return 1.0;
    }

    @Override
    protected double phaseSpeed() {
        return 0.0;
    }

    @Override
    protected boolean summonsMinions() {
        return false;
    }

    @Override
    public float damageMultiplier() {
        int players = DifficultyScaling.players(this.level());
        return specialMult() * (float) (1.0 + Balance.HG_PER_PLAYER_DAMAGE * (players - 1));
    }

    @Override
    public double cooldownMultiplier() {
        return 1.0;
    }

    @Override
    public boolean isRespawned() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        // пост у врат: место, где страж появился
        if (!hasArena()) {
            setArena(this.blockPosition());
        }
        super.customServerAiStep();
    }
}
