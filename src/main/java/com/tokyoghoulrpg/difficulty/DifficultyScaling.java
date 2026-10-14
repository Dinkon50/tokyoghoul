package com.tokyoghoulrpg.difficulty;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.entity.TraderNpc;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Сложность мода. Берётся сложность мира Minecraft (её выбирают при создании мира).
 * <ul>
 *   <li>Лёгкая и мирная: всё как раньше.</li>
 *   <li>Нормальная: у NPC мода больше здоровья и урона.</li>
 *   <li>Сложная: все враждебные мобы намного сильнее, быстрее и умнее, навыки дороже.</li>
 *   <li>Нормальная и сложная: чем больше игроков на сервере, тем сильнее враждебные мобы (до пяти игроков).</li>
 * </ul>
 * Здоровье, скорость и дальность обзора — модификаторы характеристик (сохраняются вместе с мобом
 * и пересчитываются раз в 5 секунд). Урон умножается в момент удара, так что усиливаются
 * и стрелы, и взрывы криперов, и особые атаки боссов.
 */
public final class DifficultyScaling {
    private static final UUID HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e30");
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e31");
    private static final UUID FOLLOW_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e32");
    private static final UUID KNOCKBACK_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e33");

    private DifficultyScaling() {
    }

    /** NPC мода (и мирные: торговец, информатор). */
    public static boolean isModNpc(LivingEntity entity) {
        return entity instanceof GhoulNpc || entity instanceof CcgAgentNpc || entity instanceof BossMob
                || entity instanceof TraderNpc || entity instanceof InformantNpc
                || entity instanceof com.tokyoghoulrpg.entity.HalfGhoulNpc;
    }

    /** Враждебный или боевой моб: монстры Minecraft, гули, агенты CCG и боссы. */
    public static boolean isFighter(LivingEntity entity) {
        return entity instanceof Mob && (entity instanceof Enemy || entity instanceof GhoulNpc
                || entity instanceof CcgAgentNpc || entity instanceof GhoulBoss || entity instanceof CcgBoss);
    }

    /** Сколько игроков учитывается: от 1 до 5. */
    public static int players(Level level) {
        MinecraftServer server = level.getServer();
        int count = server == null ? 1 : server.getPlayerCount();
        return Math.max(1, Math.min(Balance.PLAYER_SCALING_CAP, count));
    }

    private static boolean scaled(Difficulty difficulty) {
        return difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD;
    }

    /** Во сколько раз больше здоровья. */
    public static double healthFactor(LivingEntity mob) {
        Difficulty difficulty = mob.level().getDifficulty();
        if (!scaled(difficulty)) {
            return 1.0;
        }
        double factor = 1.0;
        boolean fighter = isFighter(mob);
        if (difficulty == Difficulty.HARD && fighter) {
            factor *= Balance.HARD_HEALTH;
        } else if (isModNpc(mob)) {
            factor *= Balance.NORMAL_NPC_HEALTH;
        }
        if (fighter) {
            factor *= 1.0 + Balance.PER_PLAYER_HEALTH * (players(mob.level()) - 1);
        }
        return factor;
    }

    /** Во сколько раз сильнее бьёт этот моб. */
    public static double damageFactor(LivingEntity mob) {
        Difficulty difficulty = mob.level().getDifficulty();
        if (!scaled(difficulty) || !isFighter(mob)) {
            return 1.0;
        }
        double factor = 1.0;
        if (difficulty == Difficulty.HARD) {
            factor *= Balance.HARD_DAMAGE;
        } else if (isModNpc(mob)) {
            factor *= Balance.NORMAL_NPC_DAMAGE;
        }
        return factor * (1.0 + Balance.PER_PLAYER_DAMAGE * (players(mob.level()) - 1));
    }

    /** Откат особых атак боссов: на сложной короче. */
    public static int bossCooldown(Mob boss, int base) {
        return boss.level().getDifficulty() == Difficulty.HARD
                ? (int) Math.round(base * Balance.HARD_BOSS_COOLDOWN) : base;
    }

    /** Для /tgrpg info: множители здоровья и урона враждебного моба Minecraft и гуля мода в этом мире. */
    public static String describe(Level level) {
        Difficulty difficulty = level.getDifficulty();
        if (!scaled(difficulty)) {
            return "мобы как обычно";
        }
        int players = players(level);
        double hp = (difficulty == Difficulty.HARD ? Balance.HARD_HEALTH : 1.0)
                * (1.0 + Balance.PER_PLAYER_HEALTH * (players - 1));
        double dmg = (difficulty == Difficulty.HARD ? Balance.HARD_DAMAGE : 1.0)
                * (1.0 + Balance.PER_PLAYER_DAMAGE * (players - 1));
        double npcHp = (difficulty == Difficulty.HARD ? Balance.HARD_HEALTH : Balance.NORMAL_NPC_HEALTH)
                * (1.0 + Balance.PER_PLAYER_HEALTH * (players - 1));
        double npcDmg = (difficulty == Difficulty.HARD ? Balance.HARD_DAMAGE : Balance.NORMAL_NPC_DAMAGE)
                * (1.0 + Balance.PER_PLAYER_DAMAGE * (players - 1));
        return String.format(java.util.Locale.ROOT, "игроков учтено %d из 5; монстры: здоровье x%.2f, урон x%.2f; NPC мода: здоровье x%.2f, урон x%.2f",
                players, hp, dmg, npcHp, npcDmg);
    }

    /** Пересчитать модификаторы моба. Здоровье сохраняет долю: раненый моб остаётся раненым. */
    public static void refresh(Mob mob) {
        boolean hard = mob.level().getDifficulty() == Difficulty.HARD && isFighter(mob);

        float oldMax = mob.getMaxHealth();
        float health = mob.getHealth();
        set(mob, Attributes.MAX_HEALTH, HEALTH_ID, healthFactor(mob) - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        float newMax = mob.getMaxHealth();
        if (Math.abs(newMax - oldMax) > 1.0E-3F && oldMax > 0.0F && mob.isAlive()) {
            mob.setHealth(Math.max(1.0F, health * newMax / oldMax));
        }

        set(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, hard ? Balance.HARD_SPEED - 1.0 : 0.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        set(mob, Attributes.FOLLOW_RANGE, FOLLOW_ID, hard ? Balance.HARD_FOLLOW - 1.0 : 0.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, hard ? Balance.HARD_KNOCKBACK_RESIST : 0.0,
                AttributeModifier.Operation.ADDITION);
    }

    /** Постоянный модификатор (сохраняется с мобом); при нулевой прибавке снимается. */
    private static void set(Mob mob, Attribute attribute, UUID id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 1.0E-4) {
                return;
            }
            instance.removeModifier(id);
        }
        if (Math.abs(amount) >= 1.0E-4) {
            instance.addPermanentModifier(new AttributeModifier(id, "tg_difficulty", amount, operation));
        }
    }
}
