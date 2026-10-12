package com.tokyoghoulrpg.data;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.ArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Смена класса инъекциями.
 * Инъекция человека: гуль, полугуль, агент CCG → человек.
 * Инъекция гуля: человек → полугуль, полугуль → гуль. На агента CCG не действует.
 * Инъекция CCG: человек → агент CCG. На гуля и полугуля не действует.
 */
public final class ClassChange {
    public enum Injection {
        HUMAN("human_injection"),
        GHOUL("ghoul_injection"),
        CCG("ccg_injection");

        private final String id;

        Injection(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private ClassChange() {
    }

    /** Каким станет игрок после инъекции, или null, если на его класс она не действует. */
    public static PlayerClass result(Injection injection, PlayerClass from) {
        return switch (injection) {
            case HUMAN -> switch (from) {
                case GHOUL, HALF_GHOUL, CCG_AGENT -> PlayerClass.HUMAN;
                default -> null;
            };
            case GHOUL -> switch (from) {
                case HUMAN -> PlayerClass.HALF_GHOUL;
                case HALF_GHOUL -> PlayerClass.GHOUL;
                default -> null;
            };
            case CCG -> from == PlayerClass.HUMAN ? PlayerClass.CCG_AGENT : null;
        };
    }

    /** Сделать инъекцию. Возвращает true, если класс сменился (тогда инъекция тратится). */
    public static boolean inject(ServerPlayer player, Injection injection) {
        PlayerData data = PlayerData.get(player);
        PlayerClass target = data == null ? null : result(injection, data.getPlayerClass());
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.injection_no_effect"), true);
            return false;
        }
        change(player, data, target);
        effects(player, injection);
        return true;
    }

    /**
     * Смена класса. Что происходит:
     * - способности, ярость и блок прерываются, кагуне убирается;
     * - навыки, которых нет в дереве нового класса, снимаются, а их очки возвращаются;
     *   гуль и полугуль делят одно дерево, поэтому между ними навыки сохраняются;
     * - прогресс заданий запоминается для каждой цепочки отдельно: вернувшись в класс,
     *   продолжишь с того же места, а за уже выполненные задания награду не получишь снова;
     * - став полугулем из человека, выбираешь вариант кагуне; полугуль, ставший гулем, свой сохраняет.
     */
    public static void change(ServerPlayer player, PlayerData data, PlayerClass target) {
        PlayerClass old = data.getPlayerClass();
        AbilityManager.resetAll(player);

        Quest.Chain oldChain = Quest.Chain.of(old);
        Quest.Chain newChain = Quest.Chain.of(target);
        if (oldChain != newChain) {
            data.stashQuest(oldChain == null ? null : oldChain.name());
            data.restoreQuest(newChain == null ? null : newChain.name());
        }

        int refund = 0;
        for (String id : new ArrayList<>(data.getSkills())) {
            SkillTree.Skill skill = SkillTree.find(id);
            if (skill == null || !SkillTree.isAvailable(target, skill.branch())) {
                refund += skill == null ? 0 : skill.cost();
                data.removeSkill(id);
            }
        }
        data.addSkillPoints(refund);
        if (!SkillTree.isAvailable(target, SkillTree.Branch.RAGE)) {
            data.setRageLevel(0);
        }

        data.setPlayerClass(target);
        data.setKaguneActive(false);
        boolean chooseVariant = false;
        if (!target.hasKagune()) {
            data.setKaguneVariant(KaguneVariant.NONE);
            data.setRage(0);
        } else if (!old.hasKagune() || data.getKaguneVariant() == KaguneVariant.NONE) {
            data.setKaguneVariant(KaguneVariant.NONE);
            data.setRage(0);
            chooseVariant = true;
        }

        AbilityManager.applyClassStats(player, data, false);
        NetworkHandler.syncTo(player);

        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.class_changed",
                Component.translatable(target.translationKey())));
        if (refund > 0) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.skills_refunded", refund));
        }
        if (chooseVariant) {
            NetworkHandler.openVariantSelection(player);
        }
    }

    private static void effects(ServerPlayer player, Injection injection) {
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        SoundEvent sound = switch (injection) {
            case HUMAN -> SoundEvents.ZOMBIE_VILLAGER_CURE;
            case GHOUL -> SoundEvents.ZOMBIE_VILLAGER_CONVERTED;
            case CCG -> SoundEvents.PLAYER_LEVELUP;
        };
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(injection == Injection.GHOUL ? ParticleTypes.DAMAGE_INDICATOR : ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
        }
    }
}
