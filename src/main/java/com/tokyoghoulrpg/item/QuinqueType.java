package com.tokyoghoulrpg.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;

/**
 * Виды квинке. У каждой своя модель, урон, скорость и особенности способностей.
 * Числа временные, всё в одном месте.
 * <ul>
 *   <li>Kura — стартовая (выдаётся агенту): чуть слабее остальных, быстрая, длинный шквал;</li>
 *   <li>13's Jason — тяжёлая коса: медленно, но больно, сильный удар x3;</li>
 *   <li>Narukami — электрическая: сильный удар — разряд на расстояние, в Освобождении раскрывается;</li>
 *   <li>IXA — копьё, самая сильная: достаёт дальше, длинный выпад, широкое окно идеального блока;</li>
 *   <li>учебная — для проверки, как железный меч.</li>
 * </ul>
 */
public enum QuinqueType {
    //         id         модель                    раскрытая модель            тир             урон скорость  дост. сильн. шквал  интер. шкв.x  выпад  парир. разряд
    TRAINING("training", null, null, Tiers.IRON, 4, -2.6F, 0.0, 2.5F, 10, 3, 0.35F, 5, 1.0, false),
    // по силе: Kura (стартовая) < 13's Jason < Narukami < IXA (самая сильная)
    KURA("kura", "quinque_kura", null, Tiers.DIAMOND, 4, -2.2F, 0.3, 2.5F, 12, 2, 0.35F, 5, 1.0, false),
    JASON("jason", "quinque_jason", null, Tiers.NETHERITE, 4, -2.8F, 0.6, 3.0F, 6, 5, 0.55F, 4, 1.0, false),
    NARUKAMI("narukami", "quinque_narukami", "quinque_narukami_open", Tiers.NETHERITE, 5, -2.4F, 0.3, 2.6F, 10, 3, 0.35F, 5, 1.2, true),
    IXA("ixa", "quinque_ixa", null, Tiers.NETHERITE, 6, -2.5F, 1.2, 3.0F, 10, 3, 0.45F, 7, 1.6, false);

    private final String id;
    private final String mesh;
    private final String openMesh;
    private final Tier tier;
    private final int damage;
    private final float speed;
    private final double reachBonus;
    private final float strongMult;
    private final int flurryHits;
    private final int flurryInterval;
    private final float flurryMult;
    private final int lungeTicks;
    private final double parryWindowMult;
    private final boolean ranged;

    QuinqueType(String id, String mesh, String openMesh, Tier tier, int damage, float speed, double reachBonus,
                float strongMult, int flurryHits, int flurryInterval, float flurryMult, int lungeTicks,
                double parryWindowMult, boolean ranged) {
        this.id = id;
        this.mesh = mesh;
        this.openMesh = openMesh;
        this.tier = tier;
        this.damage = damage;
        this.speed = speed;
        this.reachBonus = reachBonus;
        this.strongMult = strongMult;
        this.flurryHits = flurryHits;
        this.flurryInterval = flurryInterval;
        this.flurryMult = flurryMult;
        this.lungeTicks = lungeTicks;
        this.parryWindowMult = parryWindowMult;
        this.ranged = ranged;
    }

    public String id() {
        return id;
    }

    /** Имя файла сетки в assets/tokyoghoulrpg/quinque (без .tgqm); null — обычная плоская иконка. */
    public String mesh() {
        return mesh;
    }

    /** Модель в «Освобождении» (null — та же). */
    public String openMesh() {
        return openMesh;
    }

    public boolean has3dModel() {
        return mesh != null;
    }

    /**
     * Перевернуть модель в руке вокруг древка (на 180°). У косы 13's Jason лезвие в модели смотрит
     * в ту сторону, которая в руке оказывается сверху; перевёрнутая коса держится лезвием вниз.
     */
    public boolean flipInHand() {
        return this == JASON;
    }

    public Tier tier() {
        return tier;
    }

    public int damage() {
        return damage;
    }

    public float speed() {
        return speed;
    }

    /** На сколько блоков дальше достают удары. */
    public double reachBonus() {
        return reachBonus;
    }

    public float strongMult() {
        return strongMult;
    }

    public int flurryHits() {
        return flurryHits;
    }

    public int flurryInterval() {
        return flurryInterval;
    }

    public float flurryMult() {
        return flurryMult;
    }

    public int lungeTicks() {
        return lungeTicks;
    }

    public double parryWindowMult() {
        return parryWindowMult;
    }

    /** Сильный удар — электрический разряд на расстояние. */
    public boolean ranged() {
        return ranged;
    }
}
