package com.tokyoghoulrpg.style;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.ShadowDecoy;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Стиль «Тень» (скрытность). Сам стиль: удар в спину сильнее на 15%.
 * Ветки: «Удар в спину» (ещё сильнее сзади), «Тихий шаг» (завеса и приманка дольше, бег в завесе быстрее),
 * «Засада» (урон по врагу, который бьётся не с тобой).
 * Способности: «Дымовая завеса» (C) — невидимость, враги рядом теряют тебя, первый удар из неё вдвое сильнее;
 * «Подмена» (Shift + C) — на месте остаётся приманка, враги бросаются на неё, ты отпрыгиваешь назад.
 */
public final class ShadowStyle {
    public static final FightStyle STYLE = FightStyle.SHADOW;
    public static final int BACKSTAB = 0;
    public static final int SILENT = 1;
    public static final int AMBUSH = 2;

    private ShadowStyle() {
    }

    public static boolean active(PlayerData data) {
        return data != null && data.style().isActive(STYLE);
    }

    private static int lvl(PlayerData data, int branch) {
        return data.style().activeLevel(STYLE, branch);
    }

    public static boolean inSmoke(ServerPlayer player, PlayerData data) {
        return active(data) && data.style().smokeUntil > player.level().getGameTime();
    }

    /** Удар со спины цели (атакующий за её спиной, в пределах 110 градусов от затылка). */
    public static boolean fromBehind(LivingEntity victim, LivingEntity attacker) {
        Vec3 look = Vec3.directionFromRotation(0.0F, victim.getYRot());
        Vec3 to = attacker.position().subtract(victim.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        return flat.lengthSqr() > 1.0E-4 && flat.normalize().dot(look) < -0.34;
    }

    // ------------------------------------------------------------------ усиление ударов

    public static float damageFactor(ServerPlayer player, PlayerData data, LivingEntity victim, DamageSource source) {
        if (!active(data) || source.getDirectEntity() != player) {
            return 1.0F;
        }
        StyleData sd = data.style();
        double factor = 1.0;
        if (fromBehind(victim, player)) {
            factor += StyleBalance.SHADOW_BACK_DAMAGE;
            int back = lvl(data, BACKSTAB);
            if (back > 0) {
                factor += StyleBalance.BACKSTAB_BONUS[back - 1];
            }
            Effects.particles(victim, ParticleTypes.CRIT, 8, 0.3, 0.2);
        }
        int ambush = lvl(data, AMBUSH);
        if (ambush > 0 && victim instanceof Mob mob && mob.getTarget() != player) {
            factor += StyleBalance.AMBUSH_BONUS[ambush - 1];
        }
        if (inSmoke(player, data)) {
            factor += StyleBalance.SMOKE_FIRST_HIT;
            leaveSmoke(player, sd);
            Effects.sound(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.6F);
            if (player.level() instanceof ServerLevel level) {
                Vec3 at = victim.position().add(0.0, victim.getBbHeight() * 0.6, 0.0);
                level.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.05);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.3);
            }
        }
        return (float) factor;
    }

    private static void leaveSmoke(ServerPlayer player, StyleData sd) {
        sd.smokeUntil = 0L;
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        NetworkHandler.syncTo(player);
    }

    /** Каждый тик: дымок вокруг в завесе, конец завесы. */
    public static void tick(ServerPlayer player, PlayerData data) {
        StyleData sd = data.style();
        if (sd.smokeUntil == 0L) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= sd.smokeUntil || !active(data)) {
            sd.smokeUntil = 0L;
            NetworkHandler.syncTo(player);
            return;
        }
        if (now % 4 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.1, player.getZ(), 2,
                    0.25, 0.05, 0.25, 0.005);
        }
    }

    /** Враг пытается взять игрока в цель: в завесе замечает только вплотную. Возвращает true — не замечает. */
    public static boolean hidden(ServerPlayer player, PlayerData data, LivingEntity seeker) {
        return inSmoke(player, data) && seeker.distanceTo(player) > StyleBalance.SMOKE_NOTICE;
    }

    // ------------------------------------------------------------------ «Дымовая завеса»

    public static void smoke(ServerPlayer player, PlayerData data, long now) {
        StyleData sd = data.style();
        if (now < sd.ability1Ready()) {
            StyleAbilities.cooldownMessage(player, StyleAbilities.abilityName(STYLE, 1), sd.ability1Ready() - now);
            return;
        }
        int silent = lvl(data, SILENT);
        int ticks = StyleBalance.SMOKE_TICKS + (silent > 0 ? StyleBalance.SILENT_EXTRA[silent - 1] : 0);
        sd.smokeUntil = now + ticks;
        sd.setAbility1Ready(now + Math.round(StyleBalance.SMOKE_COOLDOWN * FighterStyle.cooldownFactor(data)));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        if (silent > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, silent - 1, false, false, true));
        }
        loseTarget(player);
        Effects.sound(player, com.tokyoghoulrpg.registry.ModSounds.SMOKE.get(), 1.0F, 1.0F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 50,
                    1.0, 0.8, 1.0, 0.02);
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 0.6, player.getZ(), 20,
                    0.8, 0.4, 0.8, 0.02);
        }
        NetworkHandler.syncTo(player);
    }

    /** Враги вокруг перестают видеть игрока. */
    private static void loseTarget(ServerPlayer player) {
        double r = StyleBalance.SMOKE_RADIUS;
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r),
                m -> m.getTarget() == player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ «Подмена»

    public static void decoy(ServerPlayer player, PlayerData data, long now) {
        StyleData sd = data.style();
        if (now < sd.ability2Ready()) {
            StyleAbilities.cooldownMessage(player, StyleAbilities.abilityName(STYLE, 2), sd.ability2Ready() - now);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ShadowDecoy decoy = ModEntities.SHADOW_DECOY.get().create(level);
        if (decoy == null) {
            return;
        }
        int silent = lvl(data, SILENT);
        int life = StyleBalance.DECOY_TICKS + (silent > 0 ? StyleBalance.SILENT_EXTRA[silent - 1] : 0);
        decoy.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        decoy.setYHeadRot(player.getYRot());
        decoy.setYBodyRot(player.getYRot());
        decoy.setup(player, life);
        level.addFreshEntity(decoy);
        decoy.lure();
        sd.setAbility2Ready(now + Math.round(StyleBalance.DECOY_COOLDOWN * FighterStyle.cooldownFactor(data)));
        // прыжок назад
        Vec3 look = player.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0, -look.z);
        back = back.lengthSqr() < 1.0E-4 ? Vec3.ZERO : back.normalize().scale(1.25);
        player.setDeltaMovement(back.x, 0.42, back.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        Effects.sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, decoy.getX(), decoy.getY() + 1.0, decoy.getZ(), 24,
                0.4, 0.8, 0.4, 0.02);
        NetworkHandler.syncTo(player);
    }
}
