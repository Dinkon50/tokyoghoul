package com.tokyoghoulrpg.style;

import com.tokyoghoulrpg.combat.LockManager;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Способности стилей на клавише C: просто C — первая, Shift + C — вторая. Работают с любым основным классом. */
public final class StyleAbilities {
    private StyleAbilities() {
    }

    public static void use(ServerPlayer player, PlayerData data, boolean second) {
        if (data == null || !player.isAlive() || LockManager.isLocked(player)) {
            return;
        }
        FightStyle style = data.style().active();
        if (style == null) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.style.none"), true);
            return;
        }
        long now = player.level().getGameTime();
        switch (style) {
            case FIGHTER -> {
                if (second) {
                    FighterStyle.counterStance(player, data, now);
                } else {
                    FighterStyle.flurry(player, data, now);
                }
            }
            case SHADOW -> {
                if (second) {
                    ShadowStyle.decoy(player, data, now);
                } else {
                    ShadowStyle.smoke(player, data, now);
                }
            }
            case REAPER -> {
                if (second) {
                    ReaperStyle.mark(player, data, now);
                } else {
                    ReaperStyle.execution(player, data, now);
                }
            }
        }
    }

    public static Component abilityName(FightStyle style, int number) {
        return Component.translatable(style.key("ability" + number));
    }

    static void cooldownMessage(ServerPlayer player, Component name, long ticksLeft) {
        String seconds = String.format(Locale.ROOT, "%.1f", ticksLeft / 20.0);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cooldown", name, seconds), true);
    }
}
