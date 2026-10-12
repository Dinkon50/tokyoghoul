package com.tokyoghoulrpg.combat;

import net.minecraft.server.level.ServerPlayer;

/** Способность, которая выполняется несколько тиков (рывок, барраж, захват, удар с задержкой). */
public interface ActiveAction {
    /** Вызывается раз в тик. Возвращает true, когда действие закончилось. */
    boolean tick(ServerPlayer player, long now);

    /** Вызывается, если действие прервали (смерть, выход из игры, сброс класса). */
    void cancel(ServerPlayer player);
}
