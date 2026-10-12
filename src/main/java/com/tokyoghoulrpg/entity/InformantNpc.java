package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Информатор. Нейтрален ко всем сторонам: ПКМ открывает ежедневные контракты
 * (охота, доставка, охота на босса). Не дерётся, при ударе убегает. Не исчезает.
 */
public class InformantNpc extends PathfinderMob implements SkinVariant {
    /** Внешности: парень в кепке и толстовке, женщина в плаще. */
    public static final int VARIANTS = 2;
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(InformantNpc.class, EntityDataSerializers.INT);

    public InformantNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
    }

    @Override
    public int getSkinVariant() {
        return this.entityData.get(VARIANT);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        this.entityData.set(VARIANT, level.getRandom().nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // С биркой в руке — переименовать, а не открыть окно
        if (this.isAlive() && !player.getItemInHand(hand).is(Items.NAME_TAG)) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.openContracts(serverPlayer, this.getId());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkinVariant", getSkinVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, Mth.clamp(tag.getInt("SkinVariant"), 0, VARIANTS - 1));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
