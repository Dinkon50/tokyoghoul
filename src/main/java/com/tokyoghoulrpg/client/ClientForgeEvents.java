package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.kagune.KaguneClientState;
import com.tokyoghoulrpg.client.screen.ClanScreen;
import com.tokyoghoulrpg.client.screen.QuestScreen;
import com.tokyoghoulrpg.clan.ClanManager;
import com.tokyoghoulrpg.network.packet.ClanActionC2S;
import com.tokyoghoulrpg.client.screen.SkillTreeScreen;
import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.AbilityC2S;
import com.tokyoghoulrpg.network.packet.BarrageC2S;
import com.tokyoghoulrpg.network.packet.BlockC2S;
import com.tokyoghoulrpg.network.packet.ToggleKaguneC2S;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    /** Последнее состояние клавиши блока, которое мы отправили серверу. */
    private static boolean blockHeld = false;
    /** Последнее состояние клавиши баррага, которое мы отправили серверу. */
    private static boolean barrageHeld = false;

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            blockHeld = false;
            barrageHeld = false;
            return;
        }

        ClientHooks.tickPendingScreens(mc);

        while (KeyBindings.TOGGLE_KAGUNE.consumeClick()) {
            if (mc.screen == null) {
                NetworkHandler.CHANNEL.sendToServer(new ToggleKaguneC2S());
            }
        }
        while (KeyBindings.BITE.consumeClick()) {
            sendAbility(mc, AbilityType.BITE);
        }
        while (KeyBindings.DASH.consumeClick()) {
            sendAbility(mc, AbilityType.DASH);
        }
        while (KeyBindings.RAGE.consumeClick()) {
            sendAbility(mc, AbilityType.RAGE);
        }
        while (KeyBindings.SKILL_TREE.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new SkillTreeScreen());
            }
        }
        while (KeyBindings.QUESTS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new QuestScreen());
            }
        }
        while (KeyBindings.CLAN.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new ClanScreen());
                NetworkHandler.CHANNEL.sendToServer(new ClanActionC2S(ClanManager.OPEN, "", null, 0));
            }
        }

        // Блок держится, пока нажата L: отправляем серверу только изменения
        while (KeyBindings.BLOCK.consumeClick()) {
            // нажатия считаем по состоянию клавиши ниже
        }
        boolean down = KeyBindings.BLOCK.isDown() && mc.screen == null;
        if (down != blockHeld) {
            blockHeld = down;
            NetworkHandler.CHANNEL.sendToServer(new BlockC2S(down));
        }

        // Барраж держится, пока зажата K: зажал - серия идёт, отпустил - закончилась
        while (KeyBindings.BARRAGE.consumeClick()) {
            // нажатия считаем по состоянию клавиши ниже
        }
        boolean barrageDown = KeyBindings.BARRAGE.isDown() && mc.screen == null;
        if (barrageDown != barrageHeld) {
            barrageHeld = barrageDown;
            NetworkHandler.CHANNEL.sendToServer(new BarrageC2S(barrageDown));
        }
    }

    private static void sendAbility(Minecraft mc, AbilityType type) {
        if (mc.screen == null) {
            NetworkHandler.CHANNEL.sendToServer(new AbilityC2S(type));
        }
    }

    /** Когда выбран класс, здоровье и голод показывает наша панель, а ванильные полоски скрываются. */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean chosen = mc.player.getCapability(PlayerData.CAP).map(data -> data.hasChosenClass()).orElse(false);
        if (!chosen) {
            return;
        }
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHooks.reset();
        KaguneClientState.clear();
        blockHeld = false;
        barrageHeld = false;
    }
}
