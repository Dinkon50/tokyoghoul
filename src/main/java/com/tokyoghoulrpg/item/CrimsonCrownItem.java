package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CrimsonMonarch;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.world.Cafe;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Алая корона — призывает Алого Монарха (финального босса). ПКМ: босс появляется в 6 блоках впереди.
 * Сработает, только если ты уже победил Пожирателя, Старшего следователя и Кровавого полугуля.
 */
public class CrimsonCrownItem extends Item {
    public CrimsonCrownItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel server)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        PlayerData data = PlayerData.get(serverPlayer);
        int killed = data == null ? 0 : data.getBossesKilled();
        if (!player.isCreative() && (killed & 7) != 7) {
            serverPlayer.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.crimson_crown.need",
                    mark(killed, 1, "entity.tokyoghoulrpg.ghoul_boss"),
                    mark(killed, 2, "entity.tokyoghoulrpg.ccg_boss"),
                    mark(killed, 4, "entity.tokyoghoulrpg.half_ghoul_boss")));
            return InteractionResultHolder.fail(stack);
        }
        if (Cafe.inside(player)) {
            serverPlayer.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cafe.peace"), true);
            return InteractionResultHolder.fail(stack);
        }
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        int x = (int) Math.floor(player.getX() + look.x * 6.0);
        int z = (int) Math.floor(player.getZ() + look.z * 6.0);
        int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        CrimsonMonarch boss = ModEntities.CRIMSON_MONARCH.get().create(server);
        if (boss == null) {
            return InteractionResultHolder.fail(stack);
        }
        boss.moveTo(x + 0.5, y, z + 0.5, player.getYRot() + 180.0F, 0.0F);
        boss.finalizeSpawn(server, server.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.EVENT, null, null);
        server.addFreshEntity(boss);
        for (int i = 0; i < 3; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
            if (bolt != null) {
                bolt.moveTo(boss.position().add((i - 1) * 2.0, 0.0, 0.0));
                bolt.setVisualOnly(true);
                server.addFreshEntity(bolt);
            }
        }
        server.playSound(null, boss.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.5F);
        Component broadcast = Component.translatable("message.tokyoghoulrpg.crimson_monarch.summoned",
                player.getGameProfile().getName());
        for (ServerPlayer other : server.getServer().getPlayerList().getPlayers()) {
            other.sendSystemMessage(broadcast);
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static Component mark(int killed, int bit, String key) {
        boolean done = (killed & bit) != 0;
        return Component.literal(done ? "✔ " : "✘ ").append(Component.translatable(key))
                .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.crimson_crown.tooltip").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.tokyoghoulrpg.crimson_crown.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
