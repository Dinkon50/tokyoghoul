package com.tokyoghoulrpg.style;

/**
 * Все числа стилей боя в одном месте. Время в тиках (20 тиков = 1 секунда).
 * Сила боссов стилей посчитана tools/boss_power.py: у всех трёх ~239 — чуть выше середины всех боссов мода
 * (сильнее Пожирателя и Старшего следователя, слабее Кровавого полугуля и Алого Монарха).
 */
public final class StyleBalance {
    private StyleBalance() {
    }

    // ---------- Очки мастерства (своё дерево стиля, основное дерево и его лимит 85% не трогаются)
    public static final int KILLS_PER_POINT = 15;                   // 15 убитых врагов — очко мастерства
    public static final int BOSS_POINTS = 2;                        // любой босс мода — 2 очка
    public static final int FIRST_WIN_POINTS = 3;                   // первая победа над боссом стиля — 3 очка
    public static final int[] COSTS = {1, 2, 3};                    // уровни ветки: 6 очков, всё дерево — 18
    public static final int SWITCH_COST = 2000;                     // сменить стиль у наставника (очки крови)
    public static final int CHALLENGE_COOLDOWN = 24000;             // вызов босса стиля — раз в игровые сутки
    public static final double CHALLENGE_RADIUS = 48.0;             // кто рядом с боссом при победе — открывает стиль

    // ---------- Боец: что даёт сам стиль
    public static final double FIGHTER_BASE_DAMAGE = 0.05;          // +5% урона в ближнем бою
    // «Серия»: каждый удар подряд (перерыв не больше 2 с) добавляет урон, до 5 раз
    public static final double[] COMBO_PER_HIT = {0.03, 0.04, 0.05};
    public static final int COMBO_MAX = 5;
    public static final int COMBO_WINDOW = 40;
    // «Ответный удар»: после удачного блока (или контратаки) следующий удар в течение 3 с сильнее
    public static final double[] RIPOSTE_BONUS = {0.25, 0.40, 0.60};
    public static final int RIPOSTE_WINDOW = 60;
    // «Подвижность»: рывок и способности стиля перезаряжаются быстрее
    public static final double[] FOOTWORK_COOLDOWN = {0.10, 0.20, 0.30};

    // ---------- Боец: способности (клавиша C — первая, Shift + C — вторая)
    public static final int FLURRY_COOLDOWN = 12 * 20;              // «Шквал ударов»
    public static final int FLURRY_HITS = 6;                        // 6 ударов по 3 тика
    public static final int FLURRY_INTERVAL = 3;
    public static final double FLURRY_HIT = 0.45;                   // каждый — 45% твоего урона,
    public static final double FLURRY_LAST = 1.5;                   // последний — 150% и отбрасывает
    public static final double FLURRY_RANGE = 4.0;
    public static final int COUNTER_COOLDOWN = 15 * 20;             // «Контратака»
    public static final int COUNTER_STANCE = 25;                    // стойка 1,25 с
    public static final double COUNTER_DAMAGE = 3.0;                // ответ: 300% твоего урона
    public static final int COUNTER_INVULNERABLE = 10;              // после ответа полсекунды не ранят

    // ---------- Мастер додзё (босс стиля «Боец»), сила ~239
    public static final double MASTER_HEALTH = 1240.0;
    public static final double MASTER_ATTACK = 17.0;
    public static final double MASTER_ARMOR = 12.0;
    public static final double MASTER_TOUGHNESS = 4.0;
    public static final float MASTER_FLURRY_HIT = 4.5F;             // «Шквал»: 4 удара по 4,5 и последний x1,6
    public static final float MASTER_FLURRY_LAST = 1.6F;
    public static final int MASTER_FLURRY_COOLDOWN = 7 * 20;
    public static final float MASTER_PALM_DAMAGE = 16.0F;           // «Удар ладонью»: рывок и толчок
    public static final int MASTER_PALM_COOLDOWN = 8 * 20;
    public static final float MASTER_COUNTER_DAMAGE = 20.0F;        // «Стойка»: поймал удар — ответ
    public static final int MASTER_GUARD_COOLDOWN = 10 * 20;
    public static final int MASTER_GUARD_TICKS = 40;                // стойка 2 с
    public static final double MASTER_GUARD_REDUCTION = 0.8;        // удары спереди в стойке слабее на 80%
    // ---------- Вторая половина здоровья у всех боссов стилей
    public static final float BOSS_PHASE2_DAMAGE = 1.15F;           // урон +15%,
    public static final double BOSS_PHASE2_COOLDOWN = 0.75;         // особые атаки чаще,
    public static final double BOSS_PHASE2_SPEED = 0.2;             // скорость +20%

    // ---------- Тень: что даёт сам стиль
    public static final double SHADOW_BACK_DAMAGE = 0.15;           // удар в спину: +15% урона
    // «Удар в спину»: ещё больше урона сзади
    public static final double[] BACKSTAB_BONUS = {0.15, 0.25, 0.40};
    // «Тихий шаг»: завеса дольше (тики) и быстрее бег в ней (уровень Скорости 1 / 2 / 3)
    public static final int[] SILENT_EXTRA = {20, 40, 60};
    // «Засада»: урон по тому, кто сейчас бьётся не с тобой (не смотрит на тебя как на цель)
    public static final double[] AMBUSH_BONUS = {0.20, 0.35, 0.50};
    // ---------- Тень: способности
    public static final int SMOKE_COOLDOWN = 16 * 20;               // «Дымовая завеса»
    public static final int SMOKE_TICKS = 4 * 20;                   // невидимость 4 с
    public static final double SMOKE_RADIUS = 12.0;                 // враги в этом радиусе теряют тебя
    public static final double SMOKE_FIRST_HIT = 1.0;               // первый удар из завесы: +100%
    public static final double SMOKE_NOTICE = 2.5;                  // в завесе враг замечает только вплотную
    public static final int DECOY_COOLDOWN = 14 * 20;               // «Подмена»
    public static final int DECOY_TICKS = 4 * 20;                   // приманка стоит 4 с («Тихий шаг» добавляет)
    public static final double DECOY_RADIUS = 16.0;                 // враги рядом бросаются на приманку
    public static final double DECOY_BLAST = 4.0;                   // лопнула — дым: слепота и замедление 2 с
    public static final int DECOY_BLIND = 40;

    // ---------- Жнец: что даёт сам стиль
    public static final double REAPER_LOW_DAMAGE = 0.10;            // +10% урона по цели слабее половины здоровья
    // «Точность»: шанс крита (x1,5) каждым ударом
    public static final double[] PRECISION_CHANCE = {0.08, 0.14, 0.20};
    public static final double PRECISION_CRIT = 0.5;
    // «Добивание»: урон по цели слабее 30% здоровья и порог «Казни»
    public static final double[] EXECUTE_BONUS = {0.15, 0.25, 0.40};
    public static final double[] EXECUTE_THRESHOLD = {0.05, 0.10, 0.15};
    public static final double EXECUTE_LOW = 0.30;
    // «Жатва»: за убийство — лечение (доля здоровья), Скорость 3 с и откат «Казни» короче (тики)
    public static final double[] HARVEST_HEAL = {0.04, 0.06, 0.08};
    public static final int[] HARVEST_REFUND = {40, 60, 80};
    // ---------- Жнец: способности
    public static final int EXECUTION_COOLDOWN = 14 * 20;           // «Казнь»
    public static final double EXECUTION_RANGE = 5.0;
    public static final double EXECUTION_THRESHOLD = 0.25;          // цель слабее 25% здоровья —
    public static final double EXECUTION_LOW = 6.0;                 //   600% твоего урона,
    public static final double EXECUTION_NORMAL = 1.5;              //   иначе 150%
    public static final double EXECUTION_REFUND = 0.5;              // убил «Казнью» — половина отката назад
    public static final int MARK_COOLDOWN = 18 * 20;                // «Метка смерти»
    public static final int MARK_TICKS = 8 * 20;
    public static final double MARK_RANGE = 16.0;
    public static final double MARK_BONUS = 0.20;                   // меченый получает +20% урона от всех

    // ---------- Клоун из Пьеро (босс стиля «Тень»), сила ~239
    public static final double CLOWN_HEALTH = 1240.0;
    public static final double CLOWN_ATTACK = 15.0;
    public static final double CLOWN_ARMOR = 6.0;
    public static final float CLOWN_SHADOW_STRIKE = 22.0F;          // «Удар из тени»: исчез, появился за спиной
    public static final int CLOWN_SHADOW_COOLDOWN = 9 * 20;
    public static final float CLOWN_KNIFE = 3.5F;                   // «Веер ножей»: 5 ножей по 3,5
    public static final int CLOWN_KNIVES = 5;
    public static final int CLOWN_FAN_COOLDOWN = 7 * 20;
    public static final int CLOWN_DOUBLES = 2;                      // «Двойники»: двое на 20 с
    public static final int CLOWN_DOUBLE_TICKS = 20 * 20;
    public static final int CLOWN_DOUBLES_COOLDOWN = 20 * 20;
    public static final double CLOWN_DOUBLE_HEALTH = 20.0;
    public static final double CLOWN_DOUBLE_ATTACK = 4.0;
    public static final int CLOWN_DODGE_EVERY = 4;                  // уворачивается от каждого 4-го удара вблизи

    // ---------- Белый жнец (босс стиля «Жнец»), сила ~239
    public static final double REAPER_HEALTH = 1090.0;
    public static final double REAPER_ATTACK = 20.0;
    public static final double REAPER_ARMOR = 14.0;
    public static final double REAPER_TOUGHNESS = 6.0;
    public static final float REAPER_LUNGE = 18.0F;                 // «Выпад»: рывок насквозь по линии
    public static final int REAPER_LUNGE_COOLDOWN = 6 * 20;
    public static final float REAPER_EXECUTE = 15.0F;               // «Казнь»: 15, по цели слабее половины — 30
    public static final float REAPER_EXECUTE_LOW = 30.0F;
    public static final int REAPER_EXECUTE_COOLDOWN = 12 * 20;
    public static final int REAPER_MARK_TICKS = 6 * 20;             // «Метка смерти» на цель: 6 с из 15
    public static final int REAPER_MARK_COOLDOWN = 15 * 20;
}
