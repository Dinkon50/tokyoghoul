package com.tokyoghoulrpg;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModBlocks;
import com.tokyoghoulrpg.registry.ModCreativeTab;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.registry.ModItems;
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
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModEntities::registerSpawnPlacements);
        // Настройки клиента (геймпад). На сервере файл не создаётся.
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerData.class);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
