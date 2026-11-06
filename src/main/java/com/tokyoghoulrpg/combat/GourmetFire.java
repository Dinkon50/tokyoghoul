package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.skill.GourmetEffects;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * «Гурман» Цукиямы (KaguneVariant.GOURMET): лента-спираль коукаку на правой руке, быстрый дуэлянт.
 * <ul>
 *   <li>ЛКМ (можно держать) — серия ударов лентой: рубящие взмахи, последний удар — колющий выпад насквозь;</li>
 *   <li>ПКМ дважды — «Прямой выпад»: рывок вперёд с уколом в первую цель, пробивает блок;</li>
 *   <li>блок (L) — спираль смыкается; с веткой «Парирование» удар, пойманный сразу после постановки блока,
 *       гасится полностью, а лента сама бьёт контрвыпадом;</li>
 *   <li>K (держать) — «Вихрь ленты»: спираль раскручивается вокруг игрока и бьёт всех рядом;</li>
 *   <li>Alt + J — ульта «Изящный танец»: игрок появляется за спиной цели и наносит серию молниеносных выпадов;</li>
 *   <li>захватов нет.</li>
 * </ul>
 * Ярость копят попавшие выпады, «Изящество» усиливает удары, пока игрок не получал урона, «Метка» делает цель уязвимой.
 * Прокачка — ветки G_* (вкладки «Фехтование» и «Спираль, изящество»), формулы — skill/GourmetEffects,
 * числа — GourmetBalance.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class GourmetFire {
    /** Фиолетовые блики ленты. */
    private static final DustParticleOptions RIBBON = new DustParticleOptions(new Vector3f(0.62F, 0.26F, 0.96F), 0.9F);
    private static final DustParticleOptions RIBBON_BRIGHT = new DustParticleOptions(new Vector3f(0.82F, 0.6F, 1.0F), 0.7F);
    private static final DustParticleOptions MARK = new DustParticleOptions(new Vector3f(0.75F, 0.15F, 0.85F), 1.1F);

    private static final Map<UUID, Duel> DUELS = new HashMap<>();
    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    /** Состояние дуэлянта: номер удара в серии, когда можно бить снова, когда в последний раз получил урон. */
    private static final class Duel {
        int index;
        long last = Long.MIN_VALUE / 2;
        long busyUntil;
        long lastHurt = Long.MIN_VALUE / 2;
        /** Тик, в который сломался блок, и какую часть урона этого удара «Плотная спираль» гасит. */
        long brokenAt = Long.MIN_VALUE / 2;
        double brokenReduction;
    }

    /** Метка на цели: удары того, кто её поставил, сильнее до тика until. */
    private record Mark(LivingEntity entity, UUID owner, long until, double bonus) {
    }

    private GourmetFire() {
    }

    /** У игрока сейчас «Гурман». */
    public static boolean uses(PlayerData data) {
        return data != null && data.getPlayerClass().hasKagune() && data.getKaguneVariant() == KaguneVariant.GOURMET;
    }

    private static Duel duel(ServerPlayer player) {
        return DUELS.computeIfAbsent(player.getUUID(), k -> new Duel());
    }

    private static int level(PlayerData data, SkillTree.Branch branch) {
        return SkillEffects.level(data, branch);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        DUELS.remove(id);
        MARKS.values().removeIf(mark -> mark.owner().equals(id));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DUELS.clear();
        MARKS.clear();
    }

    /** Запоминаем, когда дуэлянт последний раз получил урон: от этого зависит «Изящество». */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamaged(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (uses(PlayerData.get(player))) {
            duel(player).lastHurt = player.level().getGameTime();
        }
    }

    /** Метки: истёкшие убираем, на живых раз в полсекунды фиолетовые искры над головой. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MARKS.isEmpty()) {
            return;
        }
        Iterator<Mark> it = MARKS.values().iterator();
        while (it.hasNext()) {
            Mark mark = it.next();
            LivingEntity e = mark.entity();
            long now = e.level().getGameTime();
            if (!e.isAlive() || e.isRemoved() || now >= mark.until()) {
                it.remove();
                continue;
            }
            if (now % 10 == 0 && e.level() instanceof ServerLevel level) {
                level.sendParticles(MARK, e.getX(), e.getY() + e.getBbHeight() + 0.35, e.getZ(), 2, 0.15, 0.05, 0.15, 0.0);
            }
        }
    }

    // ------------------------------------------------------------------
    // Множители урона (CombatEvents.onLivingHurt)
    // ------------------------------------------------------------------

    /** «Изящество» и «Метка»: во сколько раз сильнее удар дуэлянта по этой цели. */
    public static double damageFactor(ServerPlayer attacker, PlayerData data, LivingEntity victim) {
        if (!uses(data)) {
            return 1.0;
        }
        long now = attacker.level().getGameTime();
        double factor = 1.0;
        if (clean(attacker, now)) {
            factor *= 1.0 + GourmetEffects.graceDamage(level(data, SkillTree.Branch.G_GRACE));
        }
        Mark mark = MARKS.get(victim.getUUID());
        if (mark != null && mark.owner().equals(attacker.getUUID()) && now < mark.until()) {
            factor *= 1.0 + mark.bonus();
        }
        return factor;
    }

    /** «Плотная спираль»: удар, который только что сломал блок дуэлянта, проходит слабее. */
    public static double brokenBlockFactor(ServerPlayer victim) {
        Duel duel = DUELS.get(victim.getUUID());
        if (duel == null || duel.brokenAt != victim.level().getGameTime()) {
            return 1.0;
        }
        return 1.0 - duel.brokenReduction;
    }

    /** Давно ли дуэлянт не получал урона. */
    private static boolean clean(ServerPlayer player, long now) {
        Duel duel = DUELS.get(player.getUUID());
        return duel == null || now - duel.lastHurt >= GourmetBalance.CLEAN_TICKS;
    }

    // ------------------------------------------------------------------
    // ЛКМ: серия ударов лентой
    // ------------------------------------------------------------------

    /** Просьба ударить (клик или зажатая ЛКМ каждый тик). Темп держит сервер. */
    public static void basic(ServerPlayer player, PlayerData data, CombatState state, long now) {
        Duel duel = duel(player);
        if (now < duel.busyUntil || now < state.attackBusyUntil) {
            return;
        }
        int seriesLevel = level(data, SkillTree.Branch.G_SERIES);
        int length = GourmetEffects.seriesLength(seriesLevel);
        if (now - duel.last > GourmetBalance.SERIES_RESET || duel.index >= length) {
            duel.index = 0;
        }
        int strike = duel.index++;
        boolean thrust = strike == length - 1;
        duel.last = now;
        duel.busyUntil = now + GourmetEffects.seriesInterval(seriesLevel) + (thrust ? GourmetBalance.SERIES_END_PAUSE : 0);
        String animation = thrust ? "gourmet_thrust" : (strike % 2 == 0 ? "gourmet_slash_a" : "gourmet_slash_b");
        NetworkHandler.playAnimation(player, animation);
        player.swing(InteractionHand.MAIN_HAND, true);
        state.actions.add(new Strike(now, thrust, strike % 2 == 0));
        if (thrust) {
            // выпад подаёт игрока вперёд
            Vec3 forward = forward(player);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(forward.x * GourmetBalance.THRUST_STEP * 0.45, motion.y,
                    forward.z * GourmetBalance.THRUST_STEP * 0.45);
            player.hurtMarked = true;
        }
    }

    /** Один удар серии: урон приходит через пару тиков после начала взмаха. */
    private static final class Strike implements ActiveAction {
        private final long start;
        private final boolean thrust;
        private final boolean fromLeft;

        private Strike(long start, boolean thrust, boolean fromLeft) {
            this.start = start;
            this.thrust = thrust;
            this.fromLeft = fromLeft;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (!player.isAlive() || LockManager.isLocked(player)) {
                return true;
            }
            int at = thrust ? GourmetBalance.THRUST_HIT_TICK : GourmetBalance.SLASH_HIT_TICK;
            if (now - start < at) {
                return false;
            }
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                if (thrust) {
                    thrust(player, data, now);
                } else {
                    slash(player, data, fromLeft);
                }
            }
            return true;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    private static void slash(ServerPlayer player, PlayerData data, boolean fromLeft) {
        double reach = GourmetEffects.reach(level(data, SkillTree.Branch.G_REACH));
        Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.75F, 1.45F + player.getRandom().nextFloat() * 0.2F);
        arc(player, reach, fromLeft);
        for (LivingEntity target : Targeting.inFront(player, reach, GourmetBalance.HALF_WIDTH)) {
            if (Combos.spared(player, target)) {
                continue;
            }
            if (CombatContext.hit(player, target, GourmetBalance.SLASH_DAMAGE, AttackType.BASIC)) {
                afterHit(player, data, target, 6);
            }
        }
    }

    private static void thrust(ServerPlayer player, PlayerData data, long now) {
        double reach = GourmetEffects.reach(level(data, SkillTree.Branch.G_REACH)) + 0.5;
        int thrustLevel = level(data, SkillTree.Branch.G_THRUST);
        float damage = GourmetEffects.thrustDamage(thrustLevel);
        Effects.sound(player, SoundEvents.TRIDENT_THROW, 0.8F, 1.5F);
        line(player, reach);
        List<LivingEntity> targets = new ArrayList<>(Targeting.inFront(player, reach, GourmetBalance.HALF_WIDTH * 0.7));
        targets.removeIf(e -> Combos.spared(player, e));
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        int left = GourmetEffects.thrustTargets(thrustLevel);
        boolean any = false;
        for (LivingEntity target : targets) {
            if (left-- <= 0) {
                break;
            }
            if (CombatContext.hit(player, target, damage, AttackType.BASIC)) {
                afterHit(player, data, target, 12);
                mark(player, data, target, now);
                any = true;
            }
        }
        if (any) {
            Effects.sound(player, SoundEvents.PLAYER_ATTACK_CRIT, 0.9F, 1.3F);
            thrustRage(player, data);
        }
    }

    /** Кровь и кровотечение после попадания лентой. */
    private static void afterHit(ServerPlayer player, PlayerData data, LivingEntity target, int blood) {
        Effects.blood(target, blood);
        int bleed = SkillEffects.bleedSeconds(level(data, SkillTree.Branch.BLEED));
        if (bleed > 0) {
            BleedManager.apply(target, bleed);
        }
    }

    /** Ярость за попавший выпад; «Изящество» добавляет, а в чистой серии ещё вдвое. */
    private static void thrustRage(ServerPlayer player, PlayerData data) {
        int points = GourmetEffects.graceRage(level(data, SkillTree.Branch.G_GRACE));
        if (clean(player, player.level().getGameTime())) {
            points *= 2;
        }
        if (AbilityManager.gainRage(data, points)) {
            NetworkHandler.syncTo(player);
        }
    }

    /** «Метка»: цель уязвима к ударам этого дуэлянта. */
    private static void mark(ServerPlayer player, PlayerData data, LivingEntity target, long now) {
        double bonus = GourmetEffects.markBonus(level(data, SkillTree.Branch.G_MARK));
        if (bonus <= 0.0 || !target.isAlive()) {
            return;
        }
        MARKS.put(target.getUUID(), new Mark(target, player.getUUID(), now + GourmetBalance.MARK_TICKS, bonus));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(MARK, target.getX(), target.getY(0.7), target.getZ(), 8, 0.25, 0.3, 0.25, 0.02);
        }
    }

    // ------------------------------------------------------------------
    // ПКМ дважды: «Прямой выпад»
    // ------------------------------------------------------------------

    public static void lunge(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.strongReady) {
            AbilityManager.cooldown(player, AbilityType.STRONG_ATTACK, state.strongReady - now);
            return;
        }
        for (ActiveAction action : state.actions) {
            if (action instanceof Lunge) {
                return;
            }
        }
        int lvl = level(data, SkillTree.Branch.G_LUNGE);
        state.strongReady = now + GourmetEffects.lungeCooldown(lvl);
        Duel duel = duel(player);
        duel.index = 0;
        duel.busyUntil = now + GourmetBalance.LUNGE_WINDUP + GourmetBalance.LUNGE_TICKS + 4;
        state.actions.removeIf(action -> action instanceof Actions.Dash);
        state.actions.add(new Lunge(now, forward(player), GourmetEffects.lungeDistance(lvl), GourmetEffects.lungeDamage(lvl)));
        NetworkHandler.playAnimation(player, "gourmet_lunge");
        Effects.sound(player, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 0.6F);
    }

    /** Короткий замах, потом рывок по прямой; первая цель на пути получает укол, и рывок кончается. */
    private static final class Lunge implements ActiveAction {
        private final long start;
        private final Vec3 dir;
        private final double speed;
        private final float damage;

        private Lunge(long start, Vec3 dir, double distance, float damage) {
            this.start = start;
            this.dir = dir;
            this.speed = distance / GourmetBalance.LUNGE_TICKS;
            this.damage = damage;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (!player.isAlive() || LockManager.isLocked(player)) {
                return true;
            }
            long elapsed = now - start;
            ServerLevel level = (ServerLevel) player.level();
            if (elapsed < GourmetBalance.LUNGE_WINDUP) {
                Vec3 hand = hand(player);
                level.sendParticles(RIBBON, hand.x, hand.y, hand.z, 3, 0.15, 0.15, 0.15, 0.01);
                return false;
            }
            if (elapsed == GourmetBalance.LUNGE_WINDUP) {
                player.swing(InteractionHand.MAIN_HAND, true);
                Effects.sound(player, SoundEvents.TRIDENT_RIPTIDE_1, 0.8F, 1.4F);
            }
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(dir.x * speed, Math.min(motion.y, 0.1), dir.z * speed);
            player.hurtMarked = true;
            Vec3 hand = hand(player);
            level.sendParticles(RIBBON, hand.x, hand.y, hand.z, 4, 0.2, 0.1, 0.2, 0.0);
            // цель прямо перед игроком: укол
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity target : Targeting.inFront(player, 1.6 + speed, GourmetBalance.LUNGE_HALF_WIDTH)) {
                if (Combos.spared(player, target)) {
                    continue;
                }
                double d = target.distanceToSqr(player);
                if (d < bestDist) {
                    bestDist = d;
                    best = target;
                }
            }
            if (best != null) {
                stop(player);
                PlayerData data = PlayerData.get(player);
                if (CombatContext.hit(player, best, damage, AttackType.STRONG) && data != null) {
                    afterHit(player, data, best, 18);
                    mark(player, data, best, now);
                    thrustRage(player, data);
                    Effects.sound(best, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.2F);
                    level.sendParticles(ParticleTypes.CRIT, best.getX(), best.getY(0.6), best.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
                }
                return true;
            }
            if (player.horizontalCollision || elapsed >= GourmetBalance.LUNGE_WINDUP + GourmetBalance.LUNGE_TICKS) {
                stop(player);
                return true;
            }
            return false;
        }

        private static void stop(ServerPlayer player) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.15, motion.y, motion.z * 0.15);
            player.hurtMarked = true;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    // ------------------------------------------------------------------
    // K: «Вихрь ленты» (барраж)
    // ------------------------------------------------------------------

    public static void whirl(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.barrageReady) {
            AbilityManager.cooldown(player, AbilityType.BARRAGE, state.barrageReady - now);
            return;
        }
        state.actions.add(new Whirl(now));
        NetworkHandler.playAnimation(player, "gourmet_whirl");
        Effects.sound(player, SoundEvents.TRIDENT_RIPTIDE_2, 0.8F, 1.3F);
    }

    /** Откат вихря после серии. */
    public static int whirlCooldown(PlayerData data) {
        return GourmetEffects.whirlCooldown(level(data, SkillTree.Branch.G_WHIRL));
    }

    /** Лента кружит вокруг игрока: раз в 4 тика бьёт всех в круге, пока держат K (не дольше 2 с). */
    private static final class Whirl implements BarrageAction {
        private final long start;

        private Whirl(long start) {
            this.start = start;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (!player.isAlive() || LockManager.isLocked(player)) {
                NetworkHandler.playAnimation(player, "");
                return true;
            }
            long elapsed = now - start;
            if (elapsed >= GourmetBalance.WHIRL_DURATION) {
                return true;
            }
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                return true;
            }
            int lvl = level(data, SkillTree.Branch.G_WHIRL);
            double radius = GourmetEffects.whirlRadius(lvl);
            ring(player, radius, elapsed);
            if (elapsed % GourmetBalance.WHIRL_INTERVAL == 0) {
                player.swing(InteractionHand.MAIN_HAND, true);
                Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.5F + player.getRandom().nextFloat() * 0.3F);
                float damage = GourmetEffects.whirlDamage(lvl);
                AABB area = player.getBoundingBox().inflate(radius, 1.0, radius);
                for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, area,
                        e -> Targeting.valid(player, e) && !Combos.spared(player, e)
                                && horizontalDistSq(e, player) <= radius * radius)) {
                    if (CombatContext.hit(player, target, damage, AttackType.BARRAGE)) {
                        afterHit(player, data, target, 3);
                        QuestManager.progress(player, Quest.Trigger.BARRAGE_HIT, 1);
                    }
                }
            }
            return false;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    private static double horizontalDistSq(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------------
    // Захваты: у «Гурмана» их нет
    // ------------------------------------------------------------------

    public static void noGrab(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.gourmet_no_grab"), true);
    }

    // ------------------------------------------------------------------
    // Блок: «Парирование» и «Плотная спираль» (CombatEvents.onLivingAttack)
    // ------------------------------------------------------------------

    /**
     * Удар пришёл в дуэлянта, который держит блок. Если блок поставлен совсем недавно (окно «Парирования»),
     * удар гасится целиком, даже сильный, а лента бьёт атакующего контрвыпадом. Возвращает true, если парировано.
     */
    public static boolean tryParry(ServerPlayer blocker, PlayerData data, Entity source) {
        if (!uses(data) || !(source instanceof LivingEntity attacker) || attacker == blocker) {
            return false;
        }
        int lvl = level(data, SkillTree.Branch.G_PARRY);
        int window = GourmetEffects.parryWindow(lvl);
        long now = blocker.level().getGameTime();
        CombatState state = data.combat();
        if (window <= 0 || now - state.blockStart > window
                || attacker.distanceToSqr(blocker) > GourmetBalance.PARRY_RANGE * GourmetBalance.PARRY_RANGE) {
            return false;
        }
        state.blockStart = Long.MIN_VALUE / 2;      // одно парирование за один блок
        blocker.displayClientMessage(Component.translatable("message.tokyoghoulrpg.parry"), true);
        QuestManager.progress(blocker, Quest.Trigger.PARRY, 1);
        Effects.sound(blocker, SoundEvents.ANVIL_LAND, 0.4F, 1.9F);
        NetworkHandler.playAnimation(blocker, "gourmet_thrust");
        blocker.swing(InteractionHand.MAIN_HAND, true);
        if (blocker.level() instanceof ServerLevel level) {
            Vec3 hand = hand(blocker);
            level.sendParticles(RIBBON_BRIGHT, hand.x, hand.y, hand.z, 10, 0.3, 0.3, 0.3, 0.05);
        }
        int stun = GourmetEffects.parryStun(lvl);
        if (stun > 0) {
            LockManager.lock(attacker, now + stun);
        }
        if (Targeting.valid(blocker, attacker) && !Combos.spared(blocker, attacker)
                && CombatContext.hit(blocker, attacker, GourmetEffects.parryDamage(lvl), AttackType.BASIC)) {
            afterHit(blocker, data, attacker, 10);
            mark(blocker, data, attacker, now);
        }
        return true;
    }

    /** Блок дуэлянта сейчас сломается: «Плотная спираль» гасит часть урона, а с III уровня отбрасывает атакующего. */
    public static void onBlockBroken(ServerPlayer blocker, PlayerData data, Entity source) {
        if (!uses(data)) {
            return;
        }
        int lvl = level(data, SkillTree.Branch.G_TIGHT);
        double reduction = GourmetEffects.tightReduction(lvl);
        if (reduction > 0.0) {
            Duel duel = duel(blocker);
            duel.brokenAt = blocker.level().getGameTime();
            duel.brokenReduction = reduction;
        }
        if (GourmetEffects.tightPush(lvl) && source instanceof LivingEntity attacker && attacker != blocker) {
            Vec3 push = attacker.position().subtract(blocker.position());
            attacker.knockback(GourmetBalance.TIGHT_PUSH, -push.x, -push.z);
            Effects.sound(blocker, SoundEvents.CHAIN_BREAK, 1.0F, 0.8F);
        }
    }

    // ------------------------------------------------------------------
    // Alt + J: «Изящный танец»
    // ------------------------------------------------------------------

    public static void ultimate(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < data.getUltimateReady()) {
            long left = (data.getUltimateReady() - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ultimate_cooldown",
                    left / 60, left % 60), true);
            return;
        }
        for (ActiveAction action : state.actions) {
            if (action instanceof Dance) {
                return;
            }
        }
        LivingEntity target = ultimateTarget(player);
        if (target == null) {
            AbilityManager.noTarget(player);
            return;
        }
        data.setUltimateReady(now + Balance.ULTIMATE_COOLDOWN);
        state.actions.removeIf(action -> action instanceof Actions.Dash || action instanceof Lunge);
        ServerLevel level = (ServerLevel) player.level();
        Vec3 from = player.position();
        level.sendParticles(RIBBON, from.x, from.y + 1.0, from.z, 30, 0.3, 0.6, 0.3, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 20, 0.3, 0.6, 0.3, 0.05);
        // за спину цели; если там стена — просто вплотную перед ней
        Vec3 behind = behind(target);
        Vec3 away = from.subtract(target.position()).multiply(1, 0, 1);
        Vec3 spot = fits(player, behind) || away.lengthSqr() < 1.0E-4 ? behind
                : target.position().add(away.normalize().scale(1.3));
        Vec3 look = target.getBoundingBox().getCenter().subtract(spot.add(0.0, player.getEyeHeight(), 0.0));
        float yaw = (float) (Math.toDegrees(Math.atan2(-look.x, look.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z))));
        player.connection.teleport(spot.x, spot.y, spot.z, yaw, pitch);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        level.sendParticles(RIBBON_BRIGHT, spot.x, spot.y + 1.0, spot.z, 30, 0.3, 0.6, 0.3, 0.05);
        Effects.sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.4F);
        Effects.sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.6F);
        long end = now + GourmetBalance.ULT_WINDUP + (long) GourmetBalance.ULT_STRIKES * GourmetBalance.ULT_INTERVAL + 4;
        LockManager.lockFull(player, end);
        LockManager.lock(target, end);
        state.actions.add(new Dance(now, target));
        NetworkHandler.playAnimation(player, "gourmet_dance");
        NetworkHandler.syncTo(player);
    }

    /** Цель ульты: та, на кого смотрит игрок, иначе ближайшая рядом. */
    private static LivingEntity ultimateTarget(ServerPlayer player) {
        Targeting.Hit hit = Targeting.raycast(player, GourmetBalance.ULT_RANGE, false, e -> !Combos.spared(player, e));
        if (hit != null && hit.entity() != null) {
            return hit.entity();
        }
        LivingEntity best = null;
        double bestDist = 36.0;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0),
                c -> Targeting.valid(player, c) && !Combos.spared(player, c))) {
            double d = e.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static Vec3 behind(LivingEntity target) {
        double yaw = Math.toRadians(target.getYRot());
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        return target.position().subtract(facing.scale(GourmetBalance.ULT_BEHIND + target.getBbWidth() / 2.0));
    }

    private static boolean fits(ServerPlayer player, Vec3 spot) {
        AABB box = player.getBoundingBox().move(spot.subtract(player.position()));
        return player.level().noCollision(player, box);
    }

    /** Серия молниеносных выпадов по цели: сквозь броню и блок, как ульты других кагуне. */
    private static final class Dance implements ActiveAction {
        private final long start;
        private final LivingEntity target;
        private int done;

        private Dance(long start, LivingEntity target) {
            this.start = start;
            this.target = target;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (!player.isAlive()) {
                LockManager.unlock(player);
                return true;
            }
            long elapsed = now - start;
            ServerLevel level = (ServerLevel) player.level();
            if (elapsed < GourmetBalance.ULT_WINDUP) {
                Vec3 hand = hand(player);
                level.sendParticles(RIBBON_BRIGHT, hand.x, hand.y, hand.z, 3, 0.2, 0.2, 0.2, 0.02);
                return false;
            }
            if (done < GourmetBalance.ULT_STRIKES) {
                if ((elapsed - GourmetBalance.ULT_WINDUP) % GourmetBalance.ULT_INTERVAL == 0) {
                    done++;
                    player.swing(InteractionHand.MAIN_HAND, true);
                    Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.3F + done * 0.08F);
                    if (target.isAlive() && target.distanceToSqr(player) < 25.0) {
                        CombatContext.hitUltimate(player, target, (float) (GourmetBalance.ULT_DAMAGE * ultimateFactor(target)));
                        Effects.blood(target, 6);
                        Vec3 c = target.getBoundingBox().getCenter();
                        level.sendParticles(RIBBON, c.x, c.y, c.z, 10, 0.4, 0.5, 0.4, 0.08);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0.2, 0.2, 0.2, 0.0);
                    }
                }
                return false;
            }
            if (target.isAlive() && target.level() instanceof ServerLevel targetLevel) {
                Vec3 c = target.getBoundingBox().getCenter();
                targetLevel.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 20, 0.4, 0.5, 0.4, 0.4);
            }
            Effects.sound(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F);
            LockManager.unlock(player);
            return true;
        }

        @Override
        public void cancel(ServerPlayer player) {
            LockManager.unlock(player);
        }
    }

    /** Люди и агенты CCG получают от ульты 75%, как от ульт других кагуне. */
    private static double ultimateFactor(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && !data.getPlayerClass().hasKagune()) {
                return Balance.ULTIMATE_HUMAN_FACTOR;
            }
        }
        return 1.0;
    }

    // ------------------------------------------------------------------
    // Геометрия и частицы
    // ------------------------------------------------------------------

    /** Куда смотрит игрок по горизонтали (в VR — куда направлена рука). */
    private static Vec3 forward(ServerPlayer player) {
        double yaw = Math.toRadians(Aim.yaw(player));
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    /** Примерно где кисть правой руки (там кончик ленты). */
    private static Vec3 hand(ServerPlayer player) {
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        return player.position().add(right.scale(0.38)).add(forward.scale(0.25)).add(0.0, player.isCrouching() ? 0.55 : 0.8, 0.0);
    }

    /** Фиолетовая дуга взмаха перед игроком. */
    private static void arc(ServerPlayer player, double reach, boolean fromLeft) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double yaw = Math.toRadians(Aim.yaw(player));
        double y = player.getY() + player.getBbHeight() * 0.62;
        for (int i = 0; i <= 8; i++) {
            double t = (fromLeft ? i : 8 - i) / 8.0;
            double a = yaw + Math.toRadians(-55.0 + 110.0 * t);
            double r = reach * 0.8;
            double x = player.getX() - Math.sin(a) * r;
            double z = player.getZ() + Math.cos(a) * r;
            level.sendParticles(i % 3 == 0 ? RIBBON_BRIGHT : RIBBON, x, y + (t - 0.5) * 0.3, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Прямая линия укола. */
    private static void line(ServerPlayer player, double reach) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 f = forward(player);
        Vec3 from = hand(player).add(0.0, 0.35, 0.0);
        for (int i = 1; i <= 10; i++) {
            Vec3 p = from.add(f.scale(reach * i / 10.0));
            level.sendParticles(i > 7 ? RIBBON_BRIGHT : RIBBON, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Кольцо вращающейся ленты вокруг игрока. */
    private static void ring(ServerPlayer player, double radius, long elapsed) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double a = elapsed * 0.9 + i * Math.PI / 2.0;
            double x = player.getX() + Math.cos(a) * radius * 0.85;
            double z = player.getZ() + Math.sin(a) * radius * 0.85;
            double y = player.getY() + 0.7 + 0.25 * Math.sin(elapsed * 0.5 + i);
            level.sendParticles(i % 2 == 0 ? RIBBON_BRIGHT : RIBBON, x, y, z, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }
}
