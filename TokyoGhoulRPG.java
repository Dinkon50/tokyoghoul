package com.tokyoghoulrpg;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TokyoGhoulRPG.MOD_ID)
public class TokyoGhoulRPG {
    public static final String MOD_ID = "tokyoghoulrpg";

    public TokyoGhoulRPG() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::commonSetup);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerData.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
