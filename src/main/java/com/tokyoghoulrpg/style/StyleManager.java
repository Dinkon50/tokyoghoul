package com.tokyoghoulrpg.style;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.entity.StyleBossMob;
import com.tokyoghoulrpg.entity.StyleMentorNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModEntities;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Стили боя на сервере: выбор и смена у наставника, прокачка веток, вызов босса стиля,
 * очки мастерства за убийства и открытие стиля победой над его боссом.
 */
public final class StyleManager {
    public static final int SELECT = 0;
    public static final int CLEAR = 1;
    public static final int UPGRADE = 2;
    public static final int CHALLENGE = 3;

    /** Наставник должен быть рядом (выбор, смена и вызов — только у него). */
    private static final double MENTOR_RANGE = 8.0;

    private StyleManager() {
    }

    /** Действие из меню стиля. mentorId — номер наставника (-1, если меню открыто не у него). */
    public static void handle(ServerPlayer player, int action, int styleIndex, int branch, int mentorId) {
        PlayerData data = PlayerData.get(player);
        FightStyle style = FightStyle.byIndex(styleIndex);
        if (data == null) {
            return;
        }
        StyleData sd = data.style();
        switch (action) {
            case SELECT -> {
                if (style == null || !nearMentor(player, mentorId)) {
                    return;
                }
                if (!sd.isUnlocked(style)) {
                    say(player, "message.tokyoghoulrpg.style.locked");
                    return;
                }
                if (sd.isActive(style)) {
                    return;
                }
                // первый стиль бесплатно, сменить уже выбранный — за очки крови
                if (sd.active() != null) {
                    if (data.getBloodPoints() < StyleBalance.SWITCH_COST) {
                        say(player, "message.tokyoghoulrpg.style.no_blood", StyleBalance.SWITCH_COST);
                        return;
                    }
                    data.addBloodPoints(-StyleBalance.SWITCH_COST);
                }
                sd.setActive(style);
                Effects.sound(player, SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0F, 0.9F);
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.style.selected",
                        Component.translatable(style.key("name"))), true);
            }
            case CLEAR -> {
                if (sd.active() == null || !nearMentor(player, mentorId)) {
                    return;
                }
                sd.setActive(null);
                say(player, "message.tokyoghoulrpg.style.cleared");
            }
            case UPGRADE -> {
                if (style == null || branch < 0 || branch > 2 || !sd.isUnlocked(style)) {
                    return;
                }
                int level = sd.level(style, branch);
                if (level >= 3) {
                    return;
                }
                int cost = StyleBalance.COSTS[level];
                if (sd.mastery() < cost) {
                    say(player, "message.tokyoghoulrpg.style.no_points", cost);
                    return;
                }
                sd.addMastery(-cost);
                sd.setLevel(style, branch, level + 1);
                if (sd.level(style, 0) >= 3 && sd.level(style, 1) >= 3 && sd.level(style, 2) >= 3) {
                    com.tokyoghoulrpg.rep.ModAdvancements.grant(player, "style_mastered");
                    com.tokyoghoulrpg.rep.Achievements.atLeast(player, "style_mastered", 1);
                }
                Effects.sound(player, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
            }
            case CHALLENGE -> challenge(player, data, style, mentorId);
            default -> {
                return;
            }
        }
        NetworkHandler.syncTo(player);
    }

    private static boolean nearMentor(ServerPlayer player, int mentorId) {
        Entity entity = player.level().getEntity(mentorId);
        boolean near = entity instanceof StyleMentorNpc && entity.isAlive() && entity.distanceTo(player) <= MENTOR_RANGE;
        if (!near) {
            say(player, "message.tokyoghoulrpg.style.need_mentor");
        }
        return near;
    }

    /** Вызов босса стиля: появляется в зале перед игроком. Раз в игровые сутки (общий откат на все стили), один босс на додзё. */
    private static void challenge(ServerPlayer player, PlayerData data, FightStyle style, int mentorId) {
        if (style == null || !style.ready() || !nearMentor(player, mentorId)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        StyleData sd = data.style();
        long now = level.getGameTime();
        if (now < sd.challengeReady() && !player.isCreative()) {
            long minutes = Math.max(1L, (sd.challengeReady() - now) / 1200L);
            say(player, "message.tokyoghoulrpg.style.challenge_wait", minutes);
            return;
        }
        Entity mentor = level.getEntity(mentorId);
        List<StyleBossMob> already = level.getEntitiesOfClass(StyleBossMob.class,
                new AABB(mentor.blockPosition()).inflate(StyleBalance.CHALLENGE_RADIUS), StyleBossMob::isAlive);
        if (!already.isEmpty()) {
            say(player, "message.tokyoghoulrpg.style.challenge_busy");
            return;
        }
        StyleBossMob boss = bossType(style).create(level);
        if (boss == null) {
            return;
        }
        // в зал: от наставника через игрока, на 5 блоков дальше игрока; если там тесно — рядом с игроком
        Vec3 away = player.position().subtract(mentor.position());
        Vec3 dir = new Vec3(away.x, 0.0, away.z);
        dir = dir.lengthSqr() < 1.0E-3 ? player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : dir.normalize();
        Vec3 spot = null;
        for (double d : new double[] {5.0, 4.0, 3.0, 6.0}) {
            Vec3 p = player.position().add(dir.scale(d));
            boss.moveTo(p.x, player.getY(), p.z, 0.0F, 0.0F);
            if (level.noCollision(boss)) {
                spot = p;
                break;
            }
        }
        if (spot == null) {
            spot = player.position();
            boss.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
        }
        boss.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null, null);
        boss.setTarget(player);
        level.addFreshEntity(boss);
        sd.setChallengeReady(now + StyleBalance.CHALLENGE_COOLDOWN);
        Effects.sound(boss, SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, spot.x, spot.y + 0.2, spot.z, 30,
                0.6, 0.1, 0.6, 0.05);
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.style.challenge_start",
                Component.translatable(style.key("boss"))));
    }

    /** Чей босс приходит на вызов. */
    private static net.minecraft.world.entity.EntityType<? extends StyleBossMob> bossType(FightStyle style) {
        return switch (style) {
            case FIGHTER -> ModEntities.DOJO_MASTER.get();
            case SHADOW -> ModEntities.CLOWN.get();
            case REAPER -> ModEntities.WHITE_REAPER.get();
        };
    }

    // ------------------------------------------------------------------ очки мастерства и открытие стиля

    /** Кто-то убит игроком: очки мастерства (если открыт хоть один стиль). */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        PlayerData data = PlayerData.get(killer);
        if (data == null || !data.style().anyUnlocked()) {
            return;
        }
        StyleData sd = data.style();
        if (victim instanceof BossMob) {
            sd.addMastery(StyleBalance.BOSS_POINTS);
            killer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.style.mastery_boss",
                    StyleBalance.BOSS_POINTS));
            NetworkHandler.syncTo(killer);
            return;
        }
        if (victim instanceof Enemy && victim instanceof Mob && !(victim instanceof com.tokyoghoulrpg.entity.ClownDouble)
                && sd.countKill() > 0) {
            Effects.sound(killer, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 0.6F);
            killer.displayClientMessage(Component.translatable("message.tokyoghoulrpg.style.mastery_point"), true);
            NetworkHandler.syncTo(killer);
        }
    }

    /** Босс стиля повержен: стиль открывается всем игрокам рядом (бились вместе — открывается всем). */
    public static void onStyleBossDefeated(LivingEntity boss, FightStyle style) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        double r = StyleBalance.CHALLENGE_RADIUS;
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(boss) <= r * r && !p.isSpectator())) {
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                continue;
            }
            StyleData sd = data.style();
            boolean fresh = !sd.isUnlocked(style);
            sd.unlock(style);
            if (!sd.hasWon(style)) {
                sd.markWin(style);
                sd.addMastery(StyleBalance.FIRST_WIN_POINTS);
            }
            if (sd.active() == null) {
                sd.setActive(style);           // первый стиль надевается сразу
            }
            int unlocked = 0;
            for (FightStyle each : FightStyle.values()) {
                unlocked += sd.isUnlocked(each) ? 1 : 0;
            }
            com.tokyoghoulrpg.rep.ModAdvancements.grant(player, "style_" + style.id());
            if (unlocked >= FightStyle.values().length) {
                com.tokyoghoulrpg.rep.ModAdvancements.grant(player, "style_all");
            }
            com.tokyoghoulrpg.rep.Achievements.atLeast(player, "styles_unlocked", unlocked);
            Component name = Component.translatable(style.key("name"));
            player.sendSystemMessage(Component.translatable(fresh ? "message.tokyoghoulrpg.style.unlocked"
                    : "message.tokyoghoulrpg.style.won_again", name, StyleBalance.FIRST_WIN_POINTS));
            Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
            NetworkHandler.syncTo(player);
        }
    }

    private static void say(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }
}
