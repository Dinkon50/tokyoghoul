package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.KaguneType;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Пассивный бонус от типа кагуне. Действует всегда, даже когда кагуне убрано: это свойство организма.
 * Только у гуля и полугуля; у других классов тип сбрасывается при смене класса.
 */
public final class KaguneTypeBonus {
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea0");
    private static final UUID KNOCKBACK_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea1");

    private KaguneTypeBonus() {
    }

    public static KaguneType type(PlayerData data) {
        if (data == null || !data.getPlayerClass().hasKagune()) {
            return KaguneType.NONE;
        }
        return data.getKaguneType();
    }

    /** Множитель регенерации (регенерация гуля и ветка «Регенерация»). */
    public static double regenFactor(PlayerData data) {
        return type(data) == KaguneType.RINKAKU ? 1.0 + Balance.RINKAKU_REGEN : 1.0;
    }

    /** Множитель исходящего урона. */
    public static double damageFactor(PlayerData data) {
        return type(data) == KaguneType.BIKAKU ? 1.0 + Balance.BIKAKU_DAMAGE : 1.0;
    }

    /** Множитель входящего урона. */
    public static double defenseFactor(PlayerData data) {
        return switch (type(data)) {
            case KOUKAKU -> 1.0 - Balance.KOUKAKU_DEFENSE;
            case BIKAKU -> 1.0 - Balance.BIKAKU_DEFENSE;
            default -> 1.0;
        };
    }

    /** Сколько тиков добавить к рывку. */
    public static int extraDashTicks(PlayerData data) {
        return type(data) == KaguneType.UKAKU ? Balance.UKAKU_DASH_TICKS : 0;
    }

    /** Каждый тик: скорость и сопротивление отбрасыванию. */
    public static void tick(ServerPlayer player, PlayerData data) {
        KaguneType type = type(data);
        double speed = switch (type) {
            case UKAKU -> Balance.UKAKU_SPEED;
            case BIKAKU -> Balance.BIKAKU_SPEED;
            default -> 0.0;
        };
        if (speed > 0.0) {
            AttrUtil.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "tg_kagune_type_speed", speed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        }
        if (type == KaguneType.KOUKAKU) {
            AttrUtil.set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "tg_kagune_type_knockback",
                    Balance.KOUKAKU_KNOCKBACK, AttributeModifier.Operation.ADDITION);
        } else {
            AttrUtil.remove(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
        }
    }

    /** Игрок выбрал тип в меню. Первый выбор бесплатный, смена стоит очков крови. */
    public static void choose(ServerPlayer player, KaguneType type) {
        PlayerData data = PlayerData.get(player);
        if (data == null || type == null || type == KaguneType.NONE || !data.getPlayerClass().hasKagune()) {
            return;
        }
        KaguneType current = data.getKaguneType();
        if (current == type) {
            return;
        }
        if (current != KaguneType.NONE) {
            if (data.getBloodPoints() < Balance.KAGUNE_TYPE_CHANGE_COST) {
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.kagune_type.no_blood",
                        Balance.KAGUNE_TYPE_CHANGE_COST).withStyle(ChatFormatting.RED));
                return;
            }
            data.addBloodPoints(-Balance.KAGUNE_TYPE_CHANGE_COST);
        }
        data.setKaguneType(type);
        tick(player, data);
        NetworkHandler.syncTo(player);
        Effects.particles(player, Effects.RAGE, 30, 0.6, 0.05);
        Effects.sound(player, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.6F, 1.4F);
        player.sendSystemMessage(Component.translatable(current == KaguneType.NONE
                        ? "message.tokyoghoulrpg.kagune_type.chosen" : "message.tokyoghoulrpg.kagune_type.changed",
                Component.translatable(type.translationKey()), Balance.KAGUNE_TYPE_CHANGE_COST));
    }
}
