package com.tokyoghoulrpg.combat;

/**
 * Все числа боевой системы в одном месте.
 * [СПЕКА] значение взято из твоего описания.
 * [ВРЕМЕННО] в описании такого числа нет, стоит запасное значение: поставь своё.
 * Время указано в тиках (20 тиков = 1 секунда).
 */
public final class Balance {
    private Balance() {
    }

    // ---------- Укус (V) ----------
    public static final double BITE_RANGE = 3.0;                    // [СПЕКА] до 3 блоков
    public static final int BITE_COOLDOWN = 10 * 20;                // [СПЕКА] 10 секунд
    public static final float BITE_HEAL = 3.0F;                     // [ВРЕМЕННО] «немного» здоровья (3 = 1,5 сердца)
    public static final int BITE_RAGE = 10;                         // [ВРЕМЕННО] «немного» очков ярости
    public static final float BITE_DAMAGE = 0.0F;                   // [ВРЕМЕННО] в описании урона от укуса нет

    // ---------- Рывок (H) ----------
    public static final int DASH_TICKS = 3;                         // [СПЕКА] 2-3 блока: 3 тика по 0,85 = 2,55 блока
    public static final double DASH_SPEED = 0.85;
    public static final int DASH_COOLDOWN = 3 * 20;                 // [ВРЕМЕННО]

    // ---------- Блок (L) ----------
    public static final double BLOCK_SLOW_DIVISOR = 1.8;            // [СПЕКА] ходьба в 1,8 раза медленнее
    public static final int BLOCK_BREAK_STUN = 2 * 20;              // [СПЕКА] оглушение 2 секунды
    public static final int BLOCK_BREAK_COOLDOWN = 10 * 20;         // [СПЕКА] перезарядка 10 секунд после пробития

    // ---------- Барраж (K) ----------
    public static final double BARRAGE_WIDTH = 3.0;                 // [СПЕКА] ширина области
    public static final double BARRAGE_LENGTH = 2.0;                // [СПЕКА] длина области вперёд
    public static final float BARRAGE_DAMAGE_PER_HIT = 0.5F;        // [ВРЕМЕННО] 32 удара = 16 урона
    public static final int BARRAGE_COOLDOWN = 10 * 20;             // [ВРЕМЕННО]
    public static final double BARRAGE_USER_SPEED_FACTOR = 0.5;     // [ВРЕМЕННО] скорость атакующего во время серии
    public static final float COUNTER_BARRAGE_FACTOR = 0.25F;       // [ВРЕМЕННО] «значительно меньше» урона при встречных баррагах

    // ---------- Обычная и сильная атаки кагуне ----------
    public static final float BASIC_DAMAGE = 5.0F;                  // [ВРЕМЕННО]
    public static final double BASIC_REACH = 3.5;                   // [ВРЕМЕННО]
    public static final double BASIC_HALF_WIDTH = 1.0;              // [ВРЕМЕННО]
    public static final float STRONG_DAMAGE_PER_HIT = 6.0F;         // [ВРЕМЕННО] два удара = 12 урона
    public static final double STRONG_REACH = 3.5;                  // [ВРЕМЕННО]
    public static final double STRONG_HALF_WIDTH = 1.2;             // [ВРЕМЕННО]
    public static final int STRONG_COOLDOWN = 3 * 20;               // [ВРЕМЕННО]
    public static final int DOUBLE_TAP_TICKS = 8;                   // [ВРЕМЕННО] окно двойного нажатия ПКМ

    // ---------- Захваты ----------
    public static final double GRAPPLE_RANGE = 10.0;                // [СПЕКА] Shift + ПКМ, до 10 блоков
    public static final int GRAPPLE_COOLDOWN = 5 * 20;              // [СПЕКА] 5 секунд
    public static final double GRAPPLE_STOP_BLOCK = 1.0;            // [ВРЕМЕННО] на каком расстоянии остановиться у блока
    public static final double GRAPPLE_STOP_ENTITY = 1.6;           // [ВРЕМЕННО] то же у существа
    public static final double GRAPPLE_MAX_SPEED = 1.3;             // [ВРЕМЕННО] блоков за тик
    public static final double PULL_RANGE = 10.0;                   // [СПЕКА] Shift + ЛКМ, до 10 блоков
    public static final int PULL_COOLDOWN = 10 * 20;                // [СПЕКА] 10 секунд
    public static final double PULL_STOP = 1.4;                     // [ВРЕМЕННО] на каком расстоянии остановить цель
    public static final double PULL_MAX_SPEED = 1.0;                // [ВРЕМЕННО] блоков за тик

    // ---------- Ярость (J) ----------
    public static final int RAGE_LEVEL_MAX = 5;                     // [ВРЕМЕННО] число уровней прокачки (задаст дерево навыков)
    public static final int RAGE_PER_KILL = 10;                     // [ВРЕМЕННО] очки ярости за убийство
    public static final double RAGE_SECONDS_MIN = 20.0;             // [СПЕКА] минимальная прокачка
    public static final double RAGE_SECONDS_MAX = 60.0;             // [СПЕКА] максимальная прокачка
    public static final double RAGE_DAMAGE_MIN = 0.15;              // [СПЕКА] +15%
    public static final double RAGE_DAMAGE_MAX = 0.40;              // [СПЕКА] +40%
    public static final double RAGE_SPEED_MIN = 0.05;               // [СПЕКА] +5%
    public static final double RAGE_SPEED_MAX = 0.20;               // [СПЕКА] +20%
    public static final double RAGE_REDUCTION_MIN = 0.10;           // [СПЕКА] -10% получаемого урона
    public static final double RAGE_REDUCTION_MAX = 0.40;           // [СПЕКА] -40% получаемого урона

    // ---------- Голод ----------
    public static final float HUNGER_DRAIN_FACTOR = 0.6F;           // [ВРЕМЕННО] у гулей голод тратится в 0,6 раза от обычного
}
