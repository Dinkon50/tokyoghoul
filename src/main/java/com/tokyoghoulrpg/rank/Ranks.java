package com.tokyoghoulrpg.rank;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.combat.RcSuppress;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.HalfGhoulNpc;
import com.tokyoghoulrpg.event.ModCommands;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ранги. У людей ранга нет.
 * <ul>
 *   <li>Агент CCG — звания: Следователь 3-го класса → 2-го → 1-го → Старший следователь →
 *       Младший особый класс → Особый класс.</li>
 *   <li>Гуль и полугуль — рейтинг опасности: C → B → A → S → SS → SSS.</li>
 * </ul>
 * Очки: враг-NPC +2, игрок враждебной стороны +5, босс +15, Кровавый полугуль +30 (участникам),
 * задание +10, контракт +8. Пороги: 0, 50, 150, 350, 700, 1200.
 * Бонусы небольшие: +2% урона и +5% крови за убийства за каждый ранг выше первого.
 * Ранг виден над головой и в чате перед ником.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class Ranks {
    /** Ладья: 0 — нет ранга (человек), 1 — звания CCG, 2 — рейтинг гулей. */
    public static final int NONE = 0;
    public static final int CCG = 1;
    public static final int GHOUL = 2;
    public static final int COUNT = Balance.RANK_THRESHOLDS.length;

    /** У клиента: ранги других игроков (номер сущности → ладья * 10 + ранг). */
    private static final Map<Integer, Integer> CLIENT = new HashMap<>();
    /** У сервера: что последним отправили про игрока, чтобы не слать лишнего. */
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    private Ranks() {
    }

    public static int ladder(PlayerClass playerClass) {
        if (playerClass == PlayerClass.CCG_AGENT) {
            return CCG;
        }
        return playerClass.hasKagune() ? GHOUL : NONE;
    }

    public static int points(PlayerData data) {
        return switch (ladder(data.getPlayerClass())) {
            case CCG -> data.getRankCcg();
            case GHOUL -> data.getRankGhoul();
            default -> 0;
        };
    }

    public static int index(int points) {
        int index = 0;
        for (int i = 0; i < COUNT; i++) {
            if (points >= Balance.RANK_THRESHOLDS[i]) {
                index = i;
            }
        }
        return index;
    }

    public static int index(PlayerData data) {
        return index(points(data));
    }

    /** Сколько очков нужно до следующего ранга (-1 — уже высший). */
    public static int nextThreshold(int index) {
        return index + 1 < COUNT ? Balance.RANK_THRESHOLDS[index + 1] : -1;
    }

    public static MutableComponent title(int ladder, int index) {
        String key = "rank.tokyoghoulrpg." + (ladder == CCG ? "ccg." : "ghoul.") + index;
        ChatFormatting color = index >= 5 ? ChatFormatting.GOLD : index >= 3 ? ChatFormatting.LIGHT_PURPLE
                : index >= 1 ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        return Component.translatable(key).withStyle(color);
    }

    /** Уровень престижа на текущей ладье (0-5). */
    public static int prestige(PlayerData data) {
        if (data == null) {
            return 0;
        }
        return switch (ladder(data.getPlayerClass())) {
            case CCG -> data.getPrestigeCcg();
            case GHOUL -> data.getPrestigeGhoul();
            default -> 0;
        };
    }

    public static double damageBonus(PlayerData data) {
        return ladder(data.getPlayerClass()) == NONE ? 0.0
                : Balance.RANK_DAMAGE_PER * index(data) + Balance.PRESTIGE_DAMAGE * prestige(data);
    }

    public static double bloodBonus(PlayerData data) {
        return ladder(data.getPlayerClass()) == NONE ? 0.0
                : Balance.RANK_BLOOD_PER * index(data) + Balance.PRESTIGE_BLOOD * prestige(data);
    }

    /** Множитель входящего урона от престижа. */
    public static double defenseFactor(PlayerData data) {
        return 1.0 - Balance.PRESTIGE_DEFENSE * prestige(data);
    }

    /** Почему нельзя взять престиж (ключ сообщения) или null — можно. */
    public static String prestigeBlock(PlayerData data) {
        if (data == null || ladder(data.getPlayerClass()) == NONE) {
            return "prestige.no_ladder";
        }
        if (prestige(data) >= Balance.PRESTIGE_MAX) {
            return "prestige.maxed";
        }
        if (index(data) < COUNT - 1) {
            return "prestige.need_max";
        }
        return null;
    }

    /** Престиж: высший ранг сбрасывается на первый, навсегда +1 уровень престижа, награда и объявление. */
    public static void prestigeUp(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        String block = prestigeBlock(data);
        if (block != null) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg." + block).withStyle(ChatFormatting.RED));
            return;
        }
        int ladder = ladder(data.getPlayerClass());
        int level = prestige(data) + 1;
        if (ladder == CCG) {
            data.setRankCcg(0);
            data.setPrestigeCcg(level);
        } else {
            data.setRankGhoul(0);
            data.setPrestigeGhoul(level);
        }
        int blood = Balance.PRESTIGE_REWARD_BLOOD * level;
        data.addBloodPoints(blood);
        data.addSkillPoints(Balance.PRESTIGE_REWARD_POINTS);
        com.tokyoghoulrpg.rep.Achievements.atLeast(player, "prestige", level);
        NetworkHandler.syncTo(player);
        Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
        Effects.sound(player, SoundEvents.TOTEM_USE, 0.6F, 1.2F);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level0) {
            level0.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0,
                    player.getZ(), 60, 0.6, 1.0, 0.6, 0.3);
        }
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.prestige.done", stars(level), blood,
                Balance.PRESTIGE_REWARD_POINTS, Math.round(Balance.PRESTIGE_DAMAGE * level * 100),
                Math.round(Balance.PRESTIGE_DEFENSE * level * 100), Math.round(Balance.PRESTIGE_BLOOD * level * 100))
                .withStyle(ChatFormatting.GOLD));
        Component news = Component.translatable("message.tokyoghoulrpg.prestige.broadcast", player.getGameProfile().getName(),
                stars(level)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(news);
        }
    }

    /** Значок престижа: «★3». */
    public static String stars(int level) {
        return level <= 0 ? "" : "\u2605" + level;
    }

    // ------------------------------------------------------------------ очки

    public static void add(ServerPlayer player, int amount) {
        PlayerData data = PlayerData.get(player);
        if (data == null || amount <= 0) {
            return;
        }
        int ladder = ladder(data.getPlayerClass());
        if (ladder == NONE) {
            return;
        }
        int before = index(data);
        if (ladder == CCG) {
            data.setRankCcg(data.getRankCcg() + amount);
        } else {
            data.setRankGhoul(data.getRankGhoul() + amount);
        }
        int after = index(data);
        if (after > before && !ModCommands.isSilent()) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.rank.up." + ladder,
                    title(ladder, after)));
            Effects.sound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.2F);
        }
        NetworkHandler.syncTo(player);
    }

    /** Убийство игроком: враг своей стороны. Кровавого полугуля считает BossRewards. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        PlayerData data = PlayerData.get(killer);
        if (data == null) {
            return;
        }
        int ladder = ladder(data.getPlayerClass());
        if (ladder == CCG) {
            if (victim instanceof GhoulBoss) {
                add(killer, Balance.RANK_KILL_BOSS);
            } else if (victim instanceof GhoulNpc || victim instanceof HalfGhoulNpc) {
                add(killer, Balance.RANK_KILL_NPC);
            } else if (victim instanceof Player && RcSuppress.isGhoul(victim)) {
                add(killer, Balance.RANK_KILL_PLAYER);
            }
        } else if (ladder == GHOUL) {
            if (victim instanceof CcgBoss) {
                add(killer, Balance.RANK_KILL_BOSS);
            } else if (victim instanceof CcgAgentNpc) {
                add(killer, Balance.RANK_KILL_NPC);
            } else if (victim instanceof Player other) {
                PlayerData od = PlayerData.get(other);
                if (od != null && od.hasChosenClass() && !od.getPlayerClass().hasKagune()) {
                    add(killer, Balance.RANK_KILL_PLAYER);
                }
            }
        }
    }

    // ------------------------------------------------------------------ над головой и в чате

    /** Код для показа: ладья * 10 + ранг (0 — нет ранга). */
    public static int code(PlayerData data) {
        int ladder = data == null || !data.hasChosenClass() ? NONE : ladder(data.getPlayerClass());
        return ladder == NONE ? 0 : prestige(data) * 100 + ladder * 10 + index(data);
    }

    /** Сервер: разослать ранг игрока, если он изменился (вызывается из syncTo). */
    public static void broadcastIfChanged(ServerPlayer player, PlayerData data) {
        int code = code(data);
        Integer last = SENT.put(player.getUUID(), code);
        if (last == null || last != code) {
            NetworkHandler.sendRank(player, code);
            player.refreshDisplayName();
        }
    }

    public static void forget(ServerPlayer player) {
        SENT.remove(player.getUUID());
    }

    /** Клиент: пришёл ранг игрока. */
    public static void acceptClient(int entityId, int code) {
        CLIENT.put(entityId, code);
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    /** Ник с рангом: «[SS] Ник» или «[Особый класс] Ник». Работает и у сервера (чат), и у клиента (над головой). */
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        int code;
        if (player.level().isClientSide()) {
            code = CLIENT.getOrDefault(player.getId(), 0);
        } else {
            code = code(PlayerData.get(player));
        }
        if (code <= 0) {
            return;
        }
        int rank = code % 100;
        int prestige = code / 100;
        MutableComponent prefix = Component.literal("[");
        if (prestige > 0) {
            prefix.append(Component.literal(stars(prestige) + " ").withStyle(ChatFormatting.GOLD));
        }
        prefix.append(title(rank / 10, rank % 10)).append("] ");
        event.setDisplayname(Component.empty().append(prefix).append(event.getDisplayname()));
    }
}
