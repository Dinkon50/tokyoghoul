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
 * Знак кофейни: блок в полу кофейни, на нём стоит хозяин. Отмечает мирную зону вокруг себя,
 * открывает кофейню днём и закрывает ночью. Сломать нельзя.
 */
public class CafeSignBlock extends BaseEntityBlock {
    public CafeSignBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CafeSignBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlocks.CAFE_SIGN_ENTITY.get(), CafeSignBlockEntity::serverTick);
    }
}
