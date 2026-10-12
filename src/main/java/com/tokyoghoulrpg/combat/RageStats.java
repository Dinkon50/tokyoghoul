package com.tokyoghoulrpg.combat;

/** Параметры ярости для уровня прокачки: от минимума (уровень 0) до максимума (последний уровень). */
public final class RageStats {
    private RageStats() {
    }

    private static double lerp(int level, double min, double max) {
        double t = Math.max(0.0, Math.min(1.0, level / (double) Balance.RAGE_LEVEL_MAX));
        return min + (max - min) * t;
    }

    public static int durationTicks(int level) {
        return (int) Math.round(lerp(level, Balance.RAGE_SECONDS_MIN, Balance.RAGE_SECONDS_MAX) * 20.0);
    }

    public static double damageBonus(int level) {
        return lerp(level, Balance.RAGE_DAMAGE_MIN, Balance.RAGE_DAMAGE_MAX);
    }

    public static double speedBonus(int level) {
        return lerp(level, Balance.RAGE_SPEED_MIN, Balance.RAGE_SPEED_MAX);
    }

    public static double damageReduction(int level) {
        return lerp(level, Balance.RAGE_REDUCTION_MIN, Balance.RAGE_REDUCTION_MAX);
    }
}
