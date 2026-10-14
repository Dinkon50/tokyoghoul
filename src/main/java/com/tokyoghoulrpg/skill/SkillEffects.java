package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;

/** Формулы всех бонусов дерева навыков. Ветка действует, только если она есть в дереве класса игрока. */
public final class SkillEffects {
    /** Множитель урона от падения: всем -20%, «Мягкое приземление» ещё -15% за уровень. */
    public static float fallFactor(int softLandingLevel) {
        return (float) (Balance.FALL_DAMAGE_BASE * (1.0 - Balance.SOFT_LANDING_PER_LEVEL * softLandingLevel));
    }

    // ---------- Агент CCG ----------

    // ---------- Агент CCG: Освобождение и квинке ----------

    private static double lerp(int level, double min, double max) {
        int top = Balance.RAGE_LEVEL_MAX;
        return min + (max - min) * Math.min(level, top) / (double) top;
    }

    /** «Освобождение квинке»: длительность, урон, защита, скорость (уровень 0 — без прокачки). */
    public static int releaseTicks(int level) {
        return (int) Math.round(lerp(level, Balance.RELEASE_SECONDS_MIN, Balance.RELEASE_SECONDS_MAX) * 20.0);
    }

    public static double releaseDamage(int level) {
        return lerp(level, Balance.RELEASE_DAMAGE_MIN, Balance.RELEASE_DAMAGE_MAX);
    }

    public static double releaseReduction(int level) {
        return lerp(level, Balance.RELEASE_REDUCTION_MIN, Balance.RELEASE_REDUCTION_MAX);
    }

    public static double releaseSpeed(int level) {
        return lerp(level, Balance.RELEASE_SPEED_MIN, Balance.RELEASE_SPEED_MAX);
    }

    public static double releaseGainBonus(int level) {
        return Balance.RELEASE_GAIN_PER_LEVEL * level;
    }

    public static double qComboDamage(int level) {
        return Balance.Q_COMBO_DAMAGE_PER_LEVEL * level;
    }

    public static float qFinisherBonus(int level) {
        return Balance.Q_FINISHER_PER_LEVEL * level;
    }

    public static double qStrongDamage(int level) {
        return Balance.Q_STRONG_DAMAGE_PER_LEVEL * level;
    }

    public static double qStrongReach(int level) {
        return Balance.Q_STRONG_REACH_PER_LEVEL * level;
    }

    public static int qFlurryExtraHits(int level) {
        return Balance.Q_FLURRY_HITS_PER_LEVEL * level;
    }

    public static int qFlurryCooldown(int level) {
        return Balance.QUINQUE_FLURRY_COOLDOWN - Balance.Q_FLURRY_COOLDOWN_PER_LEVEL * level;
    }

    public static double qParryWindowBonus(int level) {
        return Balance.Q_PARRY_WINDOW_PER_LEVEL * level;
    }

    public static int qParryStun(int level) {
        return Balance.QUINQUE_PARRY_STUN + Balance.Q_PARRY_STUN_PER_LEVEL * level;
    }

    public static double qBlockSlow(int level) {
        return Balance.QUINQUE_BLOCK_SLOW - Balance.Q_BLOCK_SLOW_PER_LEVEL * level;
    }

    public static int qLungeExtraTicks(int level) {
        return level;
    }

    public static double qLungeDamage(int level) {
        return Balance.Q_LUNGE_DAMAGE_PER_LEVEL * level;
    }

    /** Откат выпада: 4 с → 2,5 с, меньше 2 с не бывает. */
    public static int qLungeCooldown(int level) {
        return Math.max(40, Balance.QUINQUE_LUNGE_COOLDOWN - Balance.Q_LUNGE_COOLDOWN_PER_LEVEL * level);
    }

    public static double qReach(int level) {
        return Balance.Q_REACH_PER_LEVEL * level;
    }

    /** «Знание гулей»: прибавка к урону по гулям (NPC, боссу, игрокам-гулям и полугулям). */
    public static double ghoulBane(int level) {
        return Balance.GHOUL_BANE_PER_LEVEL * level;
    }

    /** «Защита от кагуне»: меньше урона от гулей. */
    public static double kaguneGuard(int level) {
        return Balance.KAGUNE_GUARD_PER_LEVEL * level;
    }

    /** «Подавитель RC»: сколько тиков гуль после удара не лечится. 0 — навыка нет. */
    public static int rcSuppressTicks(int level) {
        return level <= 0 ? 0 : Balance.RC_SUPPRESS_BASE + Balance.RC_SUPPRESS_PER_LEVEL * (level - 1);
    }

    /** «Полевая аптечка»: сколько здоровья восстанавливается раз в FIELD_MEDIC_INTERVAL. */
    public static float fieldMedic(int level) {
        return Balance.FIELD_MEDIC_PER_LEVEL * level;
    }

    /** «Звание»: больше очков крови за убийства и контракты. */
    public static double rankBloodBonus(int level) {
        return Balance.RANK_BLOOD_PER_LEVEL * level;
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
        return Balance.GRAB_RANGE_BASE + Balance.GRAB_RANGE_PER_LEVEL * level;
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
    /** Ветка «Здоровье»: прибавка к максимуму здоровья (в единицах, 2 = одно сердце). */
    public static double vitality(int level, boolean ghoul) {
        return (ghoul ? Balance.GHOUL_VITALITY : Balance.HUMAN_VITALITY) * level;
    }

    /** Здоровье без прокачки: гуль и полугуль — 2 строки сердец (40), остальные — одна (20). */
    public static double baseHealth(boolean ghoul) {
        return 20.0 + (ghoul ? Balance.GHOUL_MAX_HEALTH_BONUS : 0.0);
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
