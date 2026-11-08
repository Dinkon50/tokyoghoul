package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.CrimsonMonarch;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.BossFxS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Эффекты особых атак боссов в стиле мода: сервер говорит игрокам рядом, что и где нарисовать, клиент рисует сам
 * (client/BossFxRenderer) — кровь, тёмные пятна на земле, кристаллические шипы кагуне, пыль и куски земли, у CCG —
 * сталь и искры. Пятно-предупреждение растекается к моменту удара — по нему видно, куда отбежать.
 */
public final class BossFx {
    public static final int CIRCLE = 0;     // зона на земле: радиус size, заполняется за duration
    public static final int LINE = 1;       // полоса-предупреждение от точки к точке, ширина size
    public static final int SHOCKWAVE = 2;  // расходящаяся волна, радиус size
    public static final int BEAM = 3;       // яркий луч от точки к точке, ширина size
    public static final int BOLT = 4;       // молния цвета босса, высота size
    public static final int ARC = 5;        // дуга взмаха вокруг точки: радиус size, начальный угол extra
    public static final int SPIKES = 6;     // шипы из-под земли: радиус size, количество extra
    public static final int DOME = 7;       // расширяющийся купол, радиус size
    public static final int SHAKE = 8;      // тряска камеры: сила extra, радиус size
    public static final int SLASH = 9;      // короткий взмах перед боссом: радиус size, угол extra, ширина дуги x2-x
    public static final int SPEAR = 10;     // кагуне-копьё: кристаллический шип выстреливает от точки к точке
    public static final int BURST = 11;     // взрыв кагуне: шипы во все стороны из тела босса, радиус size

    private static final double RANGE = 80.0;

    private BossFx() {
    }

    /** Цвет эффектов босса: агент CCG — сталь с голубым, Багровый монарх — почти чёрная кровь, гули — кровь. */
    public static int color(Mob boss) {
        if (boss instanceof CcgBoss) {
            return 0x9DB8C8;
        }
        if (boss instanceof CrimsonMonarch) {
            return 0x6A0410;
        }
        if (boss instanceof com.tokyoghoulrpg.entity.DojoMaster) {
            return 0x6E4A2A;                    // Мастер додзё: пыль и земля, без кагуне
        }
        if (boss instanceof com.tokyoghoulrpg.entity.ClownBoss) {
            return 0x7A50C0;                    // Клоун: фиолетовый дым
        }
        if (boss instanceof com.tokyoghoulrpg.entity.WhiteReaper) {
            return 0xD8E2EC;                    // Белый жнец: холодный белый
        }
        return 0x9A0C18;
    }

    public static void send(Mob boss, int type, Vec3 a, Vec3 b, double size, int duration, double extra) {
        send(boss, type, a, b, size, duration, color(boss), extra);
    }

    public static void send(Mob boss, int type, Vec3 a, Vec3 b, double size, int duration, int color, double extra) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 end = b == null ? a : b;
        NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                a.x, a.y, a.z, RANGE, level.dimension())),
                new BossFxS2C(type, a.x, a.y, a.z, end.x, end.y, end.z, (float) size, duration, color, (float) extra));
    }

    public static void circle(Mob boss, Vec3 at, double radius, int ticks) {
        send(boss, CIRCLE, ground(boss, at), null, radius, ticks, 0.0);
    }

    public static void line(Mob boss, Vec3 from, Vec3 to, double width, int ticks) {
        send(boss, LINE, from, to, width, ticks, 0.0);
    }

    public static void shockwave(Mob boss, Vec3 at, double radius, int ticks) {
        send(boss, SHOCKWAVE, ground(boss, at), null, radius, ticks, 0.0);
    }

    public static void beam(Mob boss, Vec3 from, Vec3 to, double width, int ticks) {
        send(boss, BEAM, from, to, width, ticks, 0.0);
    }

    public static void bolt(Mob boss, Vec3 at, double height) {
        send(boss, BOLT, ground(boss, at), null, height, 10, 0.0);
    }

    public static void arc(Mob boss, Vec3 at, double radius, float startYaw, int ticks) {
        send(boss, ARC, at, null, radius, ticks, startYaw);
    }

    /** Взмах обычного удара: дуга sweepDeg градусов перед боссом (по направлению взгляда). */
    public static void slash(Mob boss, double radius, double sweepDeg, int ticks) {
        Vec3 at = boss.position();
        float start = boss.getYRot() + 90.0F - (float) sweepDeg / 2.0F;
        send(boss, SLASH, at, at.add(sweepDeg, 0.0, 0.0), radius, ticks, start);
    }

    public static void spikes(Mob boss, Vec3 at, double radius, int count, int ticks) {
        send(boss, SPIKES, ground(boss, at), null, radius, ticks, count);
    }

    /** Кагуне-копьё: шип выстреливает из босса до конца линии и втягивается обратно. */
    public static void spear(Mob boss, Vec3 from, Vec3 to, double width, int ticks) {
        send(boss, SPEAR, from, to, width, ticks, 0.0);
    }

    /** Взрыв кагуне: шипы разом пробивают воздух во все стороны и втягиваются. */
    public static void burst(Mob boss, Vec3 at, double radius, int count, int ticks) {
        send(boss, BURST, at, null, radius, ticks, count);
    }

    public static void dome(Mob boss, Vec3 at, double radius, int ticks) {
        send(boss, DOME, at, null, radius, ticks, 0.0);
    }

    /** Тряска камеры у игроков ближе radius: сильнее у центра. */
    public static void shake(Mob boss, Vec3 at, double radius, float strength, int ticks) {
        send(boss, SHAKE, at, null, radius, ticks, strength);
    }

    /** Куски земли разлетаются из-под удара (частицы блока под точкой). */
    public static void debris(Mob boss, Vec3 at, double radius, int count) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5, at.z);
        BlockState state = level.getBlockState(below);
        if (state.isAir()) {
            return;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + 0.2, at.z, count,
                radius * 0.5, 0.15, radius * 0.5, 0.25);
    }

    /** Земля под точкой (до 8 блоков вниз), чтобы зоны лежали на земле, даже если цель в прыжке. */
    public static Vec3 ground(Mob boss, Vec3 at) {
        Vec3 from = at.add(0.0, 0.5, 0.0);
        Vec3 to = at.add(0.0, -8.0, 0.0);
        HitResult hit = boss.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
    }
}
