package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.Locale;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import org.joml.Vector3f;

/**
 * «Бросок судьбы» (клавиша I, у всех классов после прокачки). Нажал — выпадает один из шести исходов:
 * <ul>
 *   <li>30% — бафф I: лечение 25% здоровья и меньше урона 30 секунд;</li>
 *   <li>30% — дебафф I: теряешь 25% текущего здоровья;</li>
 *   <li>15% — бафф II: лечение 40% здоровья и меньше урона 30 секунд;</li>
 *   <li>15% — дебафф II: теряешь 40% текущего здоровья, все способности на откате 3 секунды;</li>
 *   <li>5% — бафф III: лечение 75% здоровья и меньше урона 30 секунд;</li>
 *   <li>5% — дебафф III: теряешь 70% текущего здоровья, все способности на откате 6 секунд.</li>
 * </ul>
 * Дебаффы считаются от текущего здоровья, поэтому убить не могут. Откат самого броска — 1 минута.
 */
public final class Fortune {
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.25F, 0.85F, 0.45F), 1.1F);
    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.25F, 0.05F, 0.1F), 1.1F);

    private Fortune() {
    }

    public static void use(ServerPlayer player, PlayerData data) {
        if (!data.hasChosenClass() || !player.isAlive() || LockManager.isLocked(player)) {
            return;
        }
        if (SkillEffects.level(data, SkillTree.Branch.FORTUNE) <= 0) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.aura_locked",
                    SkillTree.Branch.FORTUNE.title()), true);
            return;
        }
        CombatState state = data.combat();
        long now = player.level().getGameTime();
        if (now < state.fortuneReady) {
            String seconds = String.format(Locale.ROOT, "%.1f", (state.fortuneReady - now) / 20.0);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown",
                    Component.translatable(AbilityType.FORTUNE.translationKey()), seconds), true);
            return;
        }
        state.fortuneReady = now + Balance.FORTUNE_COOLDOWN;

        int roll = player.getRandom().nextInt(100);
        int edge = Balance.FORTUNE_BUFF1_CHANCE;
        if (roll < edge) {
            buff(player, data, now, 1, Balance.FORTUNE_BUFF1_HEAL, Balance.FORTUNE_BUFF1_SHIELD);
        } else if (roll < (edge += Balance.FORTUNE_DEBUFF1_CHANCE)) {
            debuff(player, data, now, 1, Balance.FORTUNE_DEBUFF1_LOSS, 0);
        } else if (roll < (edge += Balance.FORTUNE_BUFF2_CHANCE)) {
            buff(player, data, now, 2, Balance.FORTUNE_BUFF2_HEAL, Balance.FORTUNE_BUFF2_SHIELD);
        } else if (roll < (edge += Balance.FORTUNE_DEBUFF2_CHANCE)) {
            debuff(player, data, now, 2, Balance.FORTUNE_DEBUFF2_LOSS, Balance.FORTUNE_DEBUFF2_LOCK);
        } else if (roll < (edge += Balance.FORTUNE_BUFF3_CHANCE)) {
            buff(player, data, now, 3, Balance.FORTUNE_BUFF3_HEAL, Balance.FORTUNE_BUFF3_SHIELD);
        } else {
            debuff(player, data, now, 3, Balance.FORTUNE_DEBUFF3_LOSS, Balance.FORTUNE_DEBUFF3_LOCK);
        }
        NetworkHandler.syncTo(player);
    }

    private static void buff(ServerPlayer player, PlayerData data, long now, int tier, double heal, double shield) {
        player.heal((float) (player.getMaxHealth() * heal));
        // новая защита заменяет старую, только если она не слабее
        if (shield >= data.fortuneShieldAt(now)) {
            data.setFortuneShield(shield, now + Balance.FORTUNE_SHIELD_TICKS);
        }
        Effects.sound(player, SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F + tier * 0.2F);
        Effects.particles(player, GREEN, 20 + tier * 15, 0.6, 0.05);
        Effects.particles(player, ParticleTypes.HAPPY_VILLAGER, 10 * tier, 0.6, 0.05);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.fortune_buff_" + tier,
                Math.round(heal * 100), Math.round(shield * 100), Balance.FORTUNE_SHIELD_TICKS / 20), true);
    }

    private static void debuff(ServerPlayer player, PlayerData data, long now, int tier, double loss, int lockTicks) {
        // от текущего здоровья: броском нельзя убить себя
        player.setHealth((float) (player.getHealth() * (1.0 - loss)));
        Effects.sound(player, SoundEvents.PLAYER_HURT, 1.0F, 0.7F);
        Effects.sound(player, SoundEvents.WITHER_AMBIENT, 0.4F, 1.6F);
        Effects.particles(player, DARK, 20 + tier * 15, 0.6, 0.05);
        if (lockTicks > 0) {
            lockAbilities(player, data, now + lockTicks);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.fortune_debuff_lock_" + tier,
                    Math.round(loss * 100), lockTicks / 20), true);
        } else {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.fortune_debuff_" + tier,
                    Math.round(loss * 100)), true);
        }
    }

    /** Все способности уходят на откат до until (откат, который уже длиннее, не сокращается). */
    private static void lockAbilities(ServerPlayer player, PlayerData data, long until) {
        AbilityManager.cancelAll(player);          // блок и барраж обрываются
        CombatState s = data.combat();
        s.biteReady = Math.max(s.biteReady, until);
        s.dashReady = Math.max(s.dashReady, until);
        s.kaguneToggleReady = Math.max(s.kaguneToggleReady, until);
        s.humanStrongReady = Math.max(s.humanStrongReady, until);
        s.barrageReady = Math.max(s.barrageReady, until);
        s.blockReady = Math.max(s.blockReady, until);
        s.grappleReady = Math.max(s.grappleReady, until);
        s.pullReady = Math.max(s.pullReady, until);
        s.strongReady = Math.max(s.strongReady, until);
        s.lungeReady = Math.max(s.lungeReady, until);
        s.retributionReady = Math.max(s.retributionReady, until);
        s.charismaReady = Math.max(s.charismaReady, until);
    }
}
