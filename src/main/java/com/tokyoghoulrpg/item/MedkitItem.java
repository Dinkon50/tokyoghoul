package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.skill.SkillEffects;
import com.tokyoghoulrpg.skill.SkillTree;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Аптечка: ПКМ — сразу 4 сердца и Регенерация I на 5 с. Откат 10 с.
 * Ветка «Первая помощь» лечит больше и уменьшает откат. Работает у всех классов.
 */
public class MedkitItem extends Item {
    public MedkitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.medkit.full"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int aid = SkillEffects.level(PlayerData.get(serverPlayer), SkillTree.Branch.FIRST_AID);
            float heal = (float) (Balance.MEDKIT_HEAL * (1.0 + SkillEffects.firstAidHeal(aid)));
            serverPlayer.heal(heal);
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Balance.MEDKIT_REGEN_TICKS, 0));
            serverPlayer.getCooldowns().addCooldown(this, SkillEffects.medkitCooldown(aid));
            Effects.sound(serverPlayer, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 1.4F);
            Effects.particles(serverPlayer, ParticleTypes.HEART, 5, 0.5, 0.05);
            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.medkit.tooltip",
                Math.round(Balance.MEDKIT_HEAL / 2), Balance.MEDKIT_REGEN_TICKS / 20, Balance.MEDKIT_COOLDOWN / 20)
                .withStyle(ChatFormatting.GRAY));
    }
}
