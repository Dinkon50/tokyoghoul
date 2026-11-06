package com.tokyoghoulrpg.combat;

/**
 * Числа «Гурмана» Цукиямы (KaguneVariant.GOURMET): быстрый дуэлянт, лента-спираль на правой руке.
 * Всё, что можно прокачать, лежит здесь в виде базы и таблицы по уровням ветки (0 — не прокачано);
 * формулы по уровням — skill/GourmetEffects. Тики — игровые (20 в секунду), расстояния — в блоках.
 * Все числа [ВРЕМЕННО]: их подгоняют по ощущению в игре.
 */
public final class GourmetBalance {
    private GourmetBalance() {
    }

    // ---------- ЛКМ: серия ударов лентой ----------
    /** Ударов в серии по уровням «Серии» (0-3): последний — колющий выпад. */
    public static final int[] SERIES_LENGTH = {3, 4, 4, 5};
    /** Тиков между ударами серии. */
    public static final int[] SERIES_INTERVAL = {6, 6, 5, 5};
    /** После последнего удара серии пауза дольше, чтобы серию нельзя было крутить без конца. */
    public static final int SERIES_END_PAUSE = 8;
    /** Пауза, после которой серия начинается заново. */
    public static final int SERIES_RESET = 22;
    /** Урон рубящего удара и колющего выпада (серия ударов быстрее, чем у Канеки: 5 урона раз в 0,4 с). */
    public static final float SLASH_DAMAGE = 3.5F;
    public static final float THRUST_DAMAGE = 6.0F;
    /** Когда в ударе серии наносится урон (тиков после начала взмаха). */
    public static final int SLASH_HIT_TICK = 2;
    public static final int THRUST_HIT_TICK = 3;
    /** «Колющий удар»: +15% к урону выпада за уровень; целей, которых выпад пробивает насквозь. */
    public static final double THRUST_PER_LEVEL = 0.15;
    public static final int[] THRUST_TARGETS = {2, 2, 2, 3};
    /** Дальность и половина ширины удара: «Дальность выпада» (0-3). */
    public static final double[] REACH = {3.5, 4.2, 4.9, 5.6};
    public static final double HALF_WIDTH = 1.0;
    /** Выпад подаёт игрока вперёд на столько блоков. */
    public static final double THRUST_STEP = 1.2;

    // ---------- ПКМ дважды: «Прямой выпад» ----------
    public static final float LUNGE_DAMAGE = 11.0F;
    public static final double LUNGE_PER_LEVEL = 0.15;
    /** Дальность рывка и откат по уровням ветки «Прямой выпад» (0-3). */
    public static final double[] LUNGE_DISTANCE = {6.0, 7.0, 8.0, 9.0};
    public static final int[] LUNGE_COOLDOWN = {40, 34, 28, 22};
    public static final int LUNGE_WINDUP = 4;
    public static final int LUNGE_TICKS = 5;
    public static final double LUNGE_HALF_WIDTH = 1.3;

    // ---------- K (держать): «Вихрь ленты» ----------
    /** Урон каждому в круге раз в HIT_INTERVAL тиков и радиус круга по уровням «Вихря» (0-3). */
    public static final float[] WHIRL_DAMAGE = {1.2F, 1.35F, 1.5F, 1.65F};
    public static final double[] WHIRL_RADIUS = {3.5, 3.8, 4.2, 4.5};
    public static final int[] WHIRL_COOLDOWN = {180, 150, 125, 100};
    public static final int WHIRL_INTERVAL = 4;
    public static final int WHIRL_DURATION = 40;

    // ---------- Блок и парирование ----------
    /** «Плотная спираль»: какая часть урона не проходит, когда сломан блок (0-3); с III атакующего отбрасывает. */
    public static final double[] TIGHT_REDUCTION = {0.0, 0.30, 0.55, 0.75};
    public static final int TIGHT_PUSH_LEVEL = 3;
    public static final double TIGHT_PUSH = 0.9;
    /** «Парирование»: окно после постановки блока (тиков), урон контрвыпада, оглушение (0-3). У уровня 0 парирования нет. */
    public static final int[] PARRY_WINDOW = {0, 6, 8, 10};
    public static final float[] PARRY_DAMAGE = {0.0F, 4.0F, 6.0F, 8.0F};
    public static final int[] PARRY_STUN = {0, 0, 10, 20};
    /** Парировать можно удар не дальше этого расстояния. */
    public static final double PARRY_RANGE = 6.0;

    // ---------- «Изящество»: чистая серия ----------
    /** Не получал урона столько тиков — «чисто». */
    public static final int CLEAN_TICKS = 80;
    /** Бонус к урону, пока чисто (0-3). */
    public static final double[] GRACE_DAMAGE = {0.0, 0.05, 0.10, 0.15};
    /** Очки ярости за каждый попавший выпад (0-3): без прокачки тоже 1. */
    public static final int[] GRACE_RAGE = {1, 2, 3, 4};

    // ---------- «Метка» ----------
    /** Метка цели: все удары метившего по ней сильнее на столько (0-3), столько тиков. */
    public static final double[] MARK_BONUS = {0.0, 0.10, 0.15, 0.20};
    public static final int MARK_TICKS = 80;

    // ---------- Ульта «Изящный танец» (Alt + J) ----------
    public static final double ULT_RANGE = 16.0;
    public static final int ULT_STRIKES = 7;
    public static final int ULT_INTERVAL = 2;
    public static final int ULT_WINDUP = 6;
    public static final float ULT_DAMAGE = 4.0F;
    /** Игрок появляется за спиной цели на столько блоков. */
    public static final double ULT_BEHIND = 1.5;
}
