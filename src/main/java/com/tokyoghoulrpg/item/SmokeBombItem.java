package com.tokyoghoulrpg.item;

import com.tokyoghoulrpg.clan.ClanData;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.rep.Standing;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Дымовая шашка: ПКМ — облако дыма вокруг. Враги рядом слепнут и теряют тебя из виду,
 * ты на 4 с невидим и быстрее. Чтобы уйти из боя. Своих по клану дым не слепит. Откат 20 с.
 */
public class SmokeBombItem extends Item {
    public SmokeBombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer user && level instanceof ServerLevel server) {
            double r = Balance.SMOKE_RADIUS;
            ClanData.Clan clan = Standing.clanOf(user);
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(r),
                    e -> e != user && e.isAlive() && e.distanceToSqr(user) <= r * r)) {
                if (other instanceof Player otherPlayer) {
                    if (otherPlayer instanceof ServerPlayer sp && clan != null && clan == Standing.clanOf(sp)) {
                        continue;
                    }
                } else if (other instanceof Mob mob && mob.getTarget() == user) {
                    mob.setTarget(null);
                }
                other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Balance.SMOKE_BLIND_TICKS, 0));
            }
            user.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Balance.SMOKE_INVIS_TICKS, 0, false, false, true));
            user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Balance.SMOKE_INVIS_TICKS, 1, false, false, true));
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, user.getX(), user.getY() + 0.8, user.getZ(),
                    60, r * 0.4, 0.8, r * 0.4, 0.01);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, user.getX(), user.getY() + 0.5, user.getZ(),
                    80, r * 0.5, 0.6, r * 0.5, 0.02);
            Effects.sound(user, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.6F);
            user.getCooldowns().addCooldown(this, Balance.SMOKE_COOLDOWN);
            if (!user.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tokyoghoulrpg.smoke_bomb.tooltip", Math.round(Balance.SMOKE_RADIUS),
                Balance.SMOKE_BLIND_TICKS / 20, Balance.SMOKE_COOLDOWN / 20).withStyle(ChatFormatting.GRAY));
    }
}
