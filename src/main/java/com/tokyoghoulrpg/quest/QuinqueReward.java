package com.tokyoghoulrpg.quest;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Квинке агента CCG.
 * <ul>
 *   <li>Стартовая: став агентом, игрок один раз получает Kura.</li>
 *   <li>Финальное задание «Выбей две квинке»: за каждого убитого Старшего следователя
 *       с шансом 80% выпадает Narukami, с шансом 20% — IXA. Если такая квинке уже лежит в инвентаре,
 *       она не выдаётся. Задание выполнено, когда у игрока были обе.</li>
 * </ul>
 */
public final class QuinqueReward {
    private static final int NARUKAMI = 1;
    private static final int IXA = 2;

    private QuinqueReward() {
    }

    /** Раз в тик: агент без стартовой квинке получает Kura. */
    public static void giveStarter(ServerPlayer player, PlayerData data) {
        if (data.getPlayerClass() != PlayerClass.CCG_AGENT || data.isStarterQuinqueGiven() || !player.isAlive()) {
            return;
        }
        data.setStarterQuinqueGiven(true);
        give(player, new ItemStack(ModItems.QUINQUE_KURA.get()));
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.starter_quinque"));
        NetworkHandler.syncTo(player);
    }

    /** Агент убил Старшего следователя. */
    public static void onCcgBossKilled(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data == null || QuestManager.current(player) != Quest.C_FINAL) {
            return;
        }
        boolean narukami = player.getRandom().nextDouble() < Balance.FINAL_NARUKAMI_CHANCE;
        Item item = narukami ? ModItems.QUINQUE_NARUKAMI.get() : ModItems.QUINQUE_IXA.get();
        Component name = new ItemStack(item).getHoverName();
        if (player.getInventory().countItem(item) > 0) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.final_quinque_owned", name));
        } else {
            give(player, new ItemStack(item));
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.final_quinque_got", name));
            Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.3F);
        }
        data.setFinalQuinques(data.getFinalQuinques() | (narukami ? NARUKAMI : IXA));
        QuestManager.setProgress(player, Integer.bitCount(data.getFinalQuinques()));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
