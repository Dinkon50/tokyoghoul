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
 *   <li>Стадии (25 / 50 / 100 / 200 / 350 очков): каждая навсегда даёт немного урона и защиты,
 *       а на IV и V сама форма сильнее и дольше. Усиление видно всем: красные искры, светящиеся
 *       глаза гуля, на высоких стадиях — капли крови, споры и чёрные осколки вокруг. Новая стадия —
 *       вспышка, рёв и надпись на экране; о стадии V узнаёт весь сервер.</li>
 * </ul>
 */
public final class Kakuja {
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e80");
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.08F), 1.6F);
    private static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.08F, 0.02F, 0.03F), 1.4F);
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.25F), 0.9F);
    /** Светящиеся глаза гуля: маленькие ярко-красные точки. */
    private static final DustParticleOptions EYE = new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.1F), 0.35F);

    private Kakuja() {
    }

    private static boolean eligible(PlayerData data) {
        return data != null && data.getPlayerClass().hasKagune();
    }

    public static boolean unlocked(PlayerData data) {
        return eligible(data) && data.getKakujaPoints() >= Balance.KAKUJA_UNLOCK;
    }

    /** Стадия по очкам: 0 — ещё нет, 1-5. Работает и на клиенте. */
    public static int stage(int points) {
        int stage = 0;
        for (int threshold : Balance.KAKUJA_STAGES) {
            if (points >= threshold) {
                stage++;
            }
        }
        return stage;
    }

    public static int stage(PlayerData data) {
        return eligible(data) ? stage(data.getKakujaPoints()) : 0;
    }

    /** Сколько очков нужно до следующей стадии (-1 — стадия V, дальше некуда). */
    public static int nextThreshold(int stage) {
        return stage < Balance.KAKUJA_STAGES.length ? Balance.KAKUJA_STAGES[stage] : -1;
    }

    /** Насколько форма сильнее за стадии выше III. */
    private static double formStageBonus(PlayerData data) {
        return Math.max(0, stage(data) - 3) * Balance.KAKUJA_FORM_STAGE_BONUS;
    }

    /** Сколько длится форма с учётом стадии. */
    public static int formTicks(PlayerData data) {
        return Balance.KAKUJA_TICKS + Math.max(0, stage(data) - 3) * Balance.KAKUJA_FORM_STAGE_TICKS;
    }

    /** Добавить очки Какудзя (только гулям и полугулям). На 100 — сообщение, что форма открыта. */
    public static void gain(ServerPlayer player, int points) {
        PlayerData data = PlayerData.get(player);
        if (!eligible(data) || points <= 0) {
            return;
        }
        boolean before = unlocked(data);
        int stageBefore = stage(data);
        data.setKakujaPoints(data.getKakujaPoints() + points);
        int stageNow = stage(data);
        if (stageNow > stageBefore) {
            stageUp(player, data, stageNow);
        }
        if (!before && unlocked(data)) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.kakuja.unlocked"));
        } else if (stageNow == stageBefore) {
            int next = nextThreshold(stageNow);
            player.displayClientMessage(next < 0
                    ? Component.translatable("message.tokyoghoulrpg.kakuja.progress_max", data.getKakujaPoints())
                    : Component.translatable("message.tokyoghoulrpg.kakuja.progress_stage",
                    data.getKakujaPoints(), next, com.tokyoghoulrpg.skill.SkillTree.roman(stageNow + 1)), true);
        }
        NetworkHandler.syncTo(player);
    }

    /** Новая стадия: вспышка, рёв, надпись крупно на экране; о стадии V — всему серверу. */
    private static void stageUp(ServerPlayer player, PlayerData data, int stage) {
        String roman = com.tokyoghoulrpg.skill.SkillTree.roman(stage);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(5, 50, 15));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.translatable("message.tokyoghoulrpg.kakuja.stage_title", roman)
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED)));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.translatable("message.tokyoghoulrpg.kakuja.stage_subtitle",
                        Math.round(stage * Balance.KAKUJA_STAGE_DAMAGE * 100),
                        Math.round(stage * Balance.KAKUJA_STAGE_DEFENSE * 100))
                        .withStyle(net.minecraft.ChatFormatting.RED)));
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.kakuja.stage_" + stage));
        Effects.sound(player, stage >= 4 ? SoundEvents.WARDEN_ROAR : SoundEvents.RAVAGER_ROAR, 0.8F, 1.2F);
        Effects.sound(player, SoundEvents.WITHER_SPAWN, 0.4F, 1.6F);
        if (player.level() instanceof ServerLevel level) {
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2 / 40;
                level.sendParticles(CRIMSON, player.getX() + Math.cos(a) * 1.4, player.getY() + 1.0,
                        player.getZ() + Math.sin(a) * 1.4, 1, 0.0, 0.3, 0.0, 0.0);
            }
            level.sendParticles(BLACK, player.getX(), player.getY() + 1.0, player.getZ(), 30 + stage * 10, 0.4, 0.8, 0.4, 0.08);
            level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.2, player.getZ(),
                    6 + stage * 3, 0.3, 0.6, 0.3, 0.04);
        }
        if (stage >= Balance.KAKUJA_STAGES.length && player.getServer() != null) {
            Component news = Component.translatable("message.tokyoghoulrpg.kakuja.stage_broadcast",
                    player.getDisplayName()).withStyle(net.minecraft.ChatFormatting.DARK_RED);
            for (ServerPlayer other : player.getServer().getPlayerList().getPlayers()) {
                if (other != player) {
                    other.sendSystemMessage(news);
                }
            }
        }
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
        data.setKakujaEndTick(now + formTicks(data));
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
        double extra = formStageBonus(data);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.kakuja.active",
                Math.round((Balance.KAKUJA_DAMAGE + extra) * 100), Math.round((Balance.KAKUJA_DEFENSE + extra) * 100),
                Math.round(Balance.KAKUJA_SPEED * 100), formTicks(data) / 20), true);
        NetworkHandler.syncTo(player);
    }

    /** Каждый тик: видимое усиление по стадии, аура формы, конец формы. */
    public static void tick(ServerPlayer player, PlayerData data, long now) {
        if (data.getKakujaEndTick() <= 0L) {
            passiveAura(player, data, now);
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

    /**
     * Постоянные признаки стадии (видят все игроки рядом), даже без формы:
     * I — редкие красные искры у спины; II — ещё светятся глаза гуля; III — капли крови с рук;
     * IV — багровые споры вокруг; V — чёрные осколки кружат вокруг тела.
     */
    private static void passiveAura(ServerPlayer player, PlayerData data, long now) {
        int stage = stage(data);
        if (stage <= 0 || !player.isAlive() || player.isInvisible() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        // I: искры у спины, тем чаще, чем выше стадия
        int sparkEvery = Math.max(6, 40 - stage * 7);
        if (now % sparkEvery == 0) {
            level.sendParticles(GLOW, x - fx * 0.35, y + 1.3, z - fz * 0.35, 1 + stage / 2, 0.2, 0.25, 0.2, 0.0);
        }
        // II: светящиеся глаза (две точки перед лицом)
        if (stage >= 2 && now % 4 == 0) {
            double ex = player.getX() + fx * 0.32;
            double ez = player.getZ() + fz * 0.32;
            double eyeY = player.getEyeY() + 0.02;
            double sx = -fz * 0.12;
            double sz = fx * 0.12;
            level.sendParticles(EYE, ex + sx, eyeY, ez + sz, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(EYE, ex - sx, eyeY, ez - sz, 1, 0.0, 0.0, 0.0, 0.0);
        }
        // III: капли крови с рук
        if (stage >= 3 && now % 12 == 0) {
            double side = (now / 12) % 2 == 0 ? 1.0 : -1.0;
            level.sendParticles(Effects.BLOOD, x - fz * 0.4 * side, y + 0.7, z + fx * 0.4 * side,
                    2, 0.03, 0.08, 0.03, 0.0);
        }
        // IV: багровые споры вокруг
        if (stage >= 4 && now % 8 == 0) {
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, x, y + 1.0, z, 3, 0.5, 0.6, 0.5, 0.0);
        }
        // V: чёрные осколки кружат вокруг тела
        if (stage >= 5 && now % 3 == 0) {
            double a = now * 0.18;
            for (int i = 0; i < 2; i++) {
                double b = a + i * Math.PI;
                level.sendParticles(BLACK, x + Math.cos(b) * 0.8, y + 0.9 + 0.4 * Math.sin(a * 0.7 + i), z + Math.sin(b) * 0.8,
                        1, 0.0, 0.0, 0.0, 0.0);
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

    /** Множитель урона атакующего: стадия всегда, форма — когда включена. */
    public static double damageFactor(PlayerData data, long now) {
        if (!eligible(data)) {
            return 1.0;
        }
        double factor = 1.0 + stage(data) * Balance.KAKUJA_STAGE_DAMAGE;
        if (data.isKakujaActive(now)) {
            factor *= 1.0 + Balance.KAKUJA_DAMAGE + formStageBonus(data);
        }
        return factor;
    }

    /** Множитель урона по игроку: стадия всегда, форма — когда включена. */
    public static double defenseFactor(PlayerData data, long now) {
        if (!eligible(data)) {
            return 1.0;
        }
        double factor = 1.0 - stage(data) * Balance.KAKUJA_STAGE_DEFENSE;
        if (data.isKakujaActive(now)) {
            factor *= 1.0 - Math.min(0.6, Balance.KAKUJA_DEFENSE + formStageBonus(data));
        }
        return factor;
    }
}
