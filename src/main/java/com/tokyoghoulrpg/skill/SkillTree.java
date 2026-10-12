package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.RageStats;
import com.tokyoghoulrpg.data.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

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
        HUMAN("human", PlayerClass.HUMAN);

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
        // Без прокачки захваты бьют на 5 блоков: это уровень I. Единственный навык ветки показан как II.
        GRAB("grab", Tab.ABILITIES, 1, Balance.COSTS_1, 2, 0x8E5AC9),
        BLOCK("block", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0x7A8A9A),
        STRONG("strong", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xC9822B),
        // Выживание
        HUNGER("hunger", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0xA0703A),
        VAMPIRISM("vampirism", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0x8A0010),
        SECOND_WIND("second_wind", Tab.SURVIVAL, 1, Balance.COSTS_1, 1, 0xE6D27A),
        // Ярость и кагуне
        RAGE_GAIN("rage_gain", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0xD01F4A),
        LAST_STAND("last_stand", Tab.RAGE_KAGUNE, 1, Balance.COSTS_1, 1, 0xFF5A78),
        RAGE_AURA("rage_aura", Tab.RAGE_KAGUNE, 2, Balance.COSTS_2, 1, 0x9A2A4A),
        REACH("reach", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0xB04060),
        PIERCE("pierce", Tab.RAGE_KAGUNE, 2, Balance.COSTS_2, 1, 0xC9C9C9),
        BLEED("bleed", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0x6A0A10),
        // Человек
        TOUGHNESS("toughness", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0x5A86C9),
        VITALITY("vitality", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0xB01E1E),
        ATTACK_SPEED("attack_speed", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0xC9822B),
        MINING("mining", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0x9A9A9A),
        WEAPON_DAMAGE("weapon_damage", Tab.HUMAN, Balance.HUMAN_LEVELS, Balance.HUMAN_COSTS, 1, 0xE0503A),
        HUMAN_DASH("human_dash", Tab.HUMAN, 2, Balance.COSTS_2, 1, 0x9ED0E6);

        private final String id;
        private final Tab tab;
        private final int levels;
        private final int[] costs;
        private final int firstShownTier;
        private final int color;

        Branch(String id, Tab tab, int levels, int[] costs, int firstShownTier, int color) {
            this.id = id;
            this.tab = tab;
            this.levels = levels;
            this.costs = costs;
            this.firstShownTier = firstShownTier;
            this.color = color;
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
                case VITALITY -> Component.translatable(key, num(SkillEffects.humanVitality(level) / 2.0));
                case ATTACK_SPEED -> Component.translatable(key, percent(SkillEffects.humanAttackSpeed(level)));
                case MINING -> Component.translatable(key, percent(SkillEffects.humanMining(level)));
                case WEAPON_DAMAGE -> Component.translatable(key, percent(SkillEffects.humanWeaponDamage(level)));
                case HUMAN_DASH -> Component.translatable(key, SkillEffects.otherDashCharges(level),
                        seconds(SkillEffects.otherDashCooldown(level)));
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
        return branch.tab().isFor(playerClass);
    }

    public static List<Branch> branches(Tab tab) {
        List<Branch> list = new ArrayList<>();
        for (Branch branch : Branch.values()) {
            if (branch.tab() == tab) {
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

    /** Полная стоимость дерева класса. */
    public static int totalCost(PlayerClass playerClass) {
        int sum = 0;
        for (Skill skill : ALL) {
            if (isAvailable(playerClass, skill.branch())) {
                sum += skill.cost();
            }
        }
        return sum;
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
