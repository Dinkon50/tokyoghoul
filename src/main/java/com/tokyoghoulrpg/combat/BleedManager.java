package com.tokyoghoulrpg.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

/** Кровотечение от ударов кагуне: раз в секунду небольшой урон, пока не истечёт время. */
public final class BleedManager {
    private static final Map<UUID, Entry> BLEEDING = new HashMap<>();

    private static final class Entry {
        private final LivingEntity entity;
        private long until;
        private long nextTick;

        private Entry(LivingEntity entity, long until, long nextTick) {
            this.entity = entity;
            this.until = until;
            this.nextTick = nextTick;
        }
    }

    private BleedManager() {
    }

    /** Новый удар продлевает кровотечение, но не ускоряет его. */
    public static void apply(LivingEntity entity, int seconds) {
        if (seconds <= 0 || !entity.isAlive()) {
            return;
        }
        long now = entity.level().getGameTime();
        long until = now + seconds * 20L;
        Entry entry = BLEEDING.get(entity.getUUID());
        if (entry == null) {
            BLEEDING.put(entity.getUUID(), new Entry(entity, until, now + 20));
        } else {
            entry.until = Math.max(entry.until, until);
        }
    }

    /** Раз в тик сервера. */
    public static void tick() {
        if (BLEEDING.isEmpty()) {
            return;
        }
        for (Entry entry : new ArrayList<>(BLEEDING.values())) {
            LivingEntity entity = entry.entity;
            if (entity.isRemoved() || !entity.isAlive()) {
                BLEEDING.remove(entity.getUUID());
                continue;
            }
            long now = entity.level().getGameTime();
            if (now >= entry.nextTick) {
                entry.nextTick = now + 20;
                entity.invulnerableTime = 0;
                entity.hurt(entity.damageSources().magic(), Balance.BLEED_DAMAGE);
            }
            if (now >= entry.until) {
                BLEEDING.remove(entity.getUUID());
            }
        }
    }

    public static void clear() {
        BLEEDING.clear();
    }
}
