package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;

/** Формулы всех бонусов дерева навыков. Работают только у гуля и полугуля. */
public final class SkillEffects {
    /** Множитель урона от падения: всем -20%, «Мягкое приземление» ещё -15% за уровень. */
    public static float fallFactor(int softLandingLevel) {
        return (float) (Balance.FALL_DAMAGE_BASE * (1.0 - Balance.SOFT_LANDING_PER_LEVEL * softLandingLevel));
    }

    private SkillEffects() {
    }

    /** Уровень ветки, если она есть в дереве класса игрока, иначе 0. */
    public static int level(PlayerData data, SkillTree.Branch branch) {
        if (data == null || !SkillTree.isAvailable(data.getPlayerClass(), branch)) {
            return 0;
        }
        return branch == SkillTree.Branch.RAGE ? data.getRageLevel() : data.skillLevel(branch.id());
    }

    // ---------- Основа ----------

    public static double damageBonus(int level) {
        return Balance.SKILL_DAMAGE_PER_LEVEL * level;
    }

    public static float regenAmount(int level) {
        return Balance.SKILL_REGEN_PER_LEVEL * level;
    }

    public static double damageReduction(int level) {
        return Balance.SKILL_DEFENSE_PER_LEVEL * level;
    }

    public static double speedBonus(int level) {
        return Balance.SKILL_SPEED_PER_LEVEL * level;
    }

    // ---------- Способности ----------

    public static float biteHeal(int level) {
        return Balance.BITE_HEAL + Balance.SKILL_BITE_HEAL * level;
    }

    public static int biteRage(int level) {
        return Balance.BITE_RAGE + Balance.SKILL_BITE_RAGE * level;
    }

    public static int biteStun(int level) {
        return Balance.BITE_STUN + Balance.SKILL_BITE_STUN * level;
    }

    public static int biteCooldown(int level) {
        return Balance.BITE_COOLDOWN - Balance.SKILL_BITE_COOLDOWN * level;
    }

    public static float barrageDamage(int level) {
        return Balance.BARRAGE_DAMAGE_PER_HIT + Balance.SKILL_BARRAGE_DAMAGE * level;
    }

    public static double barrageWidth(int level) {
        return Balance.BARRAGE_WIDTH + (level >= 3 ? Balance.SKILL_BARRAGE_WIDTH : 0.0);
    }

    public static int barrageCooldown(int level) {
        return Balance.BARRAGE_COOLDOWN - Balance.SKILL_BARRAGE_COOLDOWN * level;
    }

    public static int dashTicks(int level) {
        return Balance.DASH_TICKS + (level >= 1 ? Balance.SKILL_DASH_EXTRA_TICKS : 0);
    }

    public static int dashCharges(int level) {
        return Balance.DASH_CHARGES + (level >= 2 ? Balance.SKILL_DASH_EXTRA_CHARGE : 0);
    }

    public static double grabRange(int level) {
        return level >= 1 ? Balance.GRAB_RANGE_UPGRADED : Balance.GRAB_RANGE_BASE;
    }

    public static int blockStun(int level) {
        return Balance.BLOCK_BREAK_STUN - Balance.SKILL_BLOCK_STUN * level;
    }

    public static double blockSlowDivisor(int level) {
        return Balance.BLOCK_SLOW_DIVISOR - Balance.SKILL_BLOCK_SLOW * level;
    }

    public static double strongDamageBonus(int level) {
        return Balance.SKILL_STRONG_DAMAGE * level;
    }

    public static int strongCooldown(int level) {
        return Balance.STRONG_COOLDOWN - Balance.SKILL_STRONG_COOLDOWN * level;
    }

    // ---------- Выживание ----------

    public static float hungerFactor(int level) {
        return Balance.HUNGER_DRAIN_FACTOR - Balance.SKILL_HUNGER * level;
    }

    public static double vampirism(int level) {
        return Balance.SKILL_VAMPIRISM * level;
    }

    // ---------- Ярость и кагуне ----------

    public static double rageGainBonus(int level) {
        return Balance.SKILL_RAGE_GAIN * level;
    }

    public static double basicReach(int level) {
        return Balance.BASIC_REACH + Balance.SKILL_REACH * level;
    }

    public static double strongReach(int level) {
        return Balance.STRONG_REACH + Balance.SKILL_REACH * level;
    }

    public static double pierce(int level) {
        return Balance.SKILL_PIERCE * level;
    }

    public static int bleedSeconds(int level) {
        return level <= 0 ? 0 : Balance.BLEED_SECONDS_BASE + level;
    }

    // ---------- Человек ----------

    public static double humanToughness(int level) {
        return Balance.HUMAN_TOUGHNESS * level;
    }

    /** Прибавка к максимальному здоровью в единицах здоровья (2 = 1 сердце). */
    public static double humanVitality(int level) {
        return Balance.HUMAN_VITALITY * level;
    }

    public static double humanAttackSpeed(int level) {
        return Balance.HUMAN_ATTACK_SPEED * level;
    }

    public static double humanMining(int level) {
        return Balance.HUMAN_MINING * level;
    }

    public static double humanWeaponDamage(int level) {
        return Balance.HUMAN_WEAPON_DAMAGE * level;
    }

    /** Рывок человека и агента CCG: I — откат 3 с вместо 4, II — два рывка подряд. */
    public static int otherDashCooldown(int level) {
        return level >= 1 ? Balance.OTHER_DASH_COOLDOWN_UPGRADED : Balance.OTHER_DASH_COOLDOWN;
    }

    public static int otherDashCharges(int level) {
        return level >= 2 ? Balance.OTHER_DASH_MAX_CHARGES : 1;
    }
}
