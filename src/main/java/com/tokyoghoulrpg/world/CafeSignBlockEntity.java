package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.entity.CafeOwnerNpc;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.GhoulNpc;
import com.tokyoghoulrpg.entity.HalfGhoulNpc;
import com.tokyoghoulrpg.entity.Hunt;
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
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Состояние кофейни (раз в полсекунды).
 * <ul>
 *   <li>Днём открыта для всех. За минуту до заката у всех внутри сверху таймер «Кофейня закрывается».
 *       На закате всех, кто остался, выводит к двери. Ночью войти нельзя, утром снова открыта.
 *       Если смена дня и ночи выключена, кофейня открыта всегда.</li>
 *   <li>Враждебных мобов, гулей, агентов CCG и волны NPC выставляет за дверь: они ждут снаружи.</li>
 *   <li>Следит за хозяином: если его нет, появляется новый; если отошёл — возвращает за стойку.</li>
 * </ul>
 */
public class CafeSignBlockEntity extends BlockEntity {
    private UUID ownerId = null;
    private int ownerMissing = 0;
    private Vec3 exit = null;
    private ServerBossEvent timerBar = null;

    public CafeSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CAFE_SIGN_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CafeSignBlockEntity cafe) {
        if (level.getGameTime() % 10 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        cafe.update(server);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            Cafe.register(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Cafe.unregister(level, worldPosition);
        clearBar();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Cafe.unregister(level, worldPosition);
        clearBar();
    }

    /** Открыта ли кофейня в это время суток (смена дня выключена — всегда). */
    public static boolean isOpen(Level level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return true;
        }
        return level.getDayTime() % 24000L < Balance.CAFE_CLOSE_TIME;
    }

    private void update(ServerLevel level) {
        Cafe.register(level, worldPosition);
        List<ServerPlayer> inside = level.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && Cafe.inZone(worldPosition, p.getX(), p.getY(), p.getZ()));
        boolean cycle = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        long time = level.getDayTime() % 24000L;
        boolean open = !cycle || time < Balance.CAFE_CLOSE_TIME;
        boolean warning = cycle && time >= Balance.CAFE_CLOSE_TIME - Balance.CAFE_WARNING_TICKS
                && time < Balance.CAFE_CLOSE_TIME;

        if (!open) {
            // закрыто: всех, кто внутри, к двери (творческий режим остаётся)
            for (ServerPlayer player : inside) {
                if (player.isCreative()) {
                    continue;
                }
                Vec3 out = exit(level);
                player.teleportTo(out.x, out.y, out.z);
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cafe.closed"), true);
            }
        }
        updateBar(inside, warning, Balance.CAFE_CLOSE_TIME - time);

        if (level.getGameTime() % 20 == 0) {
            ejectMobs(level);
        }
        if (level.getGameTime() % 100 == 0) {
            keepOwner(level);
        }
    }

    /** Таймер закрытия у тех, кто внутри. */
    private void updateBar(List<ServerPlayer> inside, boolean warning, long ticksLeft) {
        if (!warning) {
            clearBar();
            return;
        }
        if (timerBar == null) {
            timerBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }
        int seconds = (int) Math.max(0L, (ticksLeft + 19) / 20);
        timerBar.setName(Component.translatable("bossbar.tokyoghoulrpg.cafe.closing",
                String.format("%d:%02d", seconds / 60, seconds % 60)));
        timerBar.setProgress(Math.max(0.0F, Math.min(1.0F, ticksLeft / (float) Balance.CAFE_WARNING_TICKS)));
        for (ServerPlayer player : new ArrayList<>(timerBar.getPlayers())) {
            if (!inside.contains(player)) {
                timerBar.removePlayer(player);
            }
        }
        for (ServerPlayer player : inside) {
            timerBar.addPlayer(player);
        }
    }

    private void clearBar() {
        if (timerBar != null) {
            timerBar.removeAllPlayers();
        }
    }

    /** Враждебные мобы, гули, агенты и волны ждут снаружи. */
    private void ejectMobs(ServerLevel level) {
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, Cafe.zoneBox(worldPosition), m -> m.isAlive()
                && !(m instanceof CafeOwnerNpc)
                && (m instanceof Enemy || m instanceof GhoulNpc || m instanceof CcgAgentNpc
                || m instanceof HalfGhoulNpc || Hunt.isHunter(m)));
        if (mobs.isEmpty()) {
            return;
        }
        Vec3 out = exit(level);
        for (Mob mob : mobs) {
            mob.teleportTo(out.x, out.y, out.z);
            mob.getNavigation().stop();
        }
    }

    /** Точка снаружи у двери: дверь ищется в стенах (постройка может быть повёрнута как угодно). */
    private Vec3 exit(ServerLevel level) {
        if (exit != null) {
            return exit;
        }
        int r = (int) Balance.CAFE_RADIUS;
        BlockPos best = null;
        double bestDist = -1.0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                        double d = dx * dx + dz * dz;
                        if (d > bestDist) {
                            bestDist = d;
                            best = p;
                        }
                    }
                }
            }
        }
        int x;
        int z;
        if (best != null) {
            int dx = best.getX() - worldPosition.getX();
            int dz = best.getZ() - worldPosition.getZ();
            // наружу по той оси, вдоль которой дверь дальше от центра
            if (Math.abs(dx) >= Math.abs(dz)) {
                x = best.getX() + Integer.signum(dx) * 2;
                z = best.getZ();
            } else {
                x = best.getX();
                z = best.getZ() + Integer.signum(dz) * 2;
            }
        } else {
            x = worldPosition.getX();
            z = worldPosition.getZ() + r + 2;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        exit = new Vec3(x + 0.5, y, z + 0.5);
        return exit;
    }

    /** Хозяин стоит на знаке. Пропал — через 10 секунд появится новый. */
    private void keepOwner(ServerLevel level) {
        Vec3 spot = Vec3.atBottomCenterOf(worldPosition.above());
        Entity owner = ownerId == null ? null : level.getEntity(ownerId);
        if (owner == null) {
            List<CafeOwnerNpc> found = level.getEntitiesOfClass(CafeOwnerNpc.class, Cafe.zoneBox(worldPosition),
                    Entity::isAlive);
            if (!found.isEmpty()) {
                owner = found.get(0);
                ownerId = owner.getUUID();
                setChanged();
            }
        }
        if (owner != null && owner.isAlive()) {
            ownerMissing = 0;
            if (owner.position().distanceToSqr(spot) > 2.0 * 2.0) {
                owner.teleportTo(spot.x, spot.y, spot.z);
            }
            return;
        }
        ownerMissing += 100;
        if (ownerMissing < 200) {
            return;
        }
        CafeOwnerNpc npc = ModEntities.CAFE_OWNER.get().create(level);
        if (npc == null) {
            return;
        }
        npc.moveTo(spot.x, spot.y, spot.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        npc.finalizeSpawn(level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.STRUCTURE, null, null);
        level.addFreshEntity(npc);
        ownerId = npc.getUUID();
        ownerMissing = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
