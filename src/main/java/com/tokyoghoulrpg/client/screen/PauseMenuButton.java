package com.tokyoghoulrpg.client.screen;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Кнопка «Tokyo Ghoul RPG» в левом верхнем углу меню паузы: открывает меню способностей и окон мода.
 * Так всё доступно без клавиатуры — в VR (меню паузы на кнопке Y) и на геймпаде (Menu / Options).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class PauseMenuButton {
    private PauseMenuButton() {
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen) || Minecraft.getInstance().player == null) {
            return;
        }
        event.addListener(Button.builder(Component.translatable("screen.tokyoghoulrpg.abilities.open"),
                        b -> Minecraft.getInstance().setScreen(new AbilityMenuScreen()))
                .bounds(4, 4, 120, 20).build());
    }
}
