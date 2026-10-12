package com.tokyoghoulrpg.registry;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Отдельная вкладка «Tokyo Ghoul RPG» в творческом режиме со всеми предметами мода. */
public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TokyoGhoulRPG.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tokyoghoulrpg"))
            .icon(() -> new ItemStack(ModItems.MASK_FANG.get()))
            .displayItems((parameters, output) -> ModItems.ITEMS.getEntries()
                    .forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTab() {
    }
}
