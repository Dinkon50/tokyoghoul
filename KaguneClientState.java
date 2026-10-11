package com.tokyoghoulrpg.client.kagune;

import com.tokyoghoulrpg.data.KaguneVariant;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Кагуне всех игроков, которых видит клиент. Ключ: номер сущности игрока. */
public final class KaguneClientState {
    private static final Map<Integer, KaguneAnimatable> STATES = new HashMap<>();

    private KaguneClientState() {
    }

    public static KaguneAnimatable get(int entityId) {
        return STATES.get(entityId);
    }

    public static void applyState(int entityId, boolean active, KaguneVariant variant, boolean rage) {
        STATES.computeIfAbsent(entityId, id -> new KaguneAnimatable())
                .updateState(active, variant, rage, gameTime());
    }

    public static void playAnimation(int entityId, String animation) {
        STATES.computeIfAbsent(entityId, id -> new KaguneAnimatable())
                .playOneShot(animation, gameTime());
    }

    public static void clear() {
        STATES.clear();
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }
}
