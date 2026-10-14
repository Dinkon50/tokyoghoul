package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.entity.HalfGhoulBoss;
import com.tokyoghoulrpg.registry.ModBlocks;
import com.tokyoghoulrpg.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Состояние арены. Раз в полсекунды проверяет, кто на арене (18 блоков от алтаря).
 * <ul>
 *   <li>Босса ещё не было — появляется, как только на арену зашёл игрок.</li>
 *   <li>Босса убили — сверху экрана у всех на арене таймер 2 минуты (полоса там же, где было здоровье босса).
 *       Таймер идёт, пока на арене кто-то есть; когда дойдёт до нуля, босс появляется снова.
 *       Возрождённый босс, кроме обычной награды, даёт печать сброса навыков.</li>
 *   <li>Босс пропал без победы (например, его убрали командой) — через 10 секунд появится новый.</li>
 * </ul>
 */
public class ArenaAltarBlockEntity extends BlockEntity {
    private UUID bossId = null;
    private int kills = 0;
    private int respawnTicks = -1;
    private int missingTicks = 0;
    private ServerBossEvent timerBar = null;

    public ArenaAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ARENA_ALTAR_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArenaAltarBlockEntity altar) {
        if (level.getGameTime() % 10 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        altar.update(server);
    }

    private boolean inArena(ServerPlayer player) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        double dx = player.getX() - center.x;
        double dz = player.getZ() - center.z;
        double dy = player.getY() - center.y;
        return dx * dx + dz * dz <= Balance.HG_ARENA_RADIUS * Balance.HG_ARENA_RADIUS && dy > -4.0 && dy < 14.0;
    }

    private void update(ServerLevel level) {
        List<ServerPlayer> inside = level.getPlayers(p -> p.isAlive() && !p.isSpectator() && inArena(p));
        if (bossId != null) {
            Entity boss = level.getEntity(bossId);
            if (boss != null && boss.isAlive()) {
                missingTicks = 0;
                updateBar(inside);
                return;
            }
            // босса нет: ещё не загрузился или пропал без победы
            missingTicks += 10;
            if (missingTicks < 200 || inside.isEmpty()) {
                return;
            }
            bossId = null;
            missingTicks = 0;
            setChanged();
        }
        if (respawnTicks >= 0) {
            if (!inside.isEmpty()) {
                respawnTicks = Math.max(0, respawnTicks - 10);
                setChanged();
            }
            updateBar(inside);
            if (respawnTicks == 0 && !inside.isEmpty()) {
                spawn(level, inside);
            }
            return;
        }
        if (!inside.isEmpty()) {
            spawn(level, inside);
        }
    }

    private void spawn(ServerLevel level, List<ServerPlayer> inside) {
        HalfGhoulBoss boss = ModEntities.HALF_GHOUL_BOSS.get().create(level);
        if (boss == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(worldPosition).add(0.0, 1.0, 0.0);
        boss.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        boss.setArena(worldPosition);
        boss.setRespawned(kills > 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(boss);
        bossId = boss.getUUID();
        respawnTicks = -1;
        missingTicks = 0;
        setChanged();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.7F);
        for (ServerPlayer player : inside) {
            player.displayClientMessage(Component.translatable(kills > 0
                    ? "message.tokyoghoulrpg.half_ghoul_boss.returned" : "message.tokyoghoulrpg.half_ghoul_boss.awakened"), true);
        }
        updateBar(inside);
    }

    /** Босс побеждён: запускаем таймер возрождения. */
    public void onBossKilled(HalfGhoulBoss boss) {
        if (bossId != null && !bossId.equals(boss.getUUID())) {
            return;
        }
        bossId = null;
        kills++;
        respawnTicks = Balance.HG_RESPAWN_TICKS;
        setChanged();
    }

    /** Полоса-таймер у тех, кто на арене. Пока таймера нет — полосы нет. */
    private void updateBar(List<ServerPlayer> inside) {
        if (respawnTicks < 0 || bossId != null) {
            if (timerBar != null) {
                timerBar.removeAllPlayers();
            }
            return;
        }
        if (timerBar == null) {
            timerBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        }
        int seconds = (respawnTicks + 19) / 20;
        timerBar.setName(Component.translatable("bossbar.tokyoghoulrpg.half_ghoul_boss.respawn",
                String.format("%d:%02d", seconds / 60, seconds % 60)));
        timerBar.setProgress(Math.max(0.0F, Math.min(1.0F, respawnTicks / (float) Balance.HG_RESPAWN_TICKS)));
        for (ServerPlayer player : new ArrayList<>(timerBar.getPlayers())) {
            if (!inside.contains(player)) {
                timerBar.removePlayer(player);
            }
        }
        for (ServerPlayer player : inside) {
            timerBar.addPlayer(player);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (timerBar != null) {
            timerBar.removeAllPlayers();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (bossId != null) {
            tag.putUUID("Boss", bossId);
        }
        tag.putInt("Kills", kills);
        tag.putInt("Respawn", respawnTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        bossId = tag.hasUUID("Boss") ? tag.getUUID("Boss") : null;
        kills = tag.getInt("Kills");
        respawnTicks = tag.contains("Respawn") ? tag.getInt("Respawn") : -1;
    }
}
