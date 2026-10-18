package com.tokyoghoulrpg.rep;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.item.GuideBookItem;
import com.tokyoghoulrpg.registry.ModItems;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Выдача книги новичка и её защита. Книга даётся один раз при первом входе (и игрокам из старых
 * сохранений — при следующем входе). Первые 30 минут игры (считается только время в сети):
 * <ul>
 *   <li>выбросить нельзя — книга остаётся в инвентаре;</li>
 *   <li>при смерти не выпадает и возвращается после возрождения;</li>
 *   <li>если её нет в инвентаре (убрал в сундук, сгорела) — появляется снова.</li>
 * </ul>
 * Потом привязка снимается, и книга становится обычным предметом.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class GuideBook {
    private static final String GIVEN = "tgrpg_guide_given";
    private static final String LEFT = "tgrpg_guide_left";
    /** Кому вернуть книгу после возрождения. */
    private static final Set<UUID> RETURN = new HashSet<>();

    private GuideBook() {
    }

    /** Данные, которые переживают смерть игрока. */
    private static CompoundTag persisted(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static boolean protectedNow(Player player) {
        return persisted(player).getInt(LEFT) > 0;
    }

    private static ItemStack newBook(boolean bound) {
        ItemStack stack = new ItemStack(ModItems.GUIDE_BOOK.get());
        if (bound) {
            stack.getOrCreateTag().putBoolean(GuideBookItem.BOUND, true);
        }
        return stack;
    }

    private static boolean hasBook(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GUIDE_BOOK.get())) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.GUIDE_BOOK.get())) {
                return true;
            }
        }
        return player.containerMenu.getCarried().is(ModItems.GUIDE_BOOK.get());
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = persisted(player);
        if (tag.getBoolean(GIVEN)) {
            return;
        }
        tag.putBoolean(GIVEN, true);
        tag.putInt(LEFT, Balance.GUIDE_PROTECT_TICKS);
        give(player, newBook(true));
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.guide.given",
                Balance.GUIDE_PROTECT_TICKS / 1200).withStyle(ChatFormatting.GOLD));
    }

    /** Раз в секунду: отсчёт защиты, возврат пропавшей книги, снятие привязки в конце. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 7 || !player.isAlive()) {
            return;
        }
        CompoundTag tag = persisted(player);
        int left = tag.getInt(LEFT);
        if (left <= 0) {
            return;
        }
        left = Math.max(0, left - 20);
        tag.putInt(LEFT, left);
        if (left == 0) {
            for (ItemStack stack : player.getInventory().items) {
                if (GuideBookItem.isBound(stack)) {
                    stack.getTag().remove(GuideBookItem.BOUND);
                }
            }
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.guide.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!hasBook(player)) {
            give(player, newBook(true));
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.guide.returned",
                    (left + 1199) / 1200).withStyle(ChatFormatting.GOLD));
        }
    }

    /** Выбросить привязанную книгу нельзя — она возвращается в инвентарь. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        if (!GuideBookItem.isBound(stack) || !(event.getPlayer() instanceof ServerPlayer player) || !protectedNow(player)) {
            return;
        }
        event.setCanceled(true);
        give(player, stack.copy());
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.guide.no_drop",
                (persisted(player).getInt(LEFT) + 1199) / 1200).withStyle(ChatFormatting.GOLD), true);
    }

    /** При смерти привязанная книга не выпадает — вернётся после возрождения. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !protectedNow(player)) {
            return;
        }
        boolean removed = event.getDrops().removeIf(item -> item.getItem().is(ModItems.GUIDE_BOOK.get()));
        if (removed) {
            RETURN.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RETURN.remove(player.getUUID()) && !hasBook(player)) {
            give(player, newBook(protectedNow(player)));
        }
    }
}
