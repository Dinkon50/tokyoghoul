package com.tokyoghoulrpg.entity;

/**
 * Житель «Глубин 24-го района»: монстры глубин, стражи и Древний гуль. Атаки по области друг друга
 * не задевают (см. HalfGhoulBoss.ally), друг на друга не нападают.
 */
public interface DeepDweller {
    static boolean ally(net.minecraft.world.entity.LivingEntity entity) {
        return entity instanceof DeepDweller;
    }
}
