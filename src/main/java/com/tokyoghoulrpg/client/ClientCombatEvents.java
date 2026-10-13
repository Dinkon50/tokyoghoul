package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.QuinqueAbilities;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.AbilityC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Управление атаками кагуне мышью и ограничения движения:
 * ЛКМ: обычная атака, ПКМ дважды быстро: сильная, Shift + ПКМ: рывок к цели, Shift + ЛКМ: притянуть цель.
 * Работает только когда кагуне включено (G).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class ClientCombatEvents {
    /** Была ли ПКМ отпущена после прошлого использования. Зажатая кнопка повторяет использование сама. */
    private static boolean useReleased = true;
    private static long lastFreshUse = Long.MIN_VALUE / 2;

    private ClientCombatEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        if (!mc.options.keyUse.isDown()) {
            useReleased = true;
        }
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        // В блоке бегать нельзя
        if (data.isBlocking()) {
            mc.options.keySprint.setDown(false);
            player.setSprinting(false);
        }
        // Пока кагуне включено, удержание ЛКМ не ломает блоки: ЛКМ отдана под атаки
        // С квинке в руке ЛКМ отдана под удары и не ломает блоки. С кагуне ЛКМ по блоку копает им (своя анимация).
        if (QuinqueAbilities.active(player, data) && mc.options.keyAttack.isDown() && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        // Агент CCG с квинке: ЛКМ — удары, Shift + ЛКМ — выпад, ПКМ дважды — сильный удар
        if (QuinqueAbilities.active(player, data)) {
            if (event.isAttack()) {
                cancel(event);
                send(player.isShiftKeyDown() ? AbilityType.QUINQUE_LUNGE : AbilityType.QUINQUE_ATTACK);
            } else if (event.isUseItem()) {
                boolean fresh = useReleased;
                useReleased = false;
                boolean lookingAtBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
                if (!fresh || lookingAtBlock) {
                    return;
                }
                long now = player.level().getGameTime();
                if (now - lastFreshUse <= Balance.DOUBLE_TAP_TICKS) {
                    lastFreshUse = Long.MIN_VALUE / 2;
                    cancel(event);
                    send(AbilityType.QUINQUE_STRONG);
                } else {
                    lastFreshUse = now;
                }
            }
            return;
        }
        // Человек: ПКМ дважды быстро — сильная атака с любым предметом или пустой рукой.
        // Если смотришь на блок, ПКМ работает как обычно (ставить блоки, открывать двери).
        if (data.getPlayerClass() == PlayerClass.HUMAN) {
            if (event.isUseItem()) {
                boolean fresh = useReleased;
                useReleased = false;
                boolean lookingAtBlock = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
                if (!fresh || lookingAtBlock) {
                    return;
                }
                long now = player.level().getGameTime();
                if (now - lastFreshUse <= Balance.DOUBLE_TAP_TICKS) {
                    lastFreshUse = Long.MIN_VALUE / 2;
                    cancel(event);
                    send(AbilityType.HUMAN_STRONG_ATTACK);
                } else {
                    lastFreshUse = now;
                }
            }
            return;
        }
        if (!data.getPlayerClass().hasKagune() || !data.isKaguneActive()) {
            return;
        }
        boolean shift = player.isShiftKeyDown();

        if (event.isAttack()) {
            // ЛКМ по блоку — кагуне копает, а не бьёт
            if (!shift && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                return;
            }
            cancel(event);
            send(shift ? AbilityType.PULL_TARGET : AbilityType.BASIC_ATTACK);
        } else if (event.isUseItem()) {
            boolean fresh = useReleased;
            useReleased = false;
            if (shift) {
                cancel(event);
                send(AbilityType.GRAPPLE_SELF);
                return;
            }
            // Зажатая ПКМ повторяет использование сама: это не двойное нажатие
            if (!fresh) {
                return;
            }
            long now = player.level().getGameTime();
            if (now - lastFreshUse <= Balance.DOUBLE_TAP_TICKS) {
                lastFreshUse = Long.MIN_VALUE / 2;
                cancel(event);
                send(AbilityType.STRONG_ATTACK);
            } else {
                lastFreshUse = now;
            }
        }
    }

    /** Оглушённый или обездвиженный игрок не может ходить и прыгать. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.isLockedAt(player.level().getGameTime())) {
            return;
        }
        Input input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    private static void cancel(InputEvent.InteractionKeyMappingTriggered event) {
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static void send(AbilityType type) {
        NetworkHandler.CHANNEL.sendToServer(new AbilityC2S(type));
    }
}
