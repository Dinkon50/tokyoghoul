package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Алтарь арены Кровавого полугуля. Стоит в центре арены, сломать нельзя.
 * Призывает босса, когда на арену заходит игрок, и возрождает его через 2 минуты после победы.
 */
public class ArenaAltarBlock extends BaseEntityBlock {
    public ArenaAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArenaAltarBlockEntity(pos, state);
    }

    /** BaseEntityBlock по умолчанию невидим: рисуем обычную модель блока. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlocks.ARENA_ALTAR_ENTITY.get(), ArenaAltarBlockEntity::serverTick);
    }
}
