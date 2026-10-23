package com.tokyoghoulrpg.client.vr;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.client.KeyBindings;
import com.tokyoghoulrpg.client.screen.AbilityMenuScreen;
import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.combat.QuinqueAbilities;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.AbilityC2S;
import com.tokyoghoulrpg.network.packet.ToggleKaguneC2S;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Управление способностями в VR (Vivecraft), без настройки кнопок в SteamVR.
 * Обычные кнопки Vivecraft остаются как есть (на Oculus: правый курок — удар, A — использовать,
 * B — радиальное меню, X — инвентарь, Y — меню, левый стик — ходьба, нажать — прыжок,
 * правый стик — поворот, нажать — присесть).
 * <p>
 * <b>Зоны на теле</b>: поднеси руку — контроллер слегка дрогнет — и нажми курок или хват этой руки:
 * <ul>
 *   <li>за плечо, за голову — кагуне вкл/выкл («достать из-за спины»);</li>
 *   <li>ко рту — укус;</li>
 *   <li>на макушку — какудзя;</li>
 *   <li>на грудь — ярость (у агента CCG — освобождение квинке);</li>
 *   <li>правой рукой к запястью левой (как часы) — меню способностей и окон мода;</li>
 *   <li>левую руку вытянуть вперёд (кагуне включено) — захват: указал на существо — притянешь его,
 *       на блок — притянешься сам.</li>
 * </ul>
 * <b>Жесты</b> (без кнопок):
 * <ul>
 *   <li>взмах любой рукой — удар кагуне туда, куда смотрит рука; с квинке — удар квинке (правой);</li>
 *   <li>резкий укол квинке вперёд — выпад;</li>
 *   <li>правый курок зажать (с кагуне или квинке) — барраж / шквал, пока держишь;</li>
 *   <li>обе руки перед лицом (как в стойке боксёра) — блок, пока держишь;</li>
 *   <li>левый стик дважды быстро толкнуть вперёд — рывок.</li>
 * </ul>
 * Сильный удар — дважды быстро A (как ПКМ дважды). Меню способностей есть и в меню паузы (Y).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class VrControls {
    public enum Zone { NONE, MENU, BITE, KAKUJA, KAGUNE, RAGE, GRAB }

    /** Скорость руки для удара взмахом, м/с (Vivecraft бьёт мечом от 2,5 м/с на кончике). */
    private static final double SWING_SPEED = 2.2;
    /** Скорость укола квинке для выпада, м/с, и насколько точно движение вдоль клинка. */
    private static final double STAB_SPEED = 2.6;
    private static final double STAB_ALIGN = 0.8;
    /** Сколько тиков держать курок, чтобы начался барраж. */
    private static final int BARRAGE_HOLD_TICKS = 10;
    /** Сколько тиков подряд держать стойку, чтобы поставить блок, и сколько вне её — чтобы снять. */
    private static final int GUARD_ON_TICKS = 3;
    private static final int GUARD_OFF_TICKS = 3;
    /** Дальность захвата (сервер всё равно проверит свою дальность с прокачкой). */
    private static final double GRAB_RANGE = 25.0;
    /** Взмах сильнее этого (м/с, пик) — сильный удар кагуне или квинке. */
    private static final double POWER_SPEED = 4.2;
    /** Сколько тиков следим за взмахом, чтобы поймать его пик. */
    private static final int SWING_PEAK_TICKS = 3;
    /** Барраж кулаками: столько быстрых ударов за PUNCH_WINDOW тиков — и барраж идёт, пока бьёшь. */
    private static final int PUNCHES_FOR_BARRAGE = 3;
    private static final int PUNCH_WINDOW = 12;
    /** Барраж кулаками кончается, если столько тиков не было удара. */
    private static final int PUNCH_KEEP_TICKS = 10;

    private static final Zone[] zones = {Zone.NONE, Zone.NONE};
    /** Когда Vivecraft последний раз спрашивал про зону руки (зона старше пары тиков уже не в счёт). */
    private static final long[] zoneTime = {Long.MIN_VALUE, Long.MIN_VALUE};
    private static final Vec3[] prevHand = new Vec3[2];
    private static Vec3 prevHead;
    private static final Vec3[] velocity = {Vec3.ZERO, Vec3.ZERO};
    private static final int[] swingCooldown = new int[2];

    private static boolean guarding;
    private static int guardTicks;
    private static int unguardTicks;
    private static int attackHeldTicks;
    private static boolean barrageByUs;

    // взмах: идёт ли, его пик и сколько тиков
    private static final boolean[] swinging = new boolean[2];
    private static final double[] swingPeak = new double[2];
    private static final Vec3[] swingPeakVel = {Vec3.ZERO, Vec3.ZERO};
    private static final int[] swingAge = new int[2];
    /** Когда были последние быстрые удары (для баррага кулаками). */
    private static final java.util.ArrayDeque<Long> punches = new java.util.ArrayDeque<>();
    private static long punchBarrageUntil = Long.MIN_VALUE;
    /** После сильного удара взмахом следующие мощные взмахи — обычные удары, пока идёт откат. */
    private static long powerReadyAt = Long.MIN_VALUE;

    // ульта: обе руки за спину (за плечи), потом обеими резко вперёд
    /** Сколько тиков подряд обе руки за спиной. */
    private static int ultBehindTicks;
    /** До какого тика ждём рывка руками вперёд. */
    private static long ultArmedUntil = Long.MIN_VALUE;
    /** Скорость каждой руки вперёд для ульты, м/с. */
    private static final double ULT_THRUST_SPEED = 2.0;
    /** Сколько тиков после «руки за спину» можно сделать рывок вперёд. */
    private static final int ULT_WINDOW = 15;

    // рывок: двойной толчок левого стика вперёд
    private static int flickState;
    private static int flickTimer;
    private static int dashCooldown;

    private VrControls() {
    }

    // ------------------------------------------------------------------ зоны на теле (через Vivecraft)

    /** Vivecraft каждый тик спрашивает для каждой руки: есть ли у нас зона под этой рукой. */
    static boolean zoneActive(LocalPlayer player, InteractionHand hand) {
        int i = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        zones[i] = computeZone(player, i);
        zoneTime[i] = player == null ? Long.MIN_VALUE : player.level().getGameTime();
        return zones[i] != Zone.NONE;
    }

    /** Нажат курок или хват руки, которая в зоне. true — сработало (Vivecraft даст вибрацию). */
    static boolean zonePressed(LocalPlayer player, InteractionHand hand) {
        int i = hand == InteractionHand.MAIN_HAND ? 0 : 1;
        Zone zone = zones[i];
        boolean offhand = i == 1;
        switch (zone) {
            case MENU -> Minecraft.getInstance().setScreen(new AbilityMenuScreen());
            case BITE -> send(AbilityType.BITE, offhand);
            case KAKUJA -> send(AbilityType.KAKUJA, offhand);
            case KAGUNE -> NetworkHandler.CHANNEL.sendToServer(new ToggleKaguneC2S());
            case RAGE -> send(AbilityType.RAGE, offhand);
            case GRAB -> grab(player, offhand);
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Zone computeZone(LocalPlayer player, int i) {
        Minecraft mc = Minecraft.getInstance();
        if (player == null || mc.screen != null || guarding) {
            return Zone.NONE;
        }
        PlayerData data = PlayerData.get(player);
        if (data == null || !data.hasChosenClass()) {
            return Zone.NONE;
        }
        VrClient.Pose p = VrClient.roomPose();
        if (p == null) {
            return Zone.NONE;
        }
        Vec3 hand = i == 0 ? p.main() : p.off();
        Vec3 head = p.head();
        Vec3 forward = horizontal(p.headDir());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 d = hand.subtract(head);
        double fwd = d.dot(forward);
        double up = d.y;
        double side = Math.abs(d.dot(right));
        boolean ghoul = data.getPlayerClass().hasKagune();

        // правой рукой к запястью левой, левая опущена до груди (как посмотреть на часы)
        if (i == 0) {
            Vec3 wrist = p.off().subtract(p.offDir().scale(0.08));
            if (hand.distanceTo(wrist) < 0.11 && p.off().y < head.y - 0.20) {
                return Zone.MENU;
            }
        }
        // ко рту (не с едой в руке: еду Vivecraft ест, если поднести ко рту)
        if (ghoul && !player.getItemInHand(i == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND).isEdible()) {
            Vec3 mouth = head.add(p.headDir().scale(0.07)).add(0.0, -0.09, 0.0);
            if (hand.distanceTo(mouth) < 0.10) {
                return Zone.BITE;
            }
        }
        // на макушку
        if (ghoul && up > 0.10 && Math.abs(fwd) < 0.20 && side < 0.22) {
            return Zone.KAKUJA;
        }
        // за плечо, за голову
        if (ghoul && fwd < -0.08 && up > -0.45 && up < 0.15 && side < 0.40) {
            return Zone.KAGUNE;
        }
        // ладонь на груди
        Vec3 chest = head.add(forward.scale(0.05)).add(0.0, -0.30, 0.0);
        if (hand.distanceTo(chest) < 0.13) {
            return Zone.RAGE;
        }
        // левая рука вытянута вперёд — захват кагуне
        if (i == 1 && ghoul && data.isKaguneActive() && fwd > 0.45 && Math.abs(up) < 0.5) {
            return Zone.GRAB;
        }
        return Zone.NONE;
    }

    private static Vec3 horizontal(Vec3 dir) {
        Vec3 h = new Vec3(dir.x, 0.0, dir.z);
        return h.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : h.normalize();
    }

    /** Захват левой рукой: указал на существо — притянуть его, на блок или в пустоту — притянуться самому. */
    private static void grab(LocalPlayer player, boolean offhand) {
        VrClient.Pose w = VrClient.worldPose();
        if (w == null) {
            return;
        }
        Vec3 from = offhand ? w.off() : w.main();
        Vec3 dir = offhand ? w.offDir() : w.mainDir();
        Vec3 to = from.add(dir.scale(GRAB_RANGE));
        BlockHitResult block = player.level().clip(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player.level(), player, from, to,
                new AABB(from, to).inflate(1.0),
                e -> e instanceof LivingEntity living && living != player && living.isAlive() && !living.isSpectator());
        boolean entityFirst = entity != null && (block.getType() == HitResult.Type.MISS
                || from.distanceToSqr(entity.getLocation()) <= from.distanceToSqr(block.getLocation()));
        send(entityFirst ? AbilityType.PULL_TARGET : AbilityType.GRAPPLE_SELF, offhand);
    }

    private static void send(AbilityType type, boolean offhand) {
        NetworkHandler.CHANNEL.sendToServer(new AbilityC2S(type, offhand));
    }

    // ------------------------------------------------------------------ жесты (каждый тик)

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        VrClient.Pose pose = player == null ? null : VrClient.roomPose();
        if (pose == null) {
            resetAll();
            return;
        }
        updateVelocities(pose);
        for (int i = 0; i < 2; i++) {
            if (swingCooldown[i] > 0) {
                swingCooldown[i]--;
            }
        }
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        PlayerData data = PlayerData.get(player);
        if (mc.screen != null || data == null || !data.hasChosenClass() || !player.isAlive()) {
            releaseHeld();
            return;
        }
        if (!gesturesEnabled()) {
            releaseHeld();
            return;
        }
        boolean kagune = data.getPlayerClass().hasKagune() && data.isKaguneActive();
        boolean quinque = QuinqueAbilities.active(player, data);
        boolean canBlock = data.getPlayerClass().hasKagune() || quinque;

        long now = player.level().getGameTime();
        updateGuard(pose, canBlock, quinque);
        // ульта (руки за спину и вперёд): пока она набирается, взмахи не считаются
        if (!updateUltimate(pose, kagune, now)) {
            trackSwings(player, data, pose, kagune, quinque, now);
        }
        updateBarrage(mc, kagune || quinque, now);
    }

    /** Скорость рук относительно головы (чтобы ходьба по комнате и прыжки не считались взмахом), м/с. */
    private static void updateVelocities(VrClient.Pose p) {
        Vec3[] hands = {p.main(), p.off()};
        Vec3 headMove = prevHead == null ? Vec3.ZERO : p.head().subtract(prevHead);
        for (int i = 0; i < 2; i++) {
            if (prevHand[i] == null) {
                velocity[i] = Vec3.ZERO;
            } else {
                velocity[i] = hands[i].subtract(prevHand[i]).subtract(headMove).scale(20.0);
            }
            prevHand[i] = hands[i];
        }
        prevHead = p.head();
    }

    /** Стойка: обе руки подняты перед лицом, близко друг к другу. */
    private static boolean inGuardPose(VrClient.Pose p) {
        Vec3 forward = horizontal(p.headDir());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        for (Vec3 hand : new Vec3[] {p.main(), p.off()}) {
            Vec3 d = hand.subtract(p.head());
            double fwd = d.dot(forward);
            if (d.y < -0.30 || d.y > 0.15 || fwd < 0.05 || fwd > 0.45 || Math.abs(d.dot(right)) > 0.30) {
                return false;
            }
        }
        return p.main().distanceTo(p.off()) < 0.40;
    }

    /**
     * Блок квинке как мечом: квинке поперёк перед собой (клинок горизонтально и вбок),
     * рука на уровне груди или лица.
     */
    private static boolean inSwordGuard(VrClient.Pose p) {
        Vec3 forward = horizontal(p.headDir());
        Vec3 d = p.main().subtract(p.head());
        double fwd = d.dot(forward);
        Vec3 blade = p.mainDir();
        return fwd > 0.15 && fwd < 0.60 && d.y > -0.50 && d.y < 0.15
                && Math.abs(blade.y) < 0.40 && Math.abs(blade.dot(forward)) < 0.55;
    }

    private static void updateGuard(VrClient.Pose p, boolean canBlock, boolean quinque) {
        if (canBlock && (inGuardPose(p) || quinque && inSwordGuard(p))) {
            guardTicks++;
            unguardTicks = 0;
        } else {
            unguardTicks++;
            guardTicks = 0;
        }
        if (!guarding && guardTicks >= GUARD_ON_TICKS) {
            guarding = true;
            KeyBindings.BLOCK.setDown(true);
            VrClient.vibrate(false, 0.06F);
            VrClient.vibrate(true, 0.06F);
        } else if (guarding && unguardTicks >= GUARD_OFF_TICKS) {
            guarding = false;
            KeyBindings.BLOCK.setDown(false);
        }
    }

    /**
     * Барраж (шквал квинке), пока:
     * правый курок зажат дольше полсекунды (и не копаем блок) или быстро бьёшь руками — 3 удара подряд, и дальше,
     * пока не перестанешь бить.
     */
    private static void updateBarrage(Minecraft mc, boolean armed, long now) {
        boolean digging = mc.gameMode != null && mc.gameMode.isDestroying();
        boolean trigger = armed && !guarding && mc.options.keyAttack.isDown() && !digging;
        attackHeldTicks = trigger ? attackHeldTicks + 1 : 0;
        boolean byTrigger = attackHeldTicks >= BARRAGE_HOLD_TICKS;
        boolean byPunches = armed && !guarding && now < punchBarrageUntil;
        boolean want = byTrigger || byPunches;
        if (!barrageByUs && want) {
            barrageByUs = true;
            KeyBindings.BARRAGE.setDown(true);
            VrClient.vibrate(false, 0.1F);
            VrClient.vibrate(true, 0.1F);
        } else if (barrageByUs && !want) {
            barrageByUs = false;
            KeyBindings.BARRAGE.setDown(false);
        }
    }

    /**
     * Взмахи. Начался взмах (рука быстрее SWING_SPEED) — следим пару тиков и ловим пик:
     * <ul>
     *   <li>квинке, укол вдоль клинка — выпад;</li>
     *   <li>очень сильный взмах — сильный удар (кагуне — любой рукой, квинке — правой);</li>
     *   <li>иначе обычный удар кагуне (той рукой, которой взмахнул) или квинке (правой).</li>
     * </ul>
     * Каждый взмах ещё считается ударом для баррага кулаками.
     */
    private static void trackSwings(LocalPlayer player, PlayerData data, VrClient.Pose p, boolean kagune,
                                    boolean quinque, long now) {
        while (!punches.isEmpty() && now - punches.peekFirst() > PUNCH_WINDOW) {
            punches.pollFirst();
        }
        boolean armed = kagune || quinque;
        for (int i = 0; i < 2; i++) {
            Vec3 v = velocity[i];
            double speed = v.length();
            // рука в зоне на теле (тянется за кагуне, ко рту и т. п.) — это не удар
            boolean inZone = zones[i] != Zone.NONE && now - zoneTime[i] <= 2;
            if (!swinging[i]) {
                if (armed && !guarding && !inZone && speed > SWING_SPEED) {
                    swinging[i] = true;
                    swingPeak[i] = speed;
                    swingPeakVel[i] = v;
                    swingAge[i] = 0;
                    punches.addLast(now);
                    if (punches.size() >= PUNCHES_FOR_BARRAGE || now < punchBarrageUntil) {
                        punchBarrageUntil = now + PUNCH_KEEP_TICKS;
                    }
                }
                continue;
            }
            swingAge[i]++;
            if (speed > swingPeak[i]) {
                swingPeak[i] = speed;
                swingPeakVel[i] = v;
            }
            if (speed > SWING_SPEED * 0.8 && swingAge[i] < SWING_PEAK_TICKS) {
                continue;
            }
            swinging[i] = false;
            // во время баррага (курок или кулаки) отдельные удары не шлём: бьёт барраж
            if (barrageByUs || now < punchBarrageUntil || swingCooldown[i] > 0 || !armed) {
                continue;
            }
            fireSwing(player, data, p, i, swingPeak[i], swingPeakVel[i], kagune, quinque, now);
        }
    }

    private static void fireSwing(LocalPlayer player, PlayerData data, VrClient.Pose p, int i, double peak, Vec3 vel,
                                  boolean kagune, boolean quinque, long now) {
        boolean offhand = i == 1;
        boolean power = peak > POWER_SPEED && now >= powerReadyAt && !onCooldown(data, "strong", now);
        if (quinque) {
            if (offhand) {
                return;
            }
            if (peak > STAB_SPEED && vel.scale(1.0 / peak).dot(p.mainDir()) > STAB_ALIGN) {
                send(AbilityType.QUINQUE_LUNGE, false);
                swingCooldown[0] = 12;
                VrClient.vibrate(false, 0.08F);
            } else if (power) {
                send(AbilityType.QUINQUE_STRONG, false);
                swingCooldown[0] = 12;
                powerReadyAt = now + 40;
                VrClient.vibrate(false, 0.15F);
            } else {
                send(AbilityType.QUINQUE_ATTACK, false);
                swingCooldown[0] = 6;
                VrClient.vibrate(false, 0.05F);
            }
        } else if (kagune) {
            if (power) {
                send(AbilityType.STRONG_ATTACK, offhand);
                powerReadyAt = now + 30;
                VrClient.vibrate(offhand, 0.15F);
            } else {
                send(AbilityType.BASIC_ATTACK, offhand);
                VrClient.vibrate(offhand, 0.05F);
            }
            // удар кагуне раз в 0,4 с: чаще сервер всё равно не даст
            swingCooldown[0] = Math.max(swingCooldown[0], 8);
            swingCooldown[1] = Math.max(swingCooldown[1], 8);
        }
    }

    /** Рука за плечом, за головой (как зона «достать кагуне»). */
    private static boolean behindShoulder(VrClient.Pose p, Vec3 hand) {
        Vec3 forward = horizontal(p.headDir());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 d = hand.subtract(p.head());
        return d.dot(forward) < -0.08 && d.y > -0.45 && d.y < 0.15 && Math.abs(d.dot(right)) < 0.40;
    }

    /**
     * Ульта гуля в VR: заведи обе руки за спину (за плечи) — контроллеры дрогнут — и за полсекунды резко выбрось
     * обе вперёд, к цели. Нужно включённое кагуне. true — ульта отправлена или ждём рывка (тогда взмахи не считаются).
     */
    private static boolean updateUltimate(VrClient.Pose p, boolean kagune, long now) {
        if (!kagune || guarding) {
            ultBehindTicks = 0;
            ultArmedUntil = Long.MIN_VALUE;
            return false;
        }
        if (behindShoulder(p, p.main()) && behindShoulder(p, p.off())) {
            ultBehindTicks++;
            if (ultBehindTicks == 2) {
                VrClient.vibrate(false, 0.05F, 0.4F);
                VrClient.vibrate(true, 0.05F, 0.4F);
            }
            if (ultBehindTicks >= 2) {
                ultArmedUntil = now + ULT_WINDOW;
            }
            // руки ещё за спиной: это не взмах
            swinging[0] = false;
            swinging[1] = false;
            return true;
        }
        ultBehindTicks = 0;
        if (now > ultArmedUntil) {
            return false;
        }
        Vec3 forward = horizontal(p.headDir());
        if (velocity[0].dot(forward) > ULT_THRUST_SPEED && velocity[1].dot(forward) > ULT_THRUST_SPEED) {
            ultArmedUntil = Long.MIN_VALUE;
            send(AbilityType.ULTIMATE, false);
            VrClient.vibrate(false, 0.2F, 1.0F);
            VrClient.vibrate(true, 0.2F, 1.0F);
            swinging[0] = false;
            swinging[1] = false;
            swingCooldown[0] = Math.max(swingCooldown[0], 12);
            swingCooldown[1] = Math.max(swingCooldown[1], 12);
            punches.clear();
            return true;
        }
        // пока ждём рывка, взмахи не считаются (руки идут из-за спины вперёд)
        swinging[0] = false;
        swinging[1] = false;
        return true;
    }

    /** Откат способности по последним данным с сервера. */
    private static boolean onCooldown(PlayerData data, String key, long now) {
        Long ready = data.getCooldownsCache().get(key);
        return ready != null && now < ready;
    }

    /** Рывок: левый стик дважды быстро толкнуть вперёд. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer) || !VrClient.active() || !gesturesEnabled()) {
            flickState = 0;
            return;
        }
        float f = event.getInput().forwardImpulse;
        boolean pushed = f > 0.8F;
        boolean released = f < 0.3F;
        switch (flickState) {
            case 0 -> {
                if (pushed) {
                    flickState = 1;
                    flickTimer = 0;
                }
            }
            case 1 -> {
                // первый толчок должен быть коротким, иначе это просто ходьба
                flickTimer++;
                if (released) {
                    flickState = flickTimer <= 6 ? 2 : 0;
                    flickTimer = 0;
                } else if (flickTimer > 6) {
                    flickState = 3;
                }
            }
            case 2 -> {
                flickTimer++;
                if (pushed) {
                    if (dashCooldown == 0) {
                        send(AbilityType.DASH, false);
                        dashCooldown = 5;
                        VrClient.vibrate(true, 0.06F);
                    }
                    flickState = 3;
                } else if (flickTimer > 6) {
                    flickState = 0;
                }
            }
            default -> {
                // ждём, пока стик отпустят
                if (released) {
                    flickState = 0;
                }
            }
        }
    }

    private static boolean gesturesEnabled() {
        try {
            return com.tokyoghoulrpg.ClientConfig.VR_GESTURES.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------ сброс

    private static void releaseHeld() {
        if (guarding) {
            guarding = false;
            KeyBindings.BLOCK.setDown(false);
        }
        guardTicks = 0;
        if (barrageByUs) {
            barrageByUs = false;
            KeyBindings.BARRAGE.setDown(false);
        }
        attackHeldTicks = 0;
        punchBarrageUntil = Long.MIN_VALUE;
        punches.clear();
        swinging[0] = false;
        swinging[1] = false;
        ultBehindTicks = 0;
        ultArmedUntil = Long.MIN_VALUE;
    }

    private static void resetAll() {
        releaseHeld();
        prevHand[0] = null;
        prevHand[1] = null;
        prevHead = null;
        velocity[0] = Vec3.ZERO;
        velocity[1] = Vec3.ZERO;
        zones[0] = Zone.NONE;
        zones[1] = Zone.NONE;
        flickState = 0;
    }
}
