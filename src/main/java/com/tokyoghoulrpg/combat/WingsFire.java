package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import com.tokyoghoulrpg.skill.WingsEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * «Укаку Тоуки» (крылья, KaguneVariant.WINGS): дальнее кагуне. Крылья не двигаются и не бьют, из них вылетают осколки:
 * <ul>
 *   <li>ЛКМ (держать) — автомат: осколок каждые 3 тика, чем дольше очередь, тем шире разброс;</li>
 *   <li>ПКМ дважды — большой осколок: короткое накопление, медленный тяжёлый снаряд, пробивает блок;</li>
 *   <li>K (держать) — град: короткая очередь по нескольку осколков с широким разбросом, но только вблизи;</li>
 *   <li>Alt + J — ульта «Шквал осколков»: секунда накопления и две секунды сплошного огня;</li>
 *   <li>захватов нет: крылья остаются на месте.</li>
 * </ul>
 * Осколки летят и попадают в Shards. Всё прокачивается ветками W_* (вкладки «Стрельба» и «Ярость, электричество»),
 * формулы — skill/WingsEffects, числа — WingsBalance.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class WingsFire {
    private static final Map<UUID, Gun> GUNS = new HashMap<>();

    /** Состояние автомата игрока: когда следующий выстрел, нагрев ствола (разброс), счётчик попаданий. */
    private static final class Gun {
        double next;
        long last = Long.MIN_VALUE / 2;
        double heat;
        long heatTick;
        boolean left;
        int shots;
        int hits;
        long pauseUntil;
        long lastSync;
        /** Когда последний раз просили клиентов играть анимацию стрельбы (её продлевают, пока идут выстрелы). */
        long lastAnim = Long.MIN_VALUE / 2;
    }

    private WingsFire() {
    }

    /** У игрока сейчас «Укаку Тоуки». */
    public static boolean uses(PlayerData data) {
        return data != null && data.getPlayerClass().hasKagune() && data.getKaguneVariant() == KaguneVariant.WINGS;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GUNS.remove(event.getEntity().getUUID());
    }

    private static Gun gun(ServerPlayer player) {
        return GUNS.computeIfAbsent(player.getUUID(), k -> new Gun());
    }

    private static int level(PlayerData data, SkillTree.Branch branch) {
        return SkillEffects.level(data, branch);
    }

    // ------------------------------------------------------------------
    // Откуда и куда летят осколки
    // ------------------------------------------------------------------

    /**
     * Точка на крыле, откуда вылетает осколок. Крылья за спиной: левое (со стороны игрока) больше правого.
     * center: середина между крыльями (большой осколок).
     */
    private static Vec3 muzzle(ServerPlayer player, boolean leftWing, boolean center) {
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double crouch = player.isCrouching() ? -0.3 : 0.0;
        if (center) {
            return player.position().add(0.0, 1.55 + crouch, 0.0).add(forward.scale(-0.45));
        }
        double t = 0.35 + 0.65 * player.getRandom().nextDouble();
        double reach = leftWing ? 0.9 : 0.65;
        double top = leftWing ? 1.65 : 1.45;
        double lateral = (leftWing ? -1.0 : 1.0) * (0.2 + (reach - 0.2) * t);
        double up = 1.2 + (top - 1.2) * t + crouch;
        return player.position().add(right.scale(lateral)).add(0.0, up, 0.0).add(forward.scale(-0.45));
    }

    /** Направление от крыла к точке, куда смотрит игрок (в VR — куда целится рука). Вплотную — просто по взгляду. */
    private static Vec3 aimDir(ServerPlayer player, Vec3 from, double range) {
        Vec3 look = Aim.dir(player);
        Targeting.Hit hit = Targeting.raycast(player, range, true, e -> !Combos.spared(player, e));
        Vec3 target = hit != null ? hit.point() : Aim.origin(player).add(look.scale(range));
        Vec3 delta = target.subtract(from);
        return delta.lengthSqr() < 4.0 ? look : delta.normalize();
    }

    /** Случайно отклоняет направление внутри конуса в degrees градусов. */
    private static Vec3 spread(ServerPlayer player, Vec3 dir, double degrees) {
        if (degrees <= 0.0) {
            return dir;
        }
        Vec3 helper = Math.abs(dir.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 u = dir.cross(helper).normalize();
        Vec3 v = dir.cross(u).normalize();
        double r = Math.tan(Math.toRadians(degrees)) * Math.sqrt(player.getRandom().nextDouble());
        double phi = player.getRandom().nextDouble() * Math.PI * 2.0;
        return dir.add(u.scale(r * Math.cos(phi))).add(v.scale(r * Math.sin(phi))).normalize();
    }

    private static ServerLevel level(ServerPlayer player) {
        return (ServerLevel) player.level();
    }

    // ------------------------------------------------------------------
    // ЛКМ: автомат
    // ------------------------------------------------------------------

    /**
     * Просьба выстрелить (клиент шлёт её каждый тик, пока держат ЛКМ). Темп задают «Скорострельность» и «Грозовая
     * ярость», а не частота просьб: лишние просьбы сервер пропускает.
     */
    public static void basic(ServerPlayer player, PlayerData data, CombatState state, long now) {
        Gun gun = gun(player);
        if (now < gun.pauseUntil) {
            return;
        }
        int fury = level(data, SkillTree.Branch.W_FURY);
        boolean rage = data.isRageActive();
        double interval = WingsEffects.interval(level(data, SkillTree.Branch.W_RATE), rage, fury);
        if (now - gun.last > WingsBalance.REPEAT_WINDOW) {
            gun.next = now;
        }
        int fired = 0;
        while (gun.next <= now + 1.0E-6 && fired < 3) {
            fireOne(player, data, gun, now, rage, fury);
            gun.next += interval;
            fired++;
        }
        if (fired > 0) {
            gun.last = now;
            // анимация стрельбы крыльями: пока стреляем, раз в 4 тика продлеваем её (она длится 6 тиков)
            if (now - gun.lastAnim >= 4) {
                gun.lastAnim = now;
                NetworkHandler.playAnimation(player, "wings_fire");
            }
        }
    }

    private static void fireOne(ServerPlayer player, PlayerData data, Gun gun, long now, boolean rage, int fury) {
        int crystal = level(data, SkillTree.Branch.W_CRYSTAL);
        int aim = level(data, SkillTree.Branch.W_AIM);
        double range = WingsEffects.range(level(data, SkillTree.Branch.W_RANGE));
        double heat = Math.max(0.0, gun.heat - WingsBalance.HEAT_DECAY_PER_TICK * (now - gun.heatTick));
        double degrees = WingsEffects.spread(aim) * (1.0 + heat * WingsEffects.bloom(aim));
        int count = rage ? WingsEffects.furyShards(fury) : 1;
        float damage = WingsEffects.shardDamage(crystal);
        ServerLevel level = level(player);
        for (int i = 0; i < count; i++) {
            boolean leftWing = count == 2 ? i == 0 : (gun.left = !gun.left);
            Vec3 from = muzzle(player, leftWing, false);
            Vec3 dir = spread(player, aimDir(player, from, range), degrees);
            Shards.spawn(new Shards.Shard(level, player.getUUID(), from, dir, WingsBalance.SHARD_SPEED, range, damage,
                    AttackType.BASIC, false, WingsEffects.pierce(crystal), 0.0));
            level.sendParticles(Shards.SPIKE_DUST, from.x, from.y, from.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
        gun.heat = Math.min(1.0, heat + WingsBalance.HEAT_PER_SHOT);
        gun.heatTick = now;
        if ((gun.shots++ & 1) == 0) {
            Effects.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 1.7F + player.getRandom().nextFloat() * 0.4F);
        }
    }

    // ------------------------------------------------------------------
    // ПКМ дважды: большой осколок
    // ------------------------------------------------------------------

    public static void big(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.strongReady) {
            AbilityManager.cooldown(player, AbilityType.STRONG_ATTACK, state.strongReady - now);
            return;
        }
        state.strongReady = now + WingsEffects.bigCooldown(level(data, SkillTree.Branch.W_BIG));
        gun(player).pauseUntil = now + WingsBalance.BIG_WINDUP + 2;
        state.actions.add(new BigShot(now));
        NetworkHandler.playAnimation(player, "wings_big");
        Effects.sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.6F);
    }

    /** Накопление и выстрел большого осколка. */
    private static final class BigShot implements ActiveAction {
        private final long start;

        private BigShot(long start) {
            this.start = start;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (!player.isAlive() || LockManager.isLocked(player)) {
                return true;
            }
            long elapsed = now - start;
            ServerLevel level = level(player);
            Vec3 center = muzzle(player, false, true);
            if (elapsed < WingsBalance.BIG_WINDUP) {
                level.sendParticles(Shards.SPIKE_DUST_BIG, center.x, center.y, center.z, 3, 0.35, 0.3, 0.35, 0.0);
                level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 2, 0.4, 0.3, 0.4, 0.1);
                return false;
            }
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                int lvl = level(data, SkillTree.Branch.W_BIG);
                double range = WingsBalance.BIG_RANGE;
                Vec3 dir = aimDir(player, center, range);
                Shards.spawn(new Shards.Shard(level, player.getUUID(), center, dir, WingsBalance.BIG_SPEED, range,
                        WingsEffects.bigDamage(lvl), AttackType.STRONG, true, 0, WingsEffects.bigSplash(lvl)));
                level.sendParticles(Shards.SHATTER, center.x, center.y, center.z, 8, 0.2, 0.2, 0.2, 0.1);
                Effects.sound(player, SoundEvents.TRIDENT_THROW, 1.0F, 0.6F);
                Effects.sound(player, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 0.9F);
            }
            return true;
        }

        @Override
        public void cancel(ServerPlayer player) {
        }
    }

    // ------------------------------------------------------------------
    // K: град осколков (барраж)
    // ------------------------------------------------------------------

    public static void hail(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < state.barrageReady) {
            AbilityManager.cooldown(player, AbilityType.BARRAGE, state.barrageReady - now);
            return;
        }
        // Работает как барраж Канеки: те же удары по области перед собой, тот же урон, оглушение и откат,
        // только без замаха щупалец (первый удар сразу). Шипы летят до края области и пропадают — это вид.
        AbilitySync.Timing timing = AbilitySync.get("kagune_barrage");
        int[] kaneki = timing.hits();
        int shift = kaneki.length > 0 ? Math.max(0, kaneki[0] - 1) : 0;
        int[] hits = new int[kaneki.length];
        for (int i = 0; i < kaneki.length; i++) {
            hits[i] = kaneki[i] - shift;
        }
        int lvl = level(data, SkillTree.Branch.W_HAIL);
        double width = WingsEffects.hailWidth(lvl);
        state.actions.add(new SpikeBarrage(new Actions.Barrage(now, hits, timing.duration() - shift,
                WingsEffects.hailDamage(lvl), width), now, timing.duration() - shift, width));
        // отпустили K раньше: AbilityManager.setBarrage прервёт анимацию пустым именем
        NetworkHandler.playAnimation(player, "wings_hail");
    }

    /** Откат шквала после серии (как у барража Канеки). */
    public static int hailCooldown(PlayerData data) {
        return WingsEffects.hailCooldown(level(data, SkillTree.Branch.W_HAIL));
    }

    /** Шквал шипов: урон наносит барраж Канеки внутри, а каждый тик из крыльев вылетают шипы до края области. */
    private static final class SpikeBarrage implements BarrageAction {
        private final Actions.Barrage barrage;
        private final long start;
        private final int duration;
        private final double width;

        private SpikeBarrage(Actions.Barrage barrage, long start, int duration, double width) {
            this.barrage = barrage;
            this.start = start;
            this.duration = duration;
            this.width = width;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            boolean done = barrage.tick(player, now);
            if (!done && now - start < duration) {
                spikes(player, now);
            }
            return done;
        }

        /** Шипы летят из крыльев в случайные точки на дальнем краю области барража и там пропадают. */
        private void spikes(ServerPlayer player, long now) {
            ServerLevel level = level(player);
            double yaw = Math.toRadians(Aim.yaw(player));
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            Vec3 side = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
            double half = width / 2.0;
            for (int i = 0; i < WingsBalance.HAIL_SPIKES_PER_TICK; i++) {
                Vec3 from = muzzle(player, player.getRandom().nextBoolean(), false);
                Vec3 to = player.position()
                        .add(forward.scale(Balance.BARRAGE_LENGTH))
                        .add(side.scale((player.getRandom().nextDouble() * 2.0 - 1.0) * half))
                        .add(0.0, 0.2 + player.getRandom().nextDouble() * (player.getBbHeight() - 0.1), 0.0);
                Vec3 delta = to.subtract(from);
                Shards.visual(level, from, delta.normalize().scale(WingsBalance.HAIL_SPIKE_SPEED), delta.length(), Shards.SIZE_TINY);
            }
            if ((now & 1) == 0) {
                Effects.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 1.2F + player.getRandom().nextFloat() * 0.4F);
            }
        }

        @Override
        public void cancel(ServerPlayer player) {
            barrage.cancel(player);
        }
    }

    // ------------------------------------------------------------------
    // Захваты: у крыльев их нет
    // ------------------------------------------------------------------

    public static void noGrab(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.wings_no_grab"), true);
    }

    // ------------------------------------------------------------------
    // Alt + J: «Шквал осколков»
    // ------------------------------------------------------------------

    public static void ultimate(ServerPlayer player, PlayerData data, CombatState state, long now) {
        if (now < data.getUltimateReady()) {
            long left = (data.getUltimateReady() - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.ultimate_cooldown",
                    left / 60, left % 60), true);
            return;
        }
        for (ActiveAction action : state.actions) {
            if (action instanceof Storm) {
                return;
            }
        }
        data.setUltimateReady(now + Balance.ULTIMATE_COOLDOWN);
        state.actions.removeIf(action -> action instanceof Actions.Dash);
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        player.hurtMarked = true;
        // пока крылья раскрываются и стреляют, стоим на месте; прицел остаётся свободным
        LockManager.lockFull(player, now + WingsBalance.STORM_CHARGE + WingsBalance.STORM_FIRE + 2);
        state.actions.add(new Storm(now));
        NetworkHandler.playAnimation(player, "wings_storm");
        NetworkHandler.syncTo(player);
        Effects.sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 0.5F);
        Effects.sound(player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
    }

    /** Накопление (искры вокруг крыльев) и сплошной огонь по направлению взгляда. */
    private static final class Storm implements ActiveAction {
        private final long start;

        private Storm(long start) {
            this.start = start;
        }

        @Override
        public boolean tick(ServerPlayer player, long now) {
            if (!player.isAlive()) {
                LockManager.unlock(player);
                return true;
            }
            long elapsed = now - start;
            ServerLevel level = level(player);
            if (elapsed < WingsBalance.STORM_CHARGE) {
                for (int i = 0; i < 2; i++) {
                    Vec3 p = muzzle(player, i == 0, false);
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 3, 0.25, 0.25, 0.25, 0.15);
                    level.sendParticles(Shards.SPIKE_DUST, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
                }
                if (elapsed % 5 == 0) {
                    Effects.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 0.6F + elapsed * 0.04F);
                }
                return false;
            }
            if (elapsed < WingsBalance.STORM_CHARGE + WingsBalance.STORM_FIRE) {
                for (int i = 0; i < WingsBalance.STORM_PER_TICK; i++) {
                    Vec3 from = muzzle(player, (i & 1) == 0, false);
                    Vec3 dir = spread(player, Aim.dir(player), WingsBalance.STORM_CONE);
                    Shards.spawn(new Shards.Shard(level, player.getUUID(), from, dir, WingsBalance.STORM_SPEED,
                            WingsBalance.STORM_RANGE, WingsBalance.STORM_DAMAGE, AttackType.ULTIMATE, false, 0, 0.0));
                }
                Vec3 p = muzzle(player, true, false);
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.3, 0.3, 0.3, 0.1);
                if (elapsed % 2 == 0) {
                    Effects.sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.4F + player.getRandom().nextFloat() * 0.5F);
                }
                return false;
            }
            LockManager.unlock(player);
            return true;
        }

        @Override
        public void cancel(ServerPlayer player) {
            LockManager.unlock(player);
        }
    }

    // ------------------------------------------------------------------
    // Попадание: кровь, кровотечение, статический заряд, дуга
    // ------------------------------------------------------------------

    /** Вызывается из Shards после каждого попадания (hurt — урон прошёл, иначе блок погасил осколок). */
    static void onHit(ServerPlayer owner, LivingEntity target, Shards.Shard shard, boolean hurt) {
        if (!hurt) {
            return;
        }
        PlayerData data = PlayerData.get(owner);
        if (data == null) {
            return;
        }
        if (shard.type() == AttackType.ULTIMATE) {
            Effects.blood(target, 1);
            return;
        }
        Effects.blood(target, shard.big() ? 12 : 2);
        int bleed = SkillEffects.bleedSeconds(level(data, SkillTree.Branch.BLEED));
        if (bleed > 0) {
            BleedManager.apply(target, bleed);
        }
        long now = owner.level().getGameTime();
        // «Статический заряд»: каждые несколько попаданий — очки ярости
        int need = WingsEffects.hitsPerRage(level(data, SkillTree.Branch.W_STATIC));
        if (need > 0) {
            Gun gun = gun(owner);
            gun.hits++;
            if (gun.hits >= need) {
                gun.hits = 0;
                boolean changed = AbilityManager.gainRage(data, WingsBalance.RAGE_PER_GROUP);
                if (changed && (now - gun.lastSync >= 10 || data.getRage() >= PlayerData.RAGE_MAX)) {
                    gun.lastSync = now;
                    NetworkHandler.syncTo(owner);
                }
            }
        }
        // «Рикошет»: шип отскакивает в соседнюю цель
        double chance = WingsEffects.arcChance(level(data, SkillTree.Branch.W_ARC));
        if (chance > 0.0 && owner.getRandom().nextDouble() < chance) {
            arc(owner, target, shard.damage() * (float) WingsBalance.ARC_SHARE);
        }
    }

    private static void arc(ServerPlayer owner, LivingEntity from, float damage) {
        AABB box = from.getBoundingBox().inflate(WingsBalance.ARC_RADIUS);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : owner.level().getEntitiesOfClass(LivingEntity.class, box,
                c -> c != from && Targeting.valid(owner, c) && !Combos.spared(owner, c))) {
            double d = e.distanceToSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        if (best == null || !CombatContext.hit(owner, best, damage, AttackType.BASIC)) {
            return;
        }
        Effects.blood(best, 2);
        if (owner.level() instanceof ServerLevel level) {
            // урон проходит сразу, а игроки видят, как маленький шип перелетает от первой цели ко второй
            Vec3 a = from.getBoundingBox().getCenter();
            Vec3 b = best.getBoundingBox().getCenter();
            Vec3 delta = b.subtract(a);
            Shards.visual(level, a, delta.normalize().scale(WingsBalance.SHARD_SPEED), delta.length(), Shards.SIZE_SMALL);
            level.sendParticles(Shards.SPIKE_DUST, b.x, b.y, b.z, 2, 0.1, 0.1, 0.1, 0.02);
            Effects.sound(best, SoundEvents.AMETHYST_BLOCK_HIT, 0.5F, 1.8F);
        }
    }
}
