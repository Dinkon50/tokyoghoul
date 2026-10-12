package com.tokyoghoulrpg.contract;

import com.tokyoghoulrpg.data.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Ежедневные контракты информатора. Каждый игровой день у каждого игрока своё предложение,
 * взять можно один контракт в день. Награды временные.
 */
public enum Contract {
    KILL_GHOULS("kill_ghouls", Kind.KILL_GHOUL, 5, null, 60, 1, 3, true, false),
    KILL_AGENTS("kill_agents", Kind.KILL_CCG, 4, null, 60, 1, 3, false, true),
    KILL_MONSTERS("kill_monsters", Kind.KILL_MONSTER, 12, null, 40, 0, 3, true, true),
    BRING_BONES("bring_bones", Kind.DELIVER, 16, () -> Items.BONE, 40, 0, 2, true, true),
    BRING_FLESH("bring_flesh", Kind.DELIVER, 24, () -> Items.ROTTEN_FLESH, 40, 0, 2, true, true),
    BRING_IRON("bring_iron", Kind.DELIVER, 8, () -> Items.IRON_INGOT, 50, 0, 1, true, true),
    KILL_BOSS("kill_boss", Kind.KILL_BOSS, 1, null, 200, 3, 1, true, true);

    /** Что засчитывается. */
    public enum Kind {
        KILL_GHOUL, KILL_CCG, KILL_MONSTER, KILL_BOSS, DELIVER
    }

    private final String id;
    private final Kind kind;
    private final int goal;
    private final Supplier<Item> item;
    private final int blood;
    private final int points;
    private final int weight;
    private final boolean forHumans;
    private final boolean forGhouls;

    Contract(String id, Kind kind, int goal, Supplier<Item> item, int blood, int points, int weight,
             boolean forHumans, boolean forGhouls) {
        this.id = id;
        this.kind = kind;
        this.goal = goal;
        this.item = item;
        this.blood = blood;
        this.points = points;
        this.weight = weight;
        this.forHumans = forHumans;
        this.forGhouls = forGhouls;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public int goal() {
        return goal;
    }

    public Item item() {
        return item == null ? null : item.get();
    }

    public int blood() {
        return blood;
    }

    public int points() {
        return points;
    }

    /** Люди и агенты охотятся на гулей, гули и полугули — на агентов. Остальное доступно всем. */
    public boolean availableFor(PlayerClass playerClass) {
        boolean ghoulSide = playerClass.hasKagune();
        return ghoulSide ? forGhouls : forHumans;
    }

    public Component title() {
        return Component.translatable("contract.tokyoghoulrpg." + id, goal);
    }

    public static Contract byId(String id) {
        for (Contract c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    /** Предложение на этот день: своё у каждого игрока, одно и то же весь день. */
    public static Contract offerFor(UUID player, PlayerClass playerClass, long day) {
        List<Contract> pool = new ArrayList<>();
        int total = 0;
        for (Contract c : values()) {
            if (c.availableFor(playerClass)) {
                pool.add(c);
                total += c.weight;
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        int roll = new Random(day * 31L + player.getLeastSignificantBits()).nextInt(total);
        for (Contract c : pool) {
            roll -= c.weight;
            if (roll < 0) {
                return c;
            }
        }
        return pool.get(0);
    }
}
