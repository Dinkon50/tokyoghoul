package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.entity.GhoulBoss;
import com.tokyoghoulrpg.registry.ModEntities;
import com.tokyoghoulrpg.registry.ModItems;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Клановый босс: глава клана 3+ уровня призывает у своего знамени (Shift + ПКМ) за 5000 очков крови из банка.
 * Это Пожиратель-страж: здоровья в 3 раза больше, урон в 1,5 раза. Откат — 6 часов.
 * Победа (кто бы ни добил): банку клана 6000 очков крови и 15 очков навыков, на склад клана — 2 слитка RC-стали,
 * 2 аптечки, контракт наёмника и записи лаборатории (не влезло — падает у знамени). Засчитывается в задания и рейтинг.
 */
public final class ClanBoss {
    public static final String TAG = "tgrpg_clan_boss";
    public static final int COST = 5000;
    public static final int MIN_LEVEL = 3;
    public static final long COOLDOWN_MS = 6L * 60 * 60 * 1000;
    public static final int REWARD_BLOOD = 6000;
    public static final int REWARD_POINTS = 15;
    private static final UUID HEALTH_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4ea9");
    private static final UUID DAMAGE_ID = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4eaa");

    private ClanBoss() {
    }

    public static void summon(ServerPlayer leader, ClanData.Clan clan, BlockPos flag) {
        long now = System.currentTimeMillis();
        if (clan.level < MIN_LEVEL) {
            tell(leader, "level", MIN_LEVEL);
            return;
        }
        if (clan.bossCooldownUntil > now) {
            tell(leader, "cooldown", (clan.bossCooldownUntil - now + 59_999) / 60_000);
            return;
        }
        if (clan.treasury < COST) {
            tell(leader, "no_blood", COST);
            return;
        }
        if (!(leader.level() instanceof ServerLevel level)) {
            return;
        }
        GhoulBoss boss = ModEntities.GHOUL_BOSS.get().create(level);
        if (boss == null) {
            return;
        }
        clan.treasury -= COST;
        clan.bossCooldownUntil = now + COOLDOWN_MS;
        ClanData.get(leader.getServer()).setDirty();
        boss.moveTo(flag.getX() + 0.5 + 4, flag.getY(), flag.getZ() + 0.5, 0.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(flag), MobSpawnType.EVENT, null, null);
        boss.getPersistentData().putUUID(TAG, clan.id);
        add(boss, Attributes.MAX_HEALTH, HEALTH_ID, 2.0);
        add(boss, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 0.5);
        boss.setHealth(boss.getMaxHealth());
        boss.setPersistenceRequired();
        boss.setCustomName(Component.translatable("entity.tokyoghoulrpg.clan_boss", clan.name).withStyle(ChatFormatting.DARK_RED));
        level.addFreshEntity(boss);
        Effects.sound(leader, SoundEvents.WITHER_SPAWN, 1.0F, 0.8F);
        for (UUID id : clan.members.keySet()) {
            ServerPlayer member = leader.getServer().getPlayerList().getPlayer(id);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan_boss.summoned",
                        leader.getGameProfile().getName(), flag.getX(), flag.getY(), flag.getZ()).withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    private static void add(LivingEntity mob, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, double amount) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, "tg_clan_boss", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** Погиб клановый босс: награда его клану. */
    public static void onDeath(LivingEntity entity) {
        if (!entity.getPersistentData().hasUUID(TAG) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        MinecraftServer server = level.getServer();
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.byId(entity.getPersistentData().getUUID(TAG));
        if (clan == null) {
            return;
        }
        clan.treasury = (int) Math.min(Integer.MAX_VALUE, (long) clan.treasury + REWARD_BLOOD);
        clan.bankPoints = (int) Math.min(Integer.MAX_VALUE, (long) clan.bankPoints + REWARD_POINTS);
        clan.bossKills++;
        data.setDirty();
        ItemStack[] loot = {new ItemStack(ModItems.RC_STEEL_INGOT.get(), 2), new ItemStack(ModItems.MEDKIT.get(), 2),
                new ItemStack(ModItems.MERC_CONTRACT.get()), new ItemStack(ModItems.LAB_NOTES.get())};
        for (ItemStack stack : loot) {
            ItemStack rest = clan.storage.addItem(stack);
            if (!rest.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), rest));
            }
        }
        ClanQuests.progress(server, clan, ClanQuests.Kind.KILL_BOSS, 1);
        for (UUID id : clan.members.keySet()) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan_boss.defeated", REWARD_BLOOD,
                        REWARD_POINTS).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan_boss." + key, args).withStyle(ChatFormatting.RED));
    }
}
