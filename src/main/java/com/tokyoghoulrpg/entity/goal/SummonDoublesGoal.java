package com.tokyoghoulrpg.entity.goal;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.ClownBoss;
import com.tokyoghoulrpg.entity.ClownDouble;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.style.StyleBalance;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

/** «Двойники» Клоуна: хлопок в ладоши — и по бокам из дыма выходят двое двойников (20 с). */
public class SummonDoublesGoal extends BossSkillGoal {
    private static final int WINDUP = 14;
    private final ClownBoss clown;

    public SummonDoublesGoal(ClownBoss clown, Predicate<LivingEntity> ally) {
        super(clown, ally, 160, StyleBalance.CLOWN_DOUBLES_COOLDOWN, WINDUP + 4);
        this.clown = clown;
    }

    @Override
    protected boolean ready(LivingEntity target) {
        return mob.level().getEntitiesOfClass(ClownDouble.class, mob.getBoundingBox().inflate(48.0),
                d -> d.isOwnedBy(mob)).isEmpty();
    }

    @Override
    public void start() {
        super.start();
        Effects.sound(mob, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.2F, 0.8F);
    }

    @Override
    protected void step(LivingEntity target) {
        if (ticks != WINDUP || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Effects.sound(mob, com.tokyoghoulrpg.registry.ModSounds.CLOWN_LAUGH.get(), 1.4F, 0.9F);
        Vec3 look = Vec3.directionFromRotation(0.0F, mob.getYRot());
        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        for (int i = 0; i < StyleBalance.CLOWN_DOUBLES; i++) {
            ClownDouble twin = ModEntities.CLOWN_DOUBLE.get().create(level);
            if (twin == null) {
                continue;
            }
            Vec3 p = mob.position().add(side.scale(i % 2 == 0 ? 2.5 : -2.5));
            twin.moveTo(p.x, mob.getY(), p.z, mob.getYRot(), 0.0F);
            if (!level.noCollision(twin)) {
                twin.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0.0F);
            }
            twin.finalizeSpawn(level, level.getCurrentDifficultyAt(twin.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            twin.setOwner(clown);
            twin.setTarget(target);
            level.addFreshEntity(twin);
            clown.smokePuff(twin.position());
        }
    }
}
