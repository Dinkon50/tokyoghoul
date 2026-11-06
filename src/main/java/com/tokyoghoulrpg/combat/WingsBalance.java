package com.tokyoghoulrpg.combat;

/**
 * Числа «Укаку Тоуки» (крылья, KaguneVariant.WINGS): дальнее кагуне, которое не бьёт, а стреляет осколками.
 * Всё, что можно прокачать, лежит здесь в виде базы и прибавки за уровень; формулы по уровням — skill/WingsEffects.
 * Тики — игровые (20 в секунде), расстояния — в блоках. Все числа [ВРЕМЕННО]: их подгоняют по ощущению в игре.
 */
public final class WingsBalance {
    private WingsBalance() {
    }

    // ---------- Автомат: ЛКМ (держать) ----------
    /** Интервал между выстрелами по уровням «Скорострельности» (0-3), тиков: 6,7 → 11 выстрелов в секунду. */
    public static final double[] SHOT_INTERVAL = {3.0, 2.6, 2.2, 1.8};
    public static final float SHARD_DAMAGE = 1.6F;
    public static final double SHARD_SPEED = 2.4;                // блоков за тик
    public static final double SHARD_RANGE = 24.0;
    public static final double RANGE_PER_LEVEL = 6.0;            // «Дальность»
    public static final double CRYSTAL_PER_LEVEL = 0.12;         // «Кристаллизация»: +12% урона осколка за уровень
    public static final int CRYSTAL_PIERCE_LEVEL = 3;            // с этого уровня осколок пробивает одну цель
    /** Разброс (градусы от прицела) и его рост при долгой стрельбе («Меткость», уровни 0-3). */
    public static final double[] SPREAD = {2.0, 1.4, 0.8, 0.4};
    public static final double[] BLOOM = {2.0, 1.5, 1.0, 0.5};   // во сколько раз разброс растёт на полном нагреве
    public static final double HEAT_PER_SHOT = 0.08;
    public static final double HEAT_DECAY_PER_TICK = 0.04;
    public static final int REPEAT_WINDOW = 4;                   // давно не стреляли: первый выстрел сразу

    // ---------- Большой осколок: ПКМ дважды ----------
    public static final float BIG_DAMAGE = 9.0F;
    public static final double BIG_PER_LEVEL = 0.20;             // +20% урона за уровень «Крупного осколка»
    public static final int BIG_COOLDOWN = 40;                   // 2 с
    public static final int BIG_COOLDOWN_PER_LEVEL = 6;
    public static final int BIG_WINDUP = 5;                      // накопление перед выстрелом
    public static final double BIG_SPEED = 1.8;
    public static final double BIG_RANGE = 40.0;
    public static final double BIG_SPLASH_RADIUS = 2.5;          // с III уровня большой осколок разлетается
    public static final double BIG_SPLASH_SHARE = 0.5;
    public static final int BIG_SPLASH_LEVEL = 3;

    // ---------- Град осколков (барраж): K, держать ----------
    public static final float HAIL_DAMAGE = 0.5F;
    public static final float HAIL_DAMAGE_PER_LEVEL = 0.05F;     // I и далее: 0,55 / 0,6 / 0,65
    public static final int HAIL_INTERVAL = 2;                   // залп раз в 2 тика
    public static final int HAIL_DURATION = 50;                  // не дольше 2,5 с за раз
    public static final int HAIL_PELLETS = 2;                    // осколков в залпе (III уровень: 3)
    public static final int HAIL_PELLETS_LEVEL = 3;
    public static final double HAIL_RANGE = 9.0;                 // «недалеко»; со II уровня +2
    public static final double HAIL_RANGE_BONUS = 2.0;
    public static final double HAIL_CONE = 9.0;                  // разброс, градусы
    public static final double HAIL_SPEED = 2.2;
    public static final int HAIL_COOLDOWN = 10 * 20;
    public static final int HAIL_COOLDOWN_PER_LEVEL = 30;        // -1,5 с за уровень
    public static final int HAIL_SLOW_TICKS = 12;                // цель, в которую попал град, на полсекунды замедлена

    // ---------- Электричество (вкладка «Ярость, электричество») ----------
    /** «Статический заряд»: сколько попаданий осколков дают очки ярости (уровни 1-3), 0 — навыка нет. */
    public static final int[] HITS_PER_RAGE = {0, 5, 3, 2};
    public static final int RAGE_PER_GROUP = 2;
    /** «Электрический разряд»: шанс, что попавший осколок ударит дугой ещё по одной цели. */
    public static final double[] ARC_CHANCE = {0.0, 0.10, 0.18, 0.26};
    public static final double ARC_RADIUS = 4.0;
    public static final double ARC_SHARE = 0.6;
    /** «Грозовая ярость»: в ярости выстрел — два осколка (I), скорострельность +25% (II). */
    public static final double FURY_RATE = 1.25;

    // ---------- Ульта «Шквал осколков» (Alt + J) ----------
    public static final int STORM_CHARGE = 20;                   // 1 с: крылья раскрываются и искрят, стоим на месте
    public static final int STORM_FIRE = 40;                     // 2 с огня: 3 осколка за тик
    public static final int STORM_PER_TICK = 3;
    public static final float STORM_DAMAGE = 0.3F;               // каждый осколок; сквозь броню, блок и множители
    public static final double STORM_CONE = 7.0;
    public static final double STORM_RANGE = 30.0;
    public static final double STORM_SPEED = 2.6;
}
