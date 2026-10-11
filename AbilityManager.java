package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;

/** Серверная логика способностей. Клиент только просит, всё решает сервер. */
public final class AbilityManager {
    private static final UUID BLOCK_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e10");
    private static final UUID RAGE_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e11");
    private static final UUID BARRAGE_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e12");

    private AbilityManager() {
    }

    // ------------------------------------------------------------------
    // Запросы от клиента
    // ------------------------------------------------------------------

    public static void useAbility(ServerPlayer player, AbilityType type) {
        PlayerData data = PlayerData.get(player);
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
            case DASH -> dash(player, state, now);
            case BARRAGE -> barrage(player, state, now);
            case RAGE -> rage(player, data, now);
            case BASIC_ATTACK -> attack(player, state, now, false);
            case STRONG_ATTACK -> attack(player, state, now, true);
            case GRAPPLE_SELF -> grapple(player, state, now);
            case PULL_TARGET -> pull(player, state, now);
            default -> {
            }
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
        state.biteReady = now + Balance.BITE_COOLDOWN;
        player.getFoodData().setFoodLevel(20);
        player.heal(Balance.BITE_HEAL);
        if (Balance.BITE_DAMAGE > 0.0F) {
            hit.entity().hurt(player.damageSources().playerAttack(player), Balance.BITE_DAMAGE);
        }
        // Во время ярости очки не копятся: addRage сам это проверяет
        data.addRage(Balance.BITE_RAGE);
        NetworkHandler.syncTo(player);
    }

    /** Подходящая цель для укуса: игрок или человекоподобный NPC. Позже сюда добавим своих NPC мода. */
    private static boolean isBitable(LivingEntity entity) {
        return entity instanceof Player || entity instanceof Npc || entity instanceof AbstractIllager;
    }

    private static void dash(ServerPlayer player, CombatState state, long now) {
        if (now < state.dashReady) {
            cooldown(player, AbilityType.DASH, state.dashReady - now);
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0, look.z);
        if (direction.lengthSqr() < 1.0E-4) {
            return;
        }
        state.dashReady = now + Balance.DASH_COOLDOWN;
        state.actions.add(new Actions.Dash(direction.normalize()));
        PlayerData data = PlayerData.get(player);
        if (data != null && data.isKaguneActive()) {
            NetworkHandler.playAnimation(player, "kagune_dash");
        }
    }

    private static void barrage(ServerPlayer player, CombatState state, long now) {
        if (now < state.barrageReady) {
            cooldown(player, AbilityType.BARRAGE, state.barrageReady - now);
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get("kagune_barrage");
        state.barrageReady = now + Balance.BARRAGE_COOLDOWN;
        state.actions.add(new Actions.Barrage(now, timing.hits(), timing.duration()));
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
    }

    private static void attack(ServerPlayer player, CombatState state, long now, boolean strong) {
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
            state.strongReady = now + Balance.STRONG_COOLDOWN;
            NetworkHandler.playAnimation(player, "kagune_attack_double");
            state.actions.add(new Actions.Attack(AttackType.STRONG, now, timing.hits(),
                    Balance.STRONG_DAMAGE_PER_HIT, Balance.STRONG_REACH, Balance.STRONG_HALF_WIDTH));
        } else {
            // Обычные атаки чередуют правый выпад и левый взмах
            String animation = (state.comboIndex++ % 2 == 0) ? "kagune_attack_right_thrust" : "kagune_attack_left_sweep";
            NetworkHandler.playAnimation(player, animation);
            AbilitySync.Timing timing = AbilitySync.get(animation);
            state.attackBusyUntil = now + timing.duration();
            state.actions.add(new Actions.Attack(AttackType.BASIC, now, timing.hits(),
                    Balance.BASIC_DAMAGE, Balance.BASIC_REACH, Balance.BASIC_HALF_WIDTH));
        }
    }

    private static void grapple(ServerPlayer player, CombatState state, long now) {
        if (now < state.grappleReady) {
            cooldown(player, AbilityType.GRAPPLE_SELF, state.grappleReady - now);
            return;
        }
        Targeting.Hit hit = Targeting.raycast(player, Balance.GRAPPLE_RANGE, true, e -> true);
        if (hit == null) {
            noTarget(player);
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get("kagune_grapple");
        state.grappleReady = now + Balance.GRAPPLE_COOLDOWN;
        NetworkHandler.playAnimation(player, "kagune_grapple");
        state.actions.add(new Actions.GrappleSelf(now, hit.entity(), hit.point(),
                timing.pullStart(), timing.pullEnd(), timing.duration()));
    }

    private static void pull(ServerPlayer player, CombatState state, long now) {
        if (now < state.pullReady) {
            cooldown(player, AbilityType.PULL_TARGET, state.pullReady - now);
            return;
        }
        Targeting.Hit hit = Targeting.raycast(player, Balance.PULL_RANGE, false, e -> true);
        if (hit == null || hit.entity() == null) {
            noTarget(player);
            return;
        }
        AbilitySync.Timing timing = AbilitySync.get("kagune_grab_pull");
        state.pullReady = now + Balance.PULL_COOLDOWN;
        NetworkHandler.playAnimation(player, "kagune_grab_pull");
        state.actions.add(new Actions.PullTarget(now, hit.entity(), timing));
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
        LockManager.lock(player, now + Balance.BLOCK_BREAK_STUN);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.block_broken"), true);
    }

    /**
     * Скорость ходьбы в блоке: фиксированно в 1,8 раза меньше базовой, что бы ни действовало
     * (зелья, ярость). Считаем множитель от текущей скорости без нашего же модификатора.
     */
    private static void applyBlockSlow(ServerPlayer player) {
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
        double desired = instance.getBaseValue() / Balance.BLOCK_SLOW_DIVISOR;
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
        if (data == null || !data.getPlayerClass().hasKagune()) {
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();

        // Голод у гулей тратится медленнее: возвращаем часть набранного истощения
        FoodData food = player.getFoodData();
        float exhaustion = food.getExhaustionLevel();
        if (exhaustion > state.lastExhaustion) {
            float gained = exhaustion - state.lastExhaustion;
            food.setExhaustion(exhaustion - gained * (1.0F - Balance.HUNGER_DRAIN_FACTOR));
        }
        state.lastExhaustion = food.getExhaustionLevel();

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

        // Во время серии атакующий двигается медленнее
        if (state.isBarraging()) {
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
                applyBlockSlow(player);
            }
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
