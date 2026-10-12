package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.DoubleJumpC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Двойной прыжок у всех классов: в воздухе ещё раз нажми прыжок (пробел).
 * Один раз за прыжок, снова доступен после приземления. Движением игрока управляет клиент,
 * поэтому толчок вверх даётся здесь, а сервер обнуляет высоту падения и проигрывает звук.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class DoubleJumpClient {
    private static boolean jumpWasDown = false;
    private static boolean used = false;

    private DoubleJumpClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            jumpWasDown = false;
            return;
        }
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !jumpWasDown;
        jumpWasDown = down;

        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            used = false;
            return;
        }
        if (!pressed || used || player.getAbilities().flying || player.isFallFlying() || player.isPassenger()
                || player.isSpectator()) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass() || data.isLockedAt(player.level().getGameTime())) {
            return;
        }
        used = true;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Balance.DOUBLE_JUMP_VELOCITY, motion.z);
        player.fallDistance = 0.0F;
        NetworkHandler.CHANNEL.sendToServer(new DoubleJumpC2S());
    }
}
