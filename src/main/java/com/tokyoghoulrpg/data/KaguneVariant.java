package com.tokyoghoulrpg.data;

/** Вариант кагуне. Гуль и полугуль выбирают его при создании персонажа. */
public enum KaguneVariant {
    NONE("none"),
    KANEKI("kaneki"),
    AUTHOR("author");

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
