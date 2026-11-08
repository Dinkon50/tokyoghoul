package com.tokyoghoulrpg.registry;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Свои звуки мода: музыка боссов и данжа, эффекты стилей. Файлы — assets/tokyoghoulrpg/sounds,
 * описание — sounds.json. Всё синтезировано tools/sounds_gen.py.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
            TokyoGhoulRPG.MOD_ID);

    public static final RegistryObject<SoundEvent> MUSIC_BOSS_BATTLE = register("music.boss_battle");
    public static final RegistryObject<SoundEvent> MUSIC_STYLE_DUEL = register("music.style_duel");
    public static final RegistryObject<SoundEvent> MUSIC_ANCIENT_GHOUL = register("music.ancient_ghoul");
    public static final RegistryObject<SoundEvent> MUSIC_DEEP_WARD = register("music.deep_ward");
    public static final RegistryObject<SoundEvent> SMOKE = register("sfx.smoke");
    public static final RegistryObject<SoundEvent> DEATH_MARK = register("sfx.death_mark");
    public static final RegistryObject<SoundEvent> EXECUTION = register("sfx.execution");
    public static final RegistryObject<SoundEvent> CLOWN_LAUGH = register("sfx.clown_laugh");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(TokyoGhoulRPG.MOD_ID, name)));
    }
}
