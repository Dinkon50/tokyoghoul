package com.tokyoghoulrpg.quest;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Цепочки заданий: одна у гуля и полугуля, другая у человека. Задания идут по порядку:
 * следующее открывается, когда выполнено предыдущее. Награда (очки навыков) выдаётся один раз.
 * У гуля первые четыре задания придумал ты, остальные добавлены по твоей просьбе (числа временные).
 */
public enum Quest {
    // Гуль и полугуль
    FIRST_BLOOD("first_blood", Chain.GHOUL, Trigger.KILL_MONSTER, 1, 1),
    HUNTER("hunter", Chain.GHOUL, Trigger.KILL_MONSTER, 5, 2),
    FIRST_BITE("first_bite", Chain.GHOUL, Trigger.BITE, 1, 1),
    GROWTH("growth", Chain.GHOUL, Trigger.SKILLS_OPENED, 2, 2),
    KAGUNE_HUNT("kagune_hunt", Chain.GHOUL, Trigger.KILL_MONSTER_KAGUNE, 5, 2),
    STORM("storm", Chain.GHOUL, Trigger.BARRAGE_HIT, 20, 2),
    CATCH("catch", Chain.GHOUL, Trigger.PULL, 1, 1),
    FURY("fury", Chain.GHOUL, Trigger.RAGE, 1, 2),
    HUNGER("hunger", Chain.GHOUL, Trigger.BITE, 5, 2),
    PREDATOR("predator", Chain.GHOUL, Trigger.KILL_MONSTER, 20, 3),
    ASCEND("ascend", Chain.GHOUL, Trigger.LEVEL, 15, 3),
    // новые задания добавляются в конец цепочки, чтобы не сбить прогресс в старых мирах
    INFORMANT_JOB("informant_job", Chain.GHOUL, Trigger.CONTRACT, 1, 2),
    DEVOURER("devourer", Chain.GHOUL, Trigger.KILL_BOSS, 1, 3),

    // Человек (числа временные)
    H_FIRST_KILL("h_first_kill", Chain.HUMAN, Trigger.KILL_MONSTER, 1, 1),
    H_WORKER("h_worker", Chain.HUMAN, Trigger.BREAK_BLOCK, 50, 1),
    H_GROWTH("h_growth", Chain.HUMAN, Trigger.SKILLS_OPENED, 2, 2),
    H_HUNTER("h_hunter", Chain.HUMAN, Trigger.KILL_MONSTER, 5, 2),
    H_MINER("h_miner", Chain.HUMAN, Trigger.MINE_ORE, 10, 2),
    H_FIGHTER("h_fighter", Chain.HUMAN, Trigger.KILL_MONSTER, 15, 2),
    H_EXPERIENCE("h_experience", Chain.HUMAN, Trigger.LEVEL, 10, 2),
    H_TRAINING("h_training", Chain.HUMAN, Trigger.SKILLS_OPENED, 8, 2),
    H_VETERAN("h_veteran", Chain.HUMAN, Trigger.KILL_MONSTER, 30, 3),
    H_ASCEND("h_ascend", Chain.HUMAN, Trigger.LEVEL, 20, 3),
    H_INFORMANT_JOB("h_informant_job", Chain.HUMAN, Trigger.CONTRACT, 1, 2),
    H_BOSS_HUNT("h_boss_hunt", Chain.HUMAN, Trigger.KILL_BOSS, 1, 3),

    // Агент CCG (числа временные). Последнее задание — особое: смотри QuinqueReward.
    C_FIRST_HUNT("c_first_hunt", Chain.CCG, Trigger.KILL_GHOUL, 1, 1),
    C_TRAINING("c_training", Chain.CCG, Trigger.SKILLS_OPENED, 2, 2),
    C_PATROL("c_patrol", Chain.CCG, Trigger.KILL_MONSTER, 10, 1),
    C_GHOUL_HUNTER("c_ghoul_hunter", Chain.CCG, Trigger.KILL_GHOUL, 5, 2),
    C_RELEASE("c_release", Chain.CCG, Trigger.RELEASE, 1, 2),
    C_INFORMANT("c_informant", Chain.CCG, Trigger.CONTRACT, 1, 2),
    C_PARRY("c_parry", Chain.CCG, Trigger.PARRY, 3, 2),
    C_VETERAN("c_veteran", Chain.CCG, Trigger.KILL_GHOUL, 15, 3),
    C_EXPERIENCE("c_experience", Chain.CCG, Trigger.LEVEL, 20, 3),
    C_DEVOURER("c_devourer", Chain.CCG, Trigger.KILL_GHOUL_BOSS, 1, 3),
    C_FINAL("c_final", Chain.CCG, Trigger.QUINQUE_REWARD, 2, 15);

    /** Чья цепочка. */
    public enum Chain {
        GHOUL,
        HUMAN,
        CCG;

        /** Цепочка класса или null, если у класса заданий пока нет. */
        public static Chain of(PlayerClass playerClass) {
            return switch (playerClass) {
                case GHOUL, HALF_GHOUL -> GHOUL;
                case HUMAN -> HUMAN;
                case CCG_AGENT -> CCG;
                default -> null;
            };
        }
    }

    /** Что засчитывается в задание. */
    public enum Trigger {
        /** Убийство монстра любым способом. */
        KILL_MONSTER(false),
        /** Убийство монстра ударом кагуне (ЛКМ, ПКМ, барраж). */
        KILL_MONSTER_KAGUNE(false),
        /** Удачный укус. */
        BITE(false),
        /** Попадание барражем. */
        BARRAGE_HIT(false),
        /** Удачный захват Shift + ЛКМ. */
        PULL(false),
        /** Включение ярости. */
        RAGE(false),
        /** Сломанный блок (кроме творческого режима). */
        BREAK_BLOCK(false),
        /** Добытая руда. */
        MINE_ORE(false),
        /** Убит босс (Пожиратель или Старший следователь). */
        KILL_BOSS(false),
        /** Сдан контракт информатора. */
        CONTRACT(false),
        /** Убит гуль: NPC-гуль, Пожиратель, игрок-гуль или полугуль. */
        KILL_GHOUL(false),
        /** Убит Пожиратель. */
        KILL_GHOUL_BOSS(false),
        /** Включено «Освобождение квинке». */
        RELEASE(false),
        /** Идеальный блок (парирование). */
        PARRY(false),
        /** Финал агента: получены обе квинке за Старшего следователя (считается особо, см. QuinqueReward). */
        QUINQUE_REWARD(false),
        /** Сколько всего навыков открыто: считается по факту, а не с начала задания. */
        SKILLS_OPENED(true),
        /** Уровень Minecraft: считается по факту. */
        LEVEL(true);

        private final boolean absolute;

        Trigger(boolean absolute) {
            this.absolute = absolute;
        }

        public boolean absolute() {
            return absolute;
        }
    }

    private final String id;
    private final Chain chain;
    private final Trigger trigger;
    private final int goal;
    private final int reward;

    Quest(String id, Chain chain, Trigger trigger, int goal, int reward) {
        this.id = id;
        this.chain = chain;
        this.trigger = trigger;
        this.goal = goal;
        this.reward = reward;
    }

    public Trigger trigger() {
        return trigger;
    }

    public int goal() {
        return goal;
    }

    public int reward() {
        return reward;
    }

    /** Очки крови за задание. У финального задания агента — случайно от 500 до 1000. */
    public int bloodReward(net.minecraft.util.RandomSource random) {
        if (this == C_FINAL) {
            return Balance.FINAL_BLOOD_MIN + random.nextInt(Balance.FINAL_BLOOD_MAX - Balance.FINAL_BLOOD_MIN + 1);
        }
        return bloodReward();
    }

    /** Очки крови за задание: от 10 до 20, чем больше очков навыков за задание, тем больше крови. */
    public int bloodReward() {
        int blood = Balance.BLOOD_QUEST_MIN + (reward - 1) * (Balance.BLOOD_QUEST_MAX - Balance.BLOOD_QUEST_MIN) / 2;
        return Math.max(Balance.BLOOD_QUEST_MIN, Math.min(Balance.BLOOD_QUEST_MAX, blood));
    }

    public Component title() {
        return Component.translatable("quest.tokyoghoulrpg." + id);
    }

    public Component description() {
        return Component.translatable("quest.tokyoghoulrpg." + id + ".desc", goal);
    }

    /** Все задания цепочки по порядку. */
    public static List<Quest> chain(Chain chain) {
        List<Quest> list = new ArrayList<>();
        for (Quest quest : values()) {
            if (quest.chain == chain) {
                list.add(quest);
            }
        }
        return list;
    }

    /** Задание класса по номеру в цепочке или null, если цепочка пройдена или её нет. */
    public static Quest byIndex(PlayerClass playerClass, int index) {
        Chain chain = Chain.of(playerClass);
        if (chain == null) {
            return null;
        }
        List<Quest> list = chain(chain);
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }
}
