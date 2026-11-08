package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Наставник додзё. Стоит у дальней стены зала, никуда не уходит, его нельзя ранить.
 * ПКМ — меню стилей боя: бросить вызов боссу стиля, выбрать или сменить стиль, прокачать ветки.
 */
public class StyleMentorNpc extends InformantNpc {
    public StyleMentorNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public int getSkinVariant() {
        return 0;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !player.getItemInHand(hand).is(Items.NAME_TAG)) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.syncTo(serverPlayer);
                NetworkHandler.openStyles(serverPlayer, this.getId());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }
}
