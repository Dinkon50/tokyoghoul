package com.tokyoghoulrpg.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tokyoghoulrpg.registry.ModStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

/**
 * Постройка мода, как обычная jigsaw, но только на сухой земле: перед тем как поставить, проверяем
 * девять точек вокруг (центр, углы и середины сторон). Не ставим, если хоть в одной точке сверху вода
 * (озеро, река, болото), если земля не выше уровня моря (низкий берег, там постройки затапливает)
 * или если перепад высот больше max_slope (крутой склон). Поля в JSON те же, что у minecraft:jigsaw,
 * плюс необязательные land_radius и max_slope.
 */
public class LandJigsawStructure extends Structure {
    public static final Codec<LandJigsawStructure> CODEC = RecordCodecBuilder.<LandJigsawStructure>mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
                    Codec.intRange(0, 7).fieldOf("size").forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(s -> s.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(s -> s.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    Codec.intRange(0, 32).optionalFieldOf("land_radius", 10).forGetter(s -> s.landRadius),
                    Codec.intRange(0, 64).optionalFieldOf("max_slope", 6).forGetter(s -> s.maxSlope)
            ).apply(instance, LandJigsawStructure::new)).codec();

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final int landRadius;
    private final int maxSlope;

    public LandJigsawStructure(Structure.StructureSettings settings, Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName, int maxDepth, HeightProvider startHeight, boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, int landRadius, int maxSlope) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.landRadius = landRadius;
        this.maxSlope = maxSlope;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        // постройка начинается в углу чанка и растёт в +X и +Z: проверяем землю вокруг её середины
        if (!onDryLand(context, chunk.getMinBlockX() + 8, chunk.getMinBlockZ() + 8)) {
            return Optional.empty();
        }
        int y = this.startHeight.sample(context.random(),
                new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos start = new BlockPos(chunk.getMinBlockX(), y, chunk.getMinBlockZ());
        return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName, this.maxDepth, start,
                this.useExpansionHack, this.projectStartToHeightmap, this.maxDistanceFromCenter);
    }

    private boolean onDryLand(Structure.GenerationContext context, int cx, int cz) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState random = context.randomState();
        int sea = generator.getSeaLevel();
        int r = this.landRadius;
        int[][] points = {{0, 0}, {r, r}, {-r, r}, {r, -r}, {-r, -r}, {r, 0}, {-r, 0}, {0, r}, {0, -r}};
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int[] p : points) {
            int x = cx + p[0];
            int z = cz + p[1];
            int top = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
            NoiseColumn column = generator.getBaseColumn(x, z, height, random);
            BlockState state = column.getBlock(top);
            if (!state.getFluidState().isEmpty()) {
                return false;                       // сверху вода (или лава)
            }
            if (top <= sea) {
                return false;                       // низко, у самой воды: такие места затапливает
            }
            min = Math.min(min, top);
            max = Math.max(max, top);
        }
        return max - min <= this.maxSlope;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.LAND_JIGSAW.get();
    }
}
