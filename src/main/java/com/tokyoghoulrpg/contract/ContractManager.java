package com.tokyoghoulrpg.contract;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.rep.Reputation;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;

/** Серверная логика контрактов: взять, засчитать убийства, сдать, отказаться. */
public final class ContractManager {
    public static final int ACCEPT = 0;
    public static final int TURN_IN = 1;
    public static final int ABANDON = 2;
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private ContractManager() {
    }

    /** Номер игрового дня (меняется и когда игроки спят). */
    public static long day(ServerPlayer player) {
        return player.getServer() == null ? 0L : player.getServer().overworld().getDayTime() / 24000L;
    }

    public static Contract todayOffer(ServerPlayer player, PlayerData data) {
        return Contract.offerFor(player.getUUID(), data.getPlayerClass(), day(player));
    }

    private static boolean nearInformant(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof InformantNpc && entity.isAlive() && entity.distanceToSqr(player) <= MAX_DISTANCE_SQR;
    }

    public static void action(ServerPlayer player, int informantId, int action) {
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass() || !nearInformant(player, informantId)) {
            return;
        }
        switch (action) {
            case ACCEPT -> accept(player, data);
            case TURN_IN -> turnIn(player, data);
            case ABANDON -> {
                if (data.getContractId() != null) {
                    data.setContract(null, 0);
                    player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.contract_abandoned"), true);
                }
            }
            default -> {
            }
        }
        NetworkHandler.syncTo(player);
    }

    private static void accept(ServerPlayer player, PlayerData data) {
        long today = day(player);
        if (data.getContractId() != null || data.getContractDoneDay() == today) {
            return;
        }
        if (!Reputation.informantTrusts(data)) {
            player.displayClientMessage(Component.translatable("screen.tokyoghoulrpg.contract.distrust"), true);
            return;
        }
        Contract offer = todayOffer(player, data);
        if (offer != null) {
            data.setContract(offer.id(), 0);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.contract_taken", offer.title()), true);
        }
    }

    private static void turnIn(ServerPlayer player, PlayerData data) {
        Contract contract = data.getContractId() == null ? null : Contract.byId(data.getContractId());
        if (contract == null) {
            return;
        }
        if (contract.kind() == Contract.Kind.DELIVER) {
            Item item = contract.item();
            if (player.getInventory().countItem(item) < contract.goal()) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.contract_not_done"), true);
                return;
            }
            player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), contract.goal(),
                    player.inventoryMenu.getCraftSlots());
        } else if (data.getContractProgress() < contract.goal()) {
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.contract_not_done"), true);
            return;
        }
        int blood = contract.bloodFor(data);
        data.addBloodPoints(blood);
        int points = data.earnSkillPoints(contract.points());
        com.tokyoghoulrpg.clan.ClanBank.tax(player, blood, points);
        data.setContract(null, 0);
        data.setContractDoneDay(day(player));
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.contract_done",
                contract.title(), blood, points));
        // Охота на одну сторону радует другую; остальная работа — немного плюс своей стороне
        switch (contract.kind()) {
            case KILL_GHOUL -> Reputation.change(player, Balance.REP_CONTRACT, 0);
            case KILL_CCG -> Reputation.change(player, 0, Balance.REP_CONTRACT);
            default -> {
                boolean ghoulSide = data.getPlayerClass().hasKagune();
                Reputation.change(player, ghoulSide ? 0 : Balance.REP_CONTRACT_OTHER,
                        ghoulSide ? Balance.REP_CONTRACT_OTHER : 0);
            }
        }
        Effects.sound(player, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.2F);
        com.tokyoghoulrpg.rank.Ranks.add(player, Balance.RANK_CONTRACT);
        QuestManager.progress(player, Quest.Trigger.CONTRACT, 1);
    }

    /** Убийство: засчитывается в активный контракт, если подходит. */
    public static void onKill(ServerPlayer player, LivingEntity victim) {
        PlayerData data = PlayerData.get(player);
        Contract contract = data == null || data.getContractId() == null ? null : Contract.byId(data.getContractId());
        if (contract == null || data.getContractProgress() >= contract.goal()) {
            return;
        }
        boolean counts = switch (contract.kind()) {
            case KILL_GHOUL -> victim instanceof GhoulNpc || victim instanceof GhoulBoss
                    || victim instanceof com.tokyoghoulrpg.entity.HalfGhoulNpc
                    || victim instanceof com.tokyoghoulrpg.entity.HalfGhoulBoss;
            case KILL_CCG -> victim instanceof CcgAgentNpc || victim instanceof CcgBoss;
            case KILL_MONSTER -> victim instanceof Enemy;
            case KILL_BOSS -> victim instanceof BossMob;
            case DELIVER -> false;
        };
        if (counts) {
            data.setContract(contract.id(), data.getContractProgress() + 1);
            if (data.getContractProgress() >= contract.goal()) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.contract_ready"), true);
            }
            NetworkHandler.syncTo(player);
        }
    }
}
