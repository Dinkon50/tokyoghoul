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
    public static final float BITE_DAMAGE = 2.0F;                   // [ТВОЁ: небольшой урон, число временное] 1 сердце
    public static final int BITE_STUN = 2 * 20;                     // [ТВОЁ] укушенный не двигается 2 секунды
    public static final int BITE_SCREEN_FLASH = 30;                 // [ТВОЁ] края экрана красные 1,5 секунды

    // ---------- Рывок (H) ----------
    public static final int DASH_TICKS = 3;                         // [СПЕКА] 2-3 блока: 3 тика по 0,85 = 2,55 блока
    public static final double DASH_SPEED = 0.85;
    public static final int DASH_CHARGES = 2;                       // [ТВОЁ] двойной рывок: два рывка подряд
    public static final int DASH_COOLDOWN = 2 * 20;                 // [ТВОЁ] после серии рывков откат 2 секунды, меньше не ставить
    public static final int DASH_CHAIN_WINDOW = 30;                 // [ВРЕМЕННО] сколько тиков ждать второй рывок (1,5 с)

    // ---------- Блок (L) ----------
    public static final double BLOCK_SLOW_DIVISOR = 1.8;            // [СПЕКА] ходьба в 1,8 раза медленнее
    public static final int BLOCK_BREAK_STUN = 2 * 20;              // [СПЕКА] оглушение 2 секунды
    public static final int BLOCK_BREAK_COOLDOWN = 10 * 20;         // [СПЕКА] перезарядка 10 секунд после пробития

    // ---------- Барраж (K) ----------
    public static final double BARRAGE_WIDTH = 3.0;                 // [СПЕКА] ширина области
    public static final double BARRAGE_LENGTH = 3.0;                // [ТВОЁ] длина области вперёд: 2 блока по спеке + 1
    public static final float BARRAGE_DAMAGE_PER_HIT = 0.5F;        // [ВРЕМЕННО] 32 удара = 16 урона
    public static final int BARRAGE_COOLDOWN = 10 * 20;             // [ВРЕМЕННО] считается с момента, когда серия закончилась
    public static final int BARRAGE_HIT_STUN = 5;                   // [ВРЕМЕННО] сколько тиков цель стоит после попадания (удары идут через 2-3 тика)
    public static final double BARRAGE_USER_SPEED_FACTOR = 0.5;     // [ВРЕМЕННО] скорость атакующего во время серии
    public static final float COUNTER_BARRAGE_FACTOR = 0.25F;       // [ВРЕМЕННО] «значительно меньше» урона при встречных баррагах

    // ---------- Обычная и сильная атаки кагуне ----------
    public static final float BASIC_DAMAGE = 5.0F;                  // [ВРЕМЕННО]
    public static final double BASIC_REACH = 3.5;                   // [ВРЕМЕННО]
    public static final double BASIC_HALF_WIDTH = 1.0;              // [ВРЕМЕННО]
    public static final float STRONG_DAMAGE_PER_HIT = 6.0F;         // [ВРЕМЕННО] два удара = 12 урона
    public static final double STRONG_REACH = 3.5;                  // [ВРЕМЕННО]
    public static final double STRONG_HALF_WIDTH = 1.2;             // [ВРЕМЕННО]
    public static final int BASIC_COOLDOWN = 8;                     // [ТВОЁ: меньше] ЛКМ раз в 0,4 с (раньше ждали конец анимации, 0,85-0,95 с)
    public static final int STRONG_COOLDOWN = 30;                   // [ТВОЁ: в 2 раза меньше] ПКМ: 1,5 с (было 3 с)
    public static final int DOUBLE_TAP_TICKS = 8;                   // [ВРЕМЕННО] окно двойного нажатия ПКМ

    // ---------- Кагуне (G) ----------
    public static final int KAGUNE_TOGGLE_COOLDOWN = 20;            // [ТВОЁ: небольшой, значение временное] 1 с между нажатиями G

    // ---------- Захваты ----------
    public static final double GRAB_RANGE_BASE = 5.0;               // [ТВОЁ] захваты без прокачки: 5 блоков
    public static final double GRAB_RANGE_UPGRADED = 10.0;          // [ТВОЁ] с прокачкой «Захваты II»: 10 блоков, больше нельзя
    public static final int GRAPPLE_COOLDOWN = 5 * 20;              // [СПЕКА] 5 секунд
    public static final double GRAPPLE_STOP_BLOCK = 1.0;            // [ВРЕМЕННО] на каком расстоянии остановиться у блока
    public static final double GRAPPLE_STOP_ENTITY = 1.6;           // [ВРЕМЕННО] то же у существа
    public static final double GRAPPLE_MAX_SPEED = 1.3;             // [ВРЕМЕННО] блоков за тик
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

    // ---------- Дерево навыков ----------
    public static final int[] RAGE_UPGRADE_COSTS = {1, 1, 2, 2, 3}; // [ВРЕМЕННО] стоимость улучшений ярости I-V
    public static final int PASSIVE_LEVELS = 5;                     // [ВРЕМЕННО] уровней в ветках урона, регенерации, защиты и скорости
    public static final int[] PASSIVE_UPGRADE_COSTS = {1, 1, 2, 2, 3}; // [ВРЕМЕННО] стоимость уровней I-V в этих ветках
    public static final double SKILL_DAMAGE_PER_LEVEL = 0.04;       // [ВРЕМЕННО] +4% урона за уровень (до +20%)
    public static final float SKILL_REGEN_PER_LEVEL = 0.5F;         // [ВРЕМЕННО] +0,5 здоровья за уровень (до 2,5 = 1,25 сердца)
    public static final int SKILL_REGEN_INTERVAL = 5 * 20;          // [ВРЕМЕННО] ... раз в 5 секунд
    public static final double SKILL_DEFENSE_PER_LEVEL = 0.03;      // [ВРЕМЕННО] -3% получаемого урона за уровень (до -15%)
    public static final double SKILL_SPEED_PER_LEVEL = 0.02;        // [ВРЕМЕННО] +2% скорости за уровень (до +10%)
    public static final int SKILL_POINTS_PER_LEVEL = 2;             // [ТВОЁ] 2 очка навыков за каждый новый уровень Minecraft

    // ---------- Новые ветки дерева навыков (все числа временные) ----------
    public static final int[] COSTS_3 = {1, 2, 3};                  // ветки из 3 уровней: 6 очков
    public static final int[] COSTS_2 = {2, 3};                     // ветки из 2 уровней: 5 очков
    public static final int[] COSTS_1 = {3};                        // ветки из 1 навыка: 3 очка
    // Укус, за уровень
    public static final float SKILL_BITE_HEAL = 1.0F;               // +0,5 сердца лечения
    public static final int SKILL_BITE_RAGE = 5;                    // +5 очков ярости
    public static final int SKILL_BITE_STUN = 10;                   // +0,5 с обездвиживания
    public static final int SKILL_BITE_COOLDOWN = 20;               // -1 с отката (10 → 7 с)
    // Барраж, за уровень
    public static final float SKILL_BARRAGE_DAMAGE = 0.1F;          // +0,1 урона за удар (0,5 → 0,8)
    public static final int SKILL_BARRAGE_COOLDOWN = 30;            // -1,5 с отката (10 → 5,5 с)
    public static final double SKILL_BARRAGE_WIDTH = 1.0;           // на III уровне область шире на 1 блок (3 → 4)
    // Рывок: откат не меняется и никогда не меньше 2 секунд [ТВОЁ]
    public static final int SKILL_DASH_EXTRA_TICKS = 1;             // I: рывок длиннее (2,55 → 3,4 блока)
    public static final int SKILL_DASH_EXTRA_CHARGE = 1;            // II: три рывка подряд вместо двух
    // Блок, за уровень
    public static final int SKILL_BLOCK_STUN = 8;                   // -0,4 с оглушения после пробития (2 → 0,8 с)
    public static final double SKILL_BLOCK_SLOW = 0.2;              // ходьба в блоке быстрее: в 1,8 → 1,6 → 1,4 → 1,2 раза медленнее
    // Сильная атака, за уровень
    public static final double SKILL_STRONG_DAMAGE = 0.15;          // +15% урона
    public static final int SKILL_STRONG_COOLDOWN = 5;              // -0,25 с отката (1,5 → 0,75 с)
    // Выживание
    public static final float SKILL_HUNGER = 0.1F;                  // голод тратится медленнее: 0,6 → 0,5 → 0,4 → 0,3
    public static final double SKILL_VAMPIRISM = 0.05;              // 5% нанесённого урона возвращается здоровьем, за уровень
    public static final float SECOND_WIND_HEALTH = 2.0F;            // второе дыхание оставляет 1 сердце
    public static final int SECOND_WIND_COOLDOWN = 5 * 60 * 20;     // раз в 5 минут
    // Ярость
    public static final double SKILL_RAGE_GAIN = 0.25;              // +25% очков ярости за уровень
    public static final double LAST_STAND_HEALTH = 0.3;             // ниже 30% здоровья...
    public static final int LAST_STAND_RAGE = 2;                    // ... ярость копится сама: +2 в секунду
    public static final double RAGE_AURA_RADIUS = 4.0;              // аура ярости: замедляет врагов в 4 блоках
    // Кагуне
    public static final double SKILL_REACH = 0.5;                   // +0,5 блока дальности атак кагуне за уровень (3,5 → 5)
    public static final double SKILL_PIERCE = 0.15;                 // 15% урона обычных ударов проходит сквозь блок, за уровень
    public static final float BLEED_DAMAGE = 0.5F;                  // кровотечение: 0,25 сердца в секунду...
    public static final int BLEED_SECONDS_BASE = 1;                 // ... (1 + уровень) секунд: 2, 3, 4

    // ---------- Человек: дерево навыков (числа временные) ----------
    public static final int HUMAN_LEVELS = 5;                       // в каждой ветке уровни I-V
    public static final int[] HUMAN_COSTS = {1, 1, 2, 2, 3};        // 9 очков за ветку
    public static final double HUMAN_TOUGHNESS = 0.04;              // -4% получаемого урона за уровень (до -20%)
    public static final double HUMAN_VITALITY = 2.0;                // +1 сердце здоровья за уровень (до +5 сердец)
    public static final double HUMAN_ATTACK_SPEED = 0.10;           // +10% скорости ударов любым предметом за уровень (до +50%)
    public static final double HUMAN_MINING = 0.15;                 // +15% скорости копания любым предметом за уровень (до +75%)
    public static final double HUMAN_WEAPON_DAMAGE = 0.06;          // +6% урона оружием за уровень (до +30%): мечи, топоры, луки, арбалеты, трезубцы

    // ---------- Рывок у человека и агента CCG ----------
    public static final int OTHER_DASH_COOLDOWN = 4 * 20;           // [ВРЕМЕННО] без прокачки: один рывок, откат 4 с
    public static final int OTHER_DASH_COOLDOWN_UPGRADED = 3 * 20;  // [ТВОЁ] с прокачкой откат 3 с, меньше не ставить
    public static final int OTHER_DASH_MAX_CHARGES = 2;             // [ТВОЁ] максимум два рывка подряд

    // ---------- Человек: сильная атака (ПКМ дважды) ----------
    public static final double HUMAN_STRONG_MULTIPLIER = 2.5;       // [ТВОЁ: намного сильнее обычной, число временное] урон удара × 2,5
    public static final int HUMAN_STRONG_COOLDOWN = 2 * 20;         // [ТВОЁ] откат 2 с, меньше не ставить
    public static final double HUMAN_STRONG_KNOCKBACK = 0.8;        // [ВРЕМЕННО] отбрасывание сильнее обычного удара

    // ---------- Очки крови ----------
    public static final int BLOOD_START = 100;                      // [ТВОЁ] столько получает каждый класс в начале
    public static final int BLOOD_QUEST_MIN = 10;                   // [ТВОЁ] за задание 10-20 очков крови:
    public static final int BLOOD_QUEST_MAX = 20;                   //        задание за 1 очко навыков даёт 10, за 2 — 15, за 3 — 20

    // ---------- Очки крови за убийства (числа временные) ----------
    public static final int BLOOD_KILL_NPC = 5;                     // гуль или агент CCG
    public static final int BLOOD_KILL_BOSS = 150;                  // босс
    public static final int BOSS_SKILL_POINTS = 3;                  // очки навыков за босса

    // ---------- Шкала крови гуля (критический голод, числа временные) ----------
    public static final int CRAVING_MAX = 100;
    public static final int CRAVING_FOOD = 4;                       // голод 4 и меньше (2 «окорочка»): шкала убывает
    public static final double CRAVING_HEALTH = 0.3;                // здоровье ниже 30%: убывает вдвое быстрее
    public static final int CRAVING_DRAIN_TICKS = 10;               // убывает на 1 каждые 0,5 с (пустеет за 50 с)
    public static final float CRAVING_DAMAGE = 1.0F;                // пустая шкала: полсердца урона...
    public static final int CRAVING_DAMAGE_INTERVAL = 40;           // ...раз в 2 с
    public static final int CRAVING_RECOVER = 4;                    // когда опасность прошла, восстанавливается на 4 в секунду

    // ---------- Боссы (числа временные) ----------
    public static final int BOSS_LEAP_COOLDOWN = 8 * 20;            // Пожиратель: прыжок с ударом о землю
    public static final float BOSS_LEAP_DAMAGE = 8.0F;
    public static final double BOSS_LEAP_RADIUS = 4.0;
    public static final int BOSS_CHARGE_COOLDOWN = 6 * 20;          // Старший следователь: рывок с ударом
    public static final float BOSS_CHARGE_DAMAGE = 10.0F;
    public static final int BOSS_SWEEP_COOLDOWN = 10 * 20;          // Старший следователь: круговой удар
    public static final float BOSS_SWEEP_DAMAGE = 7.0F;
    public static final double BOSS_SWEEP_RADIUS = 4.0;

    // ---------- Гуль и полугуль ----------
    public static final double GHOUL_MAX_HEALTH_BONUS = 20.0;       // [ТВОЁ] у гуля и полугуля здоровья в 2 раза больше: 20 + 20 = 40 (20 сердец)
    public static final float GHOUL_REGEN_AMOUNT = 1.0F;            // [ТВОЁ: выше, чем у человека; числа временные] +0,5 сердца...
    public static final int GHOUL_REGEN_INTERVAL = 2 * 20;          // [ВРЕМЕННО] ... раз в 2 секунды, сверх обычной регенерации Minecraft

    // ---------- Голод ----------
    public static final float HUNGER_DRAIN_FACTOR = 0.6F;           // [ВРЕМЕННО] у гулей голод тратится в 0,6 раза от обычного
}
