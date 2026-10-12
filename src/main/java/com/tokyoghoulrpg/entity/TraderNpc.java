package com.tokyoghoulrpg.entity;

import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Торговец. ПКМ открывает лавку: инъекции за очки крови. Оттуда кнопкой открывается обычная
 * торговля за изумруды: маски и одежда, покупка гнилой плоти и костей. Убегает от гулей.
 */
public class TraderNpc extends AbstractVillager implements SkinVariant {
    /** Внешности: старик в фартуке и кепке, торговка в платке. */
    public static final int VARIANTS = 2;
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(TraderNpc.class, EntityDataSerializers.INT);

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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkinVariant", getSkinVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, Mth.clamp(tag.getInt("SkinVariant"), 0, VARIANTS - 1));
    }

    public TraderNpc(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, GhoulNpc.class, 8.0F, 0.5, 0.5));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData spawnData, CompoundTag tag) {
        this.entityData.set(VARIANT, level.getRandom().nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            // Сначала открывается лавка за очки крови, оттуда можно перейти к торговле за изумруды
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHandler.openBloodShop(serverPlayer, this.getId());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(player, hand);
    }

    /** Обычная торговля за изумруды (кнопка в лавке). */
    public void openEmeraldTrade(Player player) {
        if (!this.isTrading() && !this.getOffers().isEmpty()) {
            this.setTradingPlayer(player);
            this.openTradingScreen(player, this.getDisplayName(), 1);
        }
    }

    /**
     * Торговля за изумруды: маски и одежда, плоть и кофе для гулей, покупка гнилой плоти и костей. Инъекции продаются
     * только за очки крови (лавка, shop/BloodShop). Цены временные. Последние три числа:
     * сколько раз можно купить до пополнения, опыт, множитель цены.
     */
    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        offers.add(sell(5, new ItemStack(ModItems.MASK_FANG.get())));
        offers.add(sell(5, new ItemStack(ModItems.MASK_BONE.get())));
        offers.add(sell(5, new ItemStack(ModItems.MASK_CROW.get())));
        offers.add(sell(5, new ItemStack(ModItems.MASK_ONI.get())));
        offers.add(sell(6, new ItemStack(ModItems.GHOUL_COAT.get())));
        offers.add(sell(5, new ItemStack(ModItems.GHOUL_PANTS.get())));
        offers.add(sell(3, new ItemStack(ModItems.GHOUL_BOOTS.get())));
        offers.add(sell(3, new ItemStack(ModItems.FLESH.get(), 2)));        // еда гуля
        offers.add(sell(2, new ItemStack(ModItems.COFFEE.get())));
        offers.add(new MerchantOffer(new ItemStack(Items.ROTTEN_FLESH, 24), new ItemStack(Items.EMERALD), 16, 1, 0.05F));
        offers.add(new MerchantOffer(new ItemStack(Items.BONE, 16), new ItemStack(Items.EMERALD), 16, 1, 0.05F));
    }

    private static MerchantOffer sell(int emeralds, ItemStack result) {
        return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), result, 4, 5, 0.05F);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        // Опыт за торговлю у торговца не копится: уровней у него нет
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    public boolean showProgressBar() {
        return false;
    }

    /** Торговец не исчезает, когда игрок уходит далеко. */
    /** Звук сделки у торговца. Без @Override: так код верен, абстрактный этот метод у предка или готовый. */
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
