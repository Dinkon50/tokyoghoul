package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.clan.ClanTerritory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Знамя клана: ставит глава, вокруг — территория клана. ПКМ — сведения о территории,
 * глава с Shift + ПКМ призывает кланового босса. Вся логика — в {@link ClanTerritory}.
 */
public class ClanBannerBlock extends Block {
    public ClanBannerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            ClanTerritory.onPlaced(player, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            ClanTerritory.onUse(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
