package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.WingsBalance;

/** Формулы прокачки «Укаку Тоуки»: что даёт каждый уровень веток W_*. Числа лежат в WingsBalance. */
public final class WingsEffects {
    private WingsEffects() {
    }

    private static int at(int level, int length) {
        return Math.max(0, Math.min(length - 1, level));
    }

    // ---------- «Скорострельность» ----------

    /** Тиков между выстрелами. rage и fury: во время ярости ветка «Грозовая ярость» II ускоряет стрельбу. */
    public static double interval(int rate, boolean rage, int fury) {
        double interval = WingsBalance.SHOT_INTERVAL[at(rate, WingsBalance.SHOT_INTERVAL.length)];
        return rage && fury >= 2 ? interval / WingsBalance.FURY_RATE : interval;
    }

    public static double shotsPerSecond(int rate) {
        return 20.0 / WingsBalance.SHOT_INTERVAL[at(rate, WingsBalance.SHOT_INTERVAL.length)];
    }

    // ---------- «Кристаллизация» ----------

    public static double crystalBonus(int level) {
        return WingsBalance.CRYSTAL_PER_LEVEL * level;
    }

    public static float shardDamage(int level) {
        return (float) (WingsBalance.SHARD_DAMAGE * (1.0 + crystalBonus(level)));
    }

    public static int pierce(int level) {
        return level >= WingsBalance.CRYSTAL_PIERCE_LEVEL ? 1 : 0;
    }

    // ---------- «Дальность» ----------

    public static double range(int level) {
        return WingsBalance.SHARD_RANGE + WingsBalance.RANGE_PER_LEVEL * level;
    }

    // ---------- «Меткость» ----------

    public static double spread(int level) {
        return WingsBalance.SPREAD[at(level, WingsBalance.SPREAD.length)];
    }

    /** Разброс на полном нагреве. */
    public static double spreadMax(int level) {
        return spread(level) * (1.0 + WingsBalance.BLOOM[at(level, WingsBalance.BLOOM.length)]);
    }

    public static double bloom(int level) {
        return WingsBalance.BLOOM[at(level, WingsBalance.BLOOM.length)];
    }

    // ---------- «Крупный осколок» ----------

    public static float bigDamage(int level) {
        return (float) (WingsBalance.BIG_DAMAGE * (1.0 + WingsBalance.BIG_PER_LEVEL * level));
    }

    public static int bigCooldown(int level) {
        return WingsBalance.BIG_COOLDOWN - WingsBalance.BIG_COOLDOWN_PER_LEVEL * level;
    }

    /** Радиус разлёта большого осколка; 0 — не разлетается. */
    public static double bigSplash(int level) {
        return level >= WingsBalance.BIG_SPLASH_LEVEL ? WingsBalance.BIG_SPLASH_RADIUS : 0.0;
    }

    // ---------- «Град осколков» ----------

    public static float hailDamage(int level) {
        return WingsBalance.HAIL_DAMAGE + WingsBalance.HAIL_DAMAGE_PER_LEVEL * Math.min(level, 3);
    }

    public static int hailPellets(int level) {
        return WingsBalance.HAIL_PELLETS + (level >= WingsBalance.HAIL_PELLETS_LEVEL ? 1 : 0);
    }

    public static double hailRange(int level) {
        return WingsBalance.HAIL_RANGE + (level >= 2 ? WingsBalance.HAIL_RANGE_BONUS : 0.0);
    }

    public static int hailCooldown(int level) {
        return WingsBalance.HAIL_COOLDOWN - WingsBalance.HAIL_COOLDOWN_PER_LEVEL * level;
    }

    // ---------- Электричество ----------

    /** Сколько попаданий осколков дают очки ярости; 0 — навыка нет. */
    public static int hitsPerRage(int level) {
        return WingsBalance.HITS_PER_RAGE[at(level, WingsBalance.HITS_PER_RAGE.length)];
    }

    public static double arcChance(int level) {
        return WingsBalance.ARC_CHANCE[at(level, WingsBalance.ARC_CHANCE.length)];
    }

    /** Осколков в одном выстреле во время ярости. */
    public static int furyShards(int level) {
        return level >= 1 ? 2 : 1;
    }
}
