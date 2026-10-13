package com.tokyoghoulrpg.client.quinque;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Подсказывает рендереру квинке, кто её держит: нужно для раскрытой формы в «Освобождении». */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class QuinqueClientEvents {
    private QuinqueClientEvents() {
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        QuinqueRenderer.setHolder(event.getEntity());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        QuinqueRenderer.setHolder(null);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        QuinqueRenderer.setHolder(Minecraft.getInstance().player);
    }
}
