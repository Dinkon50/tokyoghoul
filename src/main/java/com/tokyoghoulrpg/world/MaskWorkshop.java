package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.item.MaskEffects;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModBlocks;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Мастерская масок: любую из четырёх масок можно заказать за очки крови.
 * <ul>
 *   <li>обычная — как скрафченная, MASK_PRICE очков;</li>
 *   <li>работа мастера — эффект сильнее, маска блестит, MASK_MASTER_PRICE очков.</li>
 * </ul>
 * Заказывать может любой класс (например, в подарок гулю из клана), но эффект маски действует
 * только у гуля и полугуля.
 */
public final class MaskWorkshop {
    private MaskWorkshop() {
    }

    public static void order(ServerPlayer player, BlockPos pos, int mask, boolean master) {
        PlayerData data = PlayerData.get(player);
        if (data == null || mask < 0 || mask >= ModItems.MASKS.size()) {
            return;
        }
        // стоять рядом с настоящей мастерской
        if (!player.level().isLoaded(pos) || !player.level().getBlockState(pos).is(ModBlocks.MASK_WORKSHOP.get())
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                > Balance.MASK_WORKSHOP_REACH * Balance.MASK_WORKSHOP_REACH) {
            return;
        }
        int price = master ? Balance.MASK_MASTER_PRICE : Balance.MASK_PRICE;
        if (data.getBloodPoints() < price) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.mask_workshop.no_blood", price)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        data.addBloodPoints(-price);
        ItemStack stack = new ItemStack(ModItems.MASKS.get(mask).get());
        if (master) {
            MaskEffects.makeMaster(stack);
        }
        Component name = stack.getHoverName();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        NetworkHandler.syncTo(player);
        Effects.sound(player, master ? SoundEvents.ANVIL_USE : SoundEvents.VILLAGER_WORK_LEATHERWORKER, 0.8F, 1.0F);
        Effects.particles(player, master ? ParticleTypes.ENCHANT : ParticleTypes.CRIT, 20, 0.5, 0.1);
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.mask_workshop.done", name, price));
    }
}
