package com.tokyoghoulrpg.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Скупщик с чёрного рынка (оригинальный персонаж). Стоит за прилавком, никуда не уходит, ранить его нельзя.
 * ПКМ — лавка: покупает добычу (RC-сталь, алмазы, изумруды, золото...) за очки крови
 * и продаёт аптечки и дымовые шашки. Торгует со всеми классами.
 */
public class FenceNpc extends InformantNpc {
    public FenceNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public int getSkinVariant() {
        return 0;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive()) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            int id = this.getId();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.tokyoghoulrpg.client.ClientHooks.openFence(id));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
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
