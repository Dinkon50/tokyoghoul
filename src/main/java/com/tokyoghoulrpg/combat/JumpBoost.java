package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.skill.SkillEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ветка «Прыжок»: прыжок выше. Прыжок игрока считает его клиент, поэтому прибавка добавляется там же
 * (на сервере событие тоже приходит, вреда нет). Насколько выше — зависит от кагуне (SkillEffects.jumpBonus).
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class JumpBoost {
    private JumpBoost() {
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        double bonus = SkillEffects.jumpBonus(PlayerData.get(player));
        if (bonus <= 0.0) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, motion.y + bonus, motion.z);
    }
}
