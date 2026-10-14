package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Кофейня — мирная зона. Зона — квадрат 17 x 17 и 8 блоков вверх вокруг «Знака кофейни» в полу.
 * <ul>
 *   <li>внутри никого нельзя ударить (и изнутри никого снаружи): удары, способности, стрелы, взрывы;</li>
 *   <li>здание нельзя сломать и нельзя ставить в нём блоки (кроме творческого режима);</li>
 *   <li>враждебные мобы внутри не появляются; тех, кто зашёл, знак выставляет за дверь (CafeSignBlockEntity).</li>
 *   <li>исключение — драка: если два игрока били друг друга (кто-то кого-то) меньше минуты назад,
 *       кофейня их друг от друга не защищает. Каждый удар продлевает минуту.</li>
 * </ul>
 * Знаки регистрируются здесь, пока их чанк загружен (только на сервере).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class Cafe {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> SIGNS = new HashMap<>();
    /** Драки игроков: игрок → (с кем дрался → когда был последний удар). Хранится в обе стороны. */
    private static final Map<UUID, Map<UUID, Long>> FIGHTS = new HashMap<>();

    private Cafe() {
    }

    public static void register(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            SIGNS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
        }
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level != null && !level.isClientSide()) {
            Set<BlockPos> set = SIGNS.get(level.dimension());
            if (set != null) {
                set.remove(pos);
            }
        }
    }

    /** Точка внутри зоны этого знака? */
    public static boolean inZone(BlockPos sign, double x, double y, double z) {
        double dx = x - (sign.getX() + 0.5);
        double dz = z - (sign.getZ() + 0.5);
        double dy = y - sign.getY();
        return Math.abs(dx) <= Balance.CAFE_RADIUS && Math.abs(dz) <= Balance.CAFE_RADIUS
                && dy >= -1.0 && dy <= Balance.CAFE_HEIGHT;
    }

    public static AABB zoneBox(BlockPos sign) {
        double r = Balance.CAFE_RADIUS;
        return new AABB(sign.getX() + 0.5 - r, sign.getY() - 1.0, sign.getZ() + 0.5 - r,
                sign.getX() + 0.5 + r, sign.getY() + Balance.CAFE_HEIGHT, sign.getZ() + 0.5 + r);
    }

    /** Знак кофейни, в зоне которого стоит сущность (null — не в кофейне). */
    public static BlockPos signAt(Level level, double x, double y, double z) {
        if (level == null || level.isClientSide()) {
            return null;
        }
        Set<BlockPos> set = SIGNS.get(level.dimension());
        if (set == null) {
            return null;
        }
        for (BlockPos sign : set) {
            if (inZone(sign, x, y, z)) {
                return sign;
            }
        }
        return null;
    }

    public static boolean inside(Entity entity) {
        return entity != null && signAt(entity.level(), entity.getX(), entity.getY(), entity.getZ()) != null;
    }

    public static boolean contains(LevelAccessor level, BlockPos pos) {
        return level instanceof Level l && signAt(l, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) != null;
    }

    // ------------------------------------------------------------------ драки игроков

    /** Был ли удар между этими двумя игроками меньше минуты назад (в любую сторону). */
    public static boolean fought(UUID a, UUID b, long now) {
        Map<UUID, Long> map = FIGHTS.get(a);
        Long last = map == null ? null : map.get(b);
        return last != null && now - last < Balance.CAFE_FIGHT_TICKS;
    }

    /** Дрался ли игрок с кем-нибудь меньше минуты назад (тогда в кофейне ему можно бить своих противников). */
    public static boolean inFight(Player player) {
        Map<UUID, Long> map = FIGHTS.get(player.getUUID());
        if (map == null) {
            return false;
        }
        long now = player.level().getGameTime();
        map.values().removeIf(last -> now - last >= Balance.CAFE_FIGHT_TICKS);
        return !map.isEmpty();
    }

    /** Удар игрока по игроку прошёл: запомнить драку для обоих. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof Player victim)
                || !(event.getSource().getEntity() instanceof Player attacker) || attacker == victim) {
            return;
        }
        long now = victim.level().getGameTime();
        FIGHTS.computeIfAbsent(victim.getUUID(), k -> new HashMap<>()).put(attacker.getUUID(), now);
        FIGHTS.computeIfAbsent(attacker.getUUID(), k -> new HashMap<>()).put(victim.getUUID(), now);
    }

    // ------------------------------------------------------------------ правила зоны

    /** Ударить внутри нельзя никого, и изнутри наружу тоже. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide() || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if (attacker == null && direct == null) {
            return;     // падение, огонь, утопление — не драка
        }
        if (inside(event.getEntity()) || inside(attacker) || inside(direct)) {
            // драка меньше минуты назад: эти двое могут бить друг друга и здесь
            if (event.getEntity() instanceof Player victim && attacker instanceof Player hitter
                    && fought(victim.getUUID(), hitter.getUUID(), victim.level().getGameTime())) {
                return;
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player != null && !player.isCreative() && contains(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cafe.no_build"), true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && !player.isCreative()
                && contains(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.cafe.no_build"), true);
        }
    }

    /** Взрывы рядом не ломают кофейню и не задевают тех, кто внутри. */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> contains(level, pos));
        event.getAffectedEntities().removeIf(Cafe::inside);
    }

    /** Враждебные мобы внутри не появляются. */
    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy
                && signAt(event.getEntity().level(), event.getX(), event.getY(), event.getZ()) != null) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SIGNS.clear();
        FIGHTS.clear();
    }
}
