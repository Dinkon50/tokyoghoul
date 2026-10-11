package com.tokyoghoulrpg.combat;

import java.util.ArrayList;
import java.util.List;

/** Боевое состояние игрока, которое живёт только на сервере и не сохраняется в мире. Время в тиках мира. */
public class CombatState {
    public long biteReady;
    public long dashReady;
    public long barrageReady;
    public long blockReady;
    public long grappleReady;
    public long pullReady;
    public long strongReady;
    public long attackBusyUntil;
    public int comboIndex;
    public float lastExhaustion;
    public final List<ActiveAction> actions = new ArrayList<>();

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
        barrageReady = 0L;
        blockReady = 0L;
        grappleReady = 0L;
        pullReady = 0L;
        strongReady = 0L;
        attackBusyUntil = 0L;
        comboIndex = 0;
        lastExhaustion = 0.0F;
        actions.clear();
    }
}
