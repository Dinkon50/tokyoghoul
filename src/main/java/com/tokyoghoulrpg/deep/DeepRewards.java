package com.tokyoghoulrpg.deep;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.AncientGhoul;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import com.tokyoghoulrpg.item.QuinqueEvents;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.quest.Quest;
import com.tokyoghoulrpg.quest.QuestManager;
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
 * Награды данжа. Получают все, кто нанёс урон и сейчас в игре.
 * Страж: очки крови и очки навыков каждому. Древний гуль: двоим лучшим по урону — много крови и очков
 * навыков, уровни, редкая квинке и ядро III качества; остальным — поменьше. Весь сервер узнаёт о победе.
 */
public final class DeepRewards {
    private DeepRewards() {
    }

    public static void give(HalfGhoulBoss boss) {
        MinecraftServer server = boss.getServer();
        if (server == null) {
            return;
        }
        boolean ancient = boss instanceof AncientGhoul;
        List<ServerPlayer> ranked = new ArrayList<>();
        List<Float> damage = new ArrayList<>();
        for (Map.Entry<UUID, Float> entry : boss.damageRanking()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
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
            int dealt = Math.round(damage.get(i));
            if (!ancient) {
                data.addBloodPoints(DeepBalance.GUARDIAN_BLOOD);
                int points = data.earnSkillPoints(DeepBalance.GUARDIAN_POINTS);
                com.tokyoghoulrpg.clan.ClanBank.tax(player, DeepBalance.GUARDIAN_BLOOD, points);
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.deep_guardian.reward",
                        boss.getDisplayName(), dealt, DeepBalance.GUARDIAN_BLOOD, points));
            } else if (i < 2) {
                data.addBloodPoints(DeepBalance.ANCIENT_TOP_BLOOD);
                int points = data.earnSkillPoints(DeepBalance.ANCIENT_TOP_POINTS);
                com.tokyoghoulrpg.clan.ClanBank.tax(player, DeepBalance.ANCIENT_TOP_BLOOD, points);
                player.giveExperienceLevels(DeepBalance.ANCIENT_TOP_LEVELS);
                Item quinque = rareQuinque(player);
                give(player, new ItemStack(quinque));
                QuinqueEvents.giveCore(player, 3);
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ancient_ghoul.top",
                        i + 1, dealt, DeepBalance.ANCIENT_TOP_BLOOD, points, DeepBalance.ANCIENT_TOP_LEVELS,
                        new ItemStack(quinque).getHoverName()));
            } else {
                data.addBloodPoints(DeepBalance.ANCIENT_OTHER_BLOOD);
                int points = data.earnSkillPoints(DeepBalance.ANCIENT_OTHER_POINTS);
                com.tokyoghoulrpg.clan.ClanBank.tax(player, DeepBalance.ANCIENT_OTHER_BLOOD, points);
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.ancient_ghoul.other",
                        i + 1, dealt, DeepBalance.ANCIENT_OTHER_BLOOD, points));
            }
            Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, ancient ? 0.8F : 1.1F);
            QuestManager.progress(player, Quest.Trigger.KILL_BOSS, 1);
            com.tokyoghoulrpg.rep.Achievements.add(player, "boss_kills", 1);
            com.tokyoghoulrpg.rep.ModAdvancements.grantKill(player, boss);
            com.tokyoghoulrpg.rep.Achievements.add(player, ancient ? "ancient_kills" : "deep_guardians", 1);
            com.tokyoghoulrpg.clan.ClanQuests.progress(player, com.tokyoghoulrpg.clan.ClanQuests.Kind.KILL_BOSS, 1);
            com.tokyoghoulrpg.rank.Ranks.add(player, Balance.RANK_RAID_BOSS);
            com.tokyoghoulrpg.combat.Kakuja.gain(player, Balance.KAKUJA_RAID_BOSS);
            NetworkHandler.syncTo(player);
        }
        Standing.onBossKill(ranked, Balance.STANDING_RAID_BOSS);
        if (ancient && !ranked.isEmpty()) {
            Component names = Component.literal(String.join(", ", ranked.stream()
                    .map(p -> p.getGameProfile().getName()).limit(4).toList()));
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.tokyoghoulrpg.ancient_ghoul.broadcast", names), false);
        }
    }

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
