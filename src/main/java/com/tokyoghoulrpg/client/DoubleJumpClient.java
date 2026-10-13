package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.DoubleJumpC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Двойной прыжок у всех классов. Прыгнул, в воздухе нажал пробел ещё раз — второй прыжок.
 * <ul>
 *   <li>Нажатие, которым сделан первый прыжок, не считается: пробел нужно отпустить и нажать снова.</li>
 *   <li>Если второй раз нажал, пока ещё быстро летишь вверх, прыжок срабатывает чуть позже — у верхней точки.
 *       Так это два заметных прыжка, а не один длинный.</li>
 *   <li>Второй прыжок даёт подъём и толчок в сторону, куда идёшь, облачко под ногами и звук.</li>
 *   <li>Один раз за прыжок, снова доступен после приземления, в воде или на лестнице. Работает и при падении с края.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class DoubleJumpClient {
    private static boolean jumpWasDown = false;
    private static boolean used = false;
    private static boolean pending = false;
    private static int airTicks = 0;
    private static boolean releasedInAir = false;

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
            pending = false;
            return;
        }
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !jumpWasDown;
        jumpWasDown = down;

        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            used = false;
            pending = false;
            airTicks = 0;
            releasedInAir = false;
            return;
        }
        airTicks++;
        if (!down) {
            releasedInAir = true;
        }
        if (player.getAbilities().flying || player.isFallFlying() || player.isPassenger() || player.isSpectator()) {
            pending = false;
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass() || data.isLockedAt(player.level().getGameTime())) {
            pending = false;
            return;
        }
        // Второе нажатие запоминаем
        if (pressed && releasedInAir && !used && airTicks >= Balance.DOUBLE_JUMP_MIN_AIR_TICKS) {
            pending = true;
        }
        // ...и прыгаем, когда подъём от первого прыжка почти закончился
        if (pending && player.getDeltaMovement().y <= Balance.DOUBLE_JUMP_APEX_SPEED) {
            pending = false;
            used = true;
            jump(mc, player);
        }
    }

    private static void jump(Minecraft mc, LocalPlayer player) {
        Vec3 motion = player.getDeltaMovement();
        // толчок в сторону, куда нажаты клавиши движения
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        double boostX = 0.0;
        double boostZ = 0.0;
        if (Math.abs(forward) > 0.01F || Math.abs(strafe) > 0.01F) {
            double yaw = Math.toRadians(player.getYRot());
            double len = Math.sqrt(forward * forward + strafe * strafe);
            double f = forward / len;
            double s = strafe / len;
            double dx = -Math.sin(yaw) * f + Math.cos(yaw) * s;
            double dz = Math.cos(yaw) * f + Math.sin(yaw) * s;
            boostX = dx * Balance.DOUBLE_JUMP_BOOST;
            boostZ = dz * Balance.DOUBLE_JUMP_BOOST;
        }
        player.setDeltaMovement(motion.x * 0.6 + boostX, Balance.DOUBLE_JUMP_VELOCITY, motion.z * 0.6 + boostZ);
        player.fallDistance = 0.0F;
        // облачко под ногами видно сразу, не дожидаясь сервера
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            player.level().addParticle(ParticleTypes.CLOUD, player.getX() + Math.cos(a) * 0.4, player.getY(),
                    player.getZ() + Math.sin(a) * 0.4, Math.cos(a) * 0.05, -0.02, Math.sin(a) * 0.05);
        }
        NetworkHandler.CHANNEL.sendToServer(new DoubleJumpC2S());
    }
}
