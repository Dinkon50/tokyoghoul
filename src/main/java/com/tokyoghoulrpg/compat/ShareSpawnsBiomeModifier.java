package com.tokyoghoulrpg.compat;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.MobSpawnSettingsBuilder;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Появление NPC мода, которое подстраивается под другие моды. Обычный forge:add_spawns даёт фиксированный вес:
 * если другие моды добавят много своих монстров, гулей и агентов CCG станет почти не видно. Этот модификатор
 * срабатывает после всех добавлений (фаза MODIFY), считает общий вес существ той же категории в биоме и ставит
 * нашему вес не меньше доли share от него (но и не меньше веса из файла). Без других модов — всё как раньше.
 * JSON: {"type": "tokyoghoulrpg:share_spawns", "biomes": ..., "spawners": {...} или [...], "share": 0.023}
 */
public record ShareSpawnsBiomeModifier(HolderSet<Biome> biomes, List<MobSpawnSettings.SpawnerData> spawners, float share)
        implements BiomeModifier {
    private static final Codec<List<MobSpawnSettings.SpawnerData>> SPAWNERS =
            Codec.either(MobSpawnSettings.SpawnerData.CODEC.listOf(), MobSpawnSettings.SpawnerData.CODEC)
                    .xmap(e -> e.map(Function.identity(), List::of),
                            list -> list.size() == 1 ? Either.right(list.get(0)) : Either.left(list));

    public static final Codec<ShareSpawnsBiomeModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ShareSpawnsBiomeModifier::biomes),
            SPAWNERS.fieldOf("spawners").forGetter(ShareSpawnsBiomeModifier::spawners),
            Codec.FLOAT.optionalFieldOf("share", 0.0F).forGetter(ShareSpawnsBiomeModifier::share)
    ).apply(instance, ShareSpawnsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY || !biomes.contains(biome)) {
            return;
        }
        MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();
        for (MobSpawnSettings.SpawnerData data : spawners) {
            MobCategory category = data.type.getCategory();
            int total = 0;
            for (MobSpawnSettings.SpawnerData other : spawns.getSpawner(category)) {
                total += other.getWeight().asInt();
            }
            int weight = Math.max(data.getWeight().asInt(), Math.round(total * share));
            spawns.addSpawn(category, new MobSpawnSettings.SpawnerData(data.type, weight, data.minCount, data.maxCount));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ModCompat.SHARE_SPAWNS.get();
    }
}
