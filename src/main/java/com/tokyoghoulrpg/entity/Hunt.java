package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Охотник из волны NPC (гуль или агент CCG). Волна приходит за игроком с плохой репутацией:
 * <ul>
 *   <li>чует свою цель издалека и сквозь стены;</li>
 *   <li>гули и агенты из волн друг друга не трогают — у них одна цель;</li>
 *   <li>уходит, когда цель погибла, вышла из игры, ушла в другое измерение или далеко,
 *       и в любом случае через 5 минут.</li>
 * </ul>
 * Метка хранится в данных сущности (ForgeData) и сохраняется вместе с ней.
 */
public final class Hunt {
    private static final String TARGET = "tgrpg_hunt_target";
    private static final String UNTIL = "tgrpg_hunt_until";
    private static final UUID FOLLOW_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e60");
    private static final double LOST_DISTANCE = 160.0;

    private Hunt() {
    }

    /** Отправить моба охотиться на игрока. */
    public static void start(Mob mob, ServerPlayer target, long until) {
        CompoundTag tag = mob.getPersistentData();
        tag.putUUID(TARGET, target.getUUID());
        tag.putLong(UNTIL, until);
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null && follow.getModifier(FOLLOW_ID) == null) {
            follow.addPermanentModifier(new AttributeModifier(FOLLOW_ID, "tg_hunt_follow", Balance.WAVE_FOLLOW_BONUS,
                    AttributeModifier.Operation.ADDITION));
        }
        mob.setTarget(target);
    }

    /** Этот моб из волны? */
    public static boolean isHunter(Entity entity) {
        return entity != null && !entity.level().isClientSide() && entity.getPersistentData().hasUUID(TARGET);
    }

    /** Это цель моба из волны? */
    public static boolean isTarget(Mob mob, LivingEntity entity) {
        if (!isHunter(mob)) {
            return false;
        }
        return entity.getUUID().equals(mob.getPersistentData().getUUID(TARGET));
    }

    /** Двое из волн — не враги друг другу, даже если один гуль, а другой агент. */
    public static boolean allies(Entity a, Entity b) {
        return isHunter(a) && isHunter(b);
    }

    /** Охотники волны, которые сейчас идут за этим игроком (рядом с ним). */
    public static int countFor(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(96.0),
                m -> m.isAlive() && isTarget(m, player)).size();
    }

    /** Раз в секунду из тика моба: цель пропала или время вышло — волна уходит. */
    public static void tick(Mob mob) {
        if (mob.tickCount % 20 != 0 || !(mob.level() instanceof ServerLevel level) || !isHunter(mob)) {
            return;
        }
        CompoundTag tag = mob.getPersistentData();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(tag.getUUID(TARGET));
        boolean gone = player == null || !player.isAlive() || player.isCreative() || player.isSpectator()
                || player.level() != level || level.getGameTime() > tag.getLong(UNTIL)
                || player.distanceToSqr(mob) > LOST_DISTANCE * LOST_DISTANCE;
        if (gone) {
            level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    12, 0.3, 0.5, 0.3, 0.02);
            mob.discard();
            return;
        }
        if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
            mob.setTarget(player);
        }
    }
}
