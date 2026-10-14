package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.RageStats;
import com.tokyoghoulrpg.data.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;

/**
 * Дерево навыков гуля и полугуля. Ветки разложены по четырём вкладкам, уровни в каждой ветке
 * открываются по порядку. Числа эффектов считаются в SkillEffects по тем же формулам, что и в игре.
 */
public final class SkillTree {
    /** Вкладка дерева навыков. */
    public enum Tab {
        // Гуль и полугуль
        BASE("base", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        ABILITIES("abilities", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        SURVIVAL("survival", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        RAGE_KAGUNE("rage_kagune", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        // Человек
        HUMAN("human", PlayerClass.HUMAN),
        // Агент CCG: четыре вкладки, похоже на гуля, но про квинке
        CCG_TRAINING("ccg_training", PlayerClass.CCG_AGENT),
        CCG_QUINQUE("ccg_quinque", PlayerClass.CCG_AGENT),
        CCG_SURVIVAL("ccg_survival", PlayerClass.CCG_AGENT),
        CCG_HUNT("ccg_hunt", PlayerClass.CCG_AGENT),
        // У всех классов: особые способности на своих клавишах
        SPECIAL("special", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL, PlayerClass.HUMAN, PlayerClass.CCG_AGENT);

        private final String id;
        private final List<PlayerClass> classes;

        Tab(String id, PlayerClass... classes) {
            this.id = id;
            this.classes = List.of(classes);
        }

        public boolean isFor(PlayerClass playerClass) {
            return classes.contains(playerClass);
        }

        public Component title() {
            return Component.translatable("skill.tokyoghoulrpg.tab." + id);
        }
    }

    /** Ветка дерева. Навыки ветки называются «id_номер», например damage_3. */
    public enum Branch {
        // Основа
        RAGE("rage", Tab.BASE, Balance.RAGE_LEVEL_MAX, Balance.RAGE_UPGRADE_COSTS, 1, 0xD01F4A),
        DAMAGE("damage", Tab.BASE, Balance.PASSIVE_LEVELS, Balance.PASSIVE_UPGRADE_COSTS, 1, 0xC9822B),
        REGEN("regen", Tab.BASE, Balance.PASSIVE_LEVELS, Balance.PASSIVE_UPGRADE_COSTS, 1, 0x4FB05A),
        DEFENSE("defense", Tab.BASE, Balance.PASSIVE_LEVELS, Balance.PASSIVE_UPGRADE_COSTS, 1, 0x5A86C9),
        SPEED("speed", Tab.BASE, Balance.PASSIVE_LEVELS, Balance.PASSIVE_UPGRADE_COSTS, 1, 0xD8D8D8),
        // Способности
        BITE("bite", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xB01E1E),
        BARRAGE("barrage", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xE0503A),
        DASH("dash", Tab.ABILITIES, 2, Balance.COSTS_2, 1, 0x9ED0E6),
        // Без прокачки захваты достают на 10 блоков: это уровень I. Навыки ветки показаны как II-IV (15, 20, 25 блоков).
        GRAB("grab", Tab.ABILITIES, 3, Balance.COSTS_3, 2, 0x8E5AC9),
        BLOCK("block", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0x7A8A9A),
        STRONG("strong", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xC9822B),
        // Выживание
        HUNGER("hunger", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0xA0703A),
        VAMPIRISM("vampirism", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0x8A0010),
        SECOND_WIND("second_wind", Tab.SURVIVAL, 1, Balance.COSTS_1, 1, 0xE6D27A, Tab.CCG_SURVIVAL),
        // Ярость и кагуне
        RAGE_GAIN("rage_gain", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0xD01F4A),
        LAST_STAND("last_stand", Tab.RAGE_KAGUNE, 1, Balance.COSTS_1, 1, 0xFF5A78),
        RAGE_AURA("rage_aura", Tab.RAGE_KAGUNE, 2, Balance.COSTS_2, 1, 0x9A2A4A),
        REACH("reach", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0xB04060),
        PIERCE("pierce", Tab.RAGE_KAGUNE, 2, Balance.COSTS_2, 1, 0xC9C9C9, Tab.CCG_HUNT),
        BLEED("bleed", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0x6A0A10, Tab.CCG_HUNT),
        // Агент CCG: «Освобождение квинке» (как ярость у гуля) — первая строка вкладки «Подготовка»
        RELEASE("release", Tab.CCG_TRAINING, Balance.RAGE_LEVEL_MAX, Balance.RAGE_UPGRADE_COSTS, 1, 0x3FD8C0),
        // Человек
        // Человек; эти же ветки у агента CCG во вкладке «Подготовка» (агент — тоже человек, только обученный)
        TOUGHNESS("toughness", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0x5A86C9, Tab.CCG_TRAINING),
        // «Здоровье» у всех: человек и агент до 3,5 строк сердец, гуль и полугуль (вкладка «Выживание») до 5 строк
        VITALITY("vitality", Tab.HUMAN, Balance.VITALITY_LEVELS, Balance.VITALITY_COSTS, 1, 0xB01E1E, Tab.CCG_TRAINING,
                Tab.SURVIVAL),
        ATTACK_SPEED("attack_speed", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0xC9822B, Tab.CCG_TRAINING),
        MINING("mining", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0x9A9A9A, Tab.CCG_SURVIVAL),
        WEAPON_DAMAGE("weapon_damage", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0xE0503A, Tab.CCG_TRAINING),
        HUMAN_DASH("human_dash", Tab.HUMAN, 2, Balance.COSTS_2, 1, 0x9ED0E6, Tab.CCG_QUINQUE),
        // Агент CCG: прокачка способностей квинке
        Q_COMBO("q_combo", Tab.CCG_QUINQUE, 3, Balance.COSTS_3, 1, 0xE6E6E6),
        Q_STRONG("q_strong", Tab.CCG_QUINQUE, 3, Balance.COSTS_3, 1, 0xC9822B),
        Q_FLURRY("q_flurry", Tab.CCG_QUINQUE, 3, Balance.COSTS_3, 1, 0xE0503A),
        Q_BLOCK("q_block", Tab.CCG_QUINQUE, 3, Balance.COSTS_3, 1, 0x7A8A9A),
        Q_LUNGE("q_lunge", Tab.CCG_QUINQUE, 3, Balance.COSTS_3, 1, 0x9ED0E6),
        // Агент CCG: охота на гулей
        GHOUL_BANE("ghoul_bane", Tab.CCG_HUNT, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0xE6E6E6),
        KAGUNE_GUARD("kagune_guard", Tab.CCG_SURVIVAL, 3, Balance.COSTS_3, 1, 0x5A86C9),
        RC_SUPPRESS("rc_suppress", Tab.CCG_HUNT, 3, Balance.COSTS_3, 1, 0x4FB0A0),
        FIELD_MEDIC("field_medic", Tab.CCG_SURVIVAL, 3, Balance.COSTS_3, 1, 0x4FB05A),
        RANK("rank", Tab.CCG_HUNT, 3, Balance.COSTS_3, 1, 0xD8C26A),
        RELEASE_GAIN("release_gain", Tab.CCG_HUNT, 3, Balance.COSTS_3, 1, 0x2AA898),
        Q_REACH("q_reach", Tab.CCG_HUNT, 2, Balance.COSTS_2, 1, 0xB04060),
        // У всех: гуль и полугуль — вкладка «Выживание», человек — вкладка «Человек»
        SOFT_LANDING("soft_landing", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0x7FB8D8, Tab.HUMAN, Tab.CCG_SURVIVAL),
        // Секретный навык: у гуля и полугуля во вкладке «Выживание», у человека во вкладке «Человек».
        // Пока не открыт, в меню вместо названия и описания знаки вопроса.
        MIRROR("mirror", Tab.SURVIVAL, 1, Balance.MIRROR_COST, 1, 0x9A1030, Tab.HUMAN, Tab.CCG_SURVIVAL),
        // Особые способности всех классов: открывают клавиши R и Z
        RETRIBUTION("retribution", Tab.SPECIAL, 1, Balance.RETRIBUTION_COST, 1, 0x8C1AE6),
        CHARISMA("charisma", Tab.SPECIAL, 1, Balance.CHARISMA_COST, 1, 0xFFD133),
        FORTUNE("fortune", Tab.SPECIAL, 1, Balance.FORTUNE_COST, 1, 0x3CC878);

        private final String id;
        private final Tab tab;
        private final int levels;
        private final int[] costs;
        private final int firstShownTier;
        private final int color;
        private final List<Tab> extraTabs;

        Branch(String id, Tab tab, int levels, int[] costs, int firstShownTier, int color, Tab... extraTabs) {
            this.id = id;
            this.tab = tab;
            this.levels = levels;
            this.costs = costs;
            this.firstShownTier = firstShownTier;
            this.color = color;
            this.extraTabs = List.of(extraTabs);
        }

        /** Есть ли ветка на этой вкладке (секретная ветка есть сразу на нескольких). */
        public boolean inTab(Tab other) {
            return tab == other || extraTabs.contains(other);
        }

        /** Строка-описание под названием (сейчас только у «Харизмы»), null — нет. */
        public Component flavor() {
            return this == CHARISMA ? Component.translatable("skill.tokyoghoulrpg." + id + ".flavor") : null;
        }

        /** Секретная ветка: до открытия показывается знаками вопроса. */
        public boolean isSecret() {
            return this == MIRROR;
        }

        public String id() {
            return id;
        }

        public Tab tab() {
            return tab;
        }

        public int levels() {
            return levels;
        }

        public int color() {
            return color;
        }

        public int cost(int tier) {
            int index = Math.min(tier, costs.length) - 1;
            return costs[Math.max(0, index)];
        }

        /** Номер уровня, как он показан в меню (у захватов первый навык — это II). */
        public String shownTier(int tier) {
            return roman(tier + firstShownTier - 1);
        }

        public Component title() {
            return Component.translatable("skill.tokyoghoulrpg.branch." + id);
        }

        public Component skillName(int tier) {
            return Component.translatable("skill.tokyoghoulrpg.skill", title(), shownTier(tier));
        }

        /** Что даёт ветка на этом уровне. */
        public Component effect(int level) {
            return effect(level, null);
        }

        /** Описание эффекта; класс нужен веткам, у которых число зависит от класса («Здоровье»). */
        public Component effect(int level, PlayerClass playerClass) {
            String key = "skill.tokyoghoulrpg." + id + ".effect";
            return switch (this) {
                case RAGE -> Component.translatable(key,
                        RageStats.durationTicks(level) / 20,
                        percent(RageStats.damageBonus(level)),
                        percent(RageStats.speedBonus(level)),
                        percent(RageStats.damageReduction(level)));
                case DAMAGE -> Component.translatable(key, percent(SkillEffects.damageBonus(level)));
                case REGEN -> Component.translatable(key, num(SkillEffects.regenAmount(level)),
                        Balance.SKILL_REGEN_INTERVAL / 20);
                case DEFENSE -> Component.translatable(key, percent(SkillEffects.damageReduction(level)));
                case SPEED -> Component.translatable(key, percent(SkillEffects.speedBonus(level)));
                case BITE -> Component.translatable(key, num(SkillEffects.biteHeal(level)),
                        SkillEffects.biteRage(level), seconds(SkillEffects.biteStun(level)),
                        seconds(SkillEffects.biteCooldown(level)));
                case BARRAGE -> Component.translatable(key, num(SkillEffects.barrageDamage(level)),
                        num(SkillEffects.barrageWidth(level)), seconds(SkillEffects.barrageCooldown(level)));
                case DASH -> Component.translatable(key, num(SkillEffects.dashTicks(level) * Balance.DASH_SPEED),
                        SkillEffects.dashCharges(level), seconds(Balance.DASH_COOLDOWN));
                case GRAB -> Component.translatable(key, num(SkillEffects.grabRange(level)));
                case BLOCK -> Component.translatable(key, seconds(SkillEffects.blockStun(level)),
                        num(SkillEffects.blockSlowDivisor(level)));
                case STRONG -> Component.translatable(key, percent(SkillEffects.strongDamageBonus(level)),
                        seconds(SkillEffects.strongCooldown(level)));
                case HUNGER -> Component.translatable(key, num(SkillEffects.hungerFactor(level)));
                case VAMPIRISM -> Component.translatable(key, percent(SkillEffects.vampirism(level)));
                case SECOND_WIND -> Component.translatable(key, Balance.SECOND_WIND_COOLDOWN / 1200);
                case RAGE_GAIN -> Component.translatable(key, percent(SkillEffects.rageGainBonus(level)));
                case LAST_STAND -> Component.translatable(key, percent(Balance.LAST_STAND_HEALTH),
                        Balance.LAST_STAND_RAGE);
                case RAGE_AURA -> Component.translatable(key, num(Balance.RAGE_AURA_RADIUS), roman(level));
                case REACH -> Component.translatable(key, num(SkillEffects.basicReach(level)));
                case PIERCE -> Component.translatable(key, percent(SkillEffects.pierce(level)));
                case BLEED -> Component.translatable(key, num(Balance.BLEED_DAMAGE),
                        SkillEffects.bleedSeconds(level));
                case TOUGHNESS -> Component.translatable(key, percent(SkillEffects.humanToughness(level)));
                case VITALITY -> {
                    boolean ghoul = playerClass != null && playerClass.hasKagune();
                    double bonus = SkillEffects.vitality(level, ghoul);
                    yield Component.translatable(key, num(bonus / 2.0),
                            num((SkillEffects.baseHealth(ghoul) + bonus) / 20.0));
                }
                case ATTACK_SPEED -> Component.translatable(key, percent(SkillEffects.humanAttackSpeed(level)));
                case MINING -> Component.translatable(key, percent(SkillEffects.humanMining(level)));
                case WEAPON_DAMAGE -> Component.translatable(key, percent(SkillEffects.humanWeaponDamage(level)));
                case HUMAN_DASH -> Component.translatable(key, SkillEffects.otherDashCharges(level),
                        seconds(SkillEffects.otherDashCooldown(level)));
                case RELEASE -> Component.translatable(key, SkillEffects.releaseTicks(level) / 20,
                        percent(SkillEffects.releaseDamage(level)), percent(SkillEffects.releaseReduction(level)),
                        percent(SkillEffects.releaseSpeed(level)));
                case Q_COMBO -> Component.translatable(key, percent(SkillEffects.qComboDamage(level)),
                        num(Balance.QUINQUE_FINISHER_MULT + SkillEffects.qFinisherBonus(level)));
                case Q_STRONG -> Component.translatable(key, percent(SkillEffects.qStrongDamage(level)),
                        num(SkillEffects.qStrongReach(level)));
                case Q_FLURRY -> Component.translatable(key, SkillEffects.qFlurryExtraHits(level),
                        seconds(SkillEffects.qFlurryCooldown(level)));
                case Q_BLOCK -> Component.translatable(key, percent(SkillEffects.qParryWindowBonus(level)),
                        seconds(SkillEffects.qParryStun(level)), num(SkillEffects.qBlockSlow(level)));
                case Q_LUNGE -> Component.translatable(key, SkillEffects.qLungeExtraTicks(level),
                        percent(SkillEffects.qLungeDamage(level)), seconds(SkillEffects.qLungeCooldown(level)));
                case RELEASE_GAIN -> Component.translatable(key, percent(SkillEffects.releaseGainBonus(level)));
                case Q_REACH -> Component.translatable(key, num(SkillEffects.qReach(level)));
                case GHOUL_BANE -> Component.translatable(key, percent(SkillEffects.ghoulBane(level)));
                case KAGUNE_GUARD -> Component.translatable(key, percent(SkillEffects.kaguneGuard(level)));
                case RC_SUPPRESS -> Component.translatable(key, seconds(SkillEffects.rcSuppressTicks(level)));
                case FIELD_MEDIC -> Component.translatable(key, num(SkillEffects.fieldMedic(level) / 2.0),
                        Balance.FIELD_MEDIC_INTERVAL / 20);
                case RANK -> Component.translatable(key, percent(SkillEffects.rankBloodBonus(level)));
                case SOFT_LANDING -> Component.translatable(key, percent(1.0 - SkillEffects.fallFactor(level)));
                case MIRROR -> Component.translatable(key, percent(Balance.MIRROR_HEALTH),
                        percent(Balance.MIRROR_FULL_CHANCE), percent(Balance.MIRROR_SHARE));
                case RETRIBUTION -> Component.translatable(key, Balance.AURA_TICKS / 20, num(Balance.RETRIBUTION_DAMAGE / 2.0),
                        Balance.RETRIBUTION_STUN / 20, Balance.RETRIBUTION_SILENCE / 20, Balance.AURA_COOLDOWN / 20);
                case CHARISMA -> Component.translatable(key, Balance.AURA_TICKS / 20, percent(Balance.CHARISMA_HEAL),
                        Balance.CHARISMA_BUFF_TICKS / 20, Balance.AURA_COOLDOWN / 20);
                case FORTUNE -> Component.translatable(key, Balance.FORTUNE_COOLDOWN / 20);
            };
        }
    }

    /** Один навык. У первого навыка ветки requires равно null. */
    public record Skill(Branch branch, String id, int tier, int cost, String requires) {
    }

    private static final List<Skill> ALL = build();

    private SkillTree() {
    }

    private static List<Skill> build() {
        List<Skill> list = new ArrayList<>();
        for (Branch branch : Branch.values()) {
            for (int tier = 1; tier <= branch.levels(); tier++) {
                list.add(new Skill(branch, branch.id() + "_" + tier, tier, branch.cost(tier),
                        tier == 1 ? null : branch.id() + "_" + (tier - 1)));
            }
        }
        return List.copyOf(list);
    }

    /** Вкладки дерева для класса. Пустой список: у класса пока нет дерева. */
    public static List<Tab> tabs(PlayerClass playerClass) {
        List<Tab> list = new ArrayList<>();
        for (Tab tab : Tab.values()) {
            if (tab.isFor(playerClass)) {
                list.add(tab);
            }
        }
        return list;
    }

    public static boolean isAvailable(PlayerClass playerClass, Branch branch) {
        for (Tab tab : Tab.values()) {
            if (branch.inTab(tab) && tab.isFor(playerClass)) {
                return true;
            }
        }
        return false;
    }

    public static List<Branch> branches(Tab tab) {
        List<Branch> list = new ArrayList<>();
        for (Branch branch : Branch.values()) {
            if (branch.inTab(tab)) {
                list.add(branch);
            }
        }
        return list;
    }

    public static List<Skill> branch(Branch branch) {
        List<Skill> list = new ArrayList<>();
        for (Skill skill : ALL) {
            if (skill.branch() == branch) {
                list.add(skill);
            }
        }
        return list;
    }

    public static Skill find(String id) {
        for (Skill skill : ALL) {
            if (skill.id().equals(id)) {
                return skill;
            }
        }
        return null;
    }

    /** Полная стоимость дерева класса на этой сложности. */
    public static int totalCost(PlayerClass playerClass, Difficulty difficulty) {
        int sum = 0;
        for (Skill skill : ALL) {
            if (isAvailable(playerClass, skill.branch())) {
                sum += cost(skill, difficulty);
            }
        }
        return sum;
    }

    /**
     * Сколько очков (по ценам этой сложности) стоят уже открытые навыки дерева этого класса.
     * Навыки, открытые командой бесплатно, тоже считаются: лимит про то, сколько дерева открыто.
     */
    public static int spentCost(PlayerClass playerClass, java.util.Set<String> owned, Difficulty difficulty) {
        int sum = 0;
        for (Skill skill : ALL) {
            if (owned.contains(skill.id()) && isAvailable(playerClass, skill.branch())) {
                sum += cost(skill, difficulty);
            }
        }
        return sum;
    }

    /** Лимит прокачки: 85% стоимости всего дерева класса (с округлением вниз). */
    public static int capCost(PlayerClass playerClass, Difficulty difficulty) {
        return (int) Math.floor(totalCost(playerClass, difficulty) * Balance.SKILL_CAP_SHARE);
    }

    /** Упрётся ли открытие этого навыка в лимит 85%. */
    public static boolean overCap(PlayerClass playerClass, java.util.Set<String> owned, Skill skill, Difficulty difficulty) {
        return spentCost(playerClass, owned, difficulty) + cost(skill, difficulty) > capCost(playerClass, difficulty);
    }

    /** Цена навыка на этой сложности: на сложной в 1,5 раза дороже (с округлением вверх). */
    public static int cost(Skill skill, Difficulty difficulty) {
        if (difficulty == Difficulty.HARD) {
            return (int) Math.ceil(skill.cost() * Balance.HARD_SKILL_COST_FACTOR);
        }
        return skill.cost();
    }

    private static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    /** Число без лишних нулей и с запятой: 2,5 вместо 2.50. */
    private static String num(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        text = text.replaceAll("0+$", "").replaceAll("\\.$", "");
        return text.replace('.', ',');
    }

    private static String seconds(int ticks) {
        return num(ticks / 20.0);
    }

    public static String roman(int number) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number >= 0 && number < numerals.length ? numerals[number] : String.valueOf(number);
    }
}
