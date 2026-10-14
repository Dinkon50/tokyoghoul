package com.tokyoghoulrpg.entity;

/** Босс: не оглушается (укус, барраж и захват не обездвиживают его) и показывает полосу здоровья. */
public interface BossMob {
    /** Во сколько раз сильнее урон особых атак (прыжок, рывок, круговой удар и другие). */
    default float damageMultiplier() {
        return 1.0F;
    }

    /** Множитель отката особых атак (меньше 1 — чаще). */
    default double cooldownMultiplier() {
        return 1.0;
    }

    /** Может ли босс сейчас начинать особые атаки (например, не во время превращения). */
    default boolean canUseSpecials() {
        return true;
    }
}
