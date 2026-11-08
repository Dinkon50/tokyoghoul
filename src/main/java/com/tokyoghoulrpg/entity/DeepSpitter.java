package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.deep.DeepBalance;
import com.tokyoghoulrpg.entity.goal.ShardVolleyGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * «Плеватель»: гуль с укаку на спине. Держится на расстоянии и стреляет залпами кристаллических осколков
 * (их можно поймать в блок). Если подойти вплотную — отбегает.
 */
public class DeepSpitter extends DeepMonster {
    public DeepSpitter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DeepBalance.SPITTER_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, DeepBalance.SPITTER_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, DeepBalance.SPITTER_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void addAttacks() {
        this.goalSelector.addGoal(1, new ShardVolleyGoal(this, DeepDweller::ally));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 4.5F, 1.0, 1.3,
                e -> e instanceof Player p && !p.isCreative() && !p.isSpectator()));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
    }
}
