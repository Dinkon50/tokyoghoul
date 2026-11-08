package com.tokyoghoulrpg.style;

import net.minecraft.nbt.CompoundTag;

/**
 * Стили боя игрока (часть PlayerData): какой стиль активен, какие открыты, очки мастерства,
 * уровни веток каждого стиля, откаты способностей и вызова босса. Плюс то, что живёт только в бою
 * (серия ударов, окно ответного удара, стойка, идущий «Шквал», дымовая завеса) — это не сохраняется.
 */
public class StyleData {
    private static final int STYLES = FightStyle.values().length;

    private FightStyle active = null;
    private int unlocked = 0;
    private int wins = 0;
    private int mastery = 0;
    private int killProgress = 0;
    private final int[][] levels = new int[STYLES][3];
    private long ability1Ready = 0L;
    private long ability2Ready = 0L;
    private long challengeReady = 0L;

    // ---------- только в бою
    public int comboStacks = 0;
    public long comboLastTick = 0L;
    public long riposteUntil = 0L;
    public long stanceUntil = 0L;
    public int flurryTarget = -1;
    public int flurryHitsLeft = 0;
    public long flurryNextTick = 0L;
    /** Тень: до какого тика длится «Дымовая завеса». */
    public long smokeUntil = 0L;

    public FightStyle active() {
        return active;
    }

    public void setActive(FightStyle style) {
        this.active = style;
        comboStacks = 0;
        riposteUntil = 0L;
        stanceUntil = 0L;
        flurryHitsLeft = 0;
        smokeUntil = 0L;
    }

    public boolean isActive(FightStyle style) {
        return active == style;
    }

    public boolean isUnlocked(FightStyle style) {
        return (unlocked & style.bit()) != 0;
    }

    public boolean anyUnlocked() {
        return unlocked != 0;
    }

    public void unlock(FightStyle style) {
        unlocked |= style.bit();
    }

    /** Побеждал ли уже босса этого стиля (первая победа даёт очки мастерства). */
    public boolean hasWon(FightStyle style) {
        return (wins & style.bit()) != 0;
    }

    public void markWin(FightStyle style) {
        wins |= style.bit();
    }

    public int mastery() {
        return mastery;
    }

    public void setMastery(int points) {
        this.mastery = Math.max(0, points);
    }

    public void addMastery(int points) {
        setMastery(mastery + points);
    }

    public int killProgress() {
        return killProgress;
    }

    /** Убит враг: прогресс к следующему очку. Возвращает, сколько очков набралось (0 или 1). */
    public int countKill() {
        killProgress++;
        if (killProgress >= StyleBalance.KILLS_PER_POINT) {
            killProgress = 0;
            mastery++;
            return 1;
        }
        return 0;
    }

    public int level(FightStyle style, int branch) {
        return style == null || branch < 0 || branch > 2 ? 0 : levels[style.ordinal()][branch];
    }

    /** Уровень ветки активного стиля (0, если активен другой стиль или никакой). */
    public int activeLevel(FightStyle style, int branch) {
        return active == style ? level(style, branch) : 0;
    }

    public void setLevel(FightStyle style, int branch, int level) {
        levels[style.ordinal()][branch] = Math.max(0, Math.min(3, level));
    }

    /** Сколько очков вложено в стиль. */
    public int spent(FightStyle style) {
        int sum = 0;
        for (int b = 0; b < 3; b++) {
            for (int l = 0; l < levels[style.ordinal()][b]; l++) {
                sum += StyleBalance.COSTS[l];
            }
        }
        return sum;
    }

    public long ability1Ready() {
        return ability1Ready;
    }

    public long ability2Ready() {
        return ability2Ready;
    }

    public void setAbility1Ready(long tick) {
        ability1Ready = tick;
    }

    public void setAbility2Ready(long tick) {
        ability2Ready = tick;
    }

    public long challengeReady() {
        return challengeReady;
    }

    public void setChallengeReady(long tick) {
        challengeReady = tick;
    }

    public void reset() {
        active = null;
        unlocked = 0;
        wins = 0;
        mastery = 0;
        killProgress = 0;
        for (int[] row : levels) {
            java.util.Arrays.fill(row, 0);
        }
        ability1Ready = 0L;
        ability2Ready = 0L;
        challengeReady = 0L;
        comboStacks = 0;
        riposteUntil = 0L;
        stanceUntil = 0L;
        flurryHitsLeft = 0;
        smokeUntil = 0L;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("active", active == null ? "" : active.id());
        tag.putInt("unlocked", unlocked);
        tag.putInt("wins", wins);
        tag.putInt("mastery", mastery);
        tag.putInt("kill_progress", killProgress);
        for (FightStyle style : FightStyle.values()) {
            tag.putIntArray("levels_" + style.id(), levels[style.ordinal()].clone());
        }
        tag.putLong("ability1", ability1Ready);
        tag.putLong("ability2", ability2Ready);
        tag.putLong("challenge", challengeReady);
        return tag;
    }

    /** Для клиента: сохраняемое плюс серия ударов и стойка (для панели). */
    public CompoundTag saveSync() {
        CompoundTag tag = save();
        tag.putInt("combo", comboStacks);
        tag.putLong("combo_last", comboLastTick);
        tag.putLong("riposte", riposteUntil);
        tag.putLong("stance", stanceUntil);
        tag.putLong("smoke", smokeUntil);
        return tag;
    }

    public void load(CompoundTag tag) {
        active = FightStyle.byId(tag.getString("active"));
        unlocked = tag.getInt("unlocked");
        wins = tag.getInt("wins");
        mastery = Math.max(0, tag.getInt("mastery"));
        killProgress = tag.getInt("kill_progress");
        for (FightStyle style : FightStyle.values()) {
            int[] saved = tag.getIntArray("levels_" + style.id());
            for (int b = 0; b < 3; b++) {
                levels[style.ordinal()][b] = b < saved.length ? Math.max(0, Math.min(3, saved[b])) : 0;
            }
        }
        if (active != null && !isUnlocked(active)) {
            active = null;
        }
        ability1Ready = tag.getLong("ability1");
        ability2Ready = tag.getLong("ability2");
        challengeReady = tag.getLong("challenge");
        if (tag.contains("combo")) {
            comboStacks = tag.getInt("combo");
            comboLastTick = tag.getLong("combo_last");
            riposteUntil = tag.getLong("riposte");
            stanceUntil = tag.getLong("stance");
            smokeUntil = tag.getLong("smoke");
        }
    }
}
