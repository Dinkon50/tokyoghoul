package com.tokyoghoulrpg.deep;

/**
 * Числа данжа «Глубины 24-го района». Сила боссов посчитана tools/boss_power.py:
 * стражи — по ~270 каждый (сильнее боссов стилей), Древний гуль — ~600 (между Кровавым полугулем 410
 * и Алым Монархом 970). Как у Кровавого полугуля, все трое сильнее с каждым игроком на сервере.
 */
public final class DeepBalance {
    private DeepBalance() {
    }

    // ---------- Страж глубин: Жало (ринкаку, ближний бой: копьё, рывок, круговой, прыжок)
    public static final double STING_HEALTH = 1460.0;
    public static final double STING_ATTACK = 22.0;
    public static final double STING_ARMOR = 14.0;
    public static final double STING_TOUGHNESS = 4.0;
    public static final float STING_SPECIAL_MULT = 1.6F;

    // ---------- Страж глубин: Крыло (укаку, дальний бой: залп осколков, кровавая волна, рывок, прыжок)
    public static final double WING_HEALTH = 1660.0;
    public static final double WING_ATTACK = 20.0;
    public static final double WING_ARMOR = 12.0;
    public static final double WING_TOUGHNESS = 3.0;
    public static final float WING_SPECIAL_MULT = 1.8F;

    // ---------- Древний гуль: две полные полосы здоровья
    public static final double ANCIENT_HEALTH = 1420.0;
    public static final double ANCIENT_ATTACK = 32.0;
    public static final double ANCIENT_ARMOR = 16.0;
    public static final double ANCIENT_TOUGHNESS = 6.0;
    public static final float ANCIENT_SPECIAL_MULT = 2.2F;
    public static final double ANCIENT_PHASE_DAMAGE = 0.25;        // вторая полоса: урон +25%,
    public static final double ANCIENT_PHASE_SPEED = 0.15;         // скорость +15%,
    public static final double[] ANCIENT_COOLDOWN = {0.85, 0.6};   // особые атаки чаще
    /** Пока живы стражи (в 64 блоках), Древний гуль получает только 10% урона. */
    public static final float ANCIENT_SHIELD = 0.1F;
    public static final double GUARDIAN_LINK_RADIUS = 64.0;

    // ---------- Монстры глубин
    public static final double GHOUL_HEALTH = 45.0;
    public static final double GHOUL_ATTACK = 9.0;
    public static final double GHOUL_SPEED = 0.33;
    public static final double BRUTE_HEALTH = 150.0;
    public static final double BRUTE_ATTACK = 18.0;
    public static final double BRUTE_SPEED = 0.24;
    public static final double SPITTER_HEALTH = 50.0;
    public static final double SPITTER_ATTACK = 6.0;
    public static final double SPITTER_SPEED = 0.30;

    // ---------- Награды (всем, кто нанёс урон; у Древнего гуля двоим лучшим — особая)
    public static final int GUARDIAN_BLOOD = 2500;
    public static final int GUARDIAN_POINTS = 6;
    public static final int ANCIENT_TOP_BLOOD = 15000;
    public static final int ANCIENT_TOP_POINTS = 40;
    public static final int ANCIENT_TOP_LEVELS = 5;
    public static final int ANCIENT_OTHER_BLOOD = 2500;
    public static final int ANCIENT_OTHER_POINTS = 8;
}
