package com.tokyoghoulrpg.rep;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Репутация у CCG и у гулей (от -100 до 100). Убиваешь гулей — CCG относится лучше, гули хуже,
 * и наоборот. «Своя сторона»: у гуля и полугуля это гули, у человека и агента — CCG.
 * <ul>
 *   <li>от 50: сторона тебя не трогает (агенты не охотятся на гуля, гули не нападают на человека);</li>
 *   <li>до -30: сторона нападает даже на «своего» (CCG — на человека, гули — на гуля);</li>
 *   <li>CCG: чем ниже репутация, тем чаще и больше рейды; люди с -50 и ниже тоже попадают в розыск;</li>
 *   <li>своя сторона влияет на цены в лавке торговца и награды за контракты; при -50 информатор не даёт работу.</li>
 * </ul>
 */
public final class Reputation {
    private Reputation() {
    }

    /** Репутация у своей стороны. */
    public static int own(PlayerData data) {
        return data.getPlayerClass().hasKagune() ? data.getRepGhouls() : data.getRepCcg();
    }

    /** Множитель цены в лавке за очки крови: 0,8 при +100, 1,25 при -100. */
    public static double priceFactor(PlayerData data) {
        int rep = own(data);
        return rep >= 0 ? 1.0 - Balance.REP_PRICE_DISCOUNT * rep / 100.0 : 1.0 + Balance.REP_PRICE_MARKUP * -rep / 100.0;
    }

    public static int price(PlayerData data, int base) {
        return (int) Math.round(base * priceFactor(data));
    }

    /** Множитель наград за контракты: 0,5 при -100, 1,5 при +100. */
    public static double rewardFactor(PlayerData data) {
        return 1.0 + Balance.REP_REWARD_BONUS * own(data) / 100.0;
    }

    public static boolean informantTrusts(PlayerData data) {
        return own(data) > Balance.REP_INFORMANT_DISTRUST;
    }

    /** Убийство NPC мода. */
    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (victim instanceof GhoulBoss) {
            change(player, Balance.REP_KILL_BOSS_FRIEND, -Balance.REP_KILL_BOSS_ENEMY);
        } else if (victim instanceof GhoulNpc) {
            change(player, Balance.REP_KILL_NPC_FRIEND, -Balance.REP_KILL_NPC_ENEMY);
        } else if (victim instanceof CcgBoss) {
            change(player, -Balance.REP_KILL_BOSS_ENEMY, Balance.REP_KILL_BOSS_FRIEND);
        } else if (victim instanceof CcgAgentNpc) {
            change(player, -Balance.REP_KILL_NPC_ENEMY, Balance.REP_KILL_NPC_FRIEND);
        }
    }

    /** Изменить репутацию; о переходе через важные пороги игрок узнаёт сообщением. */
    public static void change(ServerPlayer player, int ccg, int ghouls) {
        PlayerData data = PlayerData.get(player);
        if (data == null || (ccg == 0 && ghouls == 0)) {
            return;
        }
        int oldCcg = data.getRepCcg();
        int oldGhouls = data.getRepGhouls();
        data.setRepCcg(oldCcg + ccg);
        data.setRepGhouls(oldGhouls + ghouls);
        announce(player, "ccg", oldCcg, data.getRepCcg());
        announce(player, "ghouls", oldGhouls, data.getRepGhouls());
        NetworkHandler.syncTo(player);
    }

    private static void announce(ServerPlayer player, String side, int before, int after) {
        String key = null;
        if (before > Balance.REP_HOSTILE && after <= Balance.REP_HOSTILE) {
            key = "hostile";
        } else if (before < Balance.REP_FRIENDLY && after >= Balance.REP_FRIENDLY) {
            key = "friendly";
        } else if (before >= Balance.REP_FRIENDLY && after < Balance.REP_FRIENDLY) {
            key = "friendly_lost";
        } else if (before <= Balance.REP_HOSTILE && after > Balance.REP_HOSTILE) {
            key = "hostile_lost";
        }
        if (key != null) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.rep." + side + "." + key));
        }
    }
}
