package com.tokyoghoulrpg.combat;

/** Вид удара кагуне. Нужен, чтобы блок знал, что делать: пробить, погасить или пропустить. */
public enum AttackType {
    BASIC,
    STRONG,
    BARRAGE
}
