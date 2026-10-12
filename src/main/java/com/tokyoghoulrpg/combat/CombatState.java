package com.tokyoghoulrpg.combat;

import java.util.ArrayList;
import java.util.List;

/** Боевое состояние игрока, которое живёт только на сервере и не сохраняется в мире. Время в тиках мира. */
public class CombatState {
    public long biteReady;
    public long dashReady;
    public long kaguneToggleReady;
    public long secondWindReady;
    public long humanStrongReady;
    /** Сколько рывков уже сделано в текущей серии (двойной рывок). */
    public int dashUsed;
    public long lastDash;
    /** Шёл ли барраж в прошлом тике: по переходу «шёл → закончился» запускается откат. */
    public boolean wasBarraging;
    public long barrageReady;
    public long blockReady;
    public long grappleReady;
    public long pullReady;
    public long strongReady;
    public long attackBusyUntil;
    public int comboIndex;
    public float lastExhaustion;
    /** Значение шкалы «Кровь», которое уже знает клиент. */
    public int syncedCraving = -1;
    public final List<ActiveAction> actions = new ArrayList<>();

    public boolean isDashing() {
        for (ActiveAction action : actions) {
            if (action instanceof Actions.Dash) {
                return true;
            }
        }
        return false;
    }

    public boolean isBarraging() {
        for (ActiveAction action : actions) {
            if (action instanceof Actions.Barrage) {
                return true;
            }
        }
        return false;
    }

    public void reset() {
        biteReady = 0L;
        dashReady = 0L;
        kaguneToggleReady = 0L;
        secondWindReady = 0L;
        humanStrongReady = 0L;
        dashUsed = 0;
        lastDash = 0L;
        wasBarraging = false;
        barrageReady = 0L;
        blockReady = 0L;
        grappleReady = 0L;
        pullReady = 0L;
        strongReady = 0L;
        attackBusyUntil = 0L;
        comboIndex = 0;
        lastExhaustion = 0.0F;
        syncedCraving = -1;
        actions.clear();
    }
}
