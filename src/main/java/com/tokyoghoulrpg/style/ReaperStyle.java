package com.tokyoghoulrpg.style;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.Targeting;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Стиль «Жнец» (точность и казнь). Сам стиль: +10% урона по цели слабее половины здоровья.
 * Ветки: «Точность» (шанс крита), «Добивание» (урон по почти убитым и порог «Казни»),
 * «Жатва» (за убийство — лечение, скорость и короче откат «Казни»).
 * Способности: «Казнь» (C) — рывок и удар: по цели слабее 25% здоровья — 600% урона, иначе 150%;
 * «Метка смерти» (Shift + C) — цель 8 с светится и получает на 20% больше урона от всех.
 */
public final class ReaperStyle {
    public static final FightStyle STYLE = FightStyle.REAPER;
    public static final int PRECISION = 0;
    public static final int EXECUTE = 1;
    public static final int HARVEST = 2;

    /** Идёт удар «Казни»: его урон уже посчитан, усиления стиля к нему не добавляются второй раз. */
    private static boolean executing = false;

    private ReaperStyle() {
    }

    public static boolean active(PlayerData data) {
        return data != null && data.style().isActive(STYLE);
    }

    private static int lvl(PlayerData data, int branch) {
        return data.style().activeLevel(STYLE, branch);
    }

    private static float attackDamage(ServerPlayer player) {
        return (float) Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private static float health(LivingEntity target) {
        return target.getMaxHealth() <= 0.0F ? 1.0F : target.getHealth() / target.getMaxHealth();
    }

    // ------------------------------------------------------------------ усиление ударов

    public static float damageFactor(ServerPlayer player, PlayerData data, LivingEntity victim, DamageSource source) {
        if (!active(data) || executing || source.getDirectEntity() != player) {
            return 1.0F;
        }
        double factor = 1.0;
        float hp = health(victim);
        if (hp < 0.5F) {
            factor += StyleBalance.REAPER_LOW_DAMAGE;
        }
        int execute = lvl(data, EXECUTE);
        if (execute > 0 && hp < StyleBalance.EXECUTE_LOW) {
            factor += StyleBalance.EXECUTE_BONUS[execute - 1];
        }
        int precision = lvl(data, PRECISION);
        if (precision > 0 && player.getRandom().nextDouble() < StyleBalance.PRECISION_CHANCE[precision - 1]) {
            factor += StyleBalance.PRECISION_CRIT;
            Effects.sound(victim, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.3F);
            if (player.level() instanceof ServerLevel level) {
                Vec3 at = victim.position().add(0.0, victim.getBbHeight() * 0.7, 0.0);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.2);
            }
        }
        return (float) factor;
    }

    /** Убит враг: «Жатва» — лечение, скорость, откат «Казни» короче. */
    public static void onKill(ServerPlayer player, PlayerData data) {
        int harvest = lvl(data, HARVEST);
        if (!active(data) || harvest <= 0) {
            return;
        }
        player.heal((float) (player.getMaxHealth() * StyleBalance.HARVEST_HEAL[harvest - 1]));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false, true));
        StyleData sd = data.style();
        sd.setAbility1Ready(sd.ability1Ready() - StyleBalance.HARVEST_REFUND[harvest - 1]);
        Effects.particles(player, ParticleTypes.SOUL, 6, 0.4, 0.02);
        NetworkHandler.syncTo(player);
    }

    // ------------------------------------------------------------------ «Казнь»

    private static LivingEntity findTarget(ServerPlayer player, double range) {
        Targeting.Hit hit = Targeting.raycast(player, range, false, e -> true);
        if (hit != null && hit.entity() != null) {
            return hit.entity();
        }
        List<LivingEntity> front = Targeting.inFront(player, Math.min(range, 3.5), 1.3);
        return front.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    public static void execution(ServerPlayer player, PlayerData data, long now) {
        StyleData sd = data.style();
        if (now < sd.ability1Ready()) {
            StyleAbilities.cooldownMessage(player, StyleAbilities.abilityName(STYLE, 1), sd.ability1Ready() - now);
            return;
        }
        LivingEntity target = findTarget(player, StyleBalance.EXECUTION_RANGE);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.no_target"), true);
            return;
        }
        int execute = lvl(data, EXECUTE);
        double threshold = StyleBalance.EXECUTION_THRESHOLD + (execute > 0 ? StyleBalance.EXECUTE_THRESHOLD[execute - 1] : 0.0);
        boolean low = health(target) < threshold;
        // рывок к цели
        Vec3 to = target.position().subtract(player.position());
        if (to.horizontalDistance() > 2.2) {
            Vec3 dash = new Vec3(to.x, 0.0, to.z).normalize().scale(Math.min(1.4, to.horizontalDistance() * 0.35));
            player.setDeltaMovement(dash.x, 0.1, dash.z);
            player.hurtMarked = true;
        }
        float damage = attackDamage(player) * (float) (low ? StyleBalance.EXECUTION_LOW : StyleBalance.EXECUTION_NORMAL);
        player.swing(InteractionHand.MAIN_HAND, true);
        boolean hurt;
        executing = true;
        try {
            target.invulnerableTime = 0;
            hurt = target.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            executing = false;
        }
        long cooldown = Math.round(StyleBalance.EXECUTION_COOLDOWN * FighterStyle.cooldownFactor(data));
        if (hurt && !target.isAlive()) {
            cooldown = Math.round(cooldown * (1.0 - StyleBalance.EXECUTION_REFUND));
        }
        sd.setAbility1Ready(now + cooldown);
        if (player.level() instanceof ServerLevel level) {
            Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, low ? 3 : 1, 0.4, 0.2, 0.4, 0.0);
            if (low) {
                level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 20, 0.4, 0.5, 0.4, 0.05);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.3);
            }
        }
        if (hurt) {
            Effects.blood(target, low ? 16 : 8);
        }
        Effects.sound(target, low ? com.tokyoghoulrpg.registry.ModSounds.EXECUTION.get() : SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, low ? 1.0F : 0.8F);
        NetworkHandler.syncTo(player);
    }

    // ------------------------------------------------------------------ «Метка смерти»

    public static void mark(ServerPlayer player, PlayerData data, long now) {
        StyleData sd = data.style();
        if (now < sd.ability2Ready()) {
            StyleAbilities.cooldownMessage(player, StyleAbilities.abilityName(STYLE, 2), sd.ability2Ready() - now);
            return;
        }
        Targeting.Hit hit = Targeting.raycast(player, StyleBalance.MARK_RANGE, false, e -> true);
        LivingEntity target = hit != null ? hit.entity() : null;
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.no_target"), true);
            return;
        }
        DeathMark.apply(target, StyleBalance.MARK_TICKS);
        sd.setAbility2Ready(now + Math.round(StyleBalance.MARK_COOLDOWN * FighterStyle.cooldownFactor(data)));
        NetworkHandler.syncTo(player);
    }
}
