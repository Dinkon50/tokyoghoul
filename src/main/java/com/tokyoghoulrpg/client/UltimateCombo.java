package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.AbilityC2S;

/**
 * Ульта гуля с клавиатуры: четыре клавиши подряд — H J K L (рывок, ярость, барраж, блок) — не дольше чем за
 * {@link #WINDOW} тиков (0,6 с). Клавиши можно переназначить: работает то, что стоит на этих действиях.
 * <p>
 * Чтобы набор комбо не тратил способности зря:
 * <ul>
 *   <li>H — рывок срабатывает как обычно (он и подводит к цели);</li>
 *   <li>J сразу после H ждёт до 0,6 с: если комбо доберут — ярость не тратится, иначе включится чуть позже;</li>
 *   <li>K после H и J тоже ждёт, а после ульты барраж не начнётся, пока K не отпустят;</li>
 *   <li>L, которая завершила комбо, не ставит блок, пока её не отпустят.</li>
 * </ul>
 */
public final class UltimateCombo {
    public static final int WINDOW = 12;

    private static long dashAt = Long.MIN_VALUE / 2;
    private static long rageAt = Long.MIN_VALUE / 2;
    private static long barrageAt = Long.MIN_VALUE / 2;
    private static long blockAt = Long.MIN_VALUE / 2;
    /** Когда комбо последний раз сработало: нажатия до этого уже не считаются. */
    private static long firedAt = Long.MIN_VALUE / 2;
    /** Отложенное нажатие J (ярость) — тик нажатия. */
    private static long pendingRage = Long.MIN_VALUE;
    private static boolean holdBarrageUntilRelease;
    private static boolean holdBlockUntilRelease;

    private UltimateCombo() {
    }

    public static void reset() {
        dashAt = rageAt = barrageAt = blockAt = firedAt = Long.MIN_VALUE / 2;
        pendingRage = Long.MIN_VALUE;
        holdBarrageUntilRelease = false;
        holdBlockUntilRelease = false;
    }

    /** H нажата. */
    public static void dash(long now) {
        dashAt = now;
        check(now);
    }

    /** J нажата. true — нажатие отложено (возможно, это часть комбо), ярость пока не включать. */
    public static boolean rage(long now) {
        rageAt = now;
        if (check(now)) {
            return true;
        }
        if (started(now)) {
            pendingRage = now;
            return true;
        }
        return false;
    }

    /** K нажата. */
    public static void barrage(long now) {
        barrageAt = now;
        check(now);
    }

    /** L нажата. */
    public static void block(long now) {
        blockAt = now;
        check(now);
    }

    /** Комбо начато: H нажата недавно и после прошлой ульты. */
    private static boolean started(long now) {
        return dashAt > firedAt && now - dashAt <= WINDOW;
    }

    /** Все четыре по порядку (в один тик — в любом порядке) и не дольше WINDOW: отправить ульту. */
    private static boolean check(long now) {
        if (!(dashAt > firedAt && dashAt <= rageAt && rageAt <= barrageAt && barrageAt <= blockAt
                && blockAt - dashAt <= WINDOW && now - dashAt <= WINDOW)) {
            return false;
        }
        firedAt = now;
        pendingRage = Long.MIN_VALUE;
        holdBarrageUntilRelease = true;
        holdBlockUntilRelease = true;
        NetworkHandler.CHANNEL.sendToServer(new AbilityC2S(AbilityType.ULTIMATE));
        return true;
    }

    /** Каждый тик: отложенная J — включить ярость, если комбо уже не доберут. true — пора отправить ярость. */
    public static boolean releaseDeferredRage(long now) {
        if (pendingRage == Long.MIN_VALUE || started(now)) {
            return false;
        }
        pendingRage = Long.MIN_VALUE;
        return true;
    }

    /** Держать ли барраж: не во время набора комбо (H и J уже нажаты) и не после ульты, пока K не отпустят. */
    public static boolean barrageHeld(boolean keyDown, long now) {
        if (holdBarrageUntilRelease) {
            if (!keyDown) {
                holdBarrageUntilRelease = false;
            }
            return false;
        }
        if (keyDown && started(now) && rageAt >= dashAt) {
            return false;
        }
        return keyDown;
    }

    /** Держать ли блок: L, которая завершила комбо, блок не ставит, пока её не отпустят. */
    public static boolean blockHeld(boolean keyDown) {
        if (holdBlockUntilRelease) {
            if (!keyDown) {
                holdBlockUntilRelease = false;
            }
            return false;
        }
        return keyDown;
    }
}
