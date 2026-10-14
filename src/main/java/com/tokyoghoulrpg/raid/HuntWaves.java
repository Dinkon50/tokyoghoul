package com.tokyoghoulrpg.raid;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.Hunt;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.rep.Standing;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Волны NPC за игроком с плохой репутацией (своей или клана, см. {@link Standing}).
 * Волна — гули и агенты CCG вперемешку, друг друга не трогают и идут только за этим игроком.
 * <ul>
 *   <li>большая: 6 NPC (на сложной 8), примерно раз в 15 минут;</li>
 *   <li>маленькая: 3 NPC (на сложной 4), примерно раз в 20 минут;</li>
 *   <li>первая приходит через ~3 минуты после того, как репутация упала; пока прошлая волна рядом, новая ждёт.</li>
 * </ul>
 * Только в верхнем мире, не на мирной сложности, не в творческом режиме.
 */
public final class HuntWaves {
    private static final int RETRY_TICKS = 60 * 20;

    private HuntWaves() {
    }

    /** Раз в 5 секунд из {@link Standing#tick}: какая волна положена игроку сейчас. */
    public static void tick(ServerPlayer player, PlayerData data, Standing.Wave wave) {
        long now = player.level().getGameTime();
        if (wave == Standing.Wave.NONE) {
            data.setNextWave(0L);
            return;
        }
        if (data.getNextWave() <= 0L) {
            data.setNextWave(now + spread(Balance.WAVE_FIRST_DELAY, player.getRandom()));
            return;
        }
        if (now < data.getNextWave() || !CcgRaids.canRaidNow(player)) {
            return;
        }
        // прошлая волна ещё рядом — новая подождёт
        if (Hunt.countFor(player) > 0) {
            data.setNextWave(now + RETRY_TICKS);
            return;
        }
        boolean big = wave == Standing.Wave.BIG;
        if (start(player, big) > 0) {
            data.setNextWave(now + spread(big ? Balance.WAVE_BIG_INTERVAL : Balance.WAVE_SMALL_INTERVAL, player.getRandom()));
        } else {
            data.setNextWave(now + RETRY_TICKS);
        }
    }

    /** ±25%, чтобы волну нельзя было ждать по часам. */
    private static long spread(int ticks, RandomSource random) {
        return Math.round(ticks * (0.75 + random.nextDouble() * 0.5));
    }

    /** Отправить волну прямо сейчас. Возвращает, сколько NPC появилось. */
    public static int start(ServerPlayer player, boolean big) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        boolean hard = level.getDifficulty() == Difficulty.HARD;
        int size = big ? Balance.WAVE_BIG_SIZE + (hard ? 2 : 0) : Balance.WAVE_SMALL_SIZE + (hard ? 1 : 0);
        RandomSource random = player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;      // волна приходит с одной стороны
        boolean ghoul = random.nextBoolean();                    // гули и агенты через одного
        long until = level.getGameTime() + Balance.WAVE_LIFETIME;
        int spawned = 0;
        for (int i = 0; i < size; i++) {
            BlockPos pos = CcgRaids.findSpot(level, player, angle + (random.nextDouble() - 0.5) * 0.9, random);
            ghoul = !ghoul;
            if (pos == null) {
                continue;
            }
            Mob mob = ghoul ? ModEntities.GHOUL.get().create(level) : ModEntities.CCG_AGENT.get().create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            Hunt.start(mob, player, until);
            level.addFreshEntity(mob);
            spawned++;
        }
        if (spawned > 0) {
            player.sendSystemMessage(Component.translatable(big
                    ? "message.tokyoghoulrpg.wave.big" : "message.tokyoghoulrpg.wave.small", spawned));
            Effects.sound(player, SoundEvents.BELL_BLOCK, 1.5F, big ? 0.5F : 0.8F);
            if (big) {
                Effects.sound(player, SoundEvents.RAVAGER_ROAR, 1.0F, 0.7F);
            }
        }
        return spawned;
    }
}
