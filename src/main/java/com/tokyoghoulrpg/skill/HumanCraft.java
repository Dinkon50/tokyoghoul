package com.tokyoghoulrpg.skill;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.AttrUtil;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.BossMob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Человек, вкладка «Тело и ремесло»: то, чего нет у гулей и агентов CCG.
 * «Бегун» — быстрее бег, «Выносливость» — медленнее голод, «Пловец» — быстрее плавание и дольше воздух,
 * «Старатель» — двойная добыча из руды, «Лесоруб и фермер» — из брёвен и спелых растений,
 * «Охотник» — двойная добыча и больше опыта с мобов.
 * У других классов уровни этих веток всегда 0 (SkillEffects.level), поэтому всё само снимается при смене класса.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class HumanCraft {
    private static final UUID RUNNER_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea1");
    private static final UUID SWIMMER_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea2");
    /** Сколько истощения было у игрока на прошлом тике (для «Выносливости»). */
    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();

    private HumanCraft() {
    }

    private static int lvl(Player player, SkillTree.Branch branch) {
        PlayerData data = PlayerData.get(player);
        return data == null ? 0 : SkillEffects.level(data, branch);
    }

    // ------------------------------------------------------------------ тело

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            int runner = SkillEffects.level(data, SkillTree.Branch.RUNNER);
            if (runner > 0) {
                AttrUtil.set(player, Attributes.MOVEMENT_SPEED, RUNNER_ID, "tg_human_runner",
                        Balance.HUMAN_RUNNER * runner, AttributeModifier.Operation.MULTIPLY_TOTAL);
            } else {
                AttrUtil.remove(player, Attributes.MOVEMENT_SPEED, RUNNER_ID);
            }
            int swimmer = SkillEffects.level(data, SkillTree.Branch.SWIMMER);
            if (swimmer > 0) {
                AttrUtil.set(player, ForgeMod.SWIM_SPEED.get(), SWIMMER_ID, "tg_human_swimmer",
                        Balance.HUMAN_SWIMMER * swimmer, AttributeModifier.Operation.MULTIPLY_TOTAL);
            } else {
                AttrUtil.remove(player, ForgeMod.SWIM_SPEED.get(), SWIMMER_ID);
            }
        }
        // «Пловец» III: под водой воздух уходит в 3 раза медленнее (2 тика из 3 возвращаем потраченное)
        if (player.isEyeInFluid(FluidTags.WATER) && player.getAirSupply() < player.getMaxAirSupply()
                && player.tickCount % Balance.HUMAN_SWIMMER_AIR != 0
                && SkillEffects.level(data, SkillTree.Branch.SWIMMER) >= 3) {
            player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 1));
        }
        // «Выносливость»: часть нового истощения возвращаем
        int stamina = SkillEffects.level(data, SkillTree.Branch.STAMINA);
        FoodData food = player.getFoodData();
        float now = food.getExhaustionLevel();
        Float last = LAST_EXHAUSTION.get(player.getUUID());
        if (stamina > 0 && last != null && now > last) {
            float gained = now - last;
            food.setExhaustion(now - gained * (float) (Balance.HUMAN_STAMINA * stamina));
        }
        LAST_EXHAUSTION.put(player.getUUID(), food.getExhaustionLevel());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_EXHAUSTION.remove(event.getEntity().getUUID());
    }

    // ------------------------------------------------------------------ ремесло

    /** Ломает блок: «Старатель» (руда) и «Лесоруб и фермер» (брёвна, спелые растения) — шанс второй добычи. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()
                || player instanceof net.minecraftforge.common.util.FakePlayer
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        double chance = 0.0;
        if (state.is(Tags.Blocks.ORES)) {
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0
                    || !player.hasCorrectToolForDrops(state)) {
                return;
            }
            chance = Balance.HUMAN_PROSPECTOR * lvl(player, SkillTree.Branch.PROSPECTOR);
        } else if (state.is(BlockTags.LOGS)
                || state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            chance = Balance.HUMAN_HARVESTER * lvl(player, SkillTree.Branch.HARVESTER);
        }
        if (chance <= 0.0 || player.getRandom().nextDouble() >= chance) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
        for (ItemStack stack : drops) {
            Block.popResource(level, pos, stack);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                6, 0.3, 0.3, 0.3, 0.0);
    }

    /** «Охотник»: шанс, что с моба выпадет вся добыча второй раз (не с боссов). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || player instanceof net.minecraftforge.common.util.FakePlayer
                || event.getEntity() instanceof Player || event.getEntity() instanceof BossMob) {
            return;
        }
        int trophy = lvl(player, SkillTree.Branch.TROPHY);
        if (trophy <= 0 || player.getRandom().nextDouble() >= Balance.HUMAN_TROPHY * trophy) {
            return;
        }
        for (ItemEntity drop : List.copyOf(event.getDrops())) {
            ItemEntity copy = new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
            copy.setDefaultPickUpDelay();
            event.getDrops().add(copy);
        }
    }

    /** «Охотник»: больше опыта с мобов. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (event.getAttackingPlayer() instanceof ServerPlayer player && !(event.getEntity() instanceof Player)
                && !(player instanceof net.minecraftforge.common.util.FakePlayer)) {
            int trophy = lvl(player, SkillTree.Branch.TROPHY);
            if (trophy > 0) {
                event.setDroppedExperience((int) Math.round(event.getDroppedExperience()
                        * (1.0 + Balance.HUMAN_TROPHY_XP * trophy)));
            }
        }
    }
}
