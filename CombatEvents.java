package com.tokyoghoulrpg.event;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.AttackType;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.CombatContext;
import com.tokyoghoulrpg.combat.LockManager;
import com.tokyoghoulrpg.combat.RageStats;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Боевые события: тики способностей, блок, бонусы и защита ярости, очки ярости за убийства. */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class CombatEvents {
    private CombatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && !event.player.level().isClientSide()
                && event.player instanceof ServerPlayer player) {
            AbilityManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LockManager.tick();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LockManager.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityManager.cancelAll(player);
            LockManager.unlock(player);
        }
    }

    /** Оглушённый игрок не может бить обычной атакой. */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && LockManager.isLocked(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Блок. Обычные удары и барраж гасятся полностью, сильный удар и любой удар в ярости пробивают блок:
     * блок ломается, игрок оглушается, а урон проходит.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(victim instanceof ServerPlayer blocker)) {
            return;
        }
        PlayerData data = PlayerData.get(blocker);
        if (data == null || !data.isBlocking()) {
            return;
        }
        DamageSource source = event.getSource();
        // Падение, огонь и прочий урон окружения блок не останавливает
        if (source.getEntity() == null) {
            return;
        }
        boolean breaks = CombatContext.type() == AttackType.STRONG || isRaging(source.getEntity());
        if (breaks) {
            AbilityManager.breakBlock(blocker, data);
        } else {
            event.setCanceled(true);
        }
    }

    /** Ярость усиливает наносимый урон и уменьшает получаемый. Встречный барраж режет урон. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        float amount = event.getAmount();
        Entity attacker = event.getSource().getEntity();

        if (attacker instanceof ServerPlayer attackingPlayer) {
            PlayerData data = PlayerData.get(attackingPlayer);
            if (data != null && data.isRageActive()) {
                amount *= (float) (1.0 + RageStats.damageBonus(data.getRageLevel()));
            }
        }

        if (CombatContext.type() == AttackType.BARRAGE && AbilityManager.isBarraging(victim)) {
            amount *= Balance.COUNTER_BARRAGE_FACTOR;
        }

        if (victim instanceof ServerPlayer defendingPlayer) {
            PlayerData data = PlayerData.get(defendingPlayer);
            if (data != null && data.isRageActive()) {
                amount *= (float) (1.0 - RageStats.damageReduction(data.getRageLevel()));
            }
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (killer instanceof ServerPlayer killerPlayer && killerPlayer != event.getEntity()) {
            PlayerData data = PlayerData.get(killerPlayer);
            if (data != null && data.getPlayerClass().hasKagune() && data.addRage(Balance.RAGE_PER_KILL)) {
                NetworkHandler.syncTo(killerPlayer);
            }
        }
        if (event.getEntity() instanceof ServerPlayer dead) {
            AbilityManager.cancelAll(dead);
            LockManager.unlock(dead);
        }
    }

    private static boolean isRaging(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            return data != null && data.isRageActive();
        }
        return false;
    }
}
