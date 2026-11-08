package com.tokyoghoulrpg.style;

/**
 * Дополнительные стили боя. Берутся поверх основного класса (гуль, полугуль, человек, агент CCG), активен один.
 * Стиль открывается победой над его боссом, прокачивается очками мастерства (3 ветки по 3 уровня),
 * у каждого две способности на клавише C (просто C — первая, Shift + C — вторая).
 * Названия и описания — в языковых файлах: style.tokyoghoulrpg.&lt;id&gt;.*
 */
public enum FightStyle {
    /** Ближний бой: серия ударов, ответ после блока, быстрые рывки. Босс — Мастер додзё. */
    FIGHTER("fighter", 0xD8903A, true, "combo", "riposte", "footwork"),
    /** Скрытность: удары в спину, невидимость, приманка. Босс — Клоун из Пьеро. */
    SHADOW("shadow", 0x8A6AD0, true, "backstab", "silent", "ambush"),
    /** Точность и казнь: криты, добивание, метка смерти. Босс — Белый жнец. */
    REAPER("reaper", 0xC8D2DC, true, "precision", "execute", "harvest");

    private final String id;
    private final int accent;
    private final boolean ready;
    private final String[] branches;

    FightStyle(String id, int accent, boolean ready, String... branches) {
        this.id = id;
        this.accent = accent;
        this.ready = ready;
        this.branches = branches;
    }

    public String id() {
        return id;
    }

    /** Цвет стиля в меню и на панели. */
    public int accent() {
        return accent;
    }

    /** Готов ли стиль (есть босс и способности). Неготовые видны в меню как «скоро». */
    public boolean ready() {
        return ready;
    }

    public int bit() {
        return 1 << ordinal();
    }

    /** Ветки дерева стиля (всегда 3). */
    public String branch(int index) {
        return branches[index];
    }

    public String key(String part) {
        return "style.tokyoghoulrpg." + id + "." + part;
    }

    public String branchKey(int index, String part) {
        return "style.tokyoghoulrpg." + id + ".branch." + branches[index] + "." + part;
    }

    public static FightStyle byId(String id) {
        for (FightStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return null;
    }

    public static FightStyle byIndex(int index) {
        FightStyle[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }
}
