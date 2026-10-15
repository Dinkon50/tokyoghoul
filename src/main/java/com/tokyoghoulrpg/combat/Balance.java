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
    public static final float BARRAGE_DAMAGE_PER_HIT = 0.6F;        // 32 удара = 19,2 урона (было 0,5 = 16)
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
    public static final double GRAB_RANGE_BASE = 10.0;              // [ТВОЁ] захваты без прокачки: 10 блоков
    public static final double GRAB_RANGE_PER_LEVEL = 5.0;          // [ТВОЁ] +5 за уровень: 15, 20, 25 (максимум)
    public static final int GRAPPLE_COOLDOWN = 5 * 20;              // [СПЕКА] 5 секунд
    public static final double GRAPPLE_STOP_BLOCK = 1.0;            // [ВРЕМЕННО] на каком расстоянии остановиться у блока
    public static final double GRAPPLE_STOP_ENTITY = 1.6;           // [ВРЕМЕННО] то же у существа
    public static final double GRAPPLE_MAX_SPEED = 1.8;             // [ВРЕМЕННО] блоков за тик
    public static final int PULL_COOLDOWN = 10 * 20;                // [СПЕКА] 10 секунд
    public static final double PULL_STOP = 1.4;                     // [ВРЕМЕННО] на каком расстоянии остановить цель
    public static final double PULL_MAX_SPEED = 1.4;                // [ВРЕМЕННО] блоков за тик

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
    public static final double SKILL_DAMAGE_PER_LEVEL = 0.06;       // +6% урона за уровень (до +30%) — урон кагуне
    public static final float SKILL_REGEN_PER_LEVEL = 1.0F;         // +1 здоровья за уровень (до 5 = 2,5 сердца)
    public static final int SKILL_REGEN_INTERVAL = 5 * 20;          // [ВРЕМЕННО] ... раз в 5 секунд
    public static final double SKILL_DEFENSE_PER_LEVEL = 0.05;      // -5% получаемого урона за уровень (до -25%)
    public static final double SKILL_SPEED_PER_LEVEL = 0.03;        // +3% скорости за уровень (до +15%)
    // Защита от снарядов — часть веток «Защита» (гуль, полугуль) и «Стойкость» (человек, агент CCG)
    public static final double PROJECTILE_DEFENSE_PER_LEVEL = 0.06; // ещё -6% урона от стрел, трезубцев, файерболов и т. п. за уровень (до -30%)
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
    /** Обычной прокачкой можно открыть не больше этой доли дерева своего класса (по очкам). Всё дерево — только командой. */
    public static final double SKILL_CAP_SHARE = 0.85;
    public static final double HARD_SKILL_COST_FACTOR = 1.5;        // навыки в 1,5 раза дороже (округление вверх)
    // Игроки на сервере (нормальная и сложная): каждый игрок сверх первого, но не больше пяти игроков всего
    public static final int PLAYER_SCALING_CAP = 5;
    // Старший следователь (второй по силе босс): квинке IXA в руках и особые атаки
    public static final float CCG_BOSS_THRUST_DAMAGE = 9.0F;        // «Разряд квинке»: линия до 14 блоков
    public static final double CCG_BOSS_THRUST_RANGE = 14.0;
    public static final int CCG_BOSS_THRUST_COOLDOWN = 9 * 20;
    public static final float CCG_BOSS_THUNDER_DAMAGE = 8.0F;       // «Удар молнии»: три круга у цели
    public static final double CCG_BOSS_THUNDER_RADIUS = 2.5;
    public static final int CCG_BOSS_THUNDER_COOLDOWN = 13 * 20;
    public static final double BOSS_HEALTH_BUFF = 1.25;             // все боссы мода: +25% здоровья
    public static final double BOSS_DAMAGE_BUFF = 1.15;             // ...и +15% урона (удары и особые атаки)
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
    public static final double GHOUL_BANE_PER_LEVEL = 0.06;         // «Знание гулей»: +6% урона по гулям за уровень (V: +30%)
    public static final double GHOUL_BANE_BOSS_SHARE = 0.5;         // по боссам-гулям — вполовину
    public static final double KAGUNE_GUARD_PER_LEVEL = 0.04;       // «Защита от кагуне»: -4% урона от гулей (III: -12%)
    public static final double CCG_TOUGHNESS_SHARE = 0.6;           // «Стойкость» у агента CCG работает на 60% (у человека полностью)
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
    public static final float QUINQUE_FLURRY_MULT = 0.40F;          // каждый удар 40% урона квинке (было 35%)
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
    public static final double RELEASE_REDUCTION_MIN = 0.12;        // -12% получаемого урона → -25%
    public static final double RELEASE_REDUCTION_MAX = 0.25;
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
    public static final double FINAL_NARUKAMI_CHANCE = 0.7;         // 70% Narukami (послабее), 30% IXA (сильнее)
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
    public static final float SKILL_BARRAGE_DAMAGE = 0.1F;          // +0,1 урона за удар (0,6 → 0,9)
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
    // Ветка «Здоровье» (у всех классов): 5 уровней, 20 очков за ветку
    public static final int VITALITY_LEVELS = 5;
    public static final int[] VITALITY_COSTS = {2, 3, 4, 5, 6};
    public static final double SKILL_POINT_GAIN = 1.5;               // очки навыков у всех, кроме полугуля, в 1,5 раза быстрее
    public static final double START_HEALTH_BONUS = 4.0;
    public static final double CCG_BASE_SPEED = 0.05;               // агент CCG от природы на 5% быстрее
    public static final double GHOUL_BASE_SPEED = 0.05;             // гуль и полугуль — тоже на 5%            // всем классам +2 сердца с самого начала
    public static final double HUMAN_VITALITY = 9.2;                // человек и агент: 24 -> 70 (3,5 строки) на V
    public static final double GHOUL_VITALITY = 11.2;               // гуль: 44 -> 100 (5 строк) на V (полугуль 48 -> 104)
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

    // ---------- Особые способности (у всех классов, открываются в дереве навыков) [СПЕКА] ----------
    public static final int[] RETRIBUTION_COST = {20};              // «Аура возмездия»: 20 очков (на сложной дороже, как все)
    public static final int[] CHARISMA_COST = {25};                 // «Харизма»: 25 очков
    public static final int AURA_TICKS = 10 * 20;                   // обе ауры держатся 10 секунд
    public static final int AURA_COOLDOWN = 60 * 20;                // откат каждой — 1 минута (считается с нажатия)
    public static final float RETRIBUTION_DAMAGE = 6.0F;            // [ВРЕМЕННО] урон ударившему (3 сердца)
    public static final int RETRIBUTION_STUN = 2 * 20;              // оглушение ударившего 2 секунды
    public static final int RETRIBUTION_SILENCE = 7 * 20;           // 7 секунд без способностей (обычные удары можно)
    public static final double CHARISMA_HEAL = 0.5;                 // удар вместо урона лечит 50% здоровья
    public static final int CHARISMA_BUFF_TICKS = 3 * 20;           // небольшие усиления на 3 секунды

    // «Бросок судьбы»: один из шести исходов [СПЕКА]; шансы в сумме 100%
    public static final int[] FORTUNE_COST = {25};                  // 25 очков (на сложной 38)
    public static final int FORTUNE_COOLDOWN = 60 * 20;             // [ВРЕМЕННО] откат 1 минута
    public static final int FORTUNE_SHIELD_TICKS = 30 * 20;         // защита у баффов держится 30 секунд
    // шанс в процентах, лечение (доля макс. здоровья), защита (доля урона, которая не проходит)
    public static final int FORTUNE_BUFF1_CHANCE = 30;
    public static final double FORTUNE_BUFF1_HEAL = 0.25;
    public static final double FORTUNE_BUFF1_SHIELD = 0.15;         // [ВРЕМЕННО] -15% урона
    public static final int FORTUNE_BUFF2_CHANCE = 15;
    public static final double FORTUNE_BUFF2_HEAL = 0.40;
    public static final double FORTUNE_BUFF2_SHIELD = 0.25;         // [ВРЕМЕННО] -25% урона
    public static final int FORTUNE_BUFF3_CHANCE = 5;
    public static final double FORTUNE_BUFF3_HEAL = 0.75;
    public static final double FORTUNE_BUFF3_SHIELD = 0.35;         // [ВРЕМЕННО] -35% урона
    // дебаффы: доля ТЕКУЩЕГО здоровья, которую теряешь (убить не могут), и откат всех способностей
    public static final int FORTUNE_DEBUFF1_CHANCE = 30;
    public static final double FORTUNE_DEBUFF1_LOSS = 0.25;
    public static final int FORTUNE_DEBUFF2_CHANCE = 15;
    public static final double FORTUNE_DEBUFF2_LOSS = 0.40;
    public static final int FORTUNE_DEBUFF2_LOCK = 3 * 20;
    public static final int FORTUNE_DEBUFF3_CHANCE = 5;
    public static final double FORTUNE_DEBUFF3_LOSS = 0.70;
    public static final int FORTUNE_DEBUFF3_LOCK = 6 * 20;

    // ---------- Кровавый полугуль (самый сильный босс, своя арена) [СПЕКА] ----------
    // Здоровье и урон втрое больше, чем у Пожирателя и Старшего следователя
    public static final double HG_BOSS_HEALTH = 750.0;              // 3 x 250
    public static final double HG_BOSS_ATTACK = 30.0;               // 3 x 10 (удар рукой-кагуне)
    public static final float HG_BOSS_SPECIAL_MULT = 3.0F;          // особые атаки Пожирателя и следователя x3
    public static final float HG_PHASE2_DAMAGE = 1.25F;             // вторая стадия: +25% урона,
    public static final double HG_PHASE2_SPEED = 0.2;               // +20% скорости,
    public static final double HG_PHASE2_COOLDOWN = 0.7;            // особые атаки чаще
    public static final int HG_TRANSFORM_TICKS = 3 * 20;            // превращение: 3 с неуязвим и восстанавливает всё здоровье
    public static final double HG_PER_PLAYER_HEALTH = 0.5;          // +50% здоровья за каждого игрока на сервере сверх первого (до 5)
    public static final double HG_PER_PLAYER_DAMAGE = 0.2;          // +20% урона за каждого игрока сверх первого
    public static final int HG_RESET_TICKS = 30 * 20;               // 30 с без игроков рядом — бой сбрасывается
    public static final double HG_ARENA_RADIUS = 18.0;              // радиус арены
    public static final int HG_RESPAWN_TICKS = 2 * 60 * 20;         // возрождение через 2 минуты
    // дальние атаки и атаки по области (базовый урон, умножается на HG_BOSS_SPECIAL_MULT)
    public static final float HG_SHARD_DAMAGE = 2.5F;               // залп осколков: 5 выстрелов
    public static final int HG_SHARD_COOLDOWN = 7 * 20;
    public static final float HG_SPEAR_DAMAGE = 9.0F;               // копьё-кагуне: пробивает всех на линии и блок
    public static final double HG_SPEAR_RANGE = 30.0;
    public static final int HG_SPEAR_COOLDOWN = 12 * 20;
    public static final float HG_NOVA_DAMAGE = 7.0F;                // кровавая волна вокруг себя
    public static final double HG_NOVA_RADIUS = 7.0;
    public static final int HG_NOVA_COOLDOWN = 11 * 20;
    public static final float HG_SPIKE_DAMAGE = 5.0F;               // шипы из земли под игроками
    public static final double HG_SPIKE_RADIUS = 2.5;
    public static final int HG_SPIKE_COOLDOWN = 14 * 20;
    // свита
    public static final int HG_MINION_LIMIT = 8;                    // не больше 8 полугулей сразу
    // награды
    public static final int HG_TOP_BLOOD = 10000;                   // двое, нанёсшие больше всего урона
    public static final int HG_TOP_POINTS = 30;
    public static final int HG_TOP_LEVELS = 3;
    public static final int HG_OTHER_BLOOD = 1000;                  // остальные участники
    public static final int HG_OTHER_POINTS = 5;
    public static final int RESET_SEAL_PRICE = 5000;                // печать сброса навыков у торговца

    // Алый Монарх — финальный босс, сильнее Кровавого полугуля
    public static final double MONARCH_HEALTH = 1000.0;             // на каждую из трёх стадий (полугуль 750 и полторы стадии)
    public static final double MONARCH_ATTACK = 40.0;               // удар (полугуль 30)
    public static final float MONARCH_SPECIAL_MULT = 4.0F;          // особые атаки x4 (у полугуля x3)
    public static final double MONARCH_PHASE_DAMAGE = 0.25;         // +25% урона за каждую стадию после первой
    public static final double MONARCH_PHASE_SPEED = 0.12;          // +12% скорости за каждую стадию после первой
    public static final double[] MONARCH_COOLDOWN = {0.85, 0.65, 0.5}; // особые атаки чаще по стадиям
    public static final int MONARCH_BLOOD = 25000;                  // награда: только очки крови, каждому, кто нанёс урон

    // Личная репутация и репутация клана (от -100 до 100, начинаются с 0)
    public static final int STANDING_HIGH = 40;                     // от 40 — высокая: бывают скидки
    public static final int STANDING_LOW = -25;                     // до -25 — низкая: приходят волны NPC
    public static final int STANDING_TRADE = 1;                     // сделка с торговцем: +1 тебе и клану...
    public static final int STANDING_TRADE_DAILY = 10;              // ...не больше 10 сделок за игровые сутки
    public static final int STANDING_BOSS = 10;                     // убил босса (любого): +10 тебе и клану
    public static final int STANDING_RAID_BOSS = 20;                // Кровавый полугуль: +20 каждому участнику и его клану
    public static final int STANDING_KILL_PEACEFUL = 15;            // убил мирного (житель, торговец, информатор): -15 тебе...
    public static final int STANDING_KILL_PEACEFUL_CLAN = 10;       // ...и -10 клану
    public static final int STANDING_DEATH = 2;                     // погиб: -2 тебе...
    public static final int STANDING_DEATH_CLAN = 1;                // ...и -1 клану
    // «Удачный день» у торговцев при высокой репутации
    public static final int DEAL_CHANCE = 35;                       // своя высокая: шанс 35% в игровые сутки...
    public static final int DEAL_PERCENT = 15;                      // ...скидка 15% в лавке торговца мода
    public static final int DEAL_CHANCE_CLAN = 60;                  // своя и клана высокие: шанс 60%...
    public static final int DEAL_PERCENT_CLAN = 25;                 // ...скидка 25% и «Герой деревни» у жителей
    // Волны NPC за игроком с плохой репутацией: гули и агенты CCG вместе
    public static final int WAVE_BIG_SIZE = 6;                      // большая волна: 6 (на сложной 8)
    public static final int WAVE_SMALL_SIZE = 3;                    // маленькая: 3 (на сложной 4)
    public static final int WAVE_BIG_INTERVAL = 15 * 60 * 20;       // большая примерно раз в 15 минут
    public static final int WAVE_SMALL_INTERVAL = 20 * 60 * 20;     // маленькая примерно раз в 20 минут
    public static final int WAVE_FIRST_DELAY = 3 * 60 * 20;         // первая — через ~3 минуты после падения репутации
    public static final int WAVE_LIFETIME = 5 * 60 * 20;            // через 5 минут волна уходит
    public static final double WAVE_FOLLOW_BONUS = 40.0;            // волна чует цель издалека (+40 блоков обзора)

    // Передача силы в клане (плюсик в меню клана)
    public static final int BOND_TICKS = 30 * 20;                   // сила и здоровье у получателя 30 секунд
    public static final int BOND_COOLDOWN = 2 * 60 * 20;            // потом 2 минуты все участники без передачи
    public static final int BOND_MAX_DONORS = 2;                    // одному игроку — не больше двух дарителей
    public static final double BOND_HP_FIRST = 0.5;                 // первый даритель отдаёт половину своего здоровья,
    public static final double BOND_WEAKEN_FIRST = 0.5;             // сам бьёт на 50% слабее,
    public static final double BOND_BONUS_FIRST = 0.30;             // получатель бьёт на 30% сильнее
    public static final double BOND_HP_SECOND = 0.25;               // второй даритель: четверть здоровья,
    public static final double BOND_WEAKEN_SECOND = 0.25;           // сам на 25% слабее,
    public static final double BOND_HP_GAIN = 1.25;                  // получатель получает на четверть больше здоровья, чем отдал даритель
    public static final double BOND_BONUS_SECOND = 0.15;            // получателю ещё +15% (всего +45%)

    // Ядра гулей и улучшение квинке
    public static final double CORE_1_CHANCE = 0.02;                // ядро I: 2% с обычных гулей и полугулей
    public static final double CORE_2_CHANCE = 0.08;                // ядро II: 8% с Пожирателя
    public static final double CORE_3_CHANCE = 0.10;                // ядро III: 10% у каждого из двух лучших по урону с Кровавого полугуля
    public static final int[] CORE_PRICE = {300, 1500, 6000};       // продажа торговцу (купить ядро нельзя нигде)
    public static final double[] QUINQUE_LEVEL_BONUS = {0.0, 0.15, 0.30, 0.50}; // урон квинке на уровнях 0-III
    public static final int QUINQUE_LOCK_TICKS = 10 * 60 * 20;      // 10 минут после улучшения квинке только в инвентаре

    // Кофейня
    public static final double CAFE_RADIUS = 8.5;                   // мирная зона: квадрат 17 x 17 вокруг знака кофейни
    public static final int CAFE_HEIGHT = 8;                        // и 8 блоков вверх
    public static final int CAFE_CLOSE_TIME = 12000;                // закрывается на закате (время суток 12000)...
    public static final int CAFE_WARNING_TICKS = 60 * 20;           // ...за минуту до этого таймер
    public static final int CAFE_COFFEE_PRICE = 40;                 // цены в очках крови
    public static final int CAFE_FLESH_PRICE = 60;
    public static final int CAFE_FIGHT_TICKS = 60 * 20;

    // Какудзя (гуль и полугуль): копится, когда поедаешь гулей
    public static final int KAKUJA_UNLOCK = 100;                    // столько очков нужно, чтобы открыть форму
    public static final int KAKUJA_BITE = 3;                        // укус гуля
    public static final int KAKUJA_KILL_NPC = 2;                    // убил гуля или полугуля (NPC)
    public static final int KAKUJA_KILL_PLAYER = 8;                 // убил игрока-гуля или полугуля
    public static final int KAKUJA_KILL_BOSS = 15;                  // Пожиратель
    public static final int KAKUJA_RAID_BOSS = 30;                  // Кровавый полугуль (участникам)
    public static final int KAKUJA_TICKS = 30 * 20;                 // форма длится 30 секунд
    public static final int KAKUJA_COOLDOWN = 5 * 60 * 20;          // откат 5 минут
    public static final double KAKUJA_DAMAGE = 0.40;                // +40% урона
    public static final double KAKUJA_DEFENSE = 0.30;               // -30% входящего урона
    public static final double KAKUJA_SPEED = 0.20;                 // +20% скорости

    // Полугуль: чуть сильнее гуля, но навыки чуть дороже
    public static final double HALF_GHOUL_DAMAGE = 0.10;            // +10% урона
    public static final double HALF_GHOUL_HEALTH = 4.0;             // +2 сердца (44 вместо 40)
    public static final double HALF_GHOUL_COST = 1.2;               // навыки в 1,2 раза дороже (округление)

    // «Выдержка» (у всех): короче оглушение. Оглушение после сломанного блока не уменьшает
    public static final double[] STUN_RESIST = {0.0, 0.20, 0.35, 0.50};

    // Ранги: агент CCG (звания) и гуль/полугуль (рейтинг опасности). У людей ранга нет
    public static final int[] RANK_THRESHOLDS = {0, 50, 150, 350, 700, 1200};
    public static final int RANK_KILL_NPC = 2;                      // враг-NPC (агент для гуля, гуль для агента)
    public static final int RANK_KILL_PLAYER = 5;                   // игрок враждебной стороны
    public static final int RANK_KILL_BOSS = 15;                    // Пожиратель / Старший следователь
    public static final int RANK_RAID_BOSS = 30;                    // Кровавый полугуль (участникам)
    public static final int RANK_QUEST = 10;                        // выполнил задание
    public static final int RANK_CONTRACT = 8;                      // сдал контракт
    public static final double RANK_DAMAGE_PER = 0.02;              // +2% урона за каждый ранг выше первого (до +10%)
    public static final double RANK_BLOOD_PER = 0.05;               // +5% крови за убийства за каждый ранг (до +25%)             // драка игроков меньше минуты назад: в кофейне они не защищены друг от друга

    // Тип кагуне (гуль и полугуль): у каждого свой пассивный бонус
    public static final int KAGUNE_TYPE_CHANGE_COST = 2000;         // первый выбор бесплатно, сменить — 2000 очков крови
    public static final double RINKAKU_REGEN = 0.6;                 // ринкаку: регенерация гуля и ветка «Регенерация» +60%
    public static final double UKAKU_SPEED = 0.08;                  // укаку: +8% скорости...
    public static final int UKAKU_DASH_TICKS = 1;                   // ...и рывок длиннее (+0,85 блока)
    public static final double KOUKAKU_DEFENSE = 0.10;              // коукаку: -10% входящего урона...
    public static final double KOUKAKU_KNOCKBACK = 0.20;            // ...и меньше отбрасывания
    public static final double BIKAKU_DAMAGE = 0.05;                // бикаку: понемногу всего: +5% урона,
    public static final double BIKAKU_DEFENSE = 0.05;               // -5% входящего урона
    public static final double BIKAKU_SPEED = 0.03;                 // и +3% скорости

    // Маски (только гуль и полугуль): обычная / работа мастера (из мастерской масок)
    public static final double[] MASK_FANG_DAMAGE = {0.06, 0.10};   // клыкастая: урон
    public static final double[] MASK_BONE_DEFENSE = {0.06, 0.10};  // костяная: меньше входящего урона
    public static final double[] MASK_CROW_SPEED = {0.06, 0.10};    // маска ворона: скорость
    public static final double[] MASK_ONI_LIFESTEAL = {0.04, 0.07}; // маска демона: часть урона возвращается здоровьем
    public static final int MASK_PRICE = 300;                       // мастерская: обычная маска за очки крови
    public static final int MASK_MASTER_PRICE = 1500;               // маска на заказ (работа мастера)
    public static final double MASK_WORKSHOP_REACH = 6.0;           // как близко нужно стоять к мастерской

    // «Ночь охоты»: каждую третью ночь по миру бродят усиленные отряды CCG и стаи гулей
    public static final int HUNT_NIGHT_EVERY_DAYS = 3;              // раз в 3 игровых дня (третья ночь: дни 3, 6, 9...)
    public static final int HUNT_NIGHT_START = 13000;               // ночь: время суток 13000...
    public static final int HUNT_NIGHT_END = 23000;                 // ...до 23000 (рассвет)
    public static final int HUNT_NIGHT_WARNING = 12000;             // на закате предупреждение
    public static final int HUNT_NIGHT_GROUP_INTERVAL = 90 * 20;    // рядом с каждым игроком группа примерно раз в 1,5 минуты
    public static final int HUNT_NIGHT_FIRST_DELAY = 15 * 20;       // первая — через 15 секунд после начала ночи
    public static final int HUNT_NIGHT_GROUP_SIZE = 3;              // 3 в группе (на сложной 4)
    public static final int HUNT_NIGHT_MAX_NEAR = 8;                // больше 8 ночных охотников рядом с игроком не будет
    public static final double HUNT_NIGHT_HEALTH = 0.50;            // ночные охотники: +50% здоровья,
    public static final double HUNT_NIGHT_DAMAGE = 0.25;            // +25% урона,
    public static final double HUNT_NIGHT_SPEED = 0.10;             // +10% скорости,
    public static final double HUNT_NIGHT_FOLLOW = 16.0;            // чуют на 16 блоков дальше
    public static final double HUNT_NIGHT_LEADER_CHANCE = 0.25;     // в стае гулей вожак-полугуль с шансом 25%
    public static final double HUNT_NIGHT_ENEMY_SIDE = 0.65;        // чаще приходит сторона, враждебная игроку
    public static final int HUNT_NIGHT_REWARD = 2;                  // очки крови за NPC и боссов в эту ночь x2
    public static final int HUNT_NIGHT_HUNTER_POINTS = 1;           // и за каждого ночного охотника ещё очко навыков

    // Подсказки для новичков
    public static final int TUTORIAL_HINT_TICKS = 9 * 20;           // каждая подсказка видна 9 секунд (пока открыт экран, время стоит)

    // «Облик гуля»: полный комплект одежды гуля + любая маска (только гуль и полугуль)
    public static final double GHOUL_SET_DAMAGE = 0.10;             // +10% урона
    public static final double GHOUL_SET_DEFENSE = 0.10;            // -10% входящего урона
    public static final double GHOUL_SET_SPEED = 0.05;              // +5% скорости
    // плюс ночное зрение; складывается с эффектом самой маски

    // «Защита CCG»: агент CCG в полном комплекте брони CCG (форма или особый класс, можно вперемешку)
    public static final double CCG_SET_DEFENSE = 0.05;              // -5% входящего урона от всего
    public static final double CCG_SET_GHOUL_DEFENSE = 0.05;        // и ещё -5% от гулей (итого около -10%)

    // Подопытный (лаборатория RC): неудачный эксперимент, нападает на всех
    public static final double SUBJECT_HEALTH = 36.0;
    public static final double SUBJECT_DAMAGE = 6.0;
    public static final double SUBJECT_SPEED = 0.32;
    public static final float SUBJECT_INFECT_CHANCE = 0.25F;        // удар: шанс «RC-заражения» (слабость и голод на 4 с)
    public static final int SUBJECT_INFECT_TICKS = 4 * 20;
    public static final float SUBJECT_NOTES_CHANCE = 0.08F;         // шанс выпасть записям лаборатории

    // Новые предметы
    public static final float MEDKIT_HEAL = 8.0F;                   // аптечка: 4 сердца сразу...
    public static final int MEDKIT_REGEN_TICKS = 5 * 20;            // ...и Регенерация I на 5 с
    public static final int MEDKIT_COOLDOWN = 10 * 20;              // откат 10 с
    public static final double SMOKE_RADIUS = 6.0;                  // дымовая шашка: радиус облака
    public static final int SMOKE_BLIND_TICKS = 4 * 20;             // враги слепнут и теряют тебя из виду на 4 с
    public static final int SMOKE_INVIS_TICKS = 4 * 20;             // ты невидим и быстрее 4 с
    public static final int SMOKE_COOLDOWN = 20 * 20;               // откат 20 с
    public static final int LAB_NOTES_POINTS = 2;                   // записи лаборатории: 2 очка навыков (x1,5, кроме полугуля)

    // Скупщик (чёрный рынок): покупает добычу за очки крови и продаёт аптечки и шашки
    public static final int FENCE_MEDKIT_PRICE = 120;
    public static final int FENCE_SMOKE_PRICE = 200;

    // Новые ветки (вкладка «Особые», у всех классов)
    public static final double[] SCAVENGER_BLOOD = {0.0, 0.10, 0.20, 0.30};   // «Добытчик»: больше крови за убийства
    public static final double[] FIRST_AID_HEAL = {0.0, 0.25, 0.50};          // «Первая помощь»: аптечка лечит больше...
    public static final int[] FIRST_AID_COOLDOWN = {10 * 20, 7 * 20, 5 * 20}; // ...и откат меньше

    // Стадии Какудзя: чем больше гулей съел, тем сильнее (и это видно — аура, глаза, частицы)
    public static final int[] KAKUJA_STAGES = {25, 50, 100, 200, 350};   // I, II, III (форма открыта), IV, V
    public static final double KAKUJA_STAGE_DAMAGE = 0.03;          // +3% урона за стадию (на V +15%), всегда
    public static final double KAKUJA_STAGE_DEFENSE = 0.02;         // -2% входящего урона за стадию (на V -10%), всегда
    public static final double KAKUJA_FORM_STAGE_BONUS = 0.05;      // форма на IV и V: ещё +5% урона и защиты за стадию
    public static final int KAKUJA_FORM_STAGE_TICKS = 5 * 20;       // и на 5 с дольше за стадию
}
