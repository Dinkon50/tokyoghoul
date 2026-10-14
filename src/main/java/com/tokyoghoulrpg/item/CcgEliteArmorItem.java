package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Броня особого класса CCG: шлем 4, нагрудник 9, поножи 7, ботинки 4 (всего 24 против 20 у незерита),
 * прочность брони 4 (у незерита 3), сопротивление отбрасыванию 0,15 за часть, не горит.
 * Надеть может только агент CCG: у других классов она сама снимается в инвентарь.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public class CcgEliteArmorItem extends ArmorItem {
    public CcgEliteArmorItem(Type type, Properties properties) {
        super(ModArmorMaterials.CCG_ELITE, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.ccg_elite.tooltip").withStyle(ChatFormatting.AQUA));
    }

    /** Раз в секунду: не-агент в этой броне — она переходит в инвентарь (или падает рядом, если места нет). */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0 || player.isCreative()) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || data.getPlayerClass() == PlayerClass.CCG_AGENT) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.getItem() instanceof CcgEliteArmorItem) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(worn)) {
                    player.drop(worn, false);
                }
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ccg_elite.only_ccg"), true);
            }
        }
    }
}
