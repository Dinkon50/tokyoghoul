package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.clan.ClanAlliances;
import com.tokyoghoulrpg.clan.ClanData;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Комбо-удары — для всех классов. Между нажатиями не больше 1,5 с, после комбо 5 с перерыв.
 * <ul>
 *   <li><b>Разрыв</b>: удар, удар, рывок — рывок прорезает всех на пути (120% удара каждому);</li>
 *   <li><b>Сокрушение</b>: удар, удар, сильный удар — сильный удар x1,5 и оглушение на 2 с;</li>
 *   <li><b>Ударная волна</b>: рывок, удар, удар, удар — третий удар бьёт волной всех вокруг (60% удара).</li>
 * </ul>
 * «Удар» — попадание обычной атакой: кагуне, квинке или оружием/рукой. Барраж, шквал и стрелы не считаются.
 * Своих (клан и союзники), питомцев и стойки для брони комбо не задевает.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class Combos {
    private static final char HIT = 'L';
    private static final char STRONG = 'S';
    private static final char DASH = 'D';

    private record Input(char kind, long tick) {
    }

    private static final class State {
        final Deque<Input> inputs = new ArrayDeque<>();
        long ready = 0L;
        /** Сырой урон последнего обычного удара — от него считаются «Разрыв» и «Ударная волна». */
        float lastHit = 0.0F;
        /** В этот тик сильный удар — «Сокрушение» (все цели этого удара). */
        long crushTick = -1L;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Урон, который наносит само комбо, — его не считаем новым нажатием и не усиливаем повторно. */
    private static boolean applying = false;

    private Combos() {
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), k -> new State());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    /** Рывок начался (вызывает AbilityManager). */
    public static void onDash(ServerPlayer player) {
        long now = player.level().getGameTime();
        State st = state(player);
        if (now >= st.ready && matches(st, now, HIT, HIT)) {
            st.inputs.clear();
            st.ready = now + Balance.COMBO_COOLDOWN;
            rush(player, st.lastHit);
            return;
        }
        push(st, DASH, now);
    }

    /** Какое это нажатие для комбо (0 — не считается). */
    private static char kindOf(LivingHurtEvent event, ServerPlayer attacker) {
        AttackType type = CombatContext.type();
        if (type == AttackType.STRONG) {
            return STRONG;
        }
        if (type == AttackType.BASIC) {
            return HIT;
        }
        if (type == null) {
            DamageSource source = event.getSource();
            // обычный удар оружием или рукой: источник и прямой виновник — сам игрок
            if (source.getDirectEntity() == attacker && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
                return HIT;
            }
        }
        return 0;
    }

    /** До всех множителей: запомнить нажатие и решить, сработало ли «Сокрушение» или «Ударная волна». */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurtEarly(LivingHurtEvent event) {
        if (applying || event.getEntity().level().isClientSide()
                || !(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player) {
            return;
        }
        char kind = kindOf(event, player);
        if (kind == 0) {
            return;
        }
        long now = player.level().getGameTime();
        State st = state(player);
        if (kind == HIT) {
            st.lastHit = event.getAmount();
        }
        // одна атака может задеть несколько целей — это одно нажатие
        Input last = st.inputs.peekLast();
        if (last != null && last.tick() == now && last.kind() == kind) {
            return;
        }
        if (now >= st.ready) {
            if (kind == STRONG && matches(st, now, HIT, HIT)) {
                st.inputs.clear();
                st.ready = now + Balance.COMBO_COOLDOWN;
                st.crushTick = now;
                announce(player, "crush");
                Effects.sound(player, SoundEvents.ANVIL_LAND, 0.6F, 0.7F);
                return;
            }
            if (kind == HIT && matches(st, now, DASH, HIT, HIT)) {
                st.inputs.clear();
                st.ready = now + Balance.COMBO_COOLDOWN;
                wave(player, event.getEntity(), event.getAmount());
                return;
            }
        }
        push(st, kind, now);
    }

    /** После всех множителей: «Сокрушение» усиливает сам сильный удар. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurtLate(LivingHurtEvent event) {
        if (applying || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State st = STATES.get(player.getUUID());
        if (st == null || st.crushTick != player.level().getGameTime() || CombatContext.type() != AttackType.STRONG) {
            return;
        }
        LivingEntity victim = event.getEntity();
        event.setAmount((float) (event.getAmount() * Balance.COMBO_CRUSH_MULT));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Balance.COMBO_CRUSH_STUN, 4, false, true));
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Balance.COMBO_CRUSH_STUN, 1, false, true));
        victim.setDeltaMovement(victim.getDeltaMovement().add(0.0, 0.35, 0.0));
        victim.hurtMarked = true;
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(),
                    20, 0.3, 0.4, 0.3, 0.3);
        }
    }

    // ------------------------------------------------------------------ сами комбо

    /** «Разрыв»: всех на пути рывка — урон. */
    private static void rush(ServerPlayer player, float base) {
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0.0, look.z);
        if (dir.lengthSqr() < 1.0E-4 || base <= 0.0F) {
            return;
        }
        dir = dir.normalize();
        Vec3 start = player.position();
        Vec3 end = start.add(dir.scale(Balance.COMBO_RUSH_LENGTH));
        AABB box = new AABB(start, end).inflate(1.2, 1.0, 1.2).expandTowards(0.0, player.getBbHeight(), 0.0);
        Set<LivingEntity> hit = new HashSet<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            if (spared(player, target)) {
                continue;
            }
            // ближе 1,4 блока к линии рывка
            Vec3 to = target.position().subtract(start);
            double along = Math.max(0.0, Math.min(Balance.COMBO_RUSH_LENGTH, to.dot(dir)));
            if (to.subtract(dir.scale(along)).horizontalDistance() <= 1.4 && hit.add(target)) {
                damage(player, target, (float) (base * Balance.COMBO_RUSH_MULT));
            }
        }
        announce(player, "rush");
        Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        if (player.level() instanceof ServerLevel level) {
            for (int i = 0; i <= 10; i++) {
                Vec3 p = start.add(dir.scale(Balance.COMBO_RUSH_LENGTH * i / 10.0));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y + 1.0, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** «Ударная волна»: всем вокруг, кроме самой цели удара (она получает свой удар). */
    private static void wave(ServerPlayer player, LivingEntity main, float base) {
        double r = Balance.COMBO_WAVE_RADIUS;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r, 1.5, r),
                e -> e != player && e != main && e.isAlive())) {
            if (spared(player, target) || target.distanceToSqr(player) > r * r) {
                continue;
            }
            damage(player, target, (float) (base * Balance.COMBO_WAVE_MULT));
            Vec3 push = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
                push = push.normalize().scale(0.9);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.3, push.z));
                target.hurtMarked = true;
            }
        }
        announce(player, "wave");
        Effects.sound(player, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.4F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.5, player.getZ(), 3, 1.0, 0.2, 1.0, 0.0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 12, r * 0.6, 0.2,
                    r * 0.6, 0.0);
        }
    }

    private static void damage(ServerPlayer player, LivingEntity target, float amount) {
        applying = true;
        try {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), amount);
        } finally {
            applying = false;
        }
    }

    /** Кого комбо не трогает: свой клан и союзники, свои питомцы, стойки для брони. */
    static boolean spared(ServerPlayer player, LivingEntity target) {
        if (target instanceof ArmorStand) {
            return true;
        }
        if (target instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) {
            return true;
        }
        if (target instanceof ServerPlayer other && player.getServer() != null) {
            if (other.isCreative() || other.isSpectator()) {
                return true;
            }
            ClanData data = ClanData.get(player.getServer());
            ClanData.Clan a = data.clanOf(player.getUUID());
            ClanData.Clan b = data.clanOf(other.getUUID());
            return a != null && (a == b || ClanAlliances.allied(a, b));
        }
        if (target instanceof com.tokyoghoulrpg.entity.GuardNpc guard) {
            return guard.friendly(player);
        }
        return false;
    }

    private static void announce(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.combo.done",
                Component.translatable("combo.tokyoghoulrpg." + key)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        com.tokyoghoulrpg.rep.Achievements.add(player, "combos", 1);
    }

    // ------------------------------------------------------------------ история нажатий

    private static void push(State st, char kind, long now) {
        st.inputs.addLast(new Input(kind, now));
        while (st.inputs.size() > 4) {
            st.inputs.removeFirst();
        }
    }

    /** Последние нажатия — ровно эта цепочка, и между ними (и до сейчас) не больше окна. */
    private static boolean matches(State st, long now, char... pattern) {
        if (st.inputs.size() < pattern.length) {
            return false;
        }
        Input[] all = st.inputs.toArray(new Input[0]);
        long after = now;
        for (int i = 0; i < pattern.length; i++) {
            Input in = all[all.length - 1 - i];
            if (in.kind() != pattern[pattern.length - 1 - i] || after - in.tick() > Balance.COMBO_WINDOW) {
                return false;
            }
            after = in.tick();
        }
        return true;
    }
}
