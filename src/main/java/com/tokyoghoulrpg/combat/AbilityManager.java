package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.raid.CcgRaids;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.tokyoghoulrpg.data.PlayerClass;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

/** Серверная логика способностей. Клиент только просит, всё решает сервер. */
public final class AbilityManager {
    private static final UUID BLOCK_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e10");
    private static final UUID RAGE_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e11");
    private static final UUID BARRAGE_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e12");
    private static final UUID SKILL_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e13");
    private static final UUID GHOUL_HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e14");
    private static final UUID HUMAN_HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e15");
    private static final UUID HUMAN_ATTACK_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e16");

    private AbilityManager() {
    }

    private static int lvl(PlayerData data, SkillTree.Branch branch) {
        return SkillEffects.level(data, branch);
    }

    /** Добавляет очки ярости с учётом ветки «Накопление ярости». Во время ярости очки не копятся. */
    public static boolean gainRage(PlayerData data, int base) {
        int points = (int) Math.round(base * (1.0 + SkillEffects.rageGainBonus(lvl(data, SkillTree.Branch.RAGE_GAIN))));
        return data.addRage(points);
    }

    // ------------------------------------------------------------------
    // Запросы от клиента
    // ------------------------------------------------------------------

    public static void useAbility(ServerPlayer player, AbilityType type) {
        PlayerData data = PlayerData.get(player);
        // Рывок есть у любого класса
        if (type == AbilityType.DASH && data != null && data.hasChosenClass() && !data.getPlayerClass().hasKagune()) {
            if (player.isAlive() && !LockManager.isLocked(player)) {
                dash(player, data, data.combat(), player.level().getGameTime());
            }
            return;
        }
        // Сильная атака человека
        if (type == AbilityType.HUMAN_STRONG_ATTACK) {
            if (data != null && data.getPlayerClass() == PlayerClass.HUMAN && player.isAlive()
                    && !LockManager.isLocked(player)) {
                humanStrongAttack(player, data.combat(), player.level().getGameTime());
            }
            return;
        }
        // Способности пока только у гуля и полугуля
        if (data == null || !data.getPlayerClass().hasKagune()) {
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();

        // Оглушённый, играющий барраж или держащий блок не может использовать другие способности
        if (!player.isAlive() || LockManager.isLocked(player) || state.isBarraging() || data.isBlocking()) {
            return;
        }
        if (type.needsKagune() && !data.isKaguneActive()) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.need_kagune"), true);
            return;
        }

        switch (type) {
            case BITE -> bite(player, data, state, now);
            case DASH -> dash(player, data, state, now);
            case BARRAGE -> barrage(player, data, state, now);
            case RAGE -> rage(player, data, now);
            case BASIC_ATTACK -> attack(player, data, state, now, false);
            case STRONG_ATTACK -> attack(player, data, state, now, true);
            case GRAPPLE_SELF -> grapple(player, data, state, now);
            case PULL_TARGET -> pull(player, data, state, now);
            default -> {
            }
        }
    }

    /**
     * Барраж удерживается кнопкой K: нажал, и серия началась; отпустил, и серия закончилась.
     * Если серия дошла до конца, а кнопку ещё держат, новая не начинается: нужно отпустить и нажать снова.
     */
    public static void setBarrage(ServerPlayer player, boolean held) {
        if (held) {
            useAbility(player, AbilityType.BARRAGE);
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        CombatState state = data.combat();
        boolean stopped = state.actions.removeIf(action -> {
            if (action instanceof Actions.Barrage) {
                action.cancel(player);
                return true;
            }
            return false;
        });
        if (stopped) {
            NetworkHandler.playAnimation(player, "");
        }
    }

    /** Блок удерживается кнопкой: клиент присылает «нажал» и «отпустил». */
    public static void setBlock(ServerPlayer player, boolean held) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        if (!held) {
            if (data.isBlocking()) {
                endBlock(player, data);
            }
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();
        if (!data.getPlayerClass().hasKagune() || data.isBlocking() || !player.isAlive()) {
            return;
        }
        if (LockManager.isLocked(player) || state.isBarraging()) {
            return;
        }
        if (now < state.blockReady) {
            cooldown(player, AbilityType.BLOCK, state.blockReady - now);
            return;
        }
        data.setBlocking(true);
        NetworkHandler.syncTo(player);
    }

    // ------------------------------------------------------------------
    // Способности
    // ------------------------------------------------------------------

    private static void bite(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.biteReady) {
            cooldown(player, AbilityType.BITE, state.biteReady - now);
            return;
        }
        Targeting.Hit hit = Targeting.raycast(player, Balance.BITE_RANGE, false, AbilityManager::isBitable);
        if (hit == null) {
            noTarget(player);
            return;
        }
        int level = lvl(data, SkillTree.Branch.BITE);
        state.biteReady = now + SkillEffects.biteCooldown(level);
        player.getFoodData().setFoodLevel(20);
        player.heal(SkillEffects.biteHeal(level));
        // Укушенный не двигается 2 секунды и получает небольшой урон.
        // Сначала обездвиживаем: так от урона его не отбросит.
        LivingEntity target = hit.entity();
        LockManager.lock(target, now + SkillEffects.biteStun(level));
        if (Balance.BITE_DAMAGE > 0.0F) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), Balance.BITE_DAMAGE);
        }
        // У укусившего края экрана краснеют на полторы секунды
        NetworkHandler.biteFlash(player);
        Effects.sound(target, SoundEvents.GENERIC_EAT, 1.0F, 0.7F);
        Effects.blood(target, 25);
        // Укус утоляет жажду крови полностью
        data.setCraving(Balance.CRAVING_MAX);
        QuestManager.progress(player, Quest.Trigger.BITE, 1);
        // Во время ярости очки не копятся: addRage сам это проверяет
        gainRage(data, SkillEffects.biteRage(level));
        NetworkHandler.syncTo(player);
    }

    /**
     * Укусить можно только человекоподобных: игроков, жителей и торговцев, разбойников (налётчики,
     * поборники, вызыватели), ведьм, всех зомби (утопленники, кадавры, зомби-жители, зомбифицированные
     * пиглины) и пиглинов, а также всех NPC мода: гулей, агентов, торговцев, информаторов и боссов
     * (боссов укус ранит, но не обездвиживает). Животных, скелетов и остальных нельзя.
     */
    private static boolean isBitable(LivingEntity entity) {
        return entity instanceof Player
                || entity instanceof AbstractVillager
                || entity instanceof AbstractIllager
                || entity instanceof Witch
                || entity instanceof Zombie
                || entity instanceof AbstractPiglin
                || entity instanceof GhoulNpc
                || entity instanceof CcgAgentNpc
                || entity instanceof InformantNpc
                || entity instanceof GhoulBoss
                || entity instanceof CcgBoss;
    }

    /**
     * Двойной рывок: можно сделать два рывка подряд (второй в течение DASH_CHAIN_WINDOW тиков),
     * после этого откат DASH_COOLDOWN. Если второй рывок не сделан вовремя, откат начинается
     * от первого рывка (это отслеживает tick()).
     */
    private static void dash(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.dashReady) {
            cooldown(player, AbilityType.DASH, state.dashReady - now);
            return;
        }
        // Пока идёт рывок (3 тика), второй не начинаем, иначе они наложатся
        if (state.isDashing()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0, look.z);
        if (direction.lengthSqr() < 1.0E-4) {
            return;
        }
        state.dashUsed++;
        state.lastDash = now;
        if (state.dashUsed >= dashCharges(data)) {
            state.dashUsed = 0;
            state.dashReady = now + dashCooldown(data);
        }
        state.actions.add(new Actions.Dash(direction.normalize(), dashTicks(data)));
        Effects.sound(player, SoundEvents.ENDER_DRAGON_FLAP, 0.35F, 1.9F);
        Effects.particles(player, ParticleTypes.CLOUD, 6, 0.3, 0.02);
        if (data.isKaguneActive()) {
            NetworkHandler.playAnimation(player, "kagune_dash");
        }
    }

    /** Гуль и полугуль: своя ветка «Рывок». Человек: ветка «Рывок» на его вкладке. Агент CCG: базовый рывок. */
    private static int dashCharges(PlayerData data) {
        return data.getPlayerClass().hasKagune()
                ? SkillEffects.dashCharges(lvl(data, SkillTree.Branch.DASH))
                : SkillEffects.otherDashCharges(lvl(data, SkillTree.Branch.HUMAN_DASH));
    }

    private static int dashCooldown(PlayerData data) {
        return data.getPlayerClass().hasKagune()
                ? Balance.DASH_COOLDOWN
                : SkillEffects.otherDashCooldown(lvl(data, SkillTree.Branch.HUMAN_DASH));
    }

    private static int dashTicks(PlayerData data) {
        return data.getPlayerClass().hasKagune()
                ? SkillEffects.dashTicks(lvl(data, SkillTree.Branch.DASH))
                : Balance.DASH_TICKS;
    }

    /** Серия рывков не продолжена вовремя: откат от последнего рывка. */
    private static void tickDashWindow(PlayerData data, CombatState state, long now) {
        if (state.dashUsed > 0 && now - state.lastDash > Balance.DASH_CHAIN_WINDOW) {
            state.dashUsed = 0;
            state.dashReady = state.lastDash + dashCooldown(data);
        }
    }

    private static void barrage(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.barrageReady) {
            cooldown(player, AbilityType.BARRAGE, state.barrageReady - now);
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get("kagune_barrage");
        // Откат начнётся, когда серия закончится (дошла до конца или кнопку отпустили)
        int level = lvl(data, SkillTree.Branch.BARRAGE);
        state.actions.add(new Actions.Barrage(now, timing.hits(), timing.duration(),
                SkillEffects.barrageDamage(level), SkillEffects.barrageWidth(level)));
        NetworkHandler.playAnimation(player, "kagune_barrage");
    }

    private static void rage(ServerPlayer player, PlayerData data, long now) {
        if (data.isRageActive()) {
            return;
        }
        if (data.getRage() < PlayerData.RAGE_MAX) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.rage_not_ready"), true);
            return;
        }
        int duration = RageStats.durationTicks(data.getRageLevel());
        data.setRage(0);
        data.setRageActive(true, now, now + duration);
        NetworkHandler.syncTo(player);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.rage_on"), true);
        Effects.sound(player, SoundEvents.RAVAGER_ROAR, 1.0F, 1.1F);
        Effects.particles(player, Effects.RAGE, 40, 0.6, 0.05);
        QuestManager.progress(player, Quest.Trigger.RAGE, 1);
    }

    private static void attack(ServerPlayer player, PlayerData data, CombatState state, long now, boolean strong) {
        int reachLevel = lvl(data, SkillTree.Branch.REACH);
        // Пока играет анимация предыдущей атаки, новую не начать
        if (now < state.attackBusyUntil) {
            return;
        }
        if (strong) {
            if (now < state.strongReady) {
                cooldown(player, AbilityType.STRONG_ATTACK, state.strongReady - now);
                return;
            }
            AbilitySync.Timing timing = AbilitySync.get("kagune_attack_double");
            state.attackBusyUntil = now + timing.duration();
            int strongLevel = lvl(data, SkillTree.Branch.STRONG);
            state.strongReady = now + SkillEffects.strongCooldown(strongLevel);
            NetworkHandler.playAnimation(player, "kagune_attack_double");
            float damage = (float) (Balance.STRONG_DAMAGE_PER_HIT * (1.0 + SkillEffects.strongDamageBonus(strongLevel)));
            state.actions.add(new Actions.Attack(AttackType.STRONG, now, timing.hits(),
                    damage, SkillEffects.strongReach(reachLevel), Balance.STRONG_HALF_WIDTH));
        } else {
            // Обычные атаки чередуют правый выпад и левый взмах
            String animation = (state.comboIndex++ % 2 == 0) ? "kagune_attack_right_thrust" : "kagune_attack_left_sweep";
            NetworkHandler.playAnimation(player, animation);
            AbilitySync.Timing timing = AbilitySync.get(animation);
            // Следующий удар можно начать, не дожидаясь конца анимации: после того как этот уже попал
            int lastHit = timing.hits().length > 0 ? timing.hits()[timing.hits().length - 1] : 0;
            state.attackBusyUntil = now + Math.max(Balance.BASIC_COOLDOWN, lastHit + 1);
            state.actions.add(new Actions.Attack(AttackType.BASIC, now, timing.hits(),
                    Balance.BASIC_DAMAGE, SkillEffects.basicReach(reachLevel), Balance.BASIC_HALF_WIDTH));
        }
    }

    /**
     * Сильная атака человека (ПКМ дважды): удар тем, что в руке (или рукой), в 2,5 раза сильнее обычного.
     * Считается сильным ударом, поэтому пробивает блок гуля: блок ломается, гуль оглушён.
     * Урон берётся из характеристики «Урон атаки» с учётом предмета в руке, ветки «Урон оружием» тоже работают.
     */
    private static void humanStrongAttack(ServerPlayer player, CombatState state, long now) {
        if (now < state.humanStrongReady) {
            cooldown(player, AbilityType.HUMAN_STRONG_ATTACK, state.humanStrongReady - now);
            return;
        }
        state.humanStrongReady = now + Balance.HUMAN_STRONG_COOLDOWN;
        player.swing(InteractionHand.MAIN_HAND, true);

        double reach = player.isCreative() ? 5.0 : 3.0;
        Targeting.Hit hit = Targeting.raycast(player, reach, false, e -> true);
        if (hit == null || hit.entity() == null) {
            return;
        }
        LivingEntity target = hit.entity();
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * Balance.HUMAN_STRONG_MULTIPLIER);
        if (CombatContext.hit(player, target, damage, AttackType.STRONG)) {
            Vec3 push = target.position().subtract(player.position());
            target.knockback(Balance.HUMAN_STRONG_KNOCKBACK, -push.x, -push.z);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(),
                        15, 0.3, 0.3, 0.3, 0.2);
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
        }
    }

    private static void grapple(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.grappleReady) {
            cooldown(player, AbilityType.GRAPPLE_SELF, state.grappleReady - now);
            return;
        }
        double range = SkillEffects.grabRange(lvl(data, SkillTree.Branch.GRAB));
        Targeting.Hit hit = Targeting.raycast(player, range, true, e -> true);
        if (hit == null) {
            noTarget(player);
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get("kagune_grapple");
        state.grappleReady = now + Balance.GRAPPLE_COOLDOWN;
        NetworkHandler.playAnimation(player, "kagune_grapple");
        Effects.sound(player, SoundEvents.FISHING_BOBBER_THROW, 0.9F, 0.6F);
        state.actions.add(new Actions.GrappleSelf(now, hit.entity(), hit.point(),
                timing.pullStart(), timing.pullEnd(), timing.duration()));
    }

    private static void pull(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.pullReady) {
            cooldown(player, AbilityType.PULL_TARGET, state.pullReady - now);
            return;
        }
        double range = SkillEffects.grabRange(lvl(data, SkillTree.Branch.GRAB));
        Targeting.Hit hit = Targeting.raycast(player, range, false, e -> true);
        if (hit == null || hit.entity() == null) {
            noTarget(player);
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get("kagune_grab_pull");
        state.pullReady = now + Balance.PULL_COOLDOWN;
        NetworkHandler.playAnimation(player, "kagune_grab_pull");
        Effects.sound(player, SoundEvents.FISHING_BOBBER_THROW, 0.9F, 0.6F);
        state.actions.add(new Actions.PullTarget(now, hit.entity(), timing));
        QuestManager.progress(player, Quest.Trigger.PULL, 1);
    }

    // ------------------------------------------------------------------
    // Блок и ярость
    // ------------------------------------------------------------------

    private static void endBlock(ServerPlayer player, PlayerData data) {
        data.setBlocking(false);
        AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, BLOCK_SPEED_ID);
        NetworkHandler.syncTo(player);
    }

    /** Сильный удар или удар в ярости ломает блок: оглушение 2 секунды и перезарядка блока 10 секунд. */
    public static void breakBlock(ServerPlayer player, PlayerData data) {
        long now = player.level().getGameTime();
        data.combat().blockReady = now + Balance.BLOCK_BREAK_COOLDOWN;
        endBlock(player, data);
        LockManager.lock(player, now + SkillEffects.blockStun(lvl(data, SkillTree.Branch.BLOCK)));
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.block_broken"), true);
        Effects.sound(player, SoundEvents.SHIELD_BREAK, 1.0F, 0.8F);
    }

    /**
     * Скорость ходьбы в блоке: фиксированно в 1,8 раза меньше базовой, что бы ни действовало
     * (зелья, ярость). Считаем множитель от текущей скорости без нашего же модификатора.
     */
    private static void applyBlockSlow(ServerPlayer player, double divisor) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            return;
        }
        AttributeModifier ours = instance.getModifier(BLOCK_SPEED_ID);
        double current = instance.getValue();
        double without = ours == null ? current : current / (1.0 + ours.getAmount());
        if (without < 1.0E-6) {
            return;
        }
        double desired = instance.getBaseValue() / divisor;
        AttrUtil.set(player, Attributes.MOVEMENT_SPEED, BLOCK_SPEED_ID, "tg_block_slow",
                desired / without - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void endRage(ServerPlayer player, PlayerData data) {
        data.setRageActive(false, 0L, 0L);
        AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, RAGE_SPEED_ID);
        NetworkHandler.syncTo(player);
        if (player.isAlive()) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.rage_off"), true);
        }
    }

    // ------------------------------------------------------------------
    // Тик, отмена, сброс
    // ------------------------------------------------------------------

    /** Раз в тик для каждого игрока на сервере. */
    public static void tick(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        rewardLevels(player, data);
        applyClassStats(player, data, false);
        CcgRaids.tick(player, data);
        // Двойной прыжок снова доступен на земле, в воде и на лестнице
        if (player.onGround() || player.isInWater() || player.onClimbable()) {
            data.combat().doubleJumpUsed = false;
        }
        QuestManager.tick(player);
        if (!data.getPlayerClass().hasKagune()) {
            // У человека и агента CCG из способностей только рывок
            CombatState other = data.combat();
            long time = player.level().getGameTime();
            for (ActiveAction action : new ArrayList<>(other.actions)) {
                if (action.tick(player, time)) {
                    other.actions.remove(action);
                }
            }
            tickDashWindow(data, other, time);
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();

        // Голод у гулей тратится медленнее: возвращаем часть набранного истощения
        FoodData food = player.getFoodData();
        float exhaustion = food.getExhaustionLevel();
        if (exhaustion > state.lastExhaustion) {
            float gained = exhaustion - state.lastExhaustion;
            float factor = SkillEffects.hungerFactor(lvl(data, SkillTree.Branch.HUNGER));
            food.setExhaustion(exhaustion - gained * (1.0F - factor));
        }
        state.lastExhaustion = food.getExhaustionLevel();

        // Ветка «Регенерация»: понемногу лечит раз в несколько секунд
        int regenLevel = SkillEffects.level(data, SkillTree.Branch.REGEN);
        if (regenLevel > 0 && player.isAlive() && player.getHealth() < player.getMaxHealth()
                && now % Balance.SKILL_REGEN_INTERVAL == 0) {
            player.heal(SkillEffects.regenAmount(regenLevel));
        }

        // Ветка «Скорость»: постоянная прибавка к скорости ходьбы
        int speedLevel = SkillEffects.level(data, SkillTree.Branch.SPEED);
        if (speedLevel > 0) {
            AttrUtil.set(player, Attributes.MOVEMENT_SPEED, SKILL_SPEED_ID, "tg_skill_speed",
                    SkillEffects.speedBonus(speedLevel), AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, SKILL_SPEED_ID);
        }

        // «Последний рубеж»: при низком здоровье ярость копится сама
        if (lvl(data, SkillTree.Branch.LAST_STAND) > 0 && !data.isRageActive() && player.isAlive()
                && player.getHealth() < player.getMaxHealth() * Balance.LAST_STAND_HEALTH && now % 20 == 0
                && gainRage(data, Balance.LAST_STAND_RAGE)) {
            NetworkHandler.syncTo(player);
        }

        // Жажда крови: шкала «Кровь»
        tickCraving(player, data, state, now);

        // Аура ярости видна всегда, пока ярость действует: красная дымка вокруг гуля
        if (data.isRageActive() && now % 4 == 0) {
            Effects.particles(player, Effects.RAGE, 4, 0.45, 0.01);
        }

        // «Аура ярости»: во время ярости враги рядом замедлены
        int auraLevel = lvl(data, SkillTree.Branch.RAGE_AURA);
        if (auraLevel > 0 && data.isRageActive() && now % 10 == 0) {
            double r = Balance.RAGE_AURA_RADIUS;
            for (LivingEntity other : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(r), e -> e != player && e.isAlive() && e.distanceToSqr(player) <= r * r)) {
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, auraLevel - 1, false, true));
            }
        }

        // Ярость
        if (data.isRageActive()) {
            if (!player.isAlive() || now >= data.getRageEndTick()) {
                endRage(player, data);
            } else {
                AttrUtil.set(player, Attributes.MOVEMENT_SPEED, RAGE_SPEED_ID, "tg_rage_speed",
                        RageStats.speedBonus(data.getRageLevel()), AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
        }

        // Действия, которые длятся несколько тиков
        for (ActiveAction action : new ArrayList<>(state.actions)) {
            if (action.tick(player, now)) {
                state.actions.remove(action);
            }
        }

        // Барраж закончился (до конца или кнопку отпустили): запускаем откат
        boolean barraging = state.isBarraging();
        if (state.wasBarraging && !barraging) {
            state.barrageReady = now + SkillEffects.barrageCooldown(lvl(data, SkillTree.Branch.BARRAGE));
        }
        state.wasBarraging = barraging;

        // Второй рывок не сделан вовремя: серия закончилась, откат от первого рывка
        tickDashWindow(data, state, now);

        // Во время серии атакующий двигается медленнее
        if (barraging) {
            AttrUtil.set(player, Attributes.MOVEMENT_SPEED, BARRAGE_SPEED_ID, "tg_barrage_slow",
                    Balance.BARRAGE_USER_SPEED_FACTOR - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, BARRAGE_SPEED_ID);
        }

        // Блок
        if (data.isBlocking()) {
            if (!player.isAlive() || LockManager.isLocked(player) || state.isBarraging()) {
                endBlock(player, data);
            } else {
                player.setSprinting(false);
                applyBlockSlow(player, SkillEffects.blockSlowDivisor(lvl(data, SkillTree.Branch.BLOCK)));
            }
        }
    }

    /**
     * Жажда крови гуля и полугуля. Шкала «Кровь» тратится, когда гуль голоден (еды 4 и меньше),
     * а если он ещё и тяжело ранен (меньше 30% здоровья), вдвое быстрее. Пустая шкала: голодная слабость
     * и урон раз в 2 секунды, пока гуль кого-нибудь не укусит. Укус сразу заполняет шкалу,
     * в остальное время она понемногу восстанавливается сама.
     */
    private static void tickCraving(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            if (data.getCraving() != Balance.CRAVING_MAX) {
                data.setCraving(Balance.CRAVING_MAX);
                NetworkHandler.syncTo(player);
            }
            return;
        }
        boolean starving = player.getFoodData().getFoodLevel() <= Balance.CRAVING_FOOD;
        boolean wounded = player.getHealth() < player.getMaxHealth() * Balance.CRAVING_HEALTH;
        int craving = data.getCraving();
        if (starving) {
            int interval = wounded ? Balance.CRAVING_DRAIN_TICKS / 2 : Balance.CRAVING_DRAIN_TICKS;
            if (craving > 0 && now % interval == 0) {
                data.setCraving(craving - 1);
            }
        } else if (craving < Balance.CRAVING_MAX && now % 20 == 0) {
            data.setCraving(craving + Balance.CRAVING_RECOVER);
        }
        if (data.getCraving() == 0) {
            if (now % Balance.CRAVING_DAMAGE_INTERVAL == 0) {
                player.hurt(player.damageSources().starve(), Balance.CRAVING_DAMAGE);
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.craving"), true);
            }
            if (now % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            }
        }
        // Клиенту шкала отправляется раз в секунду, если изменилась
        if (now % 20 == 0 && data.getCraving() != state.syncedCraving) {
            state.syncedCraving = data.getCraving();
            NetworkHandler.syncTo(player);
        }
    }

    /**
     * Очки навыков за уровни Minecraft: 2 очка за каждый новый уровень. Считается самый высокий
     * достигнутый уровень, поэтому уровни, потерянные на зачарование или после смерти и набранные
     * заново, очков повторно не дают.
     */
    private static void rewardLevels(ServerPlayer player, PlayerData data) {
        int level = player.experienceLevel;
        if (level > data.getMaxLevelRewarded()) {
            int gained = level - data.getMaxLevelRewarded();
            data.setMaxLevelRewarded(level);
            data.addSkillPoints(gained * Balance.SKILL_POINTS_PER_LEVEL);
            NetworkHandler.syncTo(player);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.level_points",
                    gained * Balance.SKILL_POINTS_PER_LEVEL), true);
        }
    }

    /**
     * Гуль и полугуль: здоровья в 2 раза больше, чем у человека, и дополнительная регенерация.
     * У человека и агента CCG модификатор здоровья снимается.
     * fillHealth: после возрождения сразу заполнить новое здоровье.
     */
    public static void applyClassStats(ServerPlayer player, PlayerData data, boolean fillHealth) {
        boolean ghoul = data.getPlayerClass() == PlayerClass.GHOUL || data.getPlayerClass() == PlayerClass.HALF_GHOUL;
        if (ghoul) {
            AttrUtil.setPermanent(player, Attributes.MAX_HEALTH, GHOUL_HEALTH_ID, "tg_ghoul_health",
                    Balance.GHOUL_MAX_HEALTH_BONUS, AttributeModifier.Operation.ADDITION);
        } else {
            AttrUtil.remove(player, Attributes.MAX_HEALTH, GHOUL_HEALTH_ID);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        // Человек: ветки «Здоровье» и «Скорость ударов» из его дерева навыков.
        // У других классов level() вернёт 0, и модификаторы снимутся.
        double vitality = SkillEffects.humanVitality(lvl(data, SkillTree.Branch.VITALITY));
        if (vitality > 0.0) {
            AttrUtil.setPermanent(player, Attributes.MAX_HEALTH, HUMAN_HEALTH_ID, "tg_human_health",
                    vitality, AttributeModifier.Operation.ADDITION);
        } else {
            AttrUtil.remove(player, Attributes.MAX_HEALTH, HUMAN_HEALTH_ID);
        }
        double attackSpeed = SkillEffects.humanAttackSpeed(lvl(data, SkillTree.Branch.ATTACK_SPEED));
        if (attackSpeed > 0.0) {
            AttrUtil.set(player, Attributes.ATTACK_SPEED, HUMAN_ATTACK_SPEED_ID, "tg_human_attack_speed",
                    attackSpeed, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AttrUtil.remove(player, Attributes.ATTACK_SPEED, HUMAN_ATTACK_SPEED_ID);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        if (fillHealth && player.isAlive()) {
            player.setHealth(player.getMaxHealth());
        }
        if (ghoul && player.isAlive() && player.getHealth() < player.getMaxHealth()
                && player.getFoodData().getFoodLevel() > 0
                && player.level().getGameTime() % Balance.GHOUL_REGEN_INTERVAL == 0) {
            player.heal(Balance.GHOUL_REGEN_AMOUNT);
        }
    }

    /** Прерывает всё, что игрок делает сейчас (смерть, выход из игры). */
    public static void cancelAll(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        CombatState state = data.combat();
        for (ActiveAction action : new ArrayList<>(state.actions)) {
            action.cancel(player);
        }
        state.actions.clear();
        AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, BARRAGE_SPEED_ID);
        if (data.isBlocking()) {
            endBlock(player, data);
        }
    }

    /** Команда /tgrpg cooldowns reset: все откаты готовы. Ярость и класс не трогает. */
    public static void resetCooldowns(ServerPlayer player) {
        cancelAll(player);
        PlayerData data = PlayerData.get(player);
        if (data != null) {
            data.combat().reset();
        }
    }

    /** Полный сброс боевого состояния, например при смене класса командой. */
    public static void resetAll(ServerPlayer player) {
        cancelAll(player);
        LockManager.unlock(player);
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        if (data.isRageActive()) {
            data.setRageActive(false, 0L, 0L);
        }
        AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, RAGE_SPEED_ID);
        AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, SKILL_SPEED_ID);
        data.combat().reset();
    }

    public static boolean isBarraging(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            return data != null && data.combat().isBarraging();
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Сообщения
    // ------------------------------------------------------------------

    private static void cooldown(ServerPlayer player, AbilityType type, long ticksLeft) {
        String seconds = String.format(Locale.ROOT, "%.1f", ticksLeft / 20.0);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown",
                Component.translatable(type.translationKey()), seconds), true);
    }

    private static void noTarget(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.no_target"), true);
    }
}
