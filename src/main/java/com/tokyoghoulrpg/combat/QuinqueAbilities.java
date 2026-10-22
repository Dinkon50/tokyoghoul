package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.QuinqueItem;
import com.tokyoghoulrpg.item.QuinqueType;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Способности агента CCG с квинке в руке. Без квинке не работают.
 * <ul>
 *   <li>ЛКМ — серия из трёх ударов, третий сильнее;</li>
 *   <li>ПКМ дважды — сильный удар, пробивает блок гуля;</li>
 *   <li>K (зажать) — «Шквал»: серия быстрых ударов, попавшие стоят на месте;</li>
 *   <li>L (зажать) — блок; поставленный за 0,5 с до удара — парирование, атакующий оглушён;</li>
 *   <li>J — «Освобождение квинке»: усиление на 15 с, шкала копится от ударов по гулям;</li>
 *   <li>Shift + ЛКМ — «Выпад»: рывок вперёд с уколом всех на пути.</li>
 * </ul>
 * Урон ударов считается от урона квинке в руке, поэтому у разных квинке он будет разный.
 * Анимаций пока нет (обычный взмах рукой): их подключим вместе с моделями квинке.
 */
public final class QuinqueAbilities {
    private static final UUID RELEASE_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e40");

    private QuinqueAbilities() {
    }

    public static boolean holdsQuinque(Player player) {
        return player.getMainHandItem().getItem() instanceof QuinqueItem;
    }

    /** Вид квинке в руке (учебная, если в руке не квинке). */
    public static QuinqueType type(Player player) {
        return player.getMainHandItem().getItem() instanceof QuinqueItem item ? item.type() : QuinqueType.TRAINING;
    }

    /** Агент CCG с квинке в руке. */
    public static boolean active(Player player, PlayerData data) {
        return data != null && data.getPlayerClass() == PlayerClass.CCG_AGENT && holdsQuinque(player);
    }

    /** Урон оружия в руке (характеристика «Урон атаки» с учётом квинке). */
    private static float weaponDamage(ServerPlayer player) {
        return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** Запрос способности от клиента. */
    public static void use(ServerPlayer player, PlayerData data, AbilityType type) {
        if (!active(player, data) || !player.isAlive() || LockManager.isLocked(player)) {
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();
        if (state.isBarraging() || data.isBlocking()) {
            return;
        }
        switch (type) {
            case QUINQUE_ATTACK -> attack(player, data, state, now);
            case QUINQUE_STRONG -> strong(player, data, state, now);
            case QUINQUE_LUNGE -> lunge(player, data, state, now);
            case QUINQUE_FLURRY -> flurry(player, data, state, now);
            case QUINQUE_RELEASE -> release(player, data, now);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ удары

    private static int lvl(PlayerData data, SkillTree.Branch branch) {
        return SkillEffects.level(data, branch);
    }

    /** Насколько дальше достают все удары квинке: свойство квинке плюс ветка «Длина квинке». */
    private static double reach(ServerPlayer player, PlayerData data) {
        return Balance.QUINQUE_REACH + type(player).reachBonus() + SkillEffects.qReach(lvl(data, SkillTree.Branch.Q_REACH));
    }

    private static void attack(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.attackBusyUntil) {
            return;
        }
        // серия сбрасывается, если долго не бить
        if (now - state.lastQuinqueAttack > Balance.QUINQUE_COMBO_RESET) {
            state.comboIndex = 0;
        }
        state.lastQuinqueAttack = now;
        int step = state.comboIndex++ % 3;
        boolean finisher = step == 2;
        state.attackBusyUntil = now + (finisher ? Balance.QUINQUE_FINISHER_BUSY : Balance.QUINQUE_ATTACK_BUSY);
        player.swing(InteractionHand.MAIN_HAND, true);
        int combo = lvl(data, SkillTree.Branch.Q_COMBO);
        float finisherMult = Balance.QUINQUE_FINISHER_MULT + SkillEffects.qFinisherBonus(combo);
        float damage = (float) (weaponDamage(player) * (finisher ? finisherMult : 1.0F)
                * (1.0 + SkillEffects.qComboDamage(combo)));
        state.actions.add(new Actions.Attack(AttackType.BASIC, now, new int[] {Balance.QUINQUE_HIT_DELAY}, damage,
                reach(player, data), Balance.QUINQUE_HALF_WIDTH));
    }

    private static void strong(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.strongReady) {
            cooldown(player, AbilityType.QUINQUE_STRONG, state.strongReady - now);
            return;
        }
        if (now < state.attackBusyUntil) {
            return;
        }
        QuinqueType type = type(player);
        int strongLevel = lvl(data, SkillTree.Branch.Q_STRONG);
        float strongDamage = (float) (weaponDamage(player) * type.strongMult() * (1.0 + SkillEffects.qStrongDamage(strongLevel)));
        state.strongReady = now + Balance.QUINQUE_STRONG_COOLDOWN;
        state.attackBusyUntil = now + Balance.QUINQUE_STRONG_DELAY + 4;
        player.swing(InteractionHand.MAIN_HAND, true);
        if (type.ranged()) {
            // Narukami: разряд на расстояние
            state.actions.add(new Bolt(now + Balance.QUINQUE_STRONG_DELAY, strongDamage));
            Effects.sound(player, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.8F);
            return;
        }
        Effects.sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        state.actions.add(new Actions.Attack(AttackType.STRONG, now, new int[] {Balance.QUINQUE_STRONG_DELAY},
                strongDamage, reach(player, data) + 0.5 + SkillEffects.qStrongReach(strongLevel),
                Balance.QUINQUE_HALF_WIDTH + 0.3));
    }

    private static void flurry(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.barrageReady) {
            cooldown(player, AbilityType.QUINQUE_FLURRY, state.barrageReady - now);
            return;
        }
        QuinqueType type = type(player);
        int count = type.flurryHits() + SkillEffects.qFlurryExtraHits(lvl(data, SkillTree.Branch.Q_FLURRY));
        int[] hits = new int[count];
        for (int i = 0; i < count; i++) {
            hits[i] = 2 + i * type.flurryInterval();
        }
        int duration = hits[count - 1] + 4;
        float damage = weaponDamage(player) * type.flurryMult();
        state.actions.add(new Actions.Barrage(now, hits, duration, damage, Balance.QUINQUE_FLURRY_WIDTH));
    }

    private static void lunge(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.lungeReady) {
            cooldown(player, AbilityType.QUINQUE_LUNGE, state.lungeReady - now);
            return;
        }
        // выпад туда, куда направлена квинке (в VR — рука, иначе взгляд)
        Vec3 look = Aim.dir(player);
        Vec3 direction = new Vec3(look.x, 0.0, look.z);
        if (direction.lengthSqr() < 1.0E-4) {
            look = player.getLookAngle();
            direction = new Vec3(look.x, 0.0, look.z);
        }
        if (direction.lengthSqr() < 1.0E-4) {
            return;
        }
        int lungeLevel = lvl(data, SkillTree.Branch.Q_LUNGE);
        state.lungeReady = now + SkillEffects.qLungeCooldown(lungeLevel);
        player.swing(InteractionHand.MAIN_HAND, true);
        Effects.sound(player, SoundEvents.TRIDENT_RIPTIDE_1, 0.8F, 1.4F);
        float damage = (float) (weaponDamage(player) * Balance.QUINQUE_LUNGE_MULT * (1.0 + SkillEffects.qLungeDamage(lungeLevel)));
        state.actions.add(new Lunge(direction.normalize(), damage,
                type(player).lungeTicks() + SkillEffects.qLungeExtraTicks(lungeLevel)));
    }

    /** «Выпад»: несколько тиков быстрого движения вперёд, каждого на пути колет один раз. */
    private static final class Lunge implements ActiveAction {
        private final Vec3 direction;
        private final float damage;
        private final Set<LivingEntity> hit = new HashSet<>();
        private int ticksLeft;

        Lunge(Vec3 direction, float damage, int ticks) {
            this.direction = direction;
            this.damage = damage;
            this.ticksLeft = ticks;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(direction.x * Balance.QUINQUE_LUNGE_SPEED, Math.min(motion.y, 0.1),
                    direction.z * Balance.QUINQUE_LUNGE_SPEED);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            Effects.particles(player, ParticleTypes.CLOUD, 2, 0.2, 0.01);
            for (LivingEntity target : Targeting.inFront(player, 1.8, 0.9)) {
                if (hit.add(target) && CombatContext.hit(player, target, damage, AttackType.BASIC)) {
                    Effects.blood(target, 10);
                    Effects.sound(target, SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
                }
            }
            ticksLeft--;
            if (ticksLeft <= 0) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.3, 1.0, 0.3));
                player.hurtMarked = true;
                return true;
            }
            return false;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    /** Narukami: после короткого замаха разряд летит по взгляду до 16 блоков и бьёт первое существо. */
    private static final class Bolt implements ActiveAction {
        private final long fireAt;
        private final float damage;

        Bolt(long fireAt, float damage) {
            this.fireAt = fireAt;
            this.damage = damage;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (now < fireAt) {
                Effects.particles(player, ParticleTypes.ELECTRIC_SPARK, 2, 0.3, 0.05);
                return false;
            }
            Vec3 from = Aim.origin(player);
            Targeting.Hit hit = Targeting.raycast(player, Balance.NARUKAMI_BOLT_RANGE, true, e -> true);
            Vec3 to = hit != null ? hit.point() : from.add(Aim.dir(player).scale(Balance.NARUKAMI_BOLT_RANGE));
            if (hit != null && hit.entity() != null) {
                to = hit.entity().getBoundingBox().getCenter();
                if (CombatContext.hit(player, hit.entity(), damage, AttackType.STRONG)) {
                    Effects.particles(hit.entity(), ParticleTypes.ELECTRIC_SPARK, 25, 0.4, 0.2);
                }
            }
            // светящаяся линия разряда
            if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                Vec3 step = to.subtract(from);
                int points = (int) Math.max(2, step.length() * 3);
                for (int i = 0; i <= points; i++) {
                    Vec3 p = from.add(step.scale(i / (double) points));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
                }
            }
            Effects.sound(player, SoundEvents.TRIDENT_THUNDER, 0.6F, 1.6F);
            return true;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    // ------------------------------------------------------------------ освобождение

    private static void release(ServerPlayer player, PlayerData data, long now) {
        if (data.isRageActive()) {
            return;
        }
        if (data.getRage() < PlayerData.RAGE_MAX) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.release_not_ready"), true);
            return;
        }
        data.setRage(0);
        data.setRageActive(true, now, now + SkillEffects.releaseTicks(lvl(data, SkillTree.Branch.RELEASE)));
        QuestManager.progress(player, Quest.Trigger.RELEASE, 1);
        NetworkHandler.syncTo(player);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.release_on"), true);
        Effects.sound(player, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.4F);
        Effects.particles(player, Effects.RC, 40, 0.6, 0.05);
    }

    /** Удар агента с квинке попал по гулю: копится шкала «Освобождения». */
    public static void onHitGhoul(ServerPlayer player, PlayerData data) {
        int gain = (int) Math.round(Balance.QUINQUE_RELEASE_GAIN
                * (1.0 + SkillEffects.releaseGainBonus(lvl(data, SkillTree.Branch.RELEASE_GAIN))));
        if (data.getPlayerClass() == PlayerClass.CCG_AGENT && holdsQuinque(player) && !data.isRageActive()
                && data.addRage(gain)) {
            NetworkHandler.syncTo(player);
        }
    }

    public static double releaseDamageBonus(PlayerData data) {
        return SkillEffects.releaseDamage(lvl(data, SkillTree.Branch.RELEASE));
    }

    public static double releaseReduction(PlayerData data) {
        return SkillEffects.releaseReduction(lvl(data, SkillTree.Branch.RELEASE));
    }

    // ------------------------------------------------------------------ тик

    /** Раз в тик для агента CCG: окончание шквала, блок, освобождение. */
    public static void tick(ServerPlayer player, PlayerData data, CombatState state, long now) {
        boolean holding = holdsQuinque(player);

        // Шквал закончился: откат
        boolean barraging = state.isBarraging();
        if (state.wasBarraging && !barraging) {
            state.barrageReady = now + SkillEffects.qFlurryCooldown(lvl(data, SkillTree.Branch.Q_FLURRY));
        }
        state.wasBarraging = barraging;
        if (barraging && !holding) {
            AbilityManager.setBarrage(player, false);       // убрал квинке — шквал прерван
        }

        // Блок держится, только пока квинке в руке
        if (data.isBlocking()) {
            if (!player.isAlive() || !holding || LockManager.isLocked(player) || barraging) {
                AbilityManager.stopBlock(player, data);
            } else {
                player.setSprinting(false);
                AbilityManager.blockSlow(player, SkillEffects.qBlockSlow(lvl(data, SkillTree.Branch.Q_BLOCK)));
            }
        }

        // Освобождение квинке
        if (data.isRageActive()) {
            if (!player.isAlive() || now >= data.getRageEndTick()) {
                data.setRageActive(false, 0L, 0L);
                AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, RELEASE_SPEED_ID);
                NetworkHandler.syncTo(player);
                if (player.isAlive()) {
                    player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.release_off"), true);
                }
            } else {
                AttrUtil.set(player, Attributes.MOVEMENT_SPEED, RELEASE_SPEED_ID, "tg_quinque_release",
                        SkillEffects.releaseSpeed(lvl(data, SkillTree.Branch.RELEASE)), AttributeModifier.Operation.MULTIPLY_TOTAL);
                if (now % 4 == 0) {
                    Effects.particles(player, Effects.RC, 3, 0.45, 0.01);
                }
            }
        } else {
            AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, RELEASE_SPEED_ID);
        }
    }

    /**
     * Идеальный блок (парирование) у всех классов с блоком. Срабатывает только против СИЛЬНОГО удара
     * (ПКМ дважды кагуне, сильная атака человека, сильный удар квинке, особые атаки боссов):
     * блок поставлен совсем недавно, удар ближний — атакующий оглушён и отброшен.
     * Вызывается из обработчика блока, когда удар уже погашен.
     */
    public static boolean tryParry(ServerPlayer blocker, PlayerData data, LivingEntity attacker) {
        long now = blocker.level().getGameTime();
        // Идеальный блок есть у всех, у кого есть блок: у гуля и полугуля (L) и у агента CCG с квинке
        boolean canParry = data.isBlocking() && (data.getPlayerClass().hasKagune() || active(blocker, data));
        double window = Balance.QUINQUE_PARRY_WINDOW * (holdsQuinque(blocker) ? type(blocker).parryWindowMult() : 1.0)
                * (1.0 + SkillEffects.qParryWindowBonus(lvl(data, SkillTree.Branch.Q_BLOCK)));
        if (!canParry || now - data.combat().blockStart > window
                || attacker.distanceToSqr(blocker) > 36.0) {
            return false;
        }
        LockManager.lock(attacker, now + SkillEffects.qParryStun(lvl(data, SkillTree.Branch.Q_BLOCK)));
        QuestManager.progress(blocker, Quest.Trigger.PARRY, 1);
        Vec3 push = attacker.position().subtract(blocker.position());
        attacker.knockback(0.6, -push.x, -push.z);
        Effects.sound(blocker, SoundEvents.ANVIL_LAND, 0.5F, 1.8F);
        Effects.particles(attacker, ParticleTypes.CRIT, 15, 0.3, 0.2);
        blocker.displayClientMessage(Component.translatable("message.tokyoghoulrpg.parry"), true);
        data.combat().blockStart = Long.MIN_VALUE / 2;      // одно парирование за один блок
        return true;
    }

    private static void cooldown(ServerPlayer player, AbilityType type, long ticksLeft) {
        String seconds = String.format(java.util.Locale.ROOT, "%.1f", ticksLeft / 20.0);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown",
                Component.translatable(type.translationKey()), seconds), true);
    }
}
