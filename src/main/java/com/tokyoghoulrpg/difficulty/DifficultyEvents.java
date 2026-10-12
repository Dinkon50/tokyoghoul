package com.tokyoghoulrpg.difficulty;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Применяет сложность к мобам: при появлении, раз в 5 секунд и в момент удара. */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class DifficultyEvents {
    private static final int REFRESH_TICKS = 100;

    private DifficultyEvents() {
    }

    /** Новый или загруженный моб сразу получает здоровье и скорость своей сложности. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob) {
            DifficultyScaling.refresh(mob);
        }
    }

    /** Раз в 5 секунд: сложность могли сменить в настройках, игроки могли зайти или выйти. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob mob)) {
            return;
        }
        if ((mob.tickCount + mob.getId()) % REFRESH_TICKS == 0) {
            DifficultyScaling.refresh(mob);
        }
    }

    /**
     * Урон моба умножается в момент удара: так усиливаются и удары, и стрелы, и взрывы, и особые
     * атаки боссов. Срабатывает раньше защиты игрока (ветки «Защита», «Стойкость», ярость).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Mob mob && attacker != event.getEntity()) {
            double factor = DifficultyScaling.damageFactor(mob);
            if (factor != 1.0) {
                event.setAmount((float) (event.getAmount() * factor));
            }
        }
    }
}
