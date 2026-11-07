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
    // Сгенерировано tools/story_quests.py (python3 story_quests.py java). Там же тексты и проверка очков.
    // Новые задания только в конец своей цепочки: прогресс в мирах хранится номером задания.
    // ---- GHOUL
    FIRST_BLOOD("first_blood", Chain.GHOUL, Trigger.KILL_MONSTER, 1, 1, 1, null),
    HUNTER("hunter", Chain.GHOUL, Trigger.KILL_MONSTER, 5, 2, 1, null),
    FIRST_BITE("first_bite", Chain.GHOUL, Trigger.BITE, 1, 1, 1, null),
    GROWTH("growth", Chain.GHOUL, Trigger.SKILLS_OPENED, 2, 2, 1, null),
    KAGUNE_HUNT("kagune_hunt", Chain.GHOUL, Trigger.KILL_MONSTER_KAGUNE, 5, 2, 1, null),
    STORM("storm", Chain.GHOUL, Trigger.BARRAGE_HIT, 20, 2, 1, null),
    CATCH("catch", Chain.GHOUL, Trigger.PULL, 1, 1, 1, null),
    FURY("fury", Chain.GHOUL, Trigger.RAGE, 1, 2, 1, null),
    HUNGER("hunger", Chain.GHOUL, Trigger.BITE, 5, 2, 1, null),
    PREDATOR("predator", Chain.GHOUL, Trigger.KILL_MONSTER, 20, 3, 1, null),
    ASCEND("ascend", Chain.GHOUL, Trigger.LEVEL, 15, 3, 1, null),
    INFORMANT_JOB("informant_job", Chain.GHOUL, Trigger.CONTRACT, 1, 2, 1, null),
    DEVOURER("devourer", Chain.GHOUL, Trigger.KILL_BOSS, 1, 3, 1, null),
    G_WHITE_COATS("g_white_coats", Chain.GHOUL, Trigger.KILL_CCG, 3, 3, 2, 60),
    G_FEAST("g_feast", Chain.GHOUL, Trigger.EAT_FLESH, 10, 1, 2, 40),
    G_SHADOW("g_shadow", Chain.GHOUL, Trigger.KILL_MONSTER_KAGUNE, 40, 1, 2, 40),
    G_ROOTS("g_roots", Chain.GHOUL, Trigger.SKILLS_OPENED, 20, 3, 2, 40),
    G_LEVEL_20("g_level_20", Chain.GHOUL, Trigger.LEVEL, 20, 3, 2, 40),
    G_THIRST("g_thirst", Chain.GHOUL, Trigger.BITE, 20, 1, 2, 40),
    G_EARS("g_ears", Chain.GHOUL, Trigger.CONTRACT, 2, 3, 2, 60),
    G_PURGE("g_purge", Chain.GHOUL, Trigger.KILL_CCG, 10, 4, 2, 100),
    G_WRATH("g_wrath", Chain.GHOUL, Trigger.RAGE, 5, 1, 2, 50),
    G_INVESTIGATOR("g_investigator", Chain.GHOUL, Trigger.KILL_CCG_BOSS, 1, 5, 2, 300),
    G_TUNNELS("g_tunnels", Chain.GHOUL, Trigger.KILL_SUBWAY_BOSS, 1, 5, 3, 250),
    G_TUNNEL_RATS("g_tunnel_rats", Chain.GHOUL, Trigger.KILL_GHOUL, 10, 1, 3, 60),
    G_LAB("g_lab", Chain.GHOUL, Trigger.KILL_TEST_SUBJECT, 5, 4, 3, 120),
    G_LAB_PURGE("g_lab_purge", Chain.GHOUL, Trigger.KILL_TEST_SUBJECT, 10, 1, 3, 80),
    G_RC_STEEL("g_rc_steel", Chain.GHOUL, Trigger.MINE_ORE, 20, 1, 3, 60),
    G_BRANCHES("g_branches", Chain.GHOUL, Trigger.SKILLS_OPENED, 35, 4, 3, 80),
    G_LEVEL_25("g_level_25", Chain.GHOUL, Trigger.LEVEL, 25, 4, 3, 80),
    G_NIGHT_HUNTER("g_night_hunter", Chain.GHOUL, Trigger.KILL_MONSTER, 100, 1, 3, 100),
    G_CANNIBAL("g_cannibal", Chain.GHOUL, Trigger.KILL_GHOUL, 15, 5, 4, 150),
    G_TEMPEST("g_tempest", Chain.GHOUL, Trigger.BARRAGE_HIT, 100, 1, 4, 80),
    G_RETINUE("g_retinue", Chain.GHOUL, Trigger.KILL_HALF_GHOUL, 5, 1, 4, 100),
    G_ARENA("g_arena", Chain.GHOUL, Trigger.KILL_HALF_GHOUL_BOSS, 1, 5, 4, 500),
    G_KAKUJA("g_kakuja", Chain.GHOUL, Trigger.KAKUJA, 1, 5, 4, 200),
    G_CCG_WAR("g_ccg_war", Chain.GHOUL, Trigger.KILL_CCG, 25, 1, 4, 150),
    G_LEVEL_28("g_level_28", Chain.GHOUL, Trigger.LEVEL, 28, 1, 4, 80),
    G_LEVEL_30("g_level_30", Chain.GHOUL, Trigger.LEVEL, 30, 5, 5, 150),
    G_DEBTS("g_debts", Chain.GHOUL, Trigger.CONTRACT, 5, 1, 5, 150),
    G_STAR("g_star", Chain.GHOUL, Trigger.KILL_WITHER, 1, 5, 5, 300),
    G_FULL_TREE("g_full_tree", Chain.GHOUL, Trigger.SKILLS_OPENED, 50, 4, 5, 150),
    G_LAST_MEAL("g_last_meal", Chain.GHOUL, Trigger.BITE, 30, 1, 5, 100),
    G_MONARCH("g_monarch", Chain.GHOUL, Trigger.KILL_MONARCH, 1, 16, 5, 2000),
    // ---- HALF_GHOUL
    HG_FIRST_BLOOD("hg_first_blood", Chain.HALF_GHOUL, Trigger.KILL_MONSTER, 1, 2, 1, null),
    HG_HUNTER("hg_hunter", Chain.HALF_GHOUL, Trigger.KILL_MONSTER, 5, 3, 1, null),
    HG_FIRST_BITE("hg_first_bite", Chain.HALF_GHOUL, Trigger.BITE, 1, 2, 1, null),
    HG_GROWTH("hg_growth", Chain.HALF_GHOUL, Trigger.SKILLS_OPENED, 2, 3, 1, null),
    HG_KAGUNE_HUNT("hg_kagune_hunt", Chain.HALF_GHOUL, Trigger.KILL_MONSTER_KAGUNE, 5, 3, 1, null),
    HG_STORM("hg_storm", Chain.HALF_GHOUL, Trigger.BARRAGE_HIT, 20, 3, 1, null),
    HG_CATCH("hg_catch", Chain.HALF_GHOUL, Trigger.PULL, 1, 2, 1, null),
    HG_FURY("hg_fury", Chain.HALF_GHOUL, Trigger.RAGE, 1, 3, 1, null),
    HG_HUNGER("hg_hunger", Chain.HALF_GHOUL, Trigger.BITE, 5, 3, 1, null),
    HG_PREDATOR("hg_predator", Chain.HALF_GHOUL, Trigger.KILL_MONSTER, 20, 5, 1, null),
    HG_ASCEND("hg_ascend", Chain.HALF_GHOUL, Trigger.LEVEL, 15, 5, 1, null),
    HG_INFORMANT_JOB("hg_informant_job", Chain.HALF_GHOUL, Trigger.CONTRACT, 1, 3, 1, null),
    HG_DEVOURER("hg_devourer", Chain.HALF_GHOUL, Trigger.KILL_BOSS, 1, 5, 1, null),
    HG_HUNTED("hg_hunted", Chain.HALF_GHOUL, Trigger.KILL_CCG, 3, 8, 2, 60),
    HG_FEAST("hg_feast", Chain.HALF_GHOUL, Trigger.EAT_FLESH, 10, 2, 2, 40),
    HG_SHADOW("hg_shadow", Chain.HALF_GHOUL, Trigger.KILL_MONSTER_KAGUNE, 40, 2, 2, 40),
    HG_ROOTS("hg_roots", Chain.HALF_GHOUL, Trigger.SKILLS_OPENED, 20, 8, 2, 40),
    HG_LEVEL_20("hg_level_20", Chain.HALF_GHOUL, Trigger.LEVEL, 20, 8, 2, 40),
    HG_THIRST("hg_thirst", Chain.HALF_GHOUL, Trigger.BITE, 20, 2, 2, 40),
    HG_EARS("hg_ears", Chain.HALF_GHOUL, Trigger.CONTRACT, 2, 8, 2, 60),
    HG_PURGE("hg_purge", Chain.HALF_GHOUL, Trigger.KILL_CCG, 10, 11, 2, 100),
    HG_WRATH("hg_wrath", Chain.HALF_GHOUL, Trigger.RAGE, 5, 2, 2, 50),
    HG_INVESTIGATOR("hg_investigator", Chain.HALF_GHOUL, Trigger.KILL_CCG_BOSS, 1, 14, 2, 300),
    HG_TUNNELS("hg_tunnels", Chain.HALF_GHOUL, Trigger.KILL_SUBWAY_BOSS, 1, 14, 3, 250),
    HG_TUNNEL_RATS("hg_tunnel_rats", Chain.HALF_GHOUL, Trigger.KILL_GHOUL, 10, 2, 3, 60),
    HG_LAB("hg_lab", Chain.HALF_GHOUL, Trigger.KILL_TEST_SUBJECT, 5, 11, 3, 120),
    HG_LAB_PURGE("hg_lab_purge", Chain.HALF_GHOUL, Trigger.KILL_TEST_SUBJECT, 10, 2, 3, 80),
    HG_RC_STEEL("hg_rc_steel", Chain.HALF_GHOUL, Trigger.MINE_ORE, 20, 2, 3, 60),
    HG_BRANCHES("hg_branches", Chain.HALF_GHOUL, Trigger.SKILLS_OPENED, 35, 11, 3, 80),
    HG_LEVEL_25("hg_level_25", Chain.HALF_GHOUL, Trigger.LEVEL, 25, 11, 3, 80),
    HG_NIGHT_HUNTER("hg_night_hunter", Chain.HALF_GHOUL, Trigger.KILL_MONSTER, 100, 2, 3, 100),
    HG_CANNIBAL("hg_cannibal", Chain.HALF_GHOUL, Trigger.KILL_GHOUL, 15, 14, 4, 150),
    HG_TEMPEST("hg_tempest", Chain.HALF_GHOUL, Trigger.BARRAGE_HIT, 100, 2, 4, 80),
    HG_RETINUE("hg_retinue", Chain.HALF_GHOUL, Trigger.KILL_HALF_GHOUL, 5, 2, 4, 100),
    HG_ARENA("hg_arena", Chain.HALF_GHOUL, Trigger.KILL_HALF_GHOUL_BOSS, 1, 13, 4, 500),
    HG_KAKUJA("hg_kakuja", Chain.HALF_GHOUL, Trigger.KAKUJA, 1, 13, 4, 200),
    HG_CCG_WAR("hg_ccg_war", Chain.HALF_GHOUL, Trigger.KILL_CCG, 25, 2, 4, 150),
    HG_LEVEL_28("hg_level_28", Chain.HALF_GHOUL, Trigger.LEVEL, 28, 2, 4, 80),
    HG_LEVEL_30("hg_level_30", Chain.HALF_GHOUL, Trigger.LEVEL, 30, 13, 5, 150),
    HG_DEBTS("hg_debts", Chain.HALF_GHOUL, Trigger.CONTRACT, 5, 2, 5, 150),
    HG_STAR("hg_star", Chain.HALF_GHOUL, Trigger.KILL_WITHER, 1, 13, 5, 300),
    HG_FULL_TREE("hg_full_tree", Chain.HALF_GHOUL, Trigger.SKILLS_OPENED, 50, 13, 5, 150),
    HG_LAST_MEAL("hg_last_meal", Chain.HALF_GHOUL, Trigger.BITE, 30, 2, 5, 100),
    HG_MONARCH("hg_monarch", Chain.HALF_GHOUL, Trigger.KILL_MONARCH, 1, 34, 5, 2000),
    // ---- HUMAN
    H_FIRST_KILL("h_first_kill", Chain.HUMAN, Trigger.KILL_MONSTER, 1, 1, 1, null),
    H_WORKER("h_worker", Chain.HUMAN, Trigger.BREAK_BLOCK, 50, 1, 1, null),
    H_GROWTH("h_growth", Chain.HUMAN, Trigger.SKILLS_OPENED, 2, 2, 1, null),
    H_HUNTER("h_hunter", Chain.HUMAN, Trigger.KILL_MONSTER, 5, 2, 1, null),
    H_MINER("h_miner", Chain.HUMAN, Trigger.MINE_ORE, 10, 2, 1, null),
    H_FIGHTER("h_fighter", Chain.HUMAN, Trigger.KILL_MONSTER, 15, 2, 1, null),
    H_EXPERIENCE("h_experience", Chain.HUMAN, Trigger.LEVEL, 10, 2, 1, null),
    H_TRAINING("h_training", Chain.HUMAN, Trigger.SKILLS_OPENED, 8, 2, 1, null),
    H_VETERAN("h_veteran", Chain.HUMAN, Trigger.KILL_MONSTER, 30, 3, 1, null),
    H_ASCEND("h_ascend", Chain.HUMAN, Trigger.LEVEL, 20, 3, 1, null),
    H_INFORMANT_JOB("h_informant_job", Chain.HUMAN, Trigger.CONTRACT, 1, 2, 1, null),
    H_BOSS_HUNT("h_boss_hunt", Chain.HUMAN, Trigger.KILL_BOSS, 1, 3, 1, null),
    H_GHOUL_SLAYER("h_ghoul_slayer", Chain.HUMAN, Trigger.KILL_GHOUL, 5, 3, 2, 60),
    H_PATROL("h_patrol", Chain.HUMAN, Trigger.KILL_MONSTER, 50, 1, 2, 40),
    H_CONTACTS("h_contacts", Chain.HUMAN, Trigger.CONTRACT, 2, 3, 2, 60),
    H_ARMORER("h_armorer", Chain.HUMAN, Trigger.MINE_ORE, 30, 1, 2, 40),
    H_DRILLS("h_drills", Chain.HUMAN, Trigger.SKILLS_OPENED, 15, 3, 2, 40),
    H_FORTIFY("h_fortify", Chain.HUMAN, Trigger.BREAK_BLOCK, 300, 1, 2, 40),
    H_LEVEL_25("h_level_25", Chain.HUMAN, Trigger.LEVEL, 25, 3, 2, 60),
    H_GHOUL_HUNT_10("h_ghoul_hunt_10", Chain.HUMAN, Trigger.KILL_GHOUL, 10, 1, 2, 80),
    H_INVESTIGATOR("h_investigator", Chain.HUMAN, Trigger.KILL_CCG_BOSS, 1, 3, 2, 300),
    H_TUNNELS("h_tunnels", Chain.HUMAN, Trigger.KILL_SUBWAY_BOSS, 1, 3, 3, 250),
    H_RC_STEEL("h_rc_steel", Chain.HUMAN, Trigger.MINE_ORE, 20, 1, 3, 60),
    H_LAB("h_lab", Chain.HUMAN, Trigger.KILL_TEST_SUBJECT, 5, 3, 3, 120),
    H_LAB_PURGE("h_lab_purge", Chain.HUMAN, Trigger.KILL_TEST_SUBJECT, 10, 1, 3, 80),
    H_GHOUL_HUNT("h_ghoul_hunt", Chain.HUMAN, Trigger.KILL_GHOUL, 15, 3, 3, 120),
    H_NIGHT_HUNTER("h_night_hunter", Chain.HUMAN, Trigger.KILL_MONSTER, 100, 1, 3, 100),
    H_SKILLS_20("h_skills_20", Chain.HUMAN, Trigger.SKILLS_OPENED, 20, 1, 3, 60),
    H_RETINUE("h_retinue", Chain.HUMAN, Trigger.KILL_HALF_GHOUL, 5, 1, 4, 100),
    H_ARENA("h_arena", Chain.HUMAN, Trigger.KILL_HALF_GHOUL_BOSS, 1, 3, 4, 500),
    H_GHOUL_WAR("h_ghoul_war", Chain.HUMAN, Trigger.KILL_GHOUL, 30, 1, 4, 150),
    H_LEVEL_30("h_level_30", Chain.HUMAN, Trigger.LEVEL, 30, 2, 4, 150),
    H_DEBTS("h_debts", Chain.HUMAN, Trigger.CONTRACT, 5, 1, 5, 150),
    H_STAR("h_star", Chain.HUMAN, Trigger.KILL_WITHER, 1, 2, 5, 300),
    H_VETERAN_200("h_veteran_200", Chain.HUMAN, Trigger.KILL_MONSTER, 200, 1, 5, 150),
    H_FULL_TREE("h_full_tree", Chain.HUMAN, Trigger.SKILLS_OPENED, 30, 2, 5, 150),
    H_MONARCH("h_monarch", Chain.HUMAN, Trigger.KILL_MONARCH, 1, 10, 5, 2000),
    // ---- CCG
    C_FIRST_HUNT("c_first_hunt", Chain.CCG, Trigger.KILL_GHOUL, 1, 2, 1, null),
    C_TRAINING("c_training", Chain.CCG, Trigger.SKILLS_OPENED, 2, 2, 1, null),
    C_PATROL("c_patrol", Chain.CCG, Trigger.KILL_MONSTER, 10, 1, 1, null),
    C_GHOUL_HUNTER("c_ghoul_hunter", Chain.CCG, Trigger.KILL_GHOUL, 5, 2, 1, null),
    C_RELEASE("c_release", Chain.CCG, Trigger.RELEASE, 1, 2, 1, null),
    C_INFORMANT("c_informant", Chain.CCG, Trigger.CONTRACT, 1, 2, 1, null),
    C_PARRY("c_parry", Chain.CCG, Trigger.PARRY, 3, 2, 1, null),
    C_VETERAN("c_veteran", Chain.CCG, Trigger.KILL_GHOUL, 15, 3, 1, null),
    C_EXPERIENCE("c_experience", Chain.CCG, Trigger.LEVEL, 20, 3, 2, null),
    C_DEVOURER("c_devourer", Chain.CCG, Trigger.KILL_GHOUL_BOSS, 1, 3, 2, null),
    C_FINAL("c_final", Chain.CCG, Trigger.QUINQUE_REWARD, 2, 15, 2, null),
    C_QUINQUE_MASTER("c_quinque_master", Chain.CCG, Trigger.RELEASE, 5, 1, 2, 60),
    C_GUARD("c_guard", Chain.CCG, Trigger.PARRY, 10, 1, 2, 60),
    C_TUNNELS("c_tunnels", Chain.CCG, Trigger.KILL_SUBWAY_BOSS, 1, 5, 3, 250),
    C_TUNNEL_RATS("c_tunnel_rats", Chain.CCG, Trigger.KILL_GHOUL, 10, 1, 3, 60),
    C_LAB("c_lab", Chain.CCG, Trigger.KILL_TEST_SUBJECT, 5, 5, 3, 120),
    C_LAB_PURGE("c_lab_purge", Chain.CCG, Trigger.KILL_TEST_SUBJECT, 10, 1, 3, 80),
    C_RC_STEEL("c_rc_steel", Chain.CCG, Trigger.MINE_ORE, 20, 1, 3, 60),
    C_BRANCHES("c_branches", Chain.CCG, Trigger.SKILLS_OPENED, 35, 5, 3, 80),
    C_LEVEL_25("c_level_25", Chain.CCG, Trigger.LEVEL, 25, 5, 3, 80),
    C_PATROL_100("c_patrol_100", Chain.CCG, Trigger.KILL_MONSTER, 100, 1, 3, 100),
    C_PURGE("c_purge", Chain.CCG, Trigger.KILL_GHOUL, 25, 5, 4, 150),
    C_RETINUE("c_retinue", Chain.CCG, Trigger.KILL_HALF_GHOUL, 5, 1, 4, 100),
    C_RELEASES("c_releases", Chain.CCG, Trigger.RELEASE, 10, 1, 4, 80),
    C_ARENA("c_arena", Chain.CCG, Trigger.KILL_HALF_GHOUL_BOSS, 1, 5, 4, 500),
    C_GHOUL_WAR("c_ghoul_war", Chain.CCG, Trigger.KILL_GHOUL, 40, 1, 4, 150),
    C_LEVEL_28("c_level_28", Chain.CCG, Trigger.LEVEL, 28, 1, 4, 80),
    C_LEVEL_30("c_level_30", Chain.CCG, Trigger.LEVEL, 30, 5, 5, 150),
    C_DEBTS("c_debts", Chain.CCG, Trigger.CONTRACT, 5, 1, 5, 150),
    C_STAR("c_star", Chain.CCG, Trigger.KILL_WITHER, 1, 5, 5, 300),
    C_FULL_TREE("c_full_tree", Chain.CCG, Trigger.SKILLS_OPENED, 50, 4, 5, 150),
    C_PARRY_20("c_parry_20", Chain.CCG, Trigger.PARRY, 20, 1, 5, 100),
    C_MONARCH("c_monarch", Chain.CCG, Trigger.KILL_MONARCH, 1, 16, 5, 2000);




    /** Чья цепочка. */
    public enum Chain {
        GHOUL,
        HALF_GHOUL,
        HUMAN,
        CCG;

        /** Цепочка класса или null, если у класса заданий пока нет. */
        public static Chain of(PlayerClass playerClass) {
            return switch (playerClass) {
                case GHOUL -> GHOUL;
                case HALF_GHOUL -> HALF_GHOUL;
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
        /** Убит агент CCG: NPC-агент или игрок-агент. */
        KILL_CCG(false),
        /** Убит Старший следователь (засчитывается всем рядом). */
        KILL_CCG_BOSS(false),
        /** Побеждён Кровавый полугуль на арене (всем, кто нанёс урон). */
        KILL_HALF_GHOUL_BOSS(false),
        /** Побеждён Алый Монарх (всем, кто нанёс урон). */
        KILL_MONARCH(false),
        /** Убит Хозяин тоннелей в заброшенном метро (всем рядом). */
        KILL_SUBWAY_BOSS(false),
        /** Убит подопытный из лаборатории RC. */
        KILL_TEST_SUBJECT(false),
        /** Убит Иссушитель (всем рядом): звезда Незера для Алой короны. */
        KILL_WITHER(false),
        /** Принята форма какудзя. */
        KAKUJA(false),
        /** Съеден кусок плоти (гуль или полугуль). */
        EAT_FLESH(false),
        /** Убит полугуль (NPC, например свита Кровавого полугуля). */
        KILL_HALF_GHOUL(false),
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
    /** Глава сюжета (1-5): заголовки в окне заданий. */
    private final int act;
    /** Очки крови; null — по старой формуле 10-20. */
    private final Integer blood;

    Quest(String id, Chain chain, Trigger trigger, int goal, int reward, int act, Integer blood) {
        this.id = id;
        this.chain = chain;
        this.trigger = trigger;
        this.goal = goal;
        this.reward = reward;
        this.act = act;
        this.blood = blood;
    }

    public Chain chain() {
        return chain;
    }

    public int act() {
        return act;
    }

    /** Базовые очки на этой сложности: на сложной x1,75 (навыки там в 1,5 раза дороже, а очки за уровни те же). */
    public int reward(net.minecraft.world.Difficulty difficulty) {
        return difficulty == net.minecraft.world.Difficulty.HARD
                ? (int) Math.ceil(reward * Balance.HARD_QUEST_POINTS_FACTOR) : reward;
    }

    /** Сколько очков навыков игрок получит на самом деле (как PlayerData.earnSkillPoints: x1,5, кроме полугуля). */
    public int pointsFor(PlayerClass playerClass, net.minecraft.world.Difficulty difficulty) {
        int base = reward(difficulty);
        return playerClass == PlayerClass.HALF_GHOUL ? base : (int) Math.round(base * Balance.SKILL_POINT_GAIN);
    }

    public Component story() {
        return Component.translatable("quest.tokyoghoulrpg." + id + ".story");
    }

    public Component actTitle() {
        return Component.translatable("quest.tokyoghoulrpg.act." + chain.name().toLowerCase(java.util.Locale.ROOT) + "." + act);
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

    /** Очки крови за задание: у сюжетных — свои, у старых от 10 до 20 (чем больше очков навыков, тем больше крови). */
    public int bloodReward() {
        if (this.blood != null) {
            return this.blood;
        }
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
