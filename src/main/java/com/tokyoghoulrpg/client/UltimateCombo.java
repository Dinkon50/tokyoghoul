package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.combat.AbilityType;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.network.packet.AbilityC2S;

/**
 * Ульта гуля с клавиатуры: две клавиши вместе — J и K (ярость и барраж) — в пределах {@link #WINDOW} тиков
 * (0,15 с) друг от друга. Работают клавиши, которые стоят на этих действиях (если их переназначили — новые).
 * <p>
 * Чтобы набор не тратил способности зря, J и K ждут друг друга эти 0,15 с:
 * <ul>
 *   <li>J (ярость) включается на 0,15 с позже; если за это время нажали K — это ульта, ярость не тратится;</li>
 *   <li>K (барраж) начинается на 0,15 с позже; после ульты барраж не начнётся, пока K не отпустят.</li>
 * </ul>
 */
public final class UltimateCombo {
    public static final int WINDOW = 3;

    private static long rageAt = Long.MIN_VALUE / 2;
    private static long barrageAt = Long.MIN_VALUE / 2;
    /** Когда комбо последний раз сработало: нажатия до этого уже не считаются. */
    private static long firedAt = Long.MIN_VALUE / 2;
    /** Отложенное нажатие J (ярость) — тик нажатия. */
    private static long pendingRage = Long.MIN_VALUE;
    private static boolean holdBarrageUntilRelease;

    private UltimateCombo() {
    }

    public static void reset() {
        rageAt = barrageAt = firedAt = Long.MIN_VALUE / 2;
        pendingRage = Long.MIN_VALUE;
        holdBarrageUntilRelease = false;
    }

    /** J нажата. true — ярость пока не включать (подождать K или это уже ульта). */
    public static boolean rage(long now) {
        rageAt = now;
        if (check(now)) {
            return true;
        }
        pendingRage = now;
        return true;
    }

    /** K нажата. */
    public static void barrage(long now) {
        barrageAt = now;
        check(now);
    }

    /** J и K нажаты почти одновременно (в любом порядке): отправить ульту. */
    private static boolean check(long now) {
        if (rageAt <= firedAt || barrageAt <= firedAt || Math.abs(rageAt - barrageAt) > WINDOW) {
            return false;
        }
        firedAt = now;
        pendingRage = Long.MIN_VALUE;
        holdBarrageUntilRelease = true;
        NetworkHandler.CHANNEL.sendToServer(new AbilityC2S(AbilityType.ULTIMATE));
        return true;
    }

    /** Каждый тик: отложенная J — включить ярость, если K так и не нажали. true — пора отправить ярость. */
    public static boolean releaseDeferredRage(long now) {
        if (pendingRage == Long.MIN_VALUE || now - pendingRage <= WINDOW) {
            return false;
        }
        pendingRage = Long.MIN_VALUE;
        return true;
    }

    /** Держать ли барраж: первые 0,15 с после нажатия K ждём J; после ульты — пока K не отпустят. */
    public static boolean barrageHeld(boolean keyDown, long now) {
        if (holdBarrageUntilRelease) {
            if (!keyDown) {
                holdBarrageUntilRelease = false;
            }
            return false;
        }
        if (keyDown && barrageAt > firedAt && now - barrageAt <= WINDOW) {
            return false;
        }
        return keyDown;
    }
}
