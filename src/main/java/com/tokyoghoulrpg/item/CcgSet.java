package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * «Защита CCG»: агент CCG в полном комплекте брони CCG — нагрудник, штаны и ботинки, каждая часть
 * из формы CCG или из брони особого класса (можно вперемешку, шлем не нужен: у формы его нет).
 * Входящий урон чуть меньше от всего, а от гулей ещё меньше. Только у агента CCG.
 */
public final class CcgSet {
    private static final Set<UUID> ON = new HashSet<>();

    private CcgSet() {
    }

    private static boolean ccgPiece(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armor
                && (armor.getMaterial() == ModArmorMaterials.CCG_UNIFORM || armor.getMaterial() == ModArmorMaterials.CCG_ELITE);
    }

    /** Работает и на клиенте (надпись на экране). */
    public static boolean fullSet(Player player, PlayerData data) {
        return data != null && data.getPlayerClass() == PlayerClass.CCG_AGENT
                && ccgPiece(player.getItemBySlot(EquipmentSlot.CHEST))
                && ccgPiece(player.getItemBySlot(EquipmentSlot.LEGS))
                && ccgPiece(player.getItemBySlot(EquipmentSlot.FEET));
    }

    /** Множитель входящего урона; fromGhoul — бьёт гуль (NPC, босс или игрок). */
    public static double defenseFactor(Player player, PlayerData data, boolean fromGhoul) {
        if (!fullSet(player, data)) {
            return 1.0;
        }
        return (1.0 - Balance.CCG_SET_DEFENSE) * (fromGhoul ? 1.0 - Balance.CCG_SET_GHOUL_DEFENSE : 1.0);
    }

    /** Каждый тик: сообщить, когда комплект собран или снят. */
    public static void tick(ServerPlayer player, PlayerData data) {
        boolean set = fullSet(player, data);
        UUID id = player.getUUID();
        if (set && ON.add(id)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ccg_set.on")
                    .withStyle(ChatFormatting.AQUA), true);
            Effects.particles(player, ParticleTypes.ELECTRIC_SPARK, 20, 0.5, 0.05);
            Effects.sound(player, SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F, 0.8F);
        } else if (!set && ON.remove(id)) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ccg_set.off")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Строки в подсказку каждой части. */
    public static void tooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.ccg_set.set").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.ccg_set.effect",
                Math.round(Balance.CCG_SET_DEFENSE * 100), Math.round(Balance.CCG_SET_GHOUL_DEFENSE * 100))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.ccg_set.only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
