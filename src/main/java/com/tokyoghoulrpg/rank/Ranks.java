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

    public static double damageBonus(PlayerData data) {
        return ladder(data.getPlayerClass()) == NONE ? 0.0 : Balance.RANK_DAMAGE_PER * index(data);
    }

    public static double bloodBonus(PlayerData data) {
        return ladder(data.getPlayerClass()) == NONE ? 0.0 : Balance.RANK_BLOOD_PER * index(data);
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
        return ladder == NONE ? 0 : ladder * 10 + index(data);
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
        Component prefix = Component.literal("[").append(title(code / 10, code % 10)).append("] ");
        event.setDisplayname(Component.empty().append(prefix).append(event.getDisplayname()));
    }
}
