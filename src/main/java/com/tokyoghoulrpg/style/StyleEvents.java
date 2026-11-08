package com.tokyoghoulrpg.style;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.StyleBossMob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * События стилей боя: усиление ударов, стойка «Контратаки», окно ответного удара, дымовая завеса,
 * метка смерти, «Жатва», очки мастерства и открытие стиля победой над его боссом.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class StyleEvents {
    private StyleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.style().active() != null) {
                FighterStyle.tick(player, data);
                ShadowStyle.tick(player, data);
            }
        }
    }

    /** Стойка «Контратаки» ловит удар раньше блока и остальных защит. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && FighterStyle.onAttacked(player, data, event.getSource())) {
                event.setCanceled(true);
            }
        }
    }

    /** Удар, остановленный блоком (обычным или идеальным): у бойца открывается ответный удар. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBlockedCheck(LivingAttackEvent event) {
        if (event.isCanceled() && event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() != null && event.getSource().getEntity() != player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && data.isBlocking()) {
                FighterStyle.onBlocked(player, data);
            }
        }
    }

    /**
     * Метка смерти видна и с шейдерами (Oculus, Complementary), где обводка «Свечения» иногда не рисуется:
     * над меченым раз в полсекунды вьются души.
     */
    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 10 == 0 && !entity.level().isClientSide()
                && entity.level() instanceof net.minecraft.server.level.ServerLevel level && DeathMark.isMarked(entity)) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, entity.getX(),
                    entity.getY() + entity.getBbHeight() + 0.35, entity.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
        }
    }

    /** В дымовой завесе враги не замечают игрока, пока не подойдут вплотную. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            if (data != null && ShadowStyle.hidden(player, data, event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        float factor = DeathMark.factor(victim);
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            PlayerData data = PlayerData.get(attacker);
            factor *= FighterStyle.damageFactor(attacker, data, event.getSource());
            factor *= ShadowStyle.damageFactor(attacker, data, victim, event.getSource());
            factor *= ReaperStyle.damageFactor(attacker, data, victim, event.getSource());
        }
        if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || event.isCanceled()) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != event.getEntity()) {
            StyleManager.onKill(killer, event.getEntity());
            PlayerData data = PlayerData.get(killer);
            if (data != null) {
                ReaperStyle.onKill(killer, data);
            }
        }
        if (event.getEntity() instanceof StyleBossMob boss) {
            StyleManager.onStyleBossDefeated(boss, boss.style());
        }
    }
}
