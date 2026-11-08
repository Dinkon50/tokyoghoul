package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.deep.DeepBalance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * «Глубинный гуль»: бледный, исхудавший, с чёрными венами. Быстрый, прыгает на цель, бьёт когтями:
 * иногда цепляет — на 2 секунды замедляет. Нападают стаями (позвал один — бегут все рядом).
 */
public class DeepGhoulNpc extends DeepMonster {
    public DeepGhoulNpc(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DeepBalance.GHOUL_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, DeepBalance.GHOUL_ATTACK)
                .add(Attributes.MOVEMENT_SPEED, DeepBalance.GHOUL_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected int variants() {
        return 2;
    }

    @Override
    protected void addAttacks() {
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && this.random.nextFloat() < 0.25F) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        return hit;
    }
}
