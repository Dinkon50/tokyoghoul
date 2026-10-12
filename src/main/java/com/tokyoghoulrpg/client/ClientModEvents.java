package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.hud.BiteVignette;
import com.tokyoghoulrpg.client.hud.HudOverlay;
import com.tokyoghoulrpg.client.kagune.KaguneLayer;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.entity.TraderNpc;
import com.tokyoghoulrpg.registry.ModEntities;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.TOGGLE_KAGUNE);
        event.register(KeyBindings.BITE);
        event.register(KeyBindings.DASH);
        event.register(KeyBindings.BLOCK);
        event.register(KeyBindings.BARRAGE);
        event.register(KeyBindings.RAGE);
        event.register(KeyBindings.SKILL_TREE);
        event.register(KeyBindings.QUESTS);
        event.register(KeyBindings.CLAN);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GHOUL.get(),
                context -> new NpcRenderer<>(context, "ghoul", GhoulNpc.VARIANTS));
        event.registerEntityRenderer(ModEntities.CCG_AGENT.get(),
                context -> new NpcRenderer<>(context, "ccg_agent", CcgAgentNpc.VARIANTS));
        event.registerEntityRenderer(ModEntities.TRADER.get(),
                context -> new NpcRenderer<>(context, "trader", TraderNpc.VARIANTS));
        event.registerEntityRenderer(ModEntities.INFORMANT.get(),
                context -> new NpcRenderer<>(context, "informant", InformantNpc.VARIANTS));
        // боссы: модель игрока в 1.23 раза крупнее (хитбокс 2.4 блока)
        event.registerEntityRenderer(ModEntities.GHOUL_BOSS.get(),
                context -> new NpcRenderer<>(context, "ghoul_boss", 1, 1.23F));
        event.registerEntityRenderer(ModEntities.CCG_BOSS.get(),
                context -> new NpcRenderer<>(context, "ccg_boss", 1, 1.23F));
    }

    /** Добавляем слой кагуне к отрисовке игроков: и к обычному скину, и к скину с тонкими руками. */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new KaguneLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Красные края после укуса рисуются под всем интерфейсом, панель и хотбар остаются поверх
        event.registerBelowAll("bite_vignette", BiteVignette::render);
        event.registerAboveAll("hud", HudOverlay::render);
    }
}
