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
    PULL_TARGET("pull_target", true),
    /** Сильная атака человека: ПКМ дважды с любым предметом или пустой рукой. */
    HUMAN_STRONG_ATTACK("human_strong_attack", false),
    // Агент CCG с квинке в руке
    QUINQUE_ATTACK("quinque_attack", false),
    QUINQUE_STRONG("quinque_strong", false),
    QUINQUE_LUNGE("quinque_lunge", false),
    QUINQUE_FLURRY("quinque_flurry", false),
    QUINQUE_RELEASE("quinque_release", false);

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
