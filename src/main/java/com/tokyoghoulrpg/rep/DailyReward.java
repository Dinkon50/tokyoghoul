package com.tokyoghoulrpg.rep;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModItems;
import java.time.LocalDate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Ежедневная награда за вход. Считается по реальному календарю (дата на компьютере сервера):
 * зашёл в новый день — получил награду. Заходишь каждый день подряд — серия растёт и награда больше
 * (дни 1-7, потом снова с 1-го, но серия в «Профиле» считается дальше). Пропустил день — серия с начала.
 * Если игра идёт через полночь, награда за новый день придёт сама.
 */
public final class DailyReward {
    private DailyReward() {
    }

    public static long today() {
        return LocalDate.now().toEpochDay();
    }

    /** Какой это день недельного круга (1-7) для серии. */
    public static int cycleDay(int streak) {
        return (Math.max(1, streak) - 1) % Balance.DAILY_BLOOD.length + 1;
    }

    /** Проверить и выдать (при входе и раз в минуту). */
    public static void check(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass()) {
            return;
        }
        long today = today();
        long last = data.getDailyDay();
        if (last == today) {
            return;
        }
        int streak = last == today - 1 ? data.getDailyStreak() + 1 : 1;
        data.setDaily(today, streak);
        Achievements.atLeast(player, "daily_streak", streak);
        int day = cycleDay(streak);
        int blood = Balance.DAILY_BLOOD[day - 1];
        data.addBloodPoints(blood);
        int points = Balance.DAILY_POINTS[day - 1] > 0 ? data.earnSkillPoints(Balance.DAILY_POINTS[day - 1]) : 0;
        Component extra = Component.empty();
        if (day == Balance.DAILY_MEDKITS_DAY || day == Balance.DAILY_SMOKE_DAY) {
            give(player, new ItemStack(ModItems.MEDKIT.get()));
            extra = Component.translatable("message.tokyoghoulrpg.daily.item", new ItemStack(ModItems.MEDKIT.get()).getHoverName());
        }
        if (day == Balance.DAILY_SMOKE_DAY) {
            give(player, new ItemStack(ModItems.SMOKE_BOMB.get()));
            extra = Component.translatable("message.tokyoghoulrpg.daily.items",
                    new ItemStack(ModItems.MEDKIT.get()).getHoverName(), new ItemStack(ModItems.SMOKE_BOMB.get()).getHoverName());
        }
        NetworkHandler.syncTo(player);
        player.sendSystemMessage(Component.translatable(points > 0 ? "message.tokyoghoulrpg.daily.reward_points"
                        : "message.tokyoghoulrpg.daily.reward", streak, day, blood, points)
                .withStyle(ChatFormatting.GOLD).append(extra));
        if (streak > 1 && day == 1) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.daily.week").withStyle(ChatFormatting.YELLOW));
        }
        Effects.sound(player, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.3F);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
