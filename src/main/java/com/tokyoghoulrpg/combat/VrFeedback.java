package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.VrHapticS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Вибрация контроллеров у игроков в VR по событиям боя: попал по врагу (дрогнет рука, которой бил),
 * получил урон (обе руки, тем сильнее, чем больнее), блок погасил удар, парирование, блок пробит.
 * Игрокам не в VR ничего не шлём.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class VrFeedback {
    private VrFeedback() {
    }

    private static void send(ServerPlayer player, int kind, boolean offhand, float strength) {
        if (Aim.inVr(player)) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new VrHapticS2C(kind, offhand, strength));
        }
    }

    /** Удар игрока попал (из CombatContext.hit). */
    public static void hit(ServerPlayer attacker) {
        send(attacker, VrHapticS2C.HIT, Aim.usingOffhand(attacker), 0.6F);
    }

    public static void blocked(ServerPlayer blocker) {
        send(blocker, VrHapticS2C.BLOCKED, false, 0.5F);
    }

    public static void parry(ServerPlayer blocker) {
        send(blocker, VrHapticS2C.PARRY, false, 1.0F);
    }

    public static void blockBroken(ServerPlayer blocker) {
        send(blocker, VrHapticS2C.BLOCK_BROKEN, false, 1.0F);
    }

    /** Урон по игроку: сила вибрации от 2 до 10 единиц урона. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            float strength = Math.max(0.3F, Math.min(1.0F, event.getAmount() / 10.0F));
            send(player, VrHapticS2C.HURT, false, strength);
        }
    }
}
