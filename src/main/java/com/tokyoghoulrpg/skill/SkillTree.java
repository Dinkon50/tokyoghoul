package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.GourmetBalance;
import com.tokyoghoulrpg.combat.RageStats;
import com.tokyoghoulrpg.combat.WingsBalance;
import com.tokyoghoulrpg.data.KaguneVariant;
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
        ABILITIES("abilities", Style.CLASSIC, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        // «Укаку Тоуки» (крылья): вместо вкладок «Способности» и «Ярость, кагуне» две свои, про стрельбу осколками
        WINGS_FIRE("wings_fire", Style.WINGS, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        // «Гурман» Цукиямы: тоже две свои вкладки — про фехтование лентой и про спираль
        GOURMET_FENCE("gourmet_fence", Style.GOURMET, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        SURVIVAL("survival", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        RAGE_KAGUNE("rage_kagune", Style.CLASSIC, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        WINGS_RAGE("wings_rage", Style.WINGS, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        GOURMET_SPIRAL("gourmet_spiral", Style.GOURMET, PlayerClass.GHOUL, PlayerClass.HALF_GHOUL),
        // Человек
        HUMAN("human", PlayerClass.HUMAN),
        // Агент CCG: четыре вкладки, похоже на гуля, но про квинке
        CCG_TRAINING("ccg_training", PlayerClass.CCG_AGENT),
        CCG_QUINQUE("ccg_quinque", PlayerClass.CCG_AGENT),
        CCG_SURVIVAL("ccg_survival", PlayerClass.CCG_AGENT),
        CCG_HUNT("ccg_hunt", PlayerClass.CCG_AGENT),
        // У всех классов: особые способности на своих клавишах
        SPECIAL("special", PlayerClass.GHOUL, PlayerClass.HALF_GHOUL, PlayerClass.HUMAN, PlayerClass.CCG_AGENT);

        /** Для какого вида кагуне вкладка: ANY — для любого, CLASSIC — для щупалец, WINGS — только для крыльев,
         *  GOURMET — только для «Гурмана». */
        enum Style {
            ANY, CLASSIC, WINGS, GOURMET
        }

        private final String id;
        private final Style style;
        private final List<PlayerClass> classes;

        Tab(String id, PlayerClass... classes) {
            this(id, Style.ANY, classes);
        }

        Tab(String id, Style style, PlayerClass... classes) {
            this.id = id;
            this.style = style;
            this.classes = List.of(classes);
        }

        public boolean isFor(PlayerClass playerClass) {
            return isFor(playerClass, KaguneVariant.NONE);
        }

        /** Вкладка есть у этого класса с этим видом кагуне (у остальных вкладок вид не важен). */
        public boolean isFor(PlayerClass playerClass, KaguneVariant variant) {
            if (!classes.contains(playerClass)) {
                return false;
            }
            return switch (style) {
                case ANY -> true;
                case CLASSIC -> variant != KaguneVariant.WINGS && variant != KaguneVariant.GOURMET;
                case WINGS -> variant == KaguneVariant.WINGS;
                case GOURMET -> variant == KaguneVariant.GOURMET;
            };
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
        // «Укаку Тоуки»: стрельба осколками (вкладка «Стрельба»)
        W_RATE("w_rate", Tab.WINGS_FIRE, 3, Balance.COSTS_3, 1, 0xFF8A2A),
        W_CRYSTAL("w_crystal", Tab.WINGS_FIRE, 3, Balance.COSTS_3, 1, 0xFFB23A),
        W_RANGE("w_range", Tab.WINGS_FIRE, 3, Balance.COSTS_3, 1, 0xFFD07A),
        W_AIM("w_aim", Tab.WINGS_FIRE, 3, Balance.COSTS_3, 1, 0xF2E6C8),
        W_BIG("w_big", Tab.WINGS_FIRE, 3, Balance.COSTS_3, 1, 0xFF5A1E),
        W_HAIL("w_hail", Tab.WINGS_FIRE, 3, Balance.COSTS_3, 1, 0xE0503A),
        // «Укаку Тоуки»: электричество (вкладка «Ярость, электричество»)
        W_STATIC("w_static", Tab.WINGS_RAGE, 3, Balance.COSTS_3, 1, 0xFFE35A),
        W_ARC("w_arc", Tab.WINGS_RAGE, 3, Balance.COSTS_3, 1, 0x8FD8FF),
        W_FURY("w_fury", Tab.WINGS_RAGE, 2, Balance.COSTS_2, 1, 0xFF4A2A),
        // «Гурман»: фехтование лентой (вкладка «Фехтование»)
        G_SERIES("g_series", Tab.GOURMET_FENCE, 3, Balance.COSTS_3, 1, 0xB27CF5),
        G_REACH("g_reach", Tab.GOURMET_FENCE, 3, Balance.COSTS_3, 1, 0x9A6AE0),
        G_THRUST("g_thrust", Tab.GOURMET_FENCE, 3, Balance.COSTS_3, 1, 0xD9B8FF),
        G_LUNGE("g_lunge", Tab.GOURMET_FENCE, 3, Balance.COSTS_3, 1, 0x8A3AD8),
        G_WHIRL("g_whirl", Tab.GOURMET_FENCE, 3, Balance.COSTS_3, 1, 0xC060E0),
        // «Гурман»: спираль и изящество (вкладка «Спираль, изящество»)
        G_TIGHT("g_tight", Tab.GOURMET_SPIRAL, 3, Balance.COSTS_3, 1, 0x6A3AA8),
        G_PARRY("g_parry", Tab.GOURMET_SPIRAL, 3, Balance.COSTS_3, 1, 0xE6E0FF),
        G_GRACE("g_grace", Tab.GOURMET_SPIRAL, 3, Balance.COSTS_3, 1, 0xF0C8F0),
        G_MARK("g_mark", Tab.GOURMET_SPIRAL, 3, Balance.COSTS_3, 1, 0xA0208A),
        // Способности
        BITE("bite", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xB01E1E, Tab.WINGS_RAGE, Tab.GOURMET_SPIRAL),
        BARRAGE("barrage", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xE0503A),
        DASH("dash", Tab.ABILITIES, 2, Balance.COSTS_2, 1, 0x9ED0E6, Tab.WINGS_FIRE, Tab.GOURMET_FENCE),
        // Без прокачки захваты достают на 10 блоков: это уровень I. Навыки ветки показаны как II-IV (15, 20, 25 блоков).
        GRAB("grab", Tab.ABILITIES, 3, Balance.COSTS_3, 2, 0x8E5AC9),
        BLOCK("block", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0x7A8A9A, Tab.WINGS_FIRE, Tab.GOURMET_FENCE),
        STRONG("strong", Tab.ABILITIES, 3, Balance.COSTS_3, 1, 0xC9822B),
        // Выживание
        HUNGER("hunger", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0xA0703A),
        VAMPIRISM("vampirism", Tab.SURVIVAL, 3, Balance.COSTS_3, 1, 0x8A0010),
        SECOND_WIND("second_wind", Tab.SURVIVAL, 1, Balance.COSTS_1, 1, 0xE6D27A, Tab.CCG_SURVIVAL),
        // Ярость и кагуне
        RAGE_GAIN("rage_gain", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0xD01F4A, Tab.WINGS_RAGE, Tab.GOURMET_SPIRAL),
        LAST_STAND("last_stand", Tab.RAGE_KAGUNE, 1, Balance.COSTS_1, 1, 0xFF5A78, Tab.WINGS_RAGE, Tab.GOURMET_SPIRAL),
        RAGE_AURA("rage_aura", Tab.RAGE_KAGUNE, 2, Balance.COSTS_2, 1, 0x9A2A4A),
        REACH("reach", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0xB04060),
        PIERCE("pierce", Tab.RAGE_KAGUNE, 2, Balance.COSTS_2, 1, 0xC9C9C9, Tab.CCG_HUNT),
        BLEED("bleed", Tab.RAGE_KAGUNE, 3, Balance.COSTS_3, 1, 0x6A0A10, Tab.CCG_HUNT, Tab.WINGS_RAGE, Tab.GOURMET_SPIRAL),
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
        FORTUNE("fortune", Tab.SPECIAL, 1, Balance.FORTUNE_COST, 1, 0x3CC878),
        // «Бессмертие» у всех классов: смертельный удар не убивает, как тотем бессмертия (раз в 15 минут)
        UNDYING("undying", Tab.SPECIAL, 1, Balance.UNDYING_COST, 1, 0xF2C94C),
        // «Выдержка» у всех классов: оглушение короче (кроме оглушения за сломанный блок)
        STUN_RESIST("stun_resist", Tab.SPECIAL, 3, Balance.COSTS_3, 1, 0xD8B070),
        // «Добытчик»: больше очков крови за убийства; «Первая помощь»: аптечки сильнее и чаще
        SCAVENGER("scavenger", Tab.SPECIAL, 3, Balance.COSTS_3, 1, 0xC04050),
        FIRST_AID("first_aid", Tab.SPECIAL, 2, Balance.COSTS_2, 1, 0xE8E8E8);

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
                case DEFENSE -> Component.translatable(key, percent(SkillEffects.damageReduction(level)),
                        percent(SkillEffects.projectileDefense(level)));
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
                case TOUGHNESS -> Component.translatable(key, percent(SkillEffects.humanToughness(level)),
                        percent(SkillEffects.projectileDefense(level)));
                case VITALITY -> {
                    boolean ghoul = playerClass != null && playerClass.hasKagune();
                    double bonus = SkillEffects.vitality(level, ghoul);
                    yield Component.translatable(key, num(bonus / 2.0),
                            num((SkillEffects.baseHealth(playerClass) + bonus) / 20.0));
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
                case STUN_RESIST -> Component.translatable(key, percent(SkillEffects.stunResist(level)));
                case SCAVENGER -> Component.translatable(key, percent(SkillEffects.scavenger(level)));
                case FIRST_AID -> Component.translatable(key, percent(SkillEffects.firstAidHeal(level)),
                        seconds(SkillEffects.medkitCooldown(level)));
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
                case UNDYING -> Component.translatable(key, Balance.UNDYING_COOLDOWN / 1200);
                case W_RATE -> Component.translatable(key, num(WingsEffects.shotsPerSecond(level)));
                case W_CRYSTAL -> Component.translatable(key, percent(WingsEffects.crystalBonus(level)),
                        WingsEffects.pierce(level) > 0 ? " и пробивают одну цель" : "");
                case W_RANGE -> Component.translatable(key, num(WingsEffects.range(level)));
                case W_AIM -> Component.translatable(key, num(WingsEffects.spread(level)),
                        num(WingsEffects.spreadMax(level)));
                case W_BIG -> Component.translatable(key, num(WingsEffects.bigDamage(level)),
                        seconds(WingsEffects.bigCooldown(level)),
                        WingsEffects.bigSplash(level) > 0 ? ", разлетается осколками (радиус " + num(WingsEffects.bigSplash(level)) + " бл.)" : "");
                case W_HAIL -> Component.translatable(key, WingsEffects.hailPellets(level),
                        num(WingsEffects.hailRange(level)), num(WingsEffects.hailDamage(level)),
                        seconds(WingsEffects.hailCooldown(level)));
                case W_STATIC -> Component.translatable(key, WingsEffects.hitsPerRage(level), WingsBalance.RAGE_PER_GROUP);
                case W_ARC -> Component.translatable(key, percent(WingsEffects.arcChance(level)),
                        percent(WingsBalance.ARC_SHARE), num(WingsBalance.ARC_RADIUS));
                case W_FURY -> Component.translatable(key, WingsEffects.furyShards(level),
                        level >= 2 ? percent(WingsBalance.FURY_RATE - 1.0) : "0%");
                case G_SERIES -> Component.translatable(key, GourmetEffects.seriesLength(level),
                        num(GourmetEffects.hitsPerSecond(level)));
                case G_REACH -> Component.translatable(key, num(GourmetEffects.reach(level)));
                case G_THRUST -> Component.translatable(key, num(GourmetEffects.thrustDamage(level)),
                        GourmetEffects.thrustTargets(level));
                case G_LUNGE -> Component.translatable(key, num(GourmetEffects.lungeDamage(level)),
                        num(GourmetEffects.lungeDistance(level)), seconds(GourmetEffects.lungeCooldown(level)));
                case G_WHIRL -> Component.translatable(key, num(GourmetEffects.whirlDamage(level)),
                        num(GourmetEffects.whirlRadius(level)), seconds(GourmetEffects.whirlCooldown(level)));
                case G_TIGHT -> Component.translatable(key, percent(GourmetEffects.tightReduction(level)),
                        GourmetEffects.tightPush(level) ? ", атакующего отбрасывает" : "");
                case G_PARRY -> Component.translatable(key, seconds(GourmetEffects.parryWindow(level)),
                        num(GourmetEffects.parryDamage(level)),
                        GourmetEffects.parryStun(level) > 0
                                ? ", оглушение " + seconds(GourmetEffects.parryStun(level)) + " с" : "");
                case G_GRACE -> Component.translatable(key, percent(GourmetEffects.graceDamage(level)),
                        GourmetBalance.CLEAN_TICKS / 20, GourmetEffects.graceRage(level));
                case G_MARK -> Component.translatable(key, percent(GourmetEffects.markBonus(level)),
                        GourmetBalance.MARK_TICKS / 20);
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
        return tabs(playerClass, KaguneVariant.NONE);
    }

    /** Вкладки дерева для класса с этим видом кагуне: у «Укаку Тоуки» свои две вкладки про стрельбу. */
    public static List<Tab> tabs(PlayerClass playerClass, KaguneVariant variant) {
        List<Tab> list = new ArrayList<>();
        for (Tab tab : Tab.values()) {
            if (tab.isFor(playerClass, variant)) {
                list.add(tab);
            }
        }
        return list;
    }

    public static boolean isAvailable(PlayerClass playerClass, Branch branch) {
        return isAvailable(playerClass, KaguneVariant.NONE, branch);
    }

    public static boolean isAvailable(PlayerClass playerClass, KaguneVariant variant, Branch branch) {
        for (Tab tab : Tab.values()) {
            if (branch.inTab(tab) && tab.isFor(playerClass, variant)) {
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
        return totalCost(playerClass, KaguneVariant.NONE, difficulty);
    }

    public static int totalCost(PlayerClass playerClass, KaguneVariant variant, Difficulty difficulty) {
        int sum = 0;
        for (Skill skill : ALL) {
            if (isAvailable(playerClass, variant, skill.branch())) {
                sum += cost(skill, difficulty, playerClass);
            }
        }
        return sum;
    }

    /**
     * Сколько очков (по ценам этой сложности) стоят уже открытые навыки дерева этого класса.
     * Навыки, открытые командой бесплатно, тоже считаются: лимит про то, сколько дерева открыто.
     */
    public static int spentCost(PlayerClass playerClass, java.util.Set<String> owned, Difficulty difficulty) {
        return spentCost(playerClass, KaguneVariant.NONE, owned, difficulty);
    }

    public static int spentCost(PlayerClass playerClass, KaguneVariant variant, java.util.Set<String> owned,
            Difficulty difficulty) {
        int sum = 0;
        for (Skill skill : ALL) {
            if (owned.contains(skill.id()) && isAvailable(playerClass, variant, skill.branch())) {
                sum += cost(skill, difficulty, playerClass);
            }
        }
        return sum;
    }

    /** Лимит прокачки: 85% стоимости всего дерева класса (с округлением вниз). */
    public static int capCost(PlayerClass playerClass, Difficulty difficulty) {
        return capCost(playerClass, KaguneVariant.NONE, difficulty);
    }

    public static int capCost(PlayerClass playerClass, KaguneVariant variant, Difficulty difficulty) {
        return (int) Math.floor(totalCost(playerClass, variant, difficulty) * Balance.SKILL_CAP_SHARE);
    }

    /** Упрётся ли открытие этого навыка в лимит 85%. */
    public static boolean overCap(PlayerClass playerClass, java.util.Set<String> owned, Skill skill, Difficulty difficulty) {
        return overCap(playerClass, KaguneVariant.NONE, owned, skill, difficulty);
    }

    public static boolean overCap(PlayerClass playerClass, KaguneVariant variant, java.util.Set<String> owned, Skill skill,
            Difficulty difficulty) {
        return spentCost(playerClass, variant, owned, difficulty) + cost(skill, difficulty, playerClass)
                > capCost(playerClass, variant, difficulty);
    }

    /** Цена навыка на этой сложности: на сложной в 1,5 раза дороже (с округлением вверх). */
    public static int cost(Skill skill, Difficulty difficulty) {
        return cost(skill, difficulty, null);
    }

    /** Цена для класса: у полугуля навыки в 1,2 раза дороже (с округлением), потом сложность. */
    public static int cost(Skill skill, Difficulty difficulty, PlayerClass playerClass) {
        int base = skill.cost();
        if (playerClass == PlayerClass.HALF_GHOUL) {
            base = (int) Math.round(base * Balance.HALF_GHOUL_COST);
        }
        if (difficulty == Difficulty.HARD) {
            return (int) Math.ceil(base * Balance.HARD_SKILL_COST_FACTOR);
        }
        return base;
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
