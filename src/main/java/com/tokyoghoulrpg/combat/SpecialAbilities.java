package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.Locale;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Особые способности всех классов. Открываются в дереве навыков (вкладка «Особые»), у каждой своя клавиша.
 * <ul>
 *   <li>«Аура возмездия» (R): 10 секунд фиолетовой ауры. Первый удар по тебе урона не наносит, а ударивший
 *       получает урон, оглушение на 2 секунды и 7 секунд «тишины» (только обычные удары).</li>
 *   <li>«Харизма» (Z): 10 секунд золотой ауры. Первый удар по тебе урона не наносит, а лечит 50% здоровья,
 *       шкала ярости (у агента — «Освобождения») сразу полная, и на 3 секунды небольшие усиления.</li>
 * </ul>
 * Обе ауры защищают только от первого удара: после него аура сразу гаснет. Откат 1 минута начинается,
 * когда аура погасла (от удара или через 10 секунд). Обе ауры одновременно включить нельзя.
 */
public final class SpecialAbilities {
    /** Фиолетовая аура возмездия и золотая аура харизмы (ярость — красная, RC — бирюзовая). */
    public static final DustParticleOptions RETRIBUTION_COLOR = new DustParticleOptions(new Vector3f(0.55F, 0.1F, 0.9F), 1.1F);
    public static final DustParticleOptions CHARISMA_COLOR = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.2F), 1.1F);

    /** Идёт урон от «Ауры возмездия»: на него ауры и «Кровавое зеркало» не отвечают (иначе удары бы отражались по кругу). */
    private static boolean retaliating = false;

    private SpecialAbilities() {
    }

    public static boolean isRetaliating() {
        return retaliating;
    }

    private static boolean has(PlayerData data, SkillTree.Branch branch) {
        return SkillEffects.level(data, branch) > 0;
    }

    /** Нажата клавиша ауры. */
    public static void use(ServerPlayer player, PlayerData data, AbilityType type) {
        if (!data.hasChosenClass() || !player.isAlive() || LockManager.isLocked(player)) {
            return;
        }
        boolean retribution = type == AbilityType.RETRIBUTION;
        SkillTree.Branch branch = retribution ? SkillTree.Branch.RETRIBUTION : SkillTree.Branch.CHARISMA;
        if (!has(data, branch)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.aura_locked",
                    branch.title()), true);
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();
        if (data.isRetributionActive(now) || data.isCharismaActive(now)) {
            // одна аура уже горит: вторую (и ту же ещё раз) не включить
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.aura_busy"), true);
            return;
        }
        long ready = retribution ? state.retributionReady : state.charismaReady;
        if (now < ready) {
            String seconds = String.format(Locale.ROOT, "%.1f", (ready - now) / 20.0);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown",
                    Component.translatable(type.translationKey()), seconds), true);
            return;
        }
        if (retribution) {
            data.setRetributionEndTick(now + Balance.AURA_TICKS);
            Effects.sound(player, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 0.8F);
            Effects.particles(player, RETRIBUTION_COLOR, 40, 0.7, 0.05);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.retribution_on"), true);
        } else {
            data.setCharismaEndTick(now + Balance.AURA_TICKS);
            Effects.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.7F);
            Effects.particles(player, CHARISMA_COLOR, 40, 0.7, 0.05);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.charisma_on"), true);
        }
        NetworkHandler.syncTo(player);
    }

    /** Раз в тик: частицы аур и их окончание. */
    public static void tick(ServerPlayer player, PlayerData data, long now) {
        long retributionEnd = data.getRetributionEndTick();
        if (retributionEnd > 0L) {
            if (now >= retributionEnd || !player.isAlive()) {
                endRetribution(data, now);
                NetworkHandler.syncTo(player);
            } else if (now % 3 == 0) {
                Effects.particles(player, RETRIBUTION_COLOR, 5, 0.5, 0.01);
            }
        }
        long charismaEnd = data.getCharismaEndTick();
        if (charismaEnd > 0L) {
            if (now >= charismaEnd || !player.isAlive()) {
                endCharisma(data, now);
                NetworkHandler.syncTo(player);
            } else if (now % 3 == 0) {
                Effects.particles(player, CHARISMA_COLOR, 5, 0.5, 0.01);
            }
        }
        // «тишина» кончилась — убрать подсказку у клиента
        if (data.getSilenceEndTick() > 0L && now >= data.getSilenceEndTick()) {
            data.setSilenceEndTick(0L);
            NetworkHandler.syncTo(player);
        }
    }

    /** Аура погасла: с этого момента идёт откат. */
    private static void endRetribution(PlayerData data, long now) {
        data.setRetributionEndTick(0L);
        data.combat().retributionReady = now + Balance.AURA_COOLDOWN;
    }

    private static void endCharisma(PlayerData data, long now) {
        data.setCharismaEndTick(0L);
        data.combat().charismaReady = now + Balance.AURA_COOLDOWN;
    }

    /**
     * Игрока бьют. Возвращает true, если удар нужно отменить: любая из аур гасит первый удар целиком
     * и сразу пропадает.
     */
    public static boolean onAttacked(ServerPlayer victim, PlayerData data, LivingEntity attacker) {
        if (retaliating || attacker == victim || !attacker.isAlive()) {
            return false;
        }
        long now = victim.level().getGameTime();
        if (data.isCharismaActive(now)) {
            charisma(victim, data, now);
            return true;
        }
        if (data.isRetributionActive(now)) {
            endRetribution(data, now);
            retribution(victim, attacker, now);
            NetworkHandler.syncTo(victim);
            return true;
        }
        return false;
    }

    private static void retribution(ServerPlayer victim, LivingEntity attacker, long now) {
        retaliating = true;
        try {
            attacker.invulnerableTime = 0;
            attacker.hurt(victim.damageSources().thorns(victim), Balance.RETRIBUTION_DAMAGE);
        } finally {
            retaliating = false;
        }
        LockManager.lock(attacker, now + Balance.RETRIBUTION_STUN);      // боссов не оглушает, но тишина действует
        Silence.apply(attacker, Balance.RETRIBUTION_SILENCE);
        Effects.sound(victim, SoundEvents.SHIELD_BLOCK, 1.0F, 0.6F);
        Effects.sound(attacker, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.7F);
        Effects.particles(attacker, RETRIBUTION_COLOR, 25, 0.4, 0.1);
        if (attacker instanceof ServerPlayer punished) {
            punished.displayClientMessage(Component.translatable("message.tokyoghoulrpg.retribution_hit"), true);
        }
    }

    private static void charisma(ServerPlayer player, PlayerData data, long now) {
        endCharisma(data, now);
        player.heal((float) (player.getMaxHealth() * Balance.CHARISMA_HEAL));
        // ярость гуля и «Освобождение» агента — сразу полная шкала
        if (data.getPlayerClass().hasKagune() || data.getPlayerClass() == PlayerClass.CCG_AGENT) {
            data.setRage(com.tokyoghoulrpg.data.PlayerData.RAGE_MAX);
        }
        int t = Balance.CHARISMA_BUFF_TICKS;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, t, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, t, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, t, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, t, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, t, 0, false, true));
        Effects.sound(player, SoundEvents.PLAYER_LEVELUP, 1.0F, 1.4F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(CHARISMA_COLOR, player.getX(), player.getY(1.0), player.getZ(), 60, 0.6, 0.8, 0.6, 0.1);
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.2), player.getZ(), 6, 0.5, 0.4, 0.5, 0.1);
        }
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.charisma_hit"), true);
        NetworkHandler.syncTo(player);
    }
}
