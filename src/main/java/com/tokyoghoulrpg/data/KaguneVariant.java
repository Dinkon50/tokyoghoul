package com.tokyoghoulrpg.data;

/** Вариант кагуне. Гуль и полугуль выбирают его при создании персонажа. */
public enum KaguneVariant {
    NONE("none"),
    KANEKI("kaneki"),
    /** Больше не выбирается игроками (его место занял WINGS); модель остаётся у боссов. */
    AUTHOR("author"),
    /** «Укаку Тоуки»: огненные крылья из лопаток (в стиле укаку), левое больше правого: модель geo/ukaku_kagune.geo.json. */
    WINGS("wings"),
    /** «Гурман» Цукиямы Шу: фиолетовая лента-спираль вдоль правой руки (коукаку): geo/gourmet_kagune.geo.json. */
    GOURMET("gourmet");

    private final String id;

    KaguneVariant(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "kagune.tokyoghoulrpg." + id;
    }

    public String descriptionKey() {
        return "kagune.tokyoghoulrpg." + id + ".desc";
    }

    public static KaguneVariant byId(String id) {
        for (KaguneVariant v : values()) {
            if (v.id.equals(id)) {
                return v;
            }
        }
        return NONE;
    }
}
