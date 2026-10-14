package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
import com.tokyoghoulrpg.item.QuinqueEvents;
import com.tokyoghoulrpg.registry.ModItems;
import com.tokyoghoulrpg.rep.Standing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Награда за Кровавого полугуля. Считается урон каждого игрока за весь бой.
 * <ul>
 *   <li>Двое, нанёсшие больше всего урона: 10 000 очков крови, 30 очков навыков, +3 уровня Minecraft
 *       и одна из редких квинке (Narukami, IXA или 13's Jason — сначала та, которой у тебя нет).
 *       За возрождённого босса им же печать сброса навыков.</li>
 *   <li>Остальные, кто нанёс боссу хоть немного урона: 1000 очков крови и 5 очков навыков.</li>
 * </ul>
 * Награду получают только те, кто в игре в момент победы.
 */
public final class BossRewards {
    private BossRewards() {
    }

    public static void give(HalfGhoulBoss boss) {
        MinecraftServer server = boss.getServer();
        if (server == null) {
            return;
        }
        List<ServerPlayer> ranked = new ArrayList<>();
        List<Float> damage = new ArrayList<>();
        for (Map.Entry<UUID, Float> entry : boss.damageRanking()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            // награду получает только тот, кто нанёс боссу хоть немного урона
            if (player != null && entry.getValue() > 0.0F) {
                ranked.add(player);
                damage.add(entry.getValue());
            }
        }
        for (int i = 0; i < ranked.size(); i++) {
            ServerPlayer player = ranked.get(i);
            PlayerData data = PlayerData.get(player);
            if (data == null) {
                continue;
            }
            int place = i + 1;
            int dealt = Math.round(damage.get(i));
            // особая награда — двоим с наибольшим уроном
            if (i < 2) {
                data.addBloodPoints(Balance.HG_TOP_BLOOD);
                data.addSkillPoints(Balance.HG_TOP_POINTS);
                player.giveExperienceLevels(Balance.HG_TOP_LEVELS);
                Item quinque = rareQuinque(player);
                give(player, new ItemStack(quinque));
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.half_ghoul_boss.top",
                        place, dealt, Balance.HG_TOP_BLOOD, Balance.HG_TOP_POINTS, Balance.HG_TOP_LEVELS,
                        new ItemStack(quinque).getHoverName()));
                // ядро III качества: редкое, только двоим лучшим
                if (player.getRandom().nextDouble() < Balance.CORE_3_CHANCE) {
                    QuinqueEvents.giveCore(player, 3);
                }
                if (boss.isRespawned()) {
                    give(player, new ItemStack(ModItems.SKILL_RESET_SEAL.get()));
                    player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.half_ghoul_boss.seal"));
                }
                Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                QuestManager.progress(player, Quest.Trigger.KILL_BOSS, 1);
            } else {
                data.addBloodPoints(Balance.HG_OTHER_BLOOD);
                data.addSkillPoints(Balance.HG_OTHER_POINTS);
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.half_ghoul_boss.other",
                        place, dealt, Balance.HG_OTHER_BLOOD, Balance.HG_OTHER_POINTS));
            }
            NetworkHandler.syncTo(player);
        }
        // репутация: +20 каждому участнику и один раз каждому их клану
        Standing.onBossKill(ranked, Balance.STANDING_RAID_BOSS);
        // ранг и Какудзя — всем, кто нанёс урон
        for (ServerPlayer player : ranked) {
            com.tokyoghoulrpg.rank.Ranks.add(player, Balance.RANK_RAID_BOSS);
            com.tokyoghoulrpg.combat.Kakuja.gain(player, Balance.KAKUJA_RAID_BOSS);
        }
    }

    /** Редкая квинке: сначала та, которой у игрока нет; если есть все — любая. */
    private static Item rareQuinque(ServerPlayer player) {
        List<Item> pool = List.of(ModItems.QUINQUE_NARUKAMI.get(), ModItems.QUINQUE_IXA.get(), ModItems.QUINQUE_JASON.get());
        List<Item> missing = new ArrayList<>();
        for (Item item : pool) {
            if (player.getInventory().countItem(item) == 0) {
                missing.add(item);
            }
        }
        List<Item> from = missing.isEmpty() ? pool : missing;
        return from.get(player.getRandom().nextInt(from.size()));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
