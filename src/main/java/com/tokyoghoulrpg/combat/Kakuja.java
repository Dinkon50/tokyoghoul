package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import com.tokyoghoulrpg.entity.HalfGhoulNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Какудзя — форма гуля и полугуля, который поедал других гулей.
 * <ul>
 *   <li>Очки: укус гуля +3, убил гуля или полугуля (NPC) +2, игрока-гуля +8, Пожирателя +15,
 *       Кровавого полугуля +30 (всем участникам). На 100 очках форма открывается навсегда.</li>
 *   <li>Клавиша U: 30 секунд урон +40%, входящий урон -30%, скорость +20%. Откат 5 минут.</li>
 *   <li>Вокруг — тёмно-багровая аура: кольцо у ног, спирали вверх, искры. Её видят все.</li>
 * </ul>
 */
public final class Kakuja {
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e80");
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.08F), 1.6F);
    private static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.08F, 0.02F, 0.03F), 1.4F);
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.25F), 0.9F);

    private Kakuja() {
    }

    private static boolean eligible(PlayerData data) {
        return data != null && data.getPlayerClass().hasKagune();
    }

    public static boolean unlocked(PlayerData data) {
        return eligible(data) && data.getKakujaPoints() >= Balance.KAKUJA_UNLOCK;
    }

    /** Добавить очки Какудзя (только гулям и полугулям). На 100 — сообщение, что форма открыта. */
    public static void gain(ServerPlayer player, int points) {
        PlayerData data = PlayerData.get(player);
        if (!eligible(data) || points <= 0) {
            return;
        }
        boolean before = unlocked(data);
        data.setKakujaPoints(data.getKakujaPoints() + points);
        if (!before && unlocked(data)) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.kakuja.unlocked"));
            Effects.sound(player, SoundEvents.WITHER_SPAWN, 0.6F, 1.4F);
            Effects.particles(player, CRIMSON, 60, 0.8, 0.1);
        } else if (!before) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.kakuja.progress",
                    Math.min(data.getKakujaPoints(), Balance.KAKUJA_UNLOCK), Balance.KAKUJA_UNLOCK), true);
        }
        NetworkHandler.syncTo(player);
    }

    /** Укусил гуля. */
    public static void onBite(ServerPlayer player, LivingEntity target) {
        if (RcSuppress.isGhoul(target)) {
            gain(player, Balance.KAKUJA_BITE);
        }
    }

    /** Убил гуля (Кровавого полугуля считает BossRewards — всем участникам). */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof GhoulBoss) {
            gain(killer, Balance.KAKUJA_KILL_BOSS);
        } else if (victim instanceof GhoulNpc || victim instanceof HalfGhoulNpc) {
            gain(killer, Balance.KAKUJA_KILL_NPC);
        } else if (victim instanceof Player && RcSuppress.isGhoul(victim) && !(victim instanceof HalfGhoulBoss)) {
            gain(killer, Balance.KAKUJA_KILL_PLAYER);
        }
    }

    /** Клавиша U. */
    public static void use(ServerPlayer player, PlayerData data) {
        if (!eligible(data)) {
            return;
        }
        if (!unlocked(data)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.kakuja.locked",
                    data.getKakujaPoints(), Balance.KAKUJA_UNLOCK), true);
            return;
        }
        long now = player.level().getGameTime();
        if (data.isKakujaActive(now)) {
            return;
        }
        CombatState state = data.combat();
        if (now < state.kakujaReady) {
            String seconds = String.format(Locale.ROOT, "%.1f", (state.kakujaReady - now) / 20.0);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown",
                    Component.translatable(AbilityType.KAKUJA.translationKey()), seconds), true);
            return;
        }
        data.setKakujaEndTick(now + Balance.KAKUJA_TICKS);
        AttrUtil.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "tg_kakuja_speed", Balance.KAKUJA_SPEED,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        Effects.sound(player, SoundEvents.WARDEN_ROAR, 0.9F, 1.3F);
        Effects.sound(player, SoundEvents.RAVAGER_ROAR, 0.8F, 0.6F);
        if (player.level() instanceof ServerLevel level) {
            // вспышка при превращении: кольцо и столб
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                level.sendParticles(CRIMSON, player.getX() + Math.cos(a) * 1.6, player.getY() + 0.1,
                        player.getZ() + Math.sin(a) * 1.6, 2, 0.05, 0.05, 0.05, 0.0);
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.4, 0.8, 0.4, 0.05);
            level.sendParticles(BLACK, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 1.0, 0.5, 0.05);
        }
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.kakuja.active",
                Balance.KAKUJA_TICKS / 20), true);
        NetworkHandler.syncTo(player);
    }

    /** Каждый тик: аура, конец формы. */
    public static void tick(ServerPlayer player, PlayerData data, long now) {
        if (data.getKakujaEndTick() <= 0L) {
            return;
        }
        if (!data.isKakujaActive(now) || !eligible(data) || !player.isAlive()) {
            end(player, data, now);
            return;
        }
        if (now % 2 == 0 && player.level() instanceof ServerLevel level) {
            double t = now * 0.25;
            // кольцо у ног вращается
            for (int i = 0; i < 3; i++) {
                double a = t + i * Math.PI * 2 / 3;
                level.sendParticles(CRIMSON, player.getX() + Math.cos(a) * 0.9, player.getY() + 0.1,
                        player.getZ() + Math.sin(a) * 0.9, 1, 0.0, 0.0, 0.0, 0.0);
            }
            // две спирали поднимаются вокруг тела
            double h = (now % 30) / 30.0 * 2.2;
            for (int i = 0; i < 2; i++) {
                double a = t * 1.6 + i * Math.PI;
                level.sendParticles(i == 0 ? GLOW : BLACK, player.getX() + Math.cos(a) * 0.6, player.getY() + h,
                        player.getZ() + Math.sin(a) * 0.6, 1, 0.0, 0.0, 0.0, 0.0);
            }
            if (now % 6 == 0) {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY() + 1.2, player.getZ(),
                        4, 0.4, 0.6, 0.4, 0.0);
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.6, player.getZ(),
                        2, 0.25, 0.2, 0.25, 0.01);
            }
        }
    }

    /** Форма закончилась (время, смерть, смена класса): скорость обратно, откат. */
    public static void end(ServerPlayer player, PlayerData data, long now) {
        if (data.getKakujaEndTick() <= 0L) {
            return;
        }
        data.setKakujaEndTick(0L);
        data.combat().kakujaReady = Math.max(data.combat().kakujaReady, now + Balance.KAKUJA_COOLDOWN);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
        }
        NetworkHandler.syncTo(player);
    }

    /** Множитель урона атакующего. */
    public static double damageFactor(PlayerData data, long now) {
        return data != null && data.isKakujaActive(now) ? 1.0 + Balance.KAKUJA_DAMAGE : 1.0;
    }

    /** Множитель урона по игроку в форме. */
    public static double defenseFactor(PlayerData data, long now) {
        return data != null && data.isKakujaActive(now) ? 1.0 - Balance.KAKUJA_DEFENSE : 1.0;
    }
}
