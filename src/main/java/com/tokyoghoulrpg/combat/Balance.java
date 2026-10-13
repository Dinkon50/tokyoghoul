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

    // Сложность мира (выбирается при создании мира, её можно менять в настройках) [СПЕКА, числа ВРЕМЕННО]
    // Лёгкая (и мирная): всё как раньше.
    public static final double NORMAL_NPC_HEALTH = 1.25;            // нормальная: NPC мода +25% здоровья...
    public static final double NORMAL_NPC_DAMAGE = 1.15;            // ...и +15% урона
    public static final double HARD_HEALTH = 1.75;                  // сложная: все враждебные мобы +75% здоровья,
    public static final double HARD_DAMAGE = 1.5;                   // +50% урона,
    public static final double HARD_SPEED = 1.15;                   // +15% скорости,
    public static final double HARD_FOLLOW = 1.5;                   // замечают в 1,5 раза дальше (и дальше зовут подмогу),
    public static final double HARD_KNOCKBACK_RESIST = 0.2;         // хуже отлетают от ударов,
    public static final double HARD_BOSS_COOLDOWN = 0.7;            // боссы чаще делают особые атаки
    public static final double HARD_SKILL_COST_FACTOR = 1.5;        // навыки в 1,5 раза дороже (округление вверх)
    // Игроки на сервере (нормальная и сложная): каждый игрок сверх первого, но не больше пяти игроков всего
    public static final int PLAYER_SCALING_CAP = 5;
    public static final double PER_PLAYER_HEALTH = 0.15;            // +15% здоровья враждебным мобам за игрока
    public static final double PER_PLAYER_DAMAGE = 0.05;            // +5% их урона за игрока

    // Еда гуля [ВРЕМЕННО]
    public static final int FLESH_CRAVING = 35;                     // плоть: +35 к шкале «Кровь»
    public static final int FLESH_HUMAN_NAUSEA = 200;               // человека от плоти мутит 10 с
    public static final int VOMIT_FOOD_LOSS = 2;                    // гуль съел обычную еду: -1 окорочок сверх съеденного
    public static final int VOMIT_NAUSEA = 200;                     // тошнота 10 с
    public static final int VOMIT_HUNGER = 300;                     // Голод II 15 с
    public static final int FLESH_DROP_MIN = 1;                     // из убитого гулем человека выпадает 1-2 плоти
    public static final int FLESH_DROP_MAX = 2;

    // Рейды CCG [ВРЕМЕННО]
    public static final int RAID_CHECK_TICKS = 100;                 // проверка раз в 5 с
    public static final int RAID_INTERVAL = 24000;                  // обычно раз в 20 минут (игровые сутки)...
    public static final double RAID_INTERVAL_MIN_FACTOR = 0.5;      // ...а при репутации CCG -100 вдвое чаще
    public static final int RAID_SQUAD_MIN = 2;                     // отряд: 2 агента, при репутации -100 до 4,
    public static final int RAID_SQUAD_MAX = 4;                     // на сложной игре ещё +1
    public static final double RAID_DIST_MIN = 18.0;                // появляются в 18-26 блоках от игрока
    public static final double RAID_DIST_MAX = 26.0;
    public static final int RAID_WANTED_REP = -50;                  // людей с репутацией CCG -50 и ниже тоже ищут

    // Репутация [ВРЕМЕННО]: от -100 до 100
    public static final int REP_KILL_NPC_FRIEND = 2;                // убил гуля: CCG +2, гули -3 (и наоборот)
    public static final int REP_KILL_NPC_ENEMY = 3;
    public static final int REP_KILL_BOSS_FRIEND = 15;              // убил босса: +15 одной стороне, -20 другой
    public static final int REP_KILL_BOSS_ENEMY = 20;
    public static final int REP_CONTRACT = 5;                       // контракт на охоту: +5 заказчику
    public static final int REP_CONTRACT_OTHER = 2;                 // другие контракты: +2 своей стороне
    public static final int REP_FRIENDLY = 50;                      // от 50: сторона тебя не трогает
    public static final int REP_HOSTILE = -30;                      // до -30: сторона нападает, даже если ты «свой»
    public static final int REP_INFORMANT_DISTRUST = -50;           // своя сторона -50 и ниже: информатор не даёт работу
    public static final double REP_PRICE_DISCOUNT = 0.2;            // цены в лавке: до -20% при +100 своей стороны...
    public static final double REP_PRICE_MARKUP = 0.25;             // ...и до +25% при -100
    public static final double REP_REWARD_BONUS = 0.5;              // награды контрактов: от x0,5 до x1,5

    // Копание кагуне: ЛКМ по блоку с выпущенным кагуне [ВРЕМЕННО]
    public static final float KAGUNE_MINING_SPEED = 2.0F;           // в 2 раза быстрее

    // Двойной прыжок и падение [СПЕКА, числа ВРЕМЕННО]
    public static final double DOUBLE_JUMP_VELOCITY = 0.5;          // второй прыжок чуть выше обычного (0,42)
    public static final int DOUBLE_JUMP_MIN_AIR_TICKS = 2;          // не раньше чем через 2 тика после отрыва от земли
    public static final double DOUBLE_JUMP_APEX_SPEED = 0.12;       // срабатывает у верхней точки первого прыжка
    public static final double DOUBLE_JUMP_BOOST = 0.3;             // толчок в сторону движения
    public static final float FALL_DAMAGE_BASE = 0.8F;              // всем: урон от падения -20%
    public static final double SOFT_LANDING_PER_LEVEL = 0.15;       // «Мягкое приземление»: ещё -15% за уровень

    // Дерево агента CCG, вкладка «Охота на гулей» [ВРЕМЕННО]
    public static final double GHOUL_BANE_PER_LEVEL = 0.08;         // «Знание гулей»: +8% урона по гулям за уровень (V: +40%)
    public static final double KAGUNE_GUARD_PER_LEVEL = 0.06;       // «Защита от кагуне»: -6% урона от гулей (III: -18%)
    public static final int RC_SUPPRESS_BASE = 40;                  // «Подавитель RC»: гуль после удара не лечится 2 с...
    public static final int RC_SUPPRESS_PER_LEVEL = 20;             // ...+1 с за уровень (III: 4 с)
    public static final float FIELD_MEDIC_PER_LEVEL = 1.0F;         // «Полевая аптечка»: +0,5 сердца за уровень...
    public static final int FIELD_MEDIC_INTERVAL = 6 * 20;          // ...раз в 6 с
    public static final double RANK_BLOOD_PER_LEVEL = 0.1;          // «Звание»: +10% очков крови за убийства и контракты

    // Квинке агента CCG [ВРЕМЕННО, пока нет моделей и анимаций]. Урон считается от урона квинке в руке.
    public static final int QUINQUE_HIT_DELAY = 3;                  // удар приходится через 3 тика после нажатия
    public static final int QUINQUE_ATTACK_BUSY = 8;                // следующий удар серии через 0,4 с
    public static final int QUINQUE_FINISHER_BUSY = 14;             // после третьего удара пауза 0,7 с
    public static final float QUINQUE_FINISHER_MULT = 1.5F;         // третий удар серии x1,5
    public static final int QUINQUE_COMBO_RESET = 30;               // серия сбрасывается через 1,5 с без ударов
    public static final double QUINQUE_REACH = 3.5;                 // достаёт на 3,5 блока
    public static final double QUINQUE_HALF_WIDTH = 1.0;
    public static final float QUINQUE_STRONG_MULT = 2.5F;           // ПКМ дважды: x2,5, пробивает блок
    public static final int QUINQUE_STRONG_DELAY = 6;               // замах 0,3 с
    public static final int QUINQUE_STRONG_COOLDOWN = 2 * 20;       // откат 2 с, не меньше
    public static final int QUINQUE_FLURRY_HITS = 10;               // K: 10 ударов...
    public static final int QUINQUE_FLURRY_INTERVAL = 3;            // ...каждые 3 тика (1,5 с)
    public static final float QUINQUE_FLURRY_MULT = 0.35F;          // каждый удар 35% урона квинке
    public static final double QUINQUE_FLURRY_WIDTH = 2.5;
    public static final int QUINQUE_FLURRY_COOLDOWN = 8 * 20;       // откат 8 с после конца серии
    public static final int QUINQUE_PARRY_WINDOW = 10;              // L: парирование, если блок поставлен за 0,5 с до удара
    public static final int QUINQUE_PARRY_STUN = 30;                // атакующий оглушён на 1,5 с
    public static final double QUINQUE_BLOCK_SLOW = 1.8;            // в блоке ходьба в 1,8 раза медленнее
    public static final int QUINQUE_LUNGE_TICKS = 5;                // Shift + ЛКМ: выпад 5 тиков...
    public static final double QUINQUE_LUNGE_SPEED = 1.2;           // ...примерно на 6 блоков
    public static final float QUINQUE_LUNGE_MULT = 1.2F;            // укол x1,2
    public static final int QUINQUE_LUNGE_COOLDOWN = 4 * 20;        // откат 4 с
    // Дерево агента CCG: «Освобождение» (уровни 0-5, как ярость) и прокачка квинке [ВРЕМЕННО]
    public static final double RELEASE_SECONDS_MIN = 15.0;          // без прокачки 15 с, на V — 30 с
    public static final double RELEASE_SECONDS_MAX = 30.0;
    public static final double RELEASE_DAMAGE_MIN = 0.30;           // +30% урона → +50%
    public static final double RELEASE_DAMAGE_MAX = 0.50;
    public static final double RELEASE_REDUCTION_MIN = 0.20;        // -20% получаемого урона → -35%
    public static final double RELEASE_REDUCTION_MAX = 0.35;
    public static final double RELEASE_SPEED_MIN = 0.15;            // +15% скорости → +25%
    public static final double RELEASE_SPEED_MAX = 0.25;
    public static final double RELEASE_GAIN_PER_LEVEL = 0.25;       // «Накопление освобождения»: +25% за уровень
    public static final double Q_COMBO_DAMAGE_PER_LEVEL = 0.10;     // «Серия»: +10% урона обычных ударов
    public static final float Q_FINISHER_PER_LEVEL = 0.25F;         // ...и третий удар сильнее (+0,25 к множителю)
    public static final double Q_STRONG_DAMAGE_PER_LEVEL = 0.15;    // «Сильный удар»: +15% урона
    public static final double Q_STRONG_REACH_PER_LEVEL = 0.3;      // ...и +0,3 блока досягаемости (откат всегда 2 с)
    public static final int Q_FLURRY_HITS_PER_LEVEL = 2;            // «Шквал»: +2 удара
    public static final int Q_FLURRY_COOLDOWN_PER_LEVEL = 26;       // ...и откат на 1,3 с меньше (8 → 4,1 с)
    public static final double Q_PARRY_WINDOW_PER_LEVEL = 0.25;     // «Блок квинке»: окно идеального блока +25%
    public static final int Q_PARRY_STUN_PER_LEVEL = 10;            // ...оглушение после парирования +0,5 с
    public static final double Q_BLOCK_SLOW_PER_LEVEL = 0.25;       // ...в блоке ходишь быстрее
    public static final double Q_LUNGE_DAMAGE_PER_LEVEL = 0.20;     // «Выпад»: +20% урона, дальше на ~1 блок
    public static final int Q_LUNGE_COOLDOWN_PER_LEVEL = 10;        // ...и откат на 0,5 с меньше (4 → 2,5 с)
    public static final double Q_REACH_PER_LEVEL = 0.5;             // «Длина квинке»: +0,5 блока ко всем ударам
    public static final double NARUKAMI_BOLT_RANGE = 16.0;
    // Финальное задание агента: Старший следователь отдаёт квинке [ТВОЁ]
    public static final double FINAL_NARUKAMI_CHANCE = 0.8;         // 80% Narukami, 20% IXA
    public static final int FINAL_BLOOD_MIN = 500;                  // награда: 500-1000 очков крови
    public static final int FINAL_BLOOD_MAX = 1000;           // Narukami: разряд летит на 16 блоков
    public static final int QUINQUE_RELEASE_GAIN = 5;               // J: +5 к шкале за каждое попадание по гулю (20 ударов)
    public static final int QUINQUE_RELEASE_TICKS = 15 * 20;        // усиление на 15 с
    public static final double QUINQUE_RELEASE_DAMAGE = 0.3;        // +30% урона
    public static final double QUINQUE_RELEASE_REDUCTION = 0.2;     // -20% получаемого урона
    public static final double QUINQUE_RELEASE_SPEED = 0.15;        // +15% скорости

    // Секретный навык «Кровавое зеркало» [СПЕКА]
    public static final int[] MIRROR_COST = {30};                   // стоит 30 очков навыков
    public static final double MIRROR_HEALTH = 0.5;                 // работает, когда здоровья меньше 50%
    public static final double MIRROR_FULL_CHANCE = 0.5;            // шанс 50%: урон не проходит и весь отражается
    public static final double MIRROR_SHARE = 0.5;                  // иначе урон проходит, а бьющий получает 50% этого урона
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
