package com.tokyoghoulrpg.rep;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Достижения во вкладке Minecraft (клавиша L), data/tokyoghoulrpg/advancements. Обычные выдаёт сама игра
 * (убийство, посещение постройки), а те, что с критерием «code», — мод через grant().
 * Боссов, которых бьют толпой, засчитывает всем участникам: grantKill() по имени существа (kill_&lt;id&gt;).
 */
public final class ModAdvancements {
    private ModAdvancements() {
    }

    public static void grant(ServerPlayer player, String id) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(new ResourceLocation(TokyoGhoulRPG.MOD_ID, id));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "code");
        }
    }

    /** Участник победы над боссом: достижение kill_&lt;id существа&gt;, если такое есть. */
    public static void grantKill(ServerPlayer player, Entity boss) {
        grant(player, "kill_" + BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).getPath());
    }
}
