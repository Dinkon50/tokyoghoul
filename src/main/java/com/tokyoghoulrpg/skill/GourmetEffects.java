package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.GourmetBalance;

/** Формулы прокачки «Гурмана»: что даёт каждый уровень веток G_*. Числа лежат в GourmetBalance. */
public final class GourmetEffects {
    private GourmetEffects() {
    }

    private static int at(int level, int length) {
        return Math.max(0, Math.min(length - 1, level));
    }

    // ---------- «Серия» ----------

    public static int seriesLength(int level) {
        return GourmetBalance.SERIES_LENGTH[at(level, GourmetBalance.SERIES_LENGTH.length)];
    }

    public static int seriesInterval(int level) {
        return GourmetBalance.SERIES_INTERVAL[at(level, GourmetBalance.SERIES_INTERVAL.length)];
    }

    /** Ударов в секунду внутри серии. */
    public static double hitsPerSecond(int level) {
        return 20.0 / seriesInterval(level);
    }

    // ---------- «Дальность выпада» ----------

    public static double reach(int level) {
        return GourmetBalance.REACH[at(level, GourmetBalance.REACH.length)];
    }

    // ---------- «Колющий удар» ----------

    public static float thrustDamage(int level) {
        return (float) (GourmetBalance.THRUST_DAMAGE * (1.0 + GourmetBalance.THRUST_PER_LEVEL * level));
    }

    public static int thrustTargets(int level) {
        return GourmetBalance.THRUST_TARGETS[at(level, GourmetBalance.THRUST_TARGETS.length)];
    }

    // ---------- «Прямой выпад» ----------

    public static float lungeDamage(int level) {
        return (float) (GourmetBalance.LUNGE_DAMAGE * (1.0 + GourmetBalance.LUNGE_PER_LEVEL * level));
    }

    public static double lungeDistance(int level) {
        return GourmetBalance.LUNGE_DISTANCE[at(level, GourmetBalance.LUNGE_DISTANCE.length)];
    }

    public static int lungeCooldown(int level) {
        return GourmetBalance.LUNGE_COOLDOWN[at(level, GourmetBalance.LUNGE_COOLDOWN.length)];
    }

    // ---------- «Вихрь ленты» ----------

    public static float whirlDamage(int level) {
        return GourmetBalance.WHIRL_DAMAGE[at(level, GourmetBalance.WHIRL_DAMAGE.length)];
    }

    public static double whirlRadius(int level) {
        return GourmetBalance.WHIRL_RADIUS[at(level, GourmetBalance.WHIRL_RADIUS.length)];
    }

    public static int whirlCooldown(int level) {
        return GourmetBalance.WHIRL_COOLDOWN[at(level, GourmetBalance.WHIRL_COOLDOWN.length)];
    }

    // ---------- «Плотная спираль» ----------

    /** Какая часть урона не проходит, когда блок сломан. */
    public static double tightReduction(int level) {
        return GourmetBalance.TIGHT_REDUCTION[at(level, GourmetBalance.TIGHT_REDUCTION.length)];
    }

    public static boolean tightPush(int level) {
        return level >= GourmetBalance.TIGHT_PUSH_LEVEL;
    }

    // ---------- «Парирование» ----------

    public static int parryWindow(int level) {
        return GourmetBalance.PARRY_WINDOW[at(level, GourmetBalance.PARRY_WINDOW.length)];
    }

    public static float parryDamage(int level) {
        return GourmetBalance.PARRY_DAMAGE[at(level, GourmetBalance.PARRY_DAMAGE.length)];
    }

    public static int parryStun(int level) {
        return GourmetBalance.PARRY_STUN[at(level, GourmetBalance.PARRY_STUN.length)];
    }

    // ---------- «Изящество» ----------

    public static double graceDamage(int level) {
        return GourmetBalance.GRACE_DAMAGE[at(level, GourmetBalance.GRACE_DAMAGE.length)];
    }

    public static int graceRage(int level) {
        return GourmetBalance.GRACE_RAGE[at(level, GourmetBalance.GRACE_RAGE.length)];
    }

    // ---------- «Метка» ----------

    public static double markBonus(int level) {
        return GourmetBalance.MARK_BONUS[at(level, GourmetBalance.MARK_BONUS.length)];
    }
}
