package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.AttrUtil;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.UUID;
import com.tokyoghoulrpg.combat.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Эффекты масок. Работают, пока маска надета, и только у гуля и полугуля: маска — вещь гулей.
 * <ul>
 *   <li>Клыкастая — больше урона;</li>
 *   <li>Костяная — меньше входящего урона;</li>
 *   <li>Маска ворона — скорость;</li>
 *   <li>Маска демона — часть нанесённого урона возвращается здоровьем.</li>
 * </ul>
 * Маска «работы мастера» (тег MaskMaster, из мастерской масок) действует сильнее.
 */
public final class MaskEffects {
    public static final String MASTER_TAG = "MaskMaster";
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea2");
    private static final UUID SET_SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea7");
    /** У кого «Облик гуля» уже включён (чтобы сообщить один раз при надевании и снятии). */
    private static final java.util.Set<UUID> SET_ON = new java.util.HashSet<>();

    public enum Kind {
        FANG("fang"),
        BONE("bone"),
        CROW("crow"),
        ONI("oni");

        private final String id;

        Kind(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        /** mask_fang → FANG. */
        public static Kind byItemName(String name) {
            for (Kind kind : values()) {
                if (name.equals("mask_" + kind.id)) {
                    return kind;
                }
            }
            return FANG;
        }
    }

    private MaskEffects() {
    }

    public static boolean isMaster(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(MASTER_TAG);
    }

    public static ItemStack makeMaster(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(MASTER_TAG, true);
        return stack;
    }

    /** Сила эффекта: 0 — обычная маска, 1 — работа мастера. */
    public static double value(Kind kind, int tier) {
        return switch (kind) {
            case FANG -> Balance.MASK_FANG_DAMAGE[tier];
            case BONE -> Balance.MASK_BONE_DEFENSE[tier];
            case CROW -> Balance.MASK_CROW_SPEED[tier];
            case ONI -> Balance.MASK_ONI_LIFESTEAL[tier];
        };
    }

    public static int percent(Kind kind, int tier) {
        return (int) Math.round(value(kind, tier) * 100);
    }

    /** Какую маску игрок носит сейчас и её сила; null — маски нет или она не действует (не гуль). */
    private static Kind worn(Player player, PlayerData data) {
        if (data == null || !data.getPlayerClass().hasKagune()) {
            return null;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof MaskItem mask ? mask.kind() : null;
    }

    private static int tier(Player player) {
        return isMaster(player.getItemBySlot(EquipmentSlot.HEAD)) ? 1 : 0;
    }

    private static double bonus(Player player, PlayerData data, Kind wanted) {
        return worn(player, data) == wanted ? value(wanted, tier(player)) : 0.0;
    }

    public static double damageFactor(Player player, PlayerData data) {
        return (1.0 + bonus(player, data, Kind.FANG)) * (fullSet(player, data) ? 1.0 + Balance.GHOUL_SET_DAMAGE : 1.0);
    }

    public static double defenseFactor(Player player, PlayerData data) {
        return (1.0 - bonus(player, data, Kind.BONE)) * (fullSet(player, data) ? 1.0 - Balance.GHOUL_SET_DEFENSE : 1.0);
    }

    /**
     * «Облик гуля»: на гуле или полугуле любая маска и весь комплект одежды гуля (плащ, штаны, ботинки).
     * Работает и на клиенте (для надписи на экране).
     */
    public static boolean fullSet(Player player, PlayerData data) {
        return worn(player, data) != null
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof GhoulClothItem
                && player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof GhoulClothItem
                && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof GhoulClothItem;
    }

    public static double lifesteal(Player player, PlayerData data) {
        return bonus(player, data, Kind.ONI);
    }

    /** Каждый тик: скорость от маски ворона и «Облик гуля». */
    public static void tick(ServerPlayer player, PlayerData data) {
        boolean set = fullSet(player, data);
        UUID id = player.getUUID();
        if (set && SET_ON.add(id)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ghoul_set.on")
                    .withStyle(ChatFormatting.DARK_RED), true);
            Effects.particles(player, Effects.RAGE, 25, 0.5, 0.03);
            Effects.sound(player, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.6F);
        } else if (!set && SET_ON.remove(id)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ghoul_set.off")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        if (set) {
            // ночное зрение: продлеваем заранее, чтобы экран не мигал
            MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
            if (vision == null || vision.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
            }
            AttrUtil.set(player, Attributes.MOVEMENT_SPEED, SET_SPEED_ID, "tg_ghoul_set_speed", Balance.GHOUL_SET_SPEED,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, SET_SPEED_ID);
        }
        double speed = bonus(player, data, Kind.CROW);
        if (speed > 0.0) {
            AttrUtil.set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "tg_mask_speed", speed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else {
            AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        }
    }
}
