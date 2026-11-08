package com.tokyoghoulrpg.style;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.Targeting;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Стиль «Боец» (рукопашный бой). Сам стиль: +5% урона вблизи.
 * Ветки: «Серия» (каждый удар подряд сильнее), «Ответный удар» (после блока следующий удар сильнее),
 * «Подвижность» (рывок и способности стиля перезаряжаются быстрее).
 * Способности: «Шквал ударов» (C) — шесть быстрых ударов по цели, последний отбрасывает;
 * «Контратака» (Shift + C) — короткая стойка: кто ударит в ней, получит втройне.
 */
public final class FighterStyle {
    public static final FightStyle STYLE = FightStyle.FIGHTER;
    public static final int COMBO = 0;
    public static final int RIPOSTE = 1;
    public static final int FOOTWORK = 2;

    /** Идёт ответный удар контратаки: чтобы две контратаки не отвечали друг другу бесконечно. */
    private static boolean countering = false;

    private FighterStyle() {
    }

    public static boolean active(PlayerData data) {
        return data != null && data.style().isActive(STYLE);
    }

    private static int lvl(PlayerData data, int branch) {
        return data.style().activeLevel(STYLE, branch);
    }

    /** Множитель отката рывка и способностей стиля («Подвижность»). */
    public static double cooldownFactor(PlayerData data) {
        if (!active(data)) {
            return 1.0;
        }
        int level = lvl(data, FOOTWORK);
        return level > 0 ? 1.0 - StyleBalance.FOOTWORK_COOLDOWN[level - 1] : 1.0;
    }

    private static float attackDamage(ServerPlayer player) {
        return (float) Math.max(1.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    // ------------------------------------------------------------------ усиление ударов

    /** Множитель урона удара бойца вблизи. Каждый удар продлевает серию. */
    public static float damageFactor(ServerPlayer player, PlayerData data, DamageSource source) {
        if (!active(data) || source.getDirectEntity() != player) {
            return 1.0F;
        }
        StyleData sd = data.style();
        long now = player.level().getGameTime();
        double factor = 1.0 + StyleBalance.FIGHTER_BASE_DAMAGE;
        if (now - sd.comboLastTick > StyleBalance.COMBO_WINDOW) {
            sd.comboStacks = 0;
        }
        int combo = lvl(data, COMBO);
        if (combo > 0) {
            factor += sd.comboStacks * StyleBalance.COMBO_PER_HIT[combo - 1];
        }
        sd.comboStacks = Math.min(StyleBalance.COMBO_MAX, sd.comboStacks + 1);
        sd.comboLastTick = now;
        int riposte = lvl(data, RIPOSTE);
        if (riposte > 0 && sd.riposteUntil >= now) {
            factor += StyleBalance.RIPOSTE_BONUS[riposte - 1];
            sd.riposteUntil = 0L;
            Effects.particles(player, ParticleTypes.CRIT, 12, 0.4, 0.2);
            Effects.sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 0.8F);
        }
        return (float) factor;
    }

    /** Удар пойман блоком (или идеальным блоком): открывается окно ответного удара. */
    public static void onBlocked(ServerPlayer player, PlayerData data) {
        if (active(data) && lvl(data, RIPOSTE) > 0) {
            data.style().riposteUntil = player.level().getGameTime() + StyleBalance.RIPOSTE_WINDOW;
        }
    }

    // ------------------------------------------------------------------ «Шквал ударов»

    public static void flurry(ServerPlayer player, PlayerData data, long now) {
        StyleData sd = data.style();
        if (now < sd.ability1Ready()) {
            StyleAbilities.cooldownMessage(player, StyleAbilities.abilityName(STYLE, 1), sd.ability1Ready() - now);
            return;
        }
        LivingEntity target = findTarget(player);
        if (target == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.tokyoghoulrpg.no_target"), true);
            return;
        }
        sd.flurryTarget = target.getId();
        sd.flurryHitsLeft = StyleBalance.FLURRY_HITS;
        sd.flurryNextTick = now + 1;
        sd.setAbility1Ready(now + Math.round(StyleBalance.FLURRY_COOLDOWN * cooldownFactor(data)));
        Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.5F);
        NetworkHandler.syncTo(player);
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Targeting.Hit hit = Targeting.raycast(player, StyleBalance.FLURRY_RANGE, false, e -> true);
        if (hit != null && hit.entity() != null) {
            return hit.entity();
        }
        List<LivingEntity> front = Targeting.inFront(player, 3.0, 1.3);
        return front.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    /** Каждый тик игрока: удары идущего «Шквала» и конец стойки. */
    public static void tick(ServerPlayer player, PlayerData data) {
        StyleData sd = data.style();
        long now = player.level().getGameTime();
        if (sd.flurryHitsLeft <= 0 || now < sd.flurryNextTick) {
            return;
        }
        Entity entity = player.level().getEntity(sd.flurryTarget);
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || !active(data)
                || target.distanceTo(player) > StyleBalance.FLURRY_RANGE + 1.5 || !player.isAlive()) {
            sd.flurryHitsLeft = 0;
            return;
        }
        boolean last = sd.flurryHitsLeft == 1;
        int index = StyleBalance.FLURRY_HITS - sd.flurryHitsLeft;
        // подшагнуть к цели, если она отошла
        Vec3 to = target.position().subtract(player.position());
        if (to.horizontalDistance() > 2.4) {
            Vec3 step = new Vec3(to.x, 0.0, to.z).normalize().scale(0.35);
            player.setDeltaMovement(step.x, player.getDeltaMovement().y, step.z);
            player.hurtMarked = true;
        }
        player.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
        float damage = attackDamage(player) * (float) (last ? StyleBalance.FLURRY_LAST : StyleBalance.FLURRY_HIT);
        target.invulnerableTime = 0;
        boolean hurt = target.hurt(player.damageSources().playerAttack(player), damage);
        if (player.level() instanceof ServerLevel level) {
            Vec3 at = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.25, 0.25, 0.25, 0.25);
            if (last) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.POOF, at.x, at.y - 0.4, at.z, 8, 0.3, 0.1, 0.3, 0.05);
            }
        }
        if (hurt) {
            Effects.blood(target, last ? 10 : 4);
        }
        Effects.sound(target, last ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG,
                0.8F, 0.9F + index * 0.06F);
        if (last && hurt) {
            target.knockback(1.3, player.getX() - target.getX(), player.getZ() - target.getZ());
        }
        sd.flurryHitsLeft--;
        sd.flurryNextTick = now + StyleBalance.FLURRY_INTERVAL;
    }

    // ------------------------------------------------------------------ «Контратака»

    public static void counterStance(ServerPlayer player, PlayerData data, long now) {
        StyleData sd = data.style();
        if (now < sd.ability2Ready()) {
            StyleAbilities.cooldownMessage(player, StyleAbilities.abilityName(STYLE, 2), sd.ability2Ready() - now);
            return;
        }
        sd.stanceUntil = now + StyleBalance.COUNTER_STANCE;
        sd.setAbility2Ready(now + Math.round(StyleBalance.COUNTER_COOLDOWN * cooldownFactor(data)));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, StyleBalance.COUNTER_STANCE, 2,
                false, false, false));
        Effects.sound(player, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.6F);
        Effects.particles(player, ParticleTypes.ENCHANTED_HIT, 10, 0.4, 0.05);
        NetworkHandler.syncTo(player);
    }

    /**
     * Удар по игроку. Если он в стойке «Контратаки» — удар не проходит, а ударивший (если он рядом)
     * получает ответ: 300% урона игрока (+50% за каждый уровень «Ответного удара»).
     * Возвращает true, если удар надо отменить.
     */
    public static boolean onAttacked(ServerPlayer player, PlayerData data, DamageSource source) {
        if (countering || !active(data)) {
            return false;
        }
        StyleData sd = data.style();
        long now = player.level().getGameTime();
        if (sd.stanceUntil < now || source.getEntity() == null || source.getEntity() == player) {
            return false;
        }
        sd.stanceUntil = 0L;
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.invulnerableTime = 10 + StyleBalance.COUNTER_INVULNERABLE;
        Effects.sound(player, SoundEvents.SHIELD_BLOCK, 1.0F, 0.7F);
        Effects.sound(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.7F);
        if (source.getEntity() instanceof LivingEntity attacker && attacker.isAlive()
                && attacker.distanceTo(player) <= 5.0) {
            float damage = attackDamage(player)
                    * (float) (StyleBalance.COUNTER_DAMAGE + 0.5 * lvl(data, RIPOSTE));
            player.swing(InteractionHand.MAIN_HAND, true);
            countering = true;
            try {
                attacker.invulnerableTime = 0;
                if (attacker.hurt(player.damageSources().playerAttack(player), damage)) {
                    attacker.knockback(1.1, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
                    Effects.blood(attacker, 12);
                }
            } finally {
                countering = false;
            }
            if (player.level() instanceof ServerLevel level) {
                Vec3 at = attacker.position().add(0.0, attacker.getBbHeight() * 0.6, 0.0);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.3);
            }
        }
        onBlocked(player, data);
        NetworkHandler.syncTo(player);
        return true;
    }
}
