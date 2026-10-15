package com.tokyoghoulrpg.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Мастерская масок. ПКМ открывает меню заказа (всё остальное проверяет сервер при заказе). */
public class MaskWorkshopBlock extends Block {
    public MaskWorkshopBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tokyoghoulrpg.client.ClientHooks.openMaskWorkshop(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
