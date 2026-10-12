package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Balance;
import java.util.ArrayList;
import java.util.List;

/**
 * Дерево навыков гуля и полугуля. Пока в нём одна ветка: улучшения ярости.
 * Каждый уровень улучшает сразу четыре параметра ярости (длительность, урон, скорость, защиту):
 * от минимума из спеки до максимума на последнем уровне.
 */
public final class SkillTree {
    /** Один навык. У первого навыка ветки requires равно null. */
    public record Skill(String id, int tier, int cost, String requires) {
    }

    private static final List<Skill> RAGE = buildRage();

    private SkillTree() {
    }

    private static List<Skill> buildRage() {
        List<Skill> list = new ArrayList<>();
        for (int tier = 1; tier <= Balance.RAGE_LEVEL_MAX; tier++) {
            int index = Math.min(tier, Balance.RAGE_UPGRADE_COSTS.length) - 1;
            list.add(new Skill("rage_" + tier, tier, Balance.RAGE_UPGRADE_COSTS[index],
                    tier == 1 ? null : "rage_" + (tier - 1)));
        }
        return List.copyOf(list);
    }

    public static List<Skill> rageBranch() {
        return RAGE;
    }

    public static Skill find(String id) {
        for (Skill skill : RAGE) {
            if (skill.id().equals(id)) {
                return skill;
            }
        }
        return null;
    }

    public static String roman(int number) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return number >= 0 && number < numerals.length ? numerals[number] : String.valueOf(number);
    }
}
