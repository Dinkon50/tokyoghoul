package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.ShardFxS2C;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Осколки «Укаку Тоуки»: летящие снаряды, которые целиком живут на сервере (сущности не нужны).
 * Каждый тик осколок пролетает свою скорость вперёд, упирается в блок или в первое существо на пути,
 * Игроки рядом видят его как летящий кристаллический шип (ShardFxS2C, client/ShardSpikes), огня нет. Попадание — обычный удар способности
 * (CombatContext.hit): блок, ярость, ветки «Урон» и «Кровотечение» работают так же, как у других ударов.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class Shards {
    /** Тёмно-красная крошка шипов (попадание, удар о стену, след большого шипа). */
    public static final DustParticleOptions SPIKE_DUST = new DustParticleOptions(new Vector3f(0.72F, 0.08F, 0.04F), 0.6F);
    public static final DustParticleOptions SPIKE_DUST_BIG = new DustParticleOptions(new Vector3f(0.6F, 0.05F, 0.03F), 1.2F);
    /** Большой шип разбивается на красные осколки, как стекло. */
    public static final BlockParticleOption SHATTER = new BlockParticleOption(ParticleTypes.BLOCK,
            Blocks.RED_STAINED_GLASS.defaultBlockState());
    /** Шип виден игрокам в этом радиусе. */
    private static final double FX_RANGE = 96.0;
    private static int nextId = 1;
    /** Размеры летящих шипов (их рисует client/ShardSpikes). */
    public static final int SIZE_TINY = 0;
    public static final int SIZE_SMALL = 1;
    public static final int SIZE_MEDIUM = 2;
    public static final int SIZE_BIG = 3;
    /** Лишнее не копим: в игре с десятками стрелков осколков не больше этого. */
    private static final int MAX_LIVE = 2500;

    /** Один осколок. */
    public static final class Shard {
        final ServerLevel level;
        final UUID owner;
        Vec3 pos;
        final Vec3 velocity;
        double left;
        final float damage;
        final AttackType type;
        final boolean big;
        int pierce;
        final double splash;
        /** Эффект попадания у града: цель на полсекунды замедлена. */
        boolean slows;
        /** Осколок, выпущенный дугой «Электрического разряда», сам дугу не даёт. */
        boolean noArc;
        final Set<UUID> hit = new HashSet<>();
        /** Номер для клиентов (летящий шип). */
        int id;

        public Shard(ServerLevel level, UUID owner, Vec3 pos, Vec3 direction, double speed, double range, float damage,
                AttackType type, boolean big, int pierce, double splash) {
            this.level = level;
            this.owner = owner;
            this.pos = pos;
            this.velocity = direction.normalize().scale(speed);
            this.left = range;
            this.damage = damage;
            this.type = type;
            this.big = big;
            this.pierce = pierce;
            this.splash = splash;
        }

        public Shard slows() {
            this.slows = true;
            return this;
        }

        public float damage() {
            return damage;
        }

        public AttackType type() {
            return type;
        }

        public boolean big() {
            return big;
        }

        public boolean noArc() {
            return noArc;
        }
    }

    private static final List<Shard> LIVE = new ArrayList<>();

    private Shards() {
    }

    public static void spawn(Shard shard) {
        if (LIVE.size() < MAX_LIVE) {
            LIVE.add(shard);
            shard.id = nextId++;
            if (nextId == Integer.MAX_VALUE) {
                nextId = 1;
            }
            // большой осколок (ПКМ) — большой шип, ульта — средние, остальное (ЛКМ) — маленькие
            int size = shard.big ? SIZE_BIG : shard.type == AttackType.ULTIMATE ? SIZE_MEDIUM : SIZE_SMALL;
            send(shard, ShardFxS2C.spawn(shard.id, shard.pos.x, shard.pos.y, shard.pos.z,
                    shard.velocity.x, shard.velocity.y, shard.velocity.z, shard.left, size));
        }
    }

    /**
     * Только видимый шип без урона (урон уже нанесён сам): например, «Рикошет» от одной цели к другой.
     * Клиент сам ведёт его и убирает, пролетев range блоков.
     */
    public static void visual(ServerLevel level, Vec3 from, Vec3 velocity, double range, int size) {
        if (range < 0.05 || velocity.lengthSqr() < 1.0E-8) {
            return;
        }
        int id = nextId++;
        if (nextId == Integer.MAX_VALUE) {
            nextId = 1;
        }
        NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                from.x, from.y, from.z, FX_RANGE, level.dimension())),
                ShardFxS2C.spawn(id, from.x, from.y, from.z, velocity.x, velocity.y, velocity.z, range, size));
    }

    private static void send(Shard s, ShardFxS2C packet) {
        NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                s.pos.x, s.pos.y, s.pos.z, FX_RANGE, s.level.dimension())), packet);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LIVE.isEmpty()) {
            return;
        }
        List<Shard> alive = new ArrayList<>(LIVE.size());
        for (Shard shard : new ArrayList<>(LIVE)) {
            if (step(shard)) {
                alive.add(shard);
            } else {
                send(shard, ShardFxS2C.end(shard.id));
            }
        }
        LIVE.clear();
        LIVE.addAll(alive);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LIVE.clear();
    }

    /** Один тик полёта. Возвращает false, когда осколок закончился. */
    private static boolean step(Shard s) {
        if (s.level.getServer() == null) {
            return false;
        }
        ServerPlayer owner = s.level.getServer().getPlayerList().getPlayer(s.owner);
        if (owner == null || !owner.isAlive() || owner.level() != s.level) {
            return false;
        }
        double length = Math.min(s.velocity.length(), s.left);
        if (length <= 1.0E-4) {
            return false;
        }
        Vec3 dir = s.velocity.normalize();
        Vec3 from = s.pos;
        Vec3 to = from.add(dir.scale(length));

        // блок на пути
        BlockHitResult blockHit = s.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner));
        boolean blocked = blockHit.getType() != HitResult.Type.MISS;
        Vec3 end = blocked ? blockHit.getLocation() : to;

        // первое существо на пути до блока
        double radius = s.big ? 0.35 : 0.12;
        LivingEntity best = null;
        Vec3 bestPoint = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = new AABB(from, end).inflate(0.5);
        for (Entity entity : s.level.getEntities(owner, box, e -> e instanceof LivingEntity living
                && Targeting.valid(owner, living) && !Combos.spared(owner, living) && !s.hit.contains(e.getUUID()))) {
            AABB body = entity.getBoundingBox().inflate(radius);
            Optional<Vec3> point = body.clip(from, end);
            if (point.isEmpty() && body.contains(from)) {
                point = Optional.of(from);
            }
            if (point.isPresent()) {
                double d = from.distanceToSqr(point.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = (LivingEntity) entity;
                    bestPoint = point.get();
                }
            }
        }

        if (best != null) {
            trail(s, from, bestPoint);
            impact(s, owner, best, bestPoint);
            s.hit.add(best.getUUID());
            if (s.pierce > 0) {
                s.pierce--;
                s.pos = bestPoint;
                s.left -= Math.sqrt(bestDist);
                return s.left > 0.0;
            }
            return false;
        }
        trail(s, from, end);
        if (blocked) {
            if (s.big) {
                explode(s, owner, end, null);
            } else {
                // шип воткнулся в стену и раскрошился
                s.level.sendParticles(SPIKE_DUST, end.x, end.y, end.z, 3, 0.08, 0.08, 0.08, 0.02);
                s.level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0.05);
            }
            return false;
        }
        s.pos = end;
        s.left -= length;
        return s.left > 0.0;
    }

    /** След: маленькие шипы летят чисто (их рисует клиент), за большим тянется редкая тёмно-красная крошка. */
    private static void trail(Shard s, Vec3 from, Vec3 to) {
        if (!s.big) {
            return;
        }
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 p = from.add(delta.scale(0.5));
        s.level.sendParticles(SPIKE_DUST_BIG, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
    }

    /** Попадание в существо. */
    private static void impact(Shard s, ServerPlayer owner, LivingEntity target, Vec3 point) {
        Vec3 before = target.getDeltaMovement();
        boolean hurt;
        if (s.type == AttackType.ULTIMATE) {
            hurt = CombatContext.hitUltimate(owner, target, (float) (s.damage * ultimateFactor(target)));
        } else {
            hurt = CombatContext.hit(owner, target, s.damage, s.type);
        }
        if (hurt && !s.big) {
            // быстрая очередь не должна отталкивать цель от стрелка: оставляем четверть отдачи
            Vec3 after = target.getDeltaMovement();
            target.setDeltaMovement(before.x + (after.x - before.x) * 0.25, after.y,
                    before.z + (after.z - before.z) * 0.25);
        }
        if (hurt && s.slows && target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WingsBalance.HAIL_SLOW_TICKS, 1, false, false));
        }
        if (s.big) {
            explode(s, owner, point, target);
        } else if (hurt) {
            s.level.sendParticles(SPIKE_DUST, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.03);
        } else {
            // блок погасил осколок
            s.level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.1);
        }
        WingsFire.onHit(owner, target, s, hurt);
    }

    /** Люди и агенты CCG получают от ульты 75%, как от ульты щупалец. */
    private static double ultimateFactor(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            com.tokyoghoulrpg.data.PlayerData data = com.tokyoghoulrpg.data.PlayerData.get(player);
            if (data != null && !data.getPlayerClass().hasKagune()) {
                return Balance.ULTIMATE_HUMAN_FACTOR;
            }
        }
        return 1.0;
    }

    /** Большой осколок разбивается: вспышка, а с III уровня «Крупного осколка» ещё и урон вокруг. */
    private static void explode(Shard s, ServerPlayer owner, Vec3 point, LivingEntity direct) {
        s.level.sendParticles(SHATTER, point.x, point.y, point.z, 20, 0.3, 0.3, 0.3, 0.15);
        s.level.sendParticles(SPIKE_DUST_BIG, point.x, point.y, point.z, 8, 0.3, 0.3, 0.3, 0.02);
        s.level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 6, 0.3, 0.3, 0.3, 0.2);
        s.level.playSound(null, point.x, point.y, point.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
        s.level.playSound(null, point.x, point.y, point.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F);
        if (s.splash <= 0.0) {
            return;
        }
        s.level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        AABB area = new AABB(point, point).inflate(s.splash);
        for (LivingEntity other : s.level.getEntitiesOfClass(LivingEntity.class, area, e -> e != direct
                && Targeting.valid(owner, e) && !Combos.spared(owner, e)
                && e.getBoundingBox().distanceToSqr(point) <= s.splash * s.splash)) {
            if (CombatContext.hit(owner, other, (float) (s.damage * WingsBalance.BIG_SPLASH_SHARE), AttackType.BASIC)) {
                Effects.blood(other, 6);
            }
        }
    }
}
