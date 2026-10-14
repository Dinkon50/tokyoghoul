package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.HalfGhoulNpc;
import com.tokyoghoulrpg.registry.ModItems;
import java.util.Iterator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ядра и привязанные квинке.
 * <ul>
 *   <li>Выпадение ядер: I — 2% с гулей и полугулей, II — 8% с Пожирателя (III — в награде за Кровавого полугуля).
 *       Ядро сразу привязано к тому, кто его выбил.</li>
 *   <li>10 минут после улучшения квинке нельзя выбросить, положить в сундук или любое другое хранилище,
 *       в рамку или на стойку — только инвентарь и руки. При смерти она не выпадает, а остаётся у игрока.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class QuinqueEvents {
    private QuinqueEvents() {
    }

    // ------------------------------------------------------------------ ядра

    /** Убийство игроком: может выпасть ядро. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof GhoulNpc || victim instanceof HalfGhoulNpc) {
            if (killer.getRandom().nextDouble() < Balance.CORE_1_CHANCE) {
                giveCore(killer, 1);
            }
        } else if (victim instanceof GhoulBoss) {
            if (killer.getRandom().nextDouble() < Balance.CORE_2_CHANCE) {
                giveCore(killer, 2);
            }
        }
    }

    /** Выдать ядро, привязанное к игроку. */
    public static void giveCore(ServerPlayer player, int tier) {
        ItemStack core = QuinqueUpgrade.boundCore(new ItemStack(ModItems.core(tier)), player);
        if (!player.getInventory().add(core)) {
            player.drop(core, false);
        }
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.core.dropped", QuinqueUpgrade.roman(tier)));
        Effects.sound(player, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.6F + tier * 0.2F);
    }

    // ------------------------------------------------------------------ привязка квинке

    private static boolean locked(Player player, ItemStack stack) {
        return QuinqueUpgrade.isLocked(stack, player.level().getGameTime());
    }

    private static void denied(Player player) {
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.core.locked"), true);
    }

    /** Выбросить нельзя: квинке возвращается в инвентарь. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getEntity().getItem();
        if (player.level().isClientSide() || !locked(player, stack)) {
            return;
        }
        ItemStack back = stack.copy();
        event.setCanceled(true);
        if (!player.getInventory().add(back)) {
            // инвентарь полон (бросок из курсора): кладём на место предмета в руке, а тот выбрасываем
            ItemStack held = player.getMainHandItem().copy();
            player.getInventory().setItem(player.getInventory().selected, back);
            if (!held.isEmpty()) {
                player.drop(held, false);
            }
        }
        denied(player);
    }

    /** В хранилищах (сундук, бочка, шалкер, эндер-сундук, печь и любые другие окна) квинке не задерживается. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 2 != 0) {
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu) {
            return;
        }
        long now = player.level().getGameTime();
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (QuinqueUpgrade.isLocked(stack, now) && player.getInventory().add(stack.copy())) {
                slot.set(ItemStack.EMPTY);
                slot.setChanged();
                menu.broadcastChanges();
                denied(player);
            }
        }
    }

    /** Рамка и стойка для брони. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if ((event.getTarget() instanceof ItemFrame || event.getTarget() instanceof ArmorStand)
                && locked(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            denied(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if ((event.getTarget() instanceof ItemFrame || event.getTarget() instanceof ArmorStand)
                && locked(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            denied(event.getEntity());
        }
    }

    /** Блоки-хранилища, в которые кладут предмет прямо из руки (горшок, кафедра, полка и т. п.). */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (locked(event.getEntity(), event.getItemStack())
                && event.getLevel().getBlockEntity(event.getPos()) != null) {
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    /** При смерти привязанная квинке не выпадает: ждёт возрождения. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        long now = player.level().getGameTime();
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next().getItem();
            if (QuinqueUpgrade.isLocked(stack, now)) {
                data.addKeptItem(stack.save(new CompoundTag()));
                it.remove();
            }
        }
    }

    /** После возрождения: вернуть сохранённые квинке. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        for (CompoundTag tag : data.takeKeptItems()) {
            ItemStack stack = ItemStack.of(tag);
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }
}
