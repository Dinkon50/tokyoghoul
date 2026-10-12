package com.tokyoghoulrpg.combat;

import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.entity.CcgAgentNpc;
import com.tokyoghoulrpg.entity.CcgBoss;
import com.tokyoghoulrpg.entity.InformantNpc;
import com.tokyoghoulrpg.network.NetworkHandler;
import com.tokyoghoulrpg.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Еда гуля. Гуль и полугуль могут есть только плоть и пить кофе: обычная еда вызывает рвоту
 * (сытость не прибавляется, ещё и отнимается, тошнота и голод). Плоть даёт много сытости
 * и утоляет жажду крови, человека от неё мутит. Плоть выпадает из людей, которых убил гуль.
 */
public final class GhoulDiet {
    private GhoulDiet() {
    }

    public static boolean isGhoulFood(ItemStack stack) {
        return stack.is(ModItems.FLESH.get()) || stack.is(ModItems.COFFEE.get());
    }

    /** Съедено (уже прибавлено к сытости): разбираемся, можно ли было. */
    public static void onEaten(ServerPlayer player, ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(player);
        PlayerData data = PlayerData.get(player);
        if (food == null || data == null || player.isCreative()) {
            return;
        }
        boolean ghoul = data.getPlayerClass().hasKagune();
        if (ghoul && stack.is(ModItems.FLESH.get())) {
            data.setCraving(data.getCraving() + Balance.FLESH_CRAVING);
            NetworkHandler.syncTo(player);
            return;
        }
        if (ghoul && !isGhoulFood(stack)) {
            vomit(player, food, stack);
            return;
        }
        if (!ghoul && stack.is(ModItems.FLESH.get())) {
            // человеку плоть противна: подташнивает
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Balance.FLESH_HUMAN_NAUSEA, 0));
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.flesh_human"), true);
        }
    }

    /** Рвота: съеденное не идёт впрок, ещё и забирает немного голода. */
    private static void vomit(ServerPlayer player, FoodProperties food, ItemStack stack) {
        FoodData data = player.getFoodData();
        int nutrition = food.getNutrition();
        data.setFoodLevel(Math.max(0, data.getFoodLevel() - nutrition - Balance.VOMIT_FOOD_LOSS));
        data.setSaturation(Math.max(0.0F, Math.min(data.getSaturationLevel() - nutrition * food.getSaturationModifier() * 2.0F,
                data.getFoodLevel())));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Balance.VOMIT_NAUSEA, 0));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, Balance.VOMIT_HUNGER, 1));
        Effects.sound(player, SoundEvents.PLAYER_BURP, 1.0F, 0.5F);
        Effects.particles(player, new ItemParticleOption(ParticleTypes.ITEM, stack.copyWithCount(1)), 12, 0.2, 0.08);
        player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.vomit"), true);
    }

    /** Из кого выпадает плоть, если его убил гуль или полугуль. */
    public static boolean dropsFlesh(LivingEntity victim) {
        return victim instanceof Player || victim instanceof AbstractVillager || victim instanceof AbstractIllager
                || victim instanceof Witch || victim instanceof CcgAgentNpc || victim instanceof CcgBoss
                || victim instanceof InformantNpc;
    }
}
