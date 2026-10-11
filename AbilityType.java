package com.tokyoghoulrpg.combat;

/** Способности, которые клиент просит выполнить у сервера. */
public enum AbilityType {
    BITE("bite", false),
    DASH("dash", false),
    BLOCK("block", false),
    BARRAGE("barrage", true),
    RAGE("rage", false),
    BASIC_ATTACK("basic_attack", true),
    STRONG_ATTACK("strong_attack", true),
    GRAPPLE_SELF("grapple_self", true),
    PULL_TARGET("pull_target", true);

    private final String id;
    private final boolean needsKagune;

    AbilityType(String id, boolean needsKagune) {
        this.id = id;
        this.needsKagune = needsKagune;
    }

    /** Нужно ли для способности включённое кагуне (G). */
    public boolean needsKagune() {
        return needsKagune;
    }

    public String translationKey() {
        return "ability.tokyoghoulrpg." + id;
    }
}
