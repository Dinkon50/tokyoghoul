package com.tokyoghoulrpg.event;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.AttackType;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.BleedManager;
import com.tokyoghoulrpg.combat.CombatContext;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.LockManager;
import com.tokyoghoulrpg.combat.RageStats;
import com.tokyoghoulrpg.contract.ContractManager;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Боевые события: тики способностей, блок, бонусы и защита ярости, очки ярости за убийства. */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class CombatEvents {
    /** Идёт урон, прошедший сквозь блок: блок его не останавливает. */
    private static boolean piercing = false;

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
            BleedManager.tick();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LockManager.clear();
        BleedManager.clear();
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
        if (piercing) {
            // Это часть урона, которая проходит сквозь блок (ветка «Пробивание»)
            return;
        }
        boolean breaks = CombatContext.type() == AttackType.STRONG || isRaging(source.getEntity());
        if (breaks) {
            AbilityManager.breakBlock(blocker, data);
            return;
        }
        event.setCanceled(true);
        if (blocker.tickCount % 3 == 0) {
            Effects.sound(blocker, SoundEvents.SHIELD_BLOCK, 0.8F, 0.9F + blocker.getRandom().nextFloat() * 0.2F);
        }
        // Ветка «Пробивание»: часть урона обычного удара кагуне всё равно проходит
        if (CombatContext.type() == AttackType.BASIC && source.getEntity() instanceof ServerPlayer attacker) {
            double pierce = SkillEffects.pierce(SkillEffects.level(PlayerData.get(attacker), SkillTree.Branch.PIERCE));
            float through = (float) (event.getAmount() * pierce);
            if (through > 0.0F) {
                piercing = true;
                try {
                    blocker.invulnerableTime = 0;
                    blocker.hurt(source, through);
                } finally {
                    piercing = false;
                }
            }
        }
    }

    /**
     * Ярость и ветки «Урон» и «Защита» из дерева навыков усиливают наносимый урон и уменьшают получаемый.
     * Встречный барраж режет урон. Все множители перемножаются.
     */
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
            amount *= (float) (1.0 + SkillEffects.damageBonus(SkillEffects.level(data, SkillTree.Branch.DAMAGE)));
            // Человек: ветка «Урон оружием». Работает и для ближнего боя, и для стрел и трезубцев:
            // у выпущенного снаряда источник урона — сам игрок.
            if (isWeaponHit(event.getSource(), attackingPlayer)) {
                amount *= (float) (1.0 + SkillEffects.humanWeaponDamage(
                        SkillEffects.level(data, SkillTree.Branch.WEAPON_DAMAGE)));
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
            amount *= (float) (1.0 - SkillEffects.damageReduction(SkillEffects.level(data, SkillTree.Branch.DEFENSE)));
            // Человек: ветка «Стойкость»
            amount *= (float) (1.0 - SkillEffects.humanToughness(SkillEffects.level(data, SkillTree.Branch.TOUGHNESS)));
        }
        event.setAmount(amount);

        // Ветка «Вампиризм»: часть нанесённого урона возвращается здоровьем
        if (attacker instanceof ServerPlayer vampire && attacker != victim) {
            double share = SkillEffects.vampirism(SkillEffects.level(PlayerData.get(vampire), SkillTree.Branch.VAMPIRISM));
            if (share > 0.0 && vampire.isAlive()) {
                vampire.heal((float) (amount * share));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // Ветка «Второе дыхание»: смертельный удар оставляет 1 сердце, раз в 5 минут.
        // Не спасает от урона, который игнорирует всё (/kill, падение в пустоту).
        if (event.getEntity() instanceof ServerPlayer dying
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            PlayerData data = PlayerData.get(dying);
            long now = dying.level().getGameTime();
            if (data != null && SkillEffects.level(data, SkillTree.Branch.SECOND_WIND) > 0
                    && now >= data.combat().secondWindReady) {
                event.setCanceled(true);
                data.combat().secondWindReady = now + Balance.SECOND_WIND_COOLDOWN;
                dying.setHealth(Balance.SECOND_WIND_HEALTH);
                dying.displayClientMessage(Component.translatable("message.tokyoghoulrpg.second_wind"), true);
                return;
            }
        }
        Entity killer = event.getSource().getEntity();
        if (killer instanceof ServerPlayer killerPlayer && killerPlayer != event.getEntity()) {
            PlayerData data = PlayerData.get(killerPlayer);
            if (data != null && data.getPlayerClass().hasKagune()) {
                if (AbilityManager.gainRage(data, Balance.RAGE_PER_KILL)) {
                    NetworkHandler.syncTo(killerPlayer);
                }
            }
            // Задания: монстр — любое враждебное существо (зомби, скелет, крипер, паук и так далее)
            if (event.getEntity() instanceof Enemy) {
                QuestManager.progress(killerPlayer, Quest.Trigger.KILL_MONSTER, 1);
                // Убит ударом кагуне: смертельный удар пришёл из способности
                if (CombatContext.type() != null) {
                    QuestManager.progress(killerPlayer, Quest.Trigger.KILL_MONSTER_KAGUNE, 1);
                }
            }
            if (data != null) {
                rewardKill(killerPlayer, data, event.getEntity());
            }
            ContractManager.onKill(killerPlayer, event.getEntity());
        }
        if (event.getEntity() instanceof ServerPlayer dead) {
            AbilityManager.cancelAll(dead);
            LockManager.unlock(dead);
        }
    }

    /**
     * Очки крови за убийства. Гуль или агент CCG — немного крови, босс — много крови и очки навыков.
     * Животные, торговцы и информаторы ничего не дают.
     */
    private static void rewardKill(ServerPlayer killer, PlayerData data, LivingEntity victim) {
        if (victim instanceof BossMob) {
            data.addBloodPoints(Balance.BLOOD_KILL_BOSS);
            data.addSkillPoints(Balance.BOSS_SKILL_POINTS);
            killer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.boss_reward",
                    victim.getDisplayName(), Balance.BLOOD_KILL_BOSS, Balance.BOSS_SKILL_POINTS));
            Effects.sound(killer, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
            NetworkHandler.syncTo(killer);
            QuestManager.progress(killer, Quest.Trigger.KILL_BOSS, 1);
        } else if (victim instanceof GhoulNpc || victim instanceof CcgAgentNpc) {
            data.addBloodPoints(Balance.BLOOD_KILL_NPC);
            killer.displayClientMessage(Component.translatable("message.tokyoghoulrpg.kill_reward",
                    Balance.BLOOD_KILL_NPC), true);
            NetworkHandler.syncTo(killer);
        }
    }

    /**
     * Человек: ветка «Копание» ускоряет добычу любых блоков любым предметом (и рукой).
     * Срабатывает и у клиента, и у сервера: уровень навыков есть у обоих, поэтому блок ломается
     * одинаково быстро с обеих сторон.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        double bonus = SkillEffects.humanMining(SkillEffects.level(PlayerData.get(event.getEntity()), SkillTree.Branch.MINING));
        if (bonus > 0.0) {
            event.setNewSpeed((float) (event.getNewSpeed() * (1.0 + bonus)));
        }
    }

    /** Задания человека: сломанные блоки и добытая руда. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !player.isCreative()) {
            QuestManager.progress(player, Quest.Trigger.BREAK_BLOCK, 1);
            if (event.getState().is(Tags.Blocks.ORES)) {
                QuestManager.progress(player, Quest.Trigger.MINE_ORE, 1);
            }
        }
    }

    /** Удар оружием: прямой удар игрока или его снаряд (стрела, трезубец), а не шипы, огонь и прочее. */
    private static boolean isWeaponHit(DamageSource source, ServerPlayer player) {
        return source.is(DamageTypes.PLAYER_ATTACK)
                || (source.isIndirect() && source.getEntity() == player && source.getDirectEntity() instanceof Projectile);
    }

    private static boolean isRaging(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerData data = PlayerData.get(player);
            return data != null && data.isRageActive();
        }
        return false;
    }
}
