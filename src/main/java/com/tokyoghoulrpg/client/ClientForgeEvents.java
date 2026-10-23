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
        long now = mc.player.level().getGameTime();
        // Ульта (H J K L быстро) — только у гуля и полугуля
        PlayerData selfData = PlayerData.get(mc.player);
        boolean ghoul = selfData != null && selfData.getPlayerClass().hasKagune();

        while (KeyBindings.TOGGLE_KAGUNE.consumeClick()) {
            if (mc.screen == null) {
                NetworkHandler.CHANNEL.sendToServer(new ToggleKaguneC2S());
            }
        }
        while (KeyBindings.BITE.consumeClick()) {
            sendAbility(mc, AbilityType.BITE);
        }
        while (KeyBindings.DASH.consumeClick()) {
            if (ghoul && mc.screen == null) {
                UltimateCombo.dash(now);
            }
            sendAbility(mc, AbilityType.DASH);
        }
        while (KeyBindings.RAGE.consumeClick()) {
            // J сразу после H может оказаться частью комбо ульты: тогда ярость подождёт
            if (!(ghoul && mc.screen == null && UltimateCombo.rage(now))) {
                sendAbility(mc, AbilityType.RAGE);
            }
        }
        if (ghoul && UltimateCombo.releaseDeferredRage(now)) {
            sendAbility(mc, AbilityType.RAGE);
        }

        while (KeyBindings.RETRIBUTION.consumeClick()) {
            sendAbility(mc, AbilityType.RETRIBUTION);
        }
        while (KeyBindings.CHARISMA.consumeClick()) {
            sendAbility(mc, AbilityType.CHARISMA);
        }
        while (KeyBindings.FORTUNE.consumeClick()) {
            sendAbility(mc, AbilityType.FORTUNE);
        }
        while (KeyBindings.KAKUJA.consumeClick()) {
            sendAbility(mc, AbilityType.KAKUJA);
        }
        while (KeyBindings.MARKET.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new com.tokyoghoulrpg.client.screen.MarketScreen());
                NetworkHandler.CHANNEL.sendToServer(new com.tokyoghoulrpg.network.packet.MarketActionC2S(
                        com.tokyoghoulrpg.market.Market.OPEN, 0, 0, false));
            }
        }
        while (KeyBindings.PROFILE.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new com.tokyoghoulrpg.client.screen.ProfileScreen());
                NetworkHandler.CHANNEL.sendToServer(new com.tokyoghoulrpg.network.packet.RequestSyncC2S());
            }
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

        // Сначала все нажатия блока и баррага (они же — конец комбо ульты), потом их состояние
        while (KeyBindings.BLOCK.consumeClick()) {
            if (ghoul && mc.screen == null) {
                UltimateCombo.block(now);
            }
        }
        while (KeyBindings.BARRAGE.consumeClick()) {
            if (ghoul && mc.screen == null) {
                UltimateCombo.barrage(now);
            }
        }

        // Блок держится, пока нажата L: отправляем серверу только изменения
        boolean down = (ghoul ? UltimateCombo.blockHeld(KeyBindings.BLOCK.isDown()) : KeyBindings.BLOCK.isDown())
                && mc.screen == null;
        if (down != blockHeld) {
            blockHeld = down;
            NetworkHandler.CHANNEL.sendToServer(new BlockC2S(down));
        }

        // Барраж держится, пока зажата K: зажал - серия идёт, отпустил - закончилась
        boolean barrageDown = (ghoul ? UltimateCombo.barrageHeld(KeyBindings.BARRAGE.isDown(), now)
                : KeyBindings.BARRAGE.isDown()) && mc.screen == null;
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
        UltimateCombo.reset();
        KaguneClientState.clear();
        com.tokyoghoulrpg.client.kagune.KaguneUltimateFx.clear();
        com.tokyoghoulrpg.client.kagune.KaguneReach.clear();
        com.tokyoghoulrpg.rank.Ranks.clearClient();
        blockHeld = false;
        barrageHeld = false;
    }
}
