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

    /** Наносит удар способности. Возвращает true, если урон прошёл (блок мог его погасить). */
    public static boolean hit(ServerPlayer attacker, LivingEntity target, float damage, AttackType type) {
        AttackType previous = current;
        current = type;
        try {
            // Серия быстрых ударов не должна упираться в ванильную неуязвимость после попадания
            target.invulnerableTime = 0;
            return target.hurt(attacker.damageSources().playerAttack(attacker), damage);
        } finally {
            current = previous;
        }
    }
}
