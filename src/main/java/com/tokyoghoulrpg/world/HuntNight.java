package com.tokyoghoulrpg.world;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.registry.ModEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * «Ночь охоты». Каждую третью ночь (дни 3, 6, 9... по счёту мира) с 13000 до 23000 по миру бродят
 * усиленные отряды CCG и стаи гулей: рядом с каждым игроком в верхнем мире примерно раз в 1,5 минуты
 * появляется группа. Ночные охотники сильнее обычных (+50% здоровья, +25% урона, +10% скорости,
 * чуют дальше), а очки крови за NPC и боссов этой ночью двойные; за каждого ночного охотника
 * ещё очко навыков. На рассвете охотники уходят.
 * <p>
 * Ночь охоты считается по времени мира, поэтому сервер и клиент видят её одинаково и ничего не сохраняется.
 * Не на мирной сложности, не в творческом режиме и не в наблюдателе.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class HuntNight {
    public static final String TAG = "tgrpg_night_hunter";
    private static final UUID HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea3");
    private static final UUID DAMAGE_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea4");
    private static final UUID SPEED_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea5");
    private static final UUID FOLLOW_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea6");

    /** Шла ли ночь охоты на прошлой проверке (чтобы объявить начало и конец один раз). */
    private static boolean wasActive = false;
    private static long warnedDay = -1L;
    /** Игрок → когда к нему придёт следующая группа. */
    private static final Map<UUID, Long> NEXT_GROUP = new HashMap<>();

    private HuntNight() {
    }

    // ------------------------------------------------------------------ время

    private static long day(Level level) {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    private static long timeOfDay(Level level) {
        return Math.floorMod(level.getDayTime(), 24000L);
    }

    /** Сегодня ночь охоты (день 3, 6, 9...). */
    private static boolean huntDay(Level level) {
        return Math.floorMod(day(level), Balance.HUNT_NIGHT_EVERY_DAYS) == Balance.HUNT_NIGHT_EVERY_DAYS - 1;
    }

    /** Идёт ли ночь охоты в этом мире прямо сейчас. Работает и на клиенте. */
    public static boolean isActive(Level level) {
        if (level == null || level.dimension() != Level.OVERWORLD || !huntDay(level)) {
            return false;
        }
        long t = timeOfDay(level);
        return t >= Balance.HUNT_NIGHT_START && t < Balance.HUNT_NIGHT_END;
    }

    /** Ночь охоты в верхнем мире сервера (награды считаются по ней в любом измерении). */
    public static boolean isActive(MinecraftServer server) {
        return server != null && isActive(server.overworld());
    }

    /** Через сколько игровых дней следующая ночь охоты (0 — сегодня). Для «Профиля». */
    public static int daysUntil(Level level) {
        long d = Math.floorMod(day(level), Balance.HUNT_NIGHT_EVERY_DAYS);
        int left = (int) (Balance.HUNT_NIGHT_EVERY_DAYS - 1 - d);
        if (left == 0 && timeOfDay(level) >= Balance.HUNT_NIGHT_END) {
            left = Balance.HUNT_NIGHT_EVERY_DAYS;
        }
        return left;
    }

    public static boolean isHunter(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(TAG);
    }

    // ------------------------------------------------------------------ сервер

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        boolean active = isActive(level);
        long t = timeOfDay(level);
        // предупреждение на закате
        if (!active && huntDay(level) && t >= Balance.HUNT_NIGHT_WARNING && t < Balance.HUNT_NIGHT_START
                && warnedDay != day(level)) {
            warnedDay = day(level);
            broadcast(server, Component.translatable("message.tokyoghoulrpg.hunt_night.warning")
                    .withStyle(ChatFormatting.GOLD), false);
        }
        if (active && !wasActive) {
            broadcast(server, Component.translatable("message.tokyoghoulrpg.hunt_night.start",
                    Balance.HUNT_NIGHT_REWARD).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            NEXT_GROUP.clear();
        } else if (!active && wasActive) {
            broadcast(server, Component.translatable("message.tokyoghoulrpg.hunt_night.end")
                    .withStyle(ChatFormatting.GRAY), false);
            NEXT_GROUP.clear();
        }
        wasActive = active;
        if (!active || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            UUID id = player.getUUID();
            Long next = NEXT_GROUP.get(id);
            if (next == null) {
                NEXT_GROUP.put(id, now + Balance.HUNT_NIGHT_FIRST_DELAY);
                continue;
            }
            if (now < next) {
                continue;
            }
            int near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64.0),
                    m -> m.isAlive() && isHunter(m)).size();
            if (near < Balance.HUNT_NIGHT_MAX_NEAR) {
                spawnGroup(level, player);
            }
            RandomSource random = player.getRandom();
            NEXT_GROUP.put(id, now + Math.round(Balance.HUNT_NIGHT_GROUP_INTERVAL * (0.75 + random.nextDouble() * 0.5)));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        wasActive = false;
        warnedDay = -1L;
        NEXT_GROUP.clear();
    }

    private static void broadcast(MinecraftServer server, Component message, boolean loud) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
            if (loud) {
                player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5F, 0.6F);
            } else {
                player.playNotifySound(SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 1.0F, 0.6F);
            }
        }
    }

    /**
     * Группа рядом с игроком: отряд CCG или стая гулей (иногда с вожаком-полугулем). Чаще приходит
     * сторона, враждебная игроку: к гулю — CCG, к человеку и агенту — гули.
     */
    public static int spawnGroup(ServerLevel level, ServerPlayer player) {
        RandomSource random = player.getRandom();
        PlayerData data = PlayerData.get(player);
        boolean ghoulPlayer = data != null && data.getPlayerClass().hasKagune();
        boolean enemySide = random.nextDouble() < Balance.HUNT_NIGHT_ENEMY_SIDE;
        boolean ccgSquad = ghoulPlayer == enemySide;
        int size = Balance.HUNT_NIGHT_GROUP_SIZE + (level.getDifficulty() == Difficulty.HARD ? 1 : 0);
        boolean leader = !ccgSquad && random.nextDouble() < Balance.HUNT_NIGHT_LEADER_CHANCE;
        double angle = random.nextDouble() * Math.PI * 2.0;
        int spawned = 0;
        for (int i = 0; i < size; i++) {
            BlockPos pos = findSpot(level, player, angle + (random.nextDouble() - 0.5) * 0.7, random);
            if (pos == null) {
                continue;
            }
            Mob mob;
            if (ccgSquad) {
                mob = ModEntities.CCG_AGENT.get().create(level);
            } else if (leader && i == 0) {
                mob = ModEntities.HALF_GHOUL.get().create(level);
            } else {
                mob = ModEntities.GHOUL.get().create(level);
            }
            if (mob == null) {
                continue;
            }
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            empower(mob, ccgSquad);
            level.addFreshEntity(mob);
            spawned++;
        }
        if (spawned > 0) {
            player.displayClientMessage(Component.translatable(ccgSquad
                    ? "message.tokyoghoulrpg.hunt_night.ccg" : "message.tokyoghoulrpg.hunt_night.ghouls", spawned)
                    .withStyle(ChatFormatting.RED), true);
            Effects.sound(player, ccgSquad ? SoundEvents.BELL_BLOCK : SoundEvents.WOLF_HOWL, 1.0F, 0.7F);
        }
        return spawned;
    }

    /** Ночной охотник: метка и усиление. */
    private static void empower(Mob mob, boolean ccg) {
        mob.getPersistentData().putBoolean(TAG, true);
        add(mob, Attributes.MAX_HEALTH, HEALTH_ID, Balance.HUNT_NIGHT_HEALTH, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(mob, Attributes.ATTACK_DAMAGE, DAMAGE_ID, Balance.HUNT_NIGHT_DAMAGE, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, Balance.HUNT_NIGHT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(mob, Attributes.FOLLOW_RANGE, FOLLOW_ID, Balance.HUNT_NIGHT_FOLLOW, AttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.translatable(ccg ? "entity.tokyoghoulrpg.night_hunter_ccg"
                : "entity.tokyoghoulrpg.night_hunter_ghoul").withStyle(ChatFormatting.DARK_RED));
        mob.setCustomNameVisible(false);
    }

    private static void add(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, "tg_hunt_night", amount, operation));
        }
    }

    /** Из тика ночного охотника (раз в секунду): ночь кончилась — уходит; иногда тёмная дымка. */
    public static void tickMob(Mob mob) {
        if (mob.tickCount % 20 != 0 || !(mob.level() instanceof ServerLevel level) || !isHunter(mob)) {
            return;
        }
        if (!isActive(level.getServer())) {
            level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    12, 0.3, 0.5, 0.3, 0.02);
            mob.discard();
            return;
        }
        level.sendParticles(Effects.BLOOD, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(),
                3, 0.25, 0.4, 0.25, 0.0);
    }

    /** Ночной охотник может пропасть, если игроки ушли далеко. */
    public static boolean despawnsFar(LivingEntity entity) {
        return isHunter(entity);
    }

    /** Место для группы: 20-32 блока от игрока, на поверхности, не в кофейне. */
    private static BlockPos findSpot(ServerLevel level, ServerPlayer player, double angle, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double distance = 20.0 + random.nextDouble() * 12.0;
            double a = angle + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 1.5);
            int x = (int) Math.floor(player.getX() + Math.cos(a) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(a) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getFluidState(below).isEmpty() && !Cafe.contains(level, pos)) {
                return pos;
            }
        }
        return null;
    }
}
