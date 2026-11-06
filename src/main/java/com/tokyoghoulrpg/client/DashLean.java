package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

/**
 * Анимация рывка для всех классов: на рывке тело резко наклоняется вперёд (в сторону рывка) и плавно
 * выпрямляется. Наклон вокруг ступней, так что ноги остаются на земле; кагуне наклоняется вместе с телом
 * и в это время играет свою анимацию рывка. Видят все, кто видит игрока (сервер шлёт «dash» или «kagune_dash»).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class DashLean {
    /** Наклон набирается за 2 тика, держится до 5-го и уходит к 12-му. */
    private static final float RISE = 2.0F;
    private static final float HOLD = 5.0F;
    private static final float END = 12.0F;
    private static final float ANGLE = 24.0F;

    private static final Map<Integer, Long> STARTS = new HashMap<>();
    /** Кому в Pre добавили поворот: только им снимаем его в Post (чтобы стек поз не разъехался). */
    private static final Set<Integer> PUSHED = new HashSet<>();

    private DashLean() {
    }

    public static void start(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            STARTS.put(entityId, mc.level.getGameTime());
        }
    }

    /** Насколько наклонён (0..1) в момент age тиков после начала рывка. */
    private static float weight(float age) {
        if (age < 0.0F || age >= END) {
            return 0.0F;
        }
        if (age < RISE) {
            float t = age / RISE;
            return t * (2.0F - t);
        }
        if (age < HOLD) {
            return 1.0F;
        }
        float t = (age - HOLD) / (END - HOLD);
        return 1.0F - t * t * (3.0F - 2.0F * t);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Long start = STARTS.get(player.getId());
        if (start == null) {
            return;
        }
        float age = player.level().getGameTime() - start + event.getPartialTick();
        float w = weight(age);
        if (w <= 0.0F) {
            if (age >= END) {
                STARTS.remove(player.getId());
            }
            return;
        }
        // рывок идёт по взгляду: ось наклона — горизонталь поперёк взгляда, положительный угол клонит вперёд
        float yaw = player.getViewYRot(event.getPartialTick()) * Mth.DEG_TO_RAD;
        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(new Quaternionf().rotateAxis(ANGLE * w * Mth.DEG_TO_RAD,
                Mth.cos(yaw), 0.0F, Mth.sin(yaw)));
        PUSHED.add(player.getId());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        if (PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        STARTS.clear();
        PUSHED.clear();
    }
}
