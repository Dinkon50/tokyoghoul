package com.tokyoghoulrpg.data;

import java.util.List;

/** Сторона, за которую играет игрок. Выбирается один раз при первом входе. */
public enum PlayerClass {
    NONE("none", false),
    HUMAN("human", false),
    GHOUL("ghoul", true),
    HALF_GHOUL("half_ghoul", true),
    CCG_AGENT("ccg_agent", false);

    private final String id;
    private final boolean hasKagune;

    PlayerClass(String id, boolean hasKagune) {
        this.id = id;
        this.hasKagune = hasKagune;
    }

    public String id() {
        return id;
    }

    /** Кагуне и его способности доступны только гулю и полугулю. */
    public boolean hasKagune() {
        return hasKagune;
    }

    /** Варианты кагуне на выбор. Для классов без кагуне список пустой. */
    public List<KaguneVariant> variants() {
        return hasKagune ? List.of(KaguneVariant.KANEKI, KaguneVariant.AUTHOR) : List.of();
    }

    public String translationKey() {
        return "class.tokyoghoulrpg." + id;
    }

    public String descriptionKey() {
        return "class.tokyoghoulrpg." + id + ".desc";
    }

    public static PlayerClass byId(String id) {
        for (PlayerClass c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return NONE;
    }
}
