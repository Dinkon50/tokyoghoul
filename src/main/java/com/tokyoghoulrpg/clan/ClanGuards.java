package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.entity.GuardNpc;
import com.tokyoghoulrpg.registry.ModEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Стражи территорий: найм (глава, кровь из казны), учёт количества и «обидчики» клана —
 * игроки, которые недавно ранили участника клана или стража; на территории стражи их атакуют.
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class ClanGuards {
    /** Клан → (игрок → до какого времени он враг стражей, мс). */
    private static final Map<UUID, Map<UUID, Long>> GRUDGES = new HashMap<>();

    private ClanGuards() {
    }

    public static void clear() {
        GRUDGES.clear();
    }

    public static boolean grudge(UUID clan, UUID player) {
        Map<UUID, Long> map = GRUDGES.get(clan);
        Long until = map == null ? null : map.get(player);
        return until != null && until > System.currentTimeMillis();
    }

    /** Сколько стражей нанято на этой территории (живых). */
    public static int count(MinecraftServer server, ClanData.Clan clan, String key) {
        return ClanTerritory.flags(clan).contains(key) ? clan.guards.getOrDefault(key, 0) : 0;
    }

    /** Нанять стража: встаёт у знамени территории, где стражей меньше всего. */
    public static void hire(ServerPlayer leader, ClanData data, ClanData.Clan clan) {
        if (clan == null || !leader.getUUID().equals(clan.leader)) {
            return;
        }
        MinecraftServer server = leader.getServer();
        clan.guards.keySet().removeIf(k -> !ClanTerritory.flags(clan).contains(k));
        String best = null;
        int bestCount = Integer.MAX_VALUE;
        for (String key : ClanTerritory.flags(clan)) {
            int n = clan.guards.getOrDefault(key, 0);
            if (n < bestCount) {
                best = key;
                bestCount = n;
            }
        }
        if (best == null) {
            fail(leader, "guard.no_territory");
            return;
        }
        if (bestCount >= Balance.GUARD_PER_TERRITORY) {
            fail(leader, "guard.full", Balance.GUARD_PER_TERRITORY);
            return;
        }
        if (clan.treasury < Balance.GUARD_PRICE) {
            fail(leader, "guard.no_money", Balance.GUARD_PRICE);
            return;
        }
        ServerLevel level = null;
        for (ServerLevel l : server.getAllLevels()) {
            if (ClanTerritory.dim(l).equals(ClanTerritory.dimOf(best))) {
                level = l;
            }
        }
        if (level == null) {
            return;
        }
        BlockPos flag = BlockPos.of(ClanTerritory.posOf(best));
        level.getChunkAt(flag);      // догрузить чанк со знаменем
        GuardNpc guard = ModEntities.GUARD.get().create(level);
        if (guard == null) {
            return;
        }
        BlockPos spot = spot(level, flag);
        guard.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        guard.setup(clan.id, best, flag, ClanTerritory.size(clan, best) / 2);
        guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        guard.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        guard.setCustomName(Component.translatable("entity.tokyoghoulrpg.guard.of", clan.name).withStyle(ChatFormatting.DARK_AQUA));
        guard.setCustomNameVisible(true);
        level.addFreshEntity(guard);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, guard.getX(), guard.getY() + 1.0, guard.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        clan.treasury -= Balance.GUARD_PRICE;
        clan.guards.merge(best, 1, Integer::sum);
        data.setDirty();
        Component msg = Component.translatable("message.tokyoghoulrpg.guard.hired", LandSale.place(best),
                clan.guards.get(best), Balance.GUARD_PER_TERRITORY).withStyle(ChatFormatting.DARK_AQUA);
        for (UUID id : clan.members.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.sendSystemMessage(msg);
            }
        }
    }

    /** Свободное место рядом со знаменем (2 блока в сторону), иначе прямо над знаменем. */
    private static BlockPos spot(ServerLevel level, BlockPos flag) {
        int[][] offsets = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, -2}, {2, -2}, {-2, 2}};
        for (int[] o : offsets) {
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos feet = flag.offset(o[0], dy - 1, o[1]);
                if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                        && !level.getBlockState(feet.below()).isAir()) {
                    return feet;
                }
            }
        }
        return flag.above();
    }

    /** Страж погиб — место освободилось. */
    public static void onGuardDied(GuardNpc guard) {
        MinecraftServer server = guard.getServer();
        if (server == null || guard.clanId() == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan clan = data.byId(guard.clanId());
        if (clan == null) {
            return;
        }
        String key = guard.territoryKey();
        int left = clan.guards.getOrDefault(key, 0) - 1;
        if (left > 0) {
            clan.guards.put(key, left);
        } else {
            clan.guards.remove(key);
        }
        data.setDirty();
        Component msg = Component.translatable("message.tokyoghoulrpg.guard.died", LandSale.place(key))
                .withStyle(ChatFormatting.RED);
        for (UUID id : clan.members.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.sendSystemMessage(msg);
            }
        }
    }

    /** Кто ранил участника клана или стража — минуту враг стражей этого клана (свои и союзники не в счёт). */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        MinecraftServer server = attacker.getServer();
        if (server == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        UUID victimClan = null;
        if (event.getEntity() instanceof GuardNpc guard) {
            victimClan = guard.clanId();
        } else if (event.getEntity() instanceof ServerPlayer victim) {
            ClanData.Clan c = data.clanOf(victim.getUUID());
            victimClan = c == null ? null : c.id;
        }
        if (victimClan == null) {
            return;
        }
        ClanData.Clan own = data.clanOf(attacker.getUUID());
        if (own != null && (own.id.equals(victimClan) || ClanAlliances.allied(own, data.byId(victimClan)))) {
            return;
        }
        GRUDGES.computeIfAbsent(victimClan, k -> new HashMap<>())
                .put(attacker.getUUID(), System.currentTimeMillis() + Balance.GUARD_GRUDGE_MS);
    }
}
