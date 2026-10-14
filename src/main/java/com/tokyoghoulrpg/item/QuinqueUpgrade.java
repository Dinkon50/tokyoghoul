package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Уровень квинке (0-III) и «привязка» после улучшения. Хранится в самом предмете:
 * <ul>
 *   <li>QLevel — уровень: каждый следующий требует ядро следующего качества (I, II, III);</li>
 *   <li>QLockedUntil — до какого тика мира квинке нельзя выбросить или положить куда-то, кроме инвентаря.</li>
 * </ul>
 */
public final class QuinqueUpgrade {
    public static final int MAX_LEVEL = 3;
    private static final String LEVEL = "QLevel";
    private static final String LOCKED_UNTIL = "QLockedUntil";

    private QuinqueUpgrade() {
    }

    /** Квинке, которую можно улучшать ядрами (учебную нельзя). */
    public static boolean upgradable(ItemStack stack) {
        return stack.getItem() instanceof QuinqueItem quinque && quinque.type() != QuinqueType.TRAINING;
    }

    public static int level(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, Math.min(MAX_LEVEL, tag.getInt(LEVEL)));
    }

    public static void setLevel(ItemStack stack, int level) {
        stack.getOrCreateTag().putInt(LEVEL, Math.max(0, Math.min(MAX_LEVEL, level)));
    }

    /** Прибавка к урону от уровня квинке. */
    public static double damageBonus(ItemStack stack) {
        return upgradable(stack) ? Balance.QUINQUE_LEVEL_BONUS[level(stack)] : 0.0;
    }

    public static long lockedUntil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(LOCKED_UNTIL);
    }

    /** Привязана ли квинке сейчас (после улучшения 10 минут). */
    public static boolean isLocked(ItemStack stack, long now) {
        return stack.getItem() instanceof QuinqueItem && lockedUntil(stack) > now;
    }

    public static void lock(ItemStack stack, long until) {
        stack.getOrCreateTag().putLong(LOCKED_UNTIL, until);
    }

    // ------------------------------------------------------------------ ядро

    private static final String OWNER = "CoreOwner";
    private static final String OWNER_NAME = "CoreOwnerName";

    /** Ядро, привязанное к выбившему его игроку. */
    public static ItemStack boundCore(ItemStack core, Player owner) {
        CompoundTag tag = core.getOrCreateTag();
        tag.putUUID(OWNER, owner.getUUID());
        tag.putString(OWNER_NAME, owner.getGameProfile().getName());
        return core;
    }

    /** Кто выбил ядро (null — ничьё, например выдано командой). */
    public static UUID coreOwner(ItemStack core) {
        CompoundTag tag = core.getTag();
        return tag != null && tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
    }

    public static String coreOwnerName(ItemStack core) {
        CompoundTag tag = core.getTag();
        return tag == null ? "" : tag.getString(OWNER_NAME);
    }

    public static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "0";
        };
    }
}
