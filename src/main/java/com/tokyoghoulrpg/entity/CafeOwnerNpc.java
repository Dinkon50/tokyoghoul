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
import net.minecraft.world.level.Level;

/**
 * Хозяин кофейни. Стоит за стойкой на знаке кофейни, никуда не уходит, его нельзя ранить.
 * ПКМ — меню кофейни: кофе и мясо за очки крови (гулям раз в сутки бесплатно) и задания
 * (те же ежедневные контракты, что у информатора).
 */
public class CafeOwnerNpc extends InformantNpc {
    public CafeOwnerNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Одна внешность (свой скин). */
    @Override
    public int getSkinVariant() {
        return 0;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive()) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.openCafe(serverPlayer, this.getId());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
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
