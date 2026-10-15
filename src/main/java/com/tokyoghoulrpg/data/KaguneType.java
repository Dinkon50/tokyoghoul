package com.tokyoghoulrpg.data;

/**
 * Тип кагуне гуля и полугуля. Не зависит от того, как кагуне выглядит (это {@link KaguneVariant}),
 * и даёт свой пассивный бонус (числа в Balance, применение в KaguneTypeBonus):
 * <ul>
 *   <li>ринкаку — быстрее регенерация;</li>
 *   <li>укаку — скорость и рывок длиннее;</li>
 *   <li>коукаку — защита и меньше отбрасывания;</li>
 *   <li>бикаку — понемногу урона, защиты и скорости.</li>
 * </ul>
 */
public enum KaguneType {
    NONE("none"),
    RINKAKU("rinkaku"),
    UKAKU("ukaku"),
    KOUKAKU("koukaku"),
    BIKAKU("bikaku");

    public static final KaguneType[] CHOICES = {RINKAKU, UKAKU, KOUKAKU, BIKAKU};

    private final String id;

    KaguneType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "kagune_type.tokyoghoulrpg." + id;
    }

    public String descriptionKey() {
        return "kagune_type.tokyoghoulrpg." + id + ".desc";
    }

    public static KaguneType byId(String id) {
        for (KaguneType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return NONE;
    }
}
