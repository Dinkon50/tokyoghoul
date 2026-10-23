package com.tokyoghoulrpg.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Помечает урон, который наносит сама способность. Работает только на серверном потоке:
 * на время вызова hurt() здесь лежит вид удара, и обработчики урона могут его прочитать.
 */
public final class CombatContext {
    private static AttackType current = null;

    private CombatContext() {
    }

    public static AttackType type() {
        return current;
    }

    /**
     * Удар ульты: свой вид урона (сквозь броню и зачарования), вид удара ULTIMATE — блок и множители урона
     * его не трогают (см. CombatEvents). Возвращает true, если урон прошёл.
     */
    public static boolean hitUltimate(ServerPlayer attacker, LivingEntity target, float damage) {
        AttackType previous = current;
        current = AttackType.ULTIMATE;
        try {
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(ModDamage.kaguneUltimate(attacker), damage);
            if (hurt) {
                VrFeedback.hit(attacker);
            }
            return hurt;
        } finally {
            current = previous;
        }
    }

    /** Наносит удар способности. Возвращает true, если урон прошёл (блок мог его погасить). */
    public static boolean hit(ServerPlayer attacker, LivingEntity target, float damage, AttackType type) {
        AttackType previous = current;
        current = type;
        try {
            // Серия быстрых ударов не должна упираться в ванильную неуязвимость после попадания
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(attacker.damageSources().playerAttack(attacker), damage);
            if (hurt) {
                VrFeedback.hit(attacker);
            }
            return hurt;
        } finally {
            current = previous;
        }
    }
}
