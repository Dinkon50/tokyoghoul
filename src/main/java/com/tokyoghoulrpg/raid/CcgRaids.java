package com.tokyoghoulrpg.raid;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Рейды CCG. За гулями и полугулями (и за людьми в розыске: репутация у CCG -50 и ниже)
 * время от времени приходит отряд агентов. Обычно раз в 20 минут, при плохой репутации у CCG
 * вдвое чаще и отряд больше. С репутацией у CCG от 50 рейдов нет. Только в верхнем мире,
 * не на мирной сложности, не в творческом режиме.
 */
public final class CcgRaids {
    private CcgRaids() {
    }

    /** Кого CCG ищут. */
    public static boolean isHunted(PlayerData data) {
        if (!data.hasChosenClass() || data.getRepCcg() >= Balance.REP_FRIENDLY) {
            return false;
        }
        return data.getPlayerClass().hasKagune() || data.getRepCcg() <= Balance.RAID_WANTED_REP;
    }

    /** «Жар»: 0 при репутации у CCG 0 и выше, 1 при -100. */
    private static double heat(PlayerData data) {
        return Math.max(0, -data.getRepCcg()) / 100.0;
    }

    private static long interval(PlayerData data, RandomSource random) {
        double factor = 1.0 - (1.0 - Balance.RAID_INTERVAL_MIN_FACTOR) * heat(data);
        // разброс ±25%, чтобы рейд нельзя было ждать по часам
        return Math.round(Balance.RAID_INTERVAL * factor * (0.75 + random.nextDouble() * 0.5));
    }

    /** Раз в несколько секунд для каждого игрока. */
    public static void tick(ServerPlayer player, PlayerData data) {
        long now = player.level().getGameTime();
        if (now % Balance.RAID_CHECK_TICKS != 0) {
            return;
        }
        if (!isHunted(data)) {
            data.setNextRaid(0L);
            return;
        }
        if (data.getNextRaid() <= 0L) {
            data.setNextRaid(now + interval(data, player.getRandom()));
            return;
        }
        if (now < data.getNextRaid() || !canRaidNow(player)) {
            return;
        }
        if (start(player)) {
            data.setNextRaid(now + interval(data, player.getRandom()));
        }
    }

    static boolean canRaidNow(ServerPlayer player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && player.level().dimension() == Level.OVERWORLD
                && player.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    /** Отправить отряд за игроком прямо сейчас. Возвращает true, если хоть один агент появился. */
    public static boolean start(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        PlayerData data = PlayerData.get(player);
        double heat = data == null ? 0.0 : heat(data);
        int size = Balance.RAID_SQUAD_MIN
                + (int) Math.round((Balance.RAID_SQUAD_MAX - Balance.RAID_SQUAD_MIN) * heat)
                + (level.getDifficulty() == Difficulty.HARD ? 1 : 0);
        RandomSource random = player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;      // отряд приходит с одной стороны
        int spawned = 0;
        for (int i = 0; i < size; i++) {
            BlockPos pos = findSpot(level, player, angle + (random.nextDouble() - 0.5) * 0.8, random);
            if (pos == null) {
                continue;
            }
            CcgAgentNpc agent = ModEntities.CCG_AGENT.get().create(level);
            if (agent == null) {
                continue;
            }
            agent.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            agent.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            agent.startRaid(player);
            level.addFreshEntity(agent);
            spawned++;
        }
        if (spawned > 0) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.raid", spawned));
            Effects.sound(player, SoundEvents.BELL_BLOCK, 1.5F, 0.7F);
        }
        return spawned > 0;
    }

    /** Место на поверхности в 18-26 блоках от игрока: твёрдый блок под ногами, два блока воздуха. */
    static BlockPos findSpot(ServerLevel level, ServerPlayer player, double angle, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double distance = Balance.RAID_DIST_MIN + random.nextDouble() * (Balance.RAID_DIST_MAX - Balance.RAID_DIST_MIN);
            double a = angle + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 1.5);
            int x = (int) Math.floor(player.getX() + Math.cos(a) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(a) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            // если игрок под землёй, отряд всё равно идёт по поверхности: найдёт по запаху
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getFluidState(below).isEmpty()) {
                return pos;
            }
        }
        return null;
    }
}
