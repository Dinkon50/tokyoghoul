package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.registry.ModBlocks;
import com.tokyoghoulrpg.registry.ModItems;
import com.tokyoghoulrpg.world.Cafe;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Территория клана (приват) вокруг его знамени: квадрат на любой высоте, размер растёт с уровнем клана и числом
 * участников — 10 x 10 на 1-м уровне с одним человеком, +6 за каждый уровень, +2 за участника (до 12): до 80 x 80 на 9-м уровне.
 * Знамя выдаётся главе при создании клана (потерял — можно скрафтить).
 * <ul>
 *   <li>ставит только глава, одно своё знамя на клан, не ближе 96 блоков к чужому знамени и не в кофейне;</li>
 *   <li>чужие не могут ломать и ставить блоки на территории — кроме врагов во время войны кланов;</li>
 *   <li>взрывы на территории блоки не ломают;</li>
 *   <li>раз в 10 минут, пока кто-то из клана в игре, банк получает доход: 100 очков крови за уровень клана;</li>
 *   <li>знамя прочное, как обсидиан. Сломал свой — территории больше нет (знамя вернётся предметом).</li>
 *   <li><b>Захват.</b> Враг сломал знамя во время войны — его клан сразу побеждает, а знамя остаётся на месте,
 *       но теперь это его территория. Выиграл войну иначе (очки, время) — 15 минут, чтобы прийти к любому знамени
 *       побеждённого клана и захватить его (Shift + ПКМ). Захваченная территория защищена так же и приносит
 *       половину дохода. Сколько территорий может держать клан: своя + захваченные, всего 1 на уровнях 2-3,
 *       2 на уровне 4, 3 на уровне 5 — большой клан не может забрать всё. Побеждённый может отбить своё в новой войне.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID)
public final class ClanTerritory {
    /** Сторона территории: 10 блоков на 1-м уровне с одним человеком, +6 за каждый уровень выше 1-го, +2 за каждого участника сверх первого (до 12). */
    public static final int BASE_SIZE = 10;
    public static final int SIZE_PER_LEVEL = 6;
    public static final int SIZE_PER_MEMBER = 2;
    public static final int MEMBERS_COUNTED = 12;
    public static final int TERRITORY_MAX_LEVEL = 9;
    /** Каждую территорию можно улучшать отдельно: +8 к стороне за улучшение, пока не станет 80 x 80. */
    public static final int MAX_SIZE = 80;
    public static final int TIER_SIZE = 8;
    public static final int TIER_COST = 3000;
    public static final int MIN_DISTANCE = 96;
    public static final int MIN_LEVEL = 1;
    public static final long INCOME_MS = 10L * 60 * 1000;
    public static final int INCOME_PER_LEVEL = 100;

    private ClanTerritory() {
    }

    public static String dim(Level level) {
        return level.dimension().location().toString();
    }

    /** Сторона квадрата территории клана в блоках (знамя — в её середине). */
    public static int size(ClanData.Clan clan) {
        // территория растёт до 9-го уровня; 10-й уровень её не увеличивает
        return BASE_SIZE + SIZE_PER_LEVEL * Math.max(0, Math.min(TERRITORY_MAX_LEVEL, clan.level) - 1)
                + SIZE_PER_MEMBER * Math.max(0, Math.min(MEMBERS_COUNTED, clan.members.size()) - 1);
    }

    /** Сторона конкретной территории: размер клана + её улучшения, не больше 80. */
    public static int size(ClanData.Clan clan, String key) {
        return Math.min(MAX_SIZE, size(clan) + TIER_SIZE * clan.tiers.getOrDefault(key, 0));
    }

    /** Сторона основной территории (своего знамени); нет знамени — какой она будет. */
    public static int mainSize(ClanData.Clan clan) {
        return hasFlag(clan) ? size(clan, key(clan.flagDim, clan.flagPos)) : size(clan);
    }

    /** Цена следующего улучшения территории (0 — уже максимум 80 x 80). */
    public static int upgradeCost(ClanData.Clan clan, String key) {
        return size(clan, key) >= MAX_SIZE ? 0 : TIER_COST * (clan.tiers.getOrDefault(key, 0) + 1);
    }

    /** Глава улучшает территорию за очки крови из казны. */
    public static void upgrade(ServerPlayer leader, ClanData data, ClanData.Clan clan, String key) {
        if (clan == null || !leader.getUUID().equals(clan.leader) || !flags(clan).contains(key)) {
            return;
        }
        int cost = upgradeCost(clan, key);
        if (cost <= 0) {
            leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.land.max", MAX_SIZE).withStyle(ChatFormatting.RED));
            return;
        }
        if (clan.treasury < cost) {
            leader.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.land.no_blood", cost).withStyle(ChatFormatting.RED));
            return;
        }
        clan.treasury -= cost;
        clan.tiers.merge(key, 1, Integer::sum);
        data.setDirty();
        int size = size(clan, key);
        for (UUID id : clan.members.keySet()) {
            ServerPlayer member = leader.getServer().getPlayerList().getPlayer(id);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.land.upgraded", com.tokyoghoulrpg.clan.LandSale.place(key),
                        size).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** Попадает ли смещение от знамени в территорию стороной size (при чётной стороне — от -size/2 до size/2 - 1). */
    private static boolean inside(int d, int size) {
        int low = -(size / 2);
        int high = size - size / 2 - 1;
        return d >= low && d <= high;
    }

    /** Внутри ли точка территории этого знамени клана (высота не важна). */
    public static boolean insideKey(ClanData.Clan clan, String key, Level level, double x, double z) {
        if (!dim(level).equals(dimOf(key))) {
            return false;
        }
        BlockPos flag = BlockPos.of(posOf(key));
        int size = size(clan, key);
        return inside((int) Math.floor(x) - flag.getX(), size) && inside((int) Math.floor(z) - flag.getZ(), size);
    }

    public static boolean hasFlag(ClanData.Clan clan) {
        return !clan.flagDim.isEmpty();
    }

    /** Сколько территорий всего может держать клан (своя + захваченные). */
    public static int maxTerritories(ClanData.Clan clan) {
        return clan.level >= 5 ? 3 : clan.level >= 4 ? 2 : 1;
    }

    public static int territoryCount(ClanData.Clan clan) {
        return (hasFlag(clan) ? 1 : 0) + clan.captured.size();
    }

    private static String key(String dim, long pos) {
        return dim + ";" + pos;
    }

    public static long posOf(String key) {
        try {
            return Long.parseLong(key.substring(key.indexOf(';') + 1));
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    public static String dimOf(String key) {
        int i = key.indexOf(';');
        return i < 0 ? "" : key.substring(0, i);
    }

    /** Все знамёна клана: своё и захваченные (ключи «измерение;позиция»). */
    public static java.util.List<String> flags(ClanData.Clan clan) {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (hasFlag(clan)) {
            list.add(key(clan.flagDim, clan.flagPos));
        }
        list.addAll(clan.captured);
        return list;
    }

    /** Чья территория в этой точке (null — ничья). */
    public static ClanData.Clan ownerAt(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        String dim = dim(level);
        for (ClanData.Clan clan : ClanData.get(server).all()) {
            for (String f : flags(clan)) {
                if (!dim.equals(dimOf(f))) {
                    continue;
                }
                BlockPos flag = BlockPos.of(posOf(f));
                int size = size(clan, f);
                if (inside(pos.getX() - flag.getX(), size) && inside(pos.getZ() - flag.getZ(), size)) {
                    return clan;
                }
            }
        }
        return null;
    }

    /** Знамя (ключ), на чьей территории эта точка; null — ничья. */
    public static String flagAt(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        String dim = dim(level);
        for (ClanData.Clan clan : ClanData.get(server).all()) {
            for (String f : flags(clan)) {
                if (!dim.equals(dimOf(f))) {
                    continue;
                }
                BlockPos flag = BlockPos.of(posOf(f));
                int size = size(clan, f);
                if (inside(pos.getX() - flag.getX(), size) && inside(pos.getZ() - flag.getZ(), size)) {
                    return f;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ границы и «Домой»

    /** Игрок → на чьей территории он был в прошлую проверку (id клана или null). */
    private static final java.util.Map<UUID, UUID> LAST_OWNER = new java.util.HashMap<>();
    /** Игрок → {ключ знамени, до какого времени показывать границы (мс)}. */
    private static final java.util.Map<UUID, Object[]> BORDERS = new java.util.HashMap<>();
    /** Игрок → когда снова можно «Домой» (мс). */
    private static final java.util.Map<UUID, Long> HOME_READY = new java.util.HashMap<>();
    public static final long HOME_COOLDOWN_MS = 10L * 60 * 1000;
    public static final long BORDER_SHOW_MS = 15_000L;
    private static final net.minecraft.core.particles.DustParticleOptions BORDER_OWN =
            new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.3F, 1.0F, 0.4F), 1.2F);
    private static final net.minecraft.core.particles.DustParticleOptions BORDER_OTHER =
            new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.25F, 0.2F), 1.2F);

    /** Раз в полсекунды: вошёл / вышел с территории — надпись; показ границ. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 10 != 3) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ClanData data = ClanData.get(server);
        ClanData.Clan owner = ownerAt(player.level(), player.blockPosition());
        UUID ownerId = owner == null ? null : owner.id;
        UUID before = LAST_OWNER.get(player.getUUID());
        if (!java.util.Objects.equals(before, ownerId) || !LAST_OWNER.containsKey(player.getUUID())) {
            LAST_OWNER.put(player.getUUID(), ownerId);
            if (owner != null) {
                ClanData.Clan own = data.clanOf(player.getUUID());
                boolean mine = own == owner;
                boolean ally = own != null && ClanAlliances.allied(own, owner);
                player.displayClientMessage(Component.translatable(mine ? "message.tokyoghoulrpg.territory.enter_own"
                        : ally ? "message.tokyoghoulrpg.territory.enter_ally" : "message.tokyoghoulrpg.territory.enter", owner.name)
                        .withStyle(mine ? ChatFormatting.GREEN : ally ? ChatFormatting.AQUA : ChatFormatting.RED), true);
            } else if (before != null) {
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.territory.leave")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        Object[] view = BORDERS.get(player.getUUID());
        if (view != null) {
            if ((long) view[1] < System.currentTimeMillis()) {
                BORDERS.remove(player.getUUID());
            } else {
                drawBorder(player, data, (String) view[0]);
            }
        }
    }

    /** Нарисовать границу территории частицами — только этому игроку. */
    private static void drawBorder(ServerPlayer player, ClanData data, String key) {
        if (!(player.level() instanceof ServerLevel level) || !dim(level).equals(dimOf(key))) {
            return;
        }
        ClanData.Clan owner = null;
        for (ClanData.Clan clan : data.all()) {
            if (flags(clan).contains(key)) {
                owner = clan;
                break;
            }
        }
        if (owner == null) {
            BORDERS.remove(player.getUUID());
            return;
        }
        BlockPos flag = BlockPos.of(posOf(key));
        int size = size(owner, key);
        int low = -(size / 2);
        int high = size - size / 2;      // внешняя грань последнего блока
        boolean mine = owner.members.containsKey(player.getUUID());
        net.minecraft.core.particles.DustParticleOptions dust = mine ? BORDER_OWN : BORDER_OTHER;
        double y = player.getY() + 1.0;
        for (int i = low; i <= high; i += 2) {
            for (double[] p : new double[][] {{flag.getX() + i, flag.getZ() + low}, {flag.getX() + i, flag.getZ() + high},
                    {flag.getX() + low, flag.getZ() + i}, {flag.getX() + high, flag.getZ() + i}}) {
                if (Math.abs(p[0] - player.getX()) > 48 || Math.abs(p[1] - player.getZ()) > 48) {
                    continue;
                }
                level.sendParticles(player, dust, true, p[0], y, p[1], 1, 0.0, 0.6, 0.0, 0.0);
            }
        }
    }

    /** «Домой»: телепорт к знамени клана (основному, иначе первому захваченному). */
    public static void home(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ClanData.Clan clan = server == null ? null : ClanData.get(server).clanOf(player.getUUID());
        if (clan == null) {
            return;
        }
        java.util.List<String> all = flags(clan);
        if (all.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.home_none").withStyle(ChatFormatting.RED));
            return;
        }
        long now = System.currentTimeMillis();
        long ready = HOME_READY.getOrDefault(player.getUUID(), 0L);
        if (ready > now) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.home_cooldown",
                    (ready - now + 59_999) / 60_000).withStyle(ChatFormatting.RED));
            return;
        }
        if (player.getLastHurtByMob() != null && player.tickCount - player.getLastHurtByMobTimestamp() < 200) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.home_combat").withStyle(ChatFormatting.RED));
            return;
        }
        String key = all.get(0);
        ServerLevel target = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (dim(level).equals(dimOf(key))) {
                target = level;
            }
        }
        if (target == null) {
            return;
        }
        BlockPos flag = BlockPos.of(posOf(key));
        HOME_READY.put(player.getUUID(), now + HOME_COOLDOWN_MS);
        if (player.level() instanceof ServerLevel from) {
            from.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(),
                    40, 0.4, 0.8, 0.4, 0.2);
        }
        player.teleportTo(target, flag.getX() + 0.5, flag.getY() + 1.0, flag.getZ() + 0.5, player.getYRot(), player.getXRot());
        Effects.sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.home_done", clan.name)
                .withStyle(ChatFormatting.GOLD));
    }

    /** Может ли игрок строить и ломать здесь. */
    private static boolean allowed(Player player, ClanData.Clan owner) {
        if (owner == null || player.isCreative() || owner.members.containsKey(player.getUUID())) {
            return true;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return true;
        }
        ClanData data = ClanData.get(sp.getServer());
        ClanData.Clan own = data.clanOf(sp.getUUID());
        return own != null && ClanWars.enemies(data, own.id, owner.id);
    }

    /** Глава поставил знамя. Нельзя — блок исчезает, знамя возвращается. */
    public static void onPlaced(ServerPlayer player, BlockPos pos) {
        ClanData data = ClanData.get(player.getServer());
        ClanData.Clan clan = data.clanOf(player.getUUID());
        String reason = null;
        if (clan == null || !player.getUUID().equals(clan.leader)) {
            reason = "territory.leader_only";
        } else if (clan.level < MIN_LEVEL) {
            reason = "territory.level";
        } else if (hasFlag(clan)) {
            reason = "territory.one_flag";
        } else if (territoryCount(clan) >= maxTerritories(clan)) {
            reason = "territory.limit";
        } else if (Cafe.contains(player.level(), pos)) {
            reason = "territory.cafe";
        } else {
            String dim = dim(player.level());
            for (ClanData.Clan other : data.all()) {
                for (String f : flags(other)) {
                    if (dim.equals(dimOf(f)) && BlockPos.of(posOf(f)).distSqr(pos) < (double) MIN_DISTANCE * MIN_DISTANCE) {
                        reason = "territory.too_close";
                        break;
                    }
                }
            }
        }
        if (reason != null) {
            player.level().removeBlock(pos, false);
            if (!player.getAbilities().instabuild) {
                ItemStack back = new ItemStack(ModItems.CLAN_BANNER.get());
                if (!player.getInventory().add(back)) {
                    player.drop(back, false);
                }
            }
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg." + reason, MIN_LEVEL, MIN_DISTANCE)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        clan.flagDim = dim(player.level());
        clan.flagPos = pos.asLong();
        clan.lastIncome = System.currentTimeMillis();
        data.setDirty();
        Effects.sound(player, SoundEvents.ARMOR_EQUIP_GOLD, 1.0F, 0.8F);
        for (UUID id : clan.members.keySet()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(id);
            if (member != null) {
                member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.claimed", clan.name,
                        pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** ПКМ по знамени: сведения; глава с Shift — призвать кланового босса. */
    public static void onUse(ServerPlayer player, BlockPos pos) {
        ClanData.Clan owner = flagOwner(player.level(), pos);
        if (owner == null) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.unclaimed"));
            return;
        }
        // захват: Shift + ПКМ по знамени побеждённого клана, пока не прошли 15 минут после победы
        ClanData data = ClanData.get(player.getServer());
        ClanData.Clan own = data.clanOf(player.getUUID());
        if (player.isShiftKeyDown() && own != null && own != owner && ClanWars.canCapture(own.id, owner.id)) {
            if (capture(player.getServer(), data, own, owner, dim(player.level()), pos.asLong())) {
                ClanWars.useCapture(own.id);
                announceCapture(player.getServer(), own, owner, player.getGameProfile().getName());
            } else {
                player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.limit_capture",
                        maxTerritories(own)).withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (player.isShiftKeyDown() && player.getUUID().equals(owner.leader)
                && pos.asLong() == owner.flagPos && dim(player.level()).equals(owner.flagDim)) {
            ClanBoss.summon(player, owner, pos);
            return;
        }
        BORDERS.put(player.getUUID(), new Object[] {key(dim(player.level()), pos.asLong()),
                System.currentTimeMillis() + BORDER_SHOW_MS});
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.info", owner.name, owner.level,
                INCOME_PER_LEVEL * owner.level, size(owner, key(dim(player.level()), pos.asLong()))).withStyle(ChatFormatting.GOLD));
        if (player.getUUID().equals(owner.leader)) {
            player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.clan_boss.hint", ClanBoss.COST,
                    ClanBoss.MIN_LEVEL).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Клан, чьё знамя (своё или захваченное) стоит ровно здесь. */
    private static ClanData.Clan flagOwner(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }
        String at = key(dim(level), pos.asLong());
        for (ClanData.Clan clan : ClanData.get(server).all()) {
            if (flags(clan).contains(at)) {
                return clan;
            }
        }
        return null;
    }

    /** Убрать у клана это знамя (своё или захваченное). */
    private static void removeFlag(MinecraftServer server, ClanData.Clan clan, String dim, long pos) {
        if (dim.equals(clan.flagDim) && pos == clan.flagPos) {
            clan.flagDim = "";
            clan.flagPos = 0L;
        }
        clan.captured.remove(key(dim, pos));
        clan.forSale.remove(key(dim, pos));
        clan.tiers.remove(key(dim, pos));
        clan.guards.remove(key(dim, pos));
        ClanData.get(server).setDirty();
    }

    /** Передать знамя от побеждённого клана победителю. false — у победителя нет места для новой территории. */
    private static boolean capture(MinecraftServer server, ClanData data, ClanData.Clan winner, ClanData.Clan loser,
                                   String dim, long pos) {
        if (territoryCount(winner) >= maxTerritories(winner)) {
            return false;
        }
        Integer tier = loser.tiers.get(key(dim, pos));   // улучшения территории переходят вместе с ней
        removeFlag(server, loser, dim, pos);
        winner.captured.add(key(dim, pos));
        if (tier != null) {
            winner.tiers.put(key(dim, pos), tier);
        }
        data.setDirty();
        return true;
    }

    private static void announceCapture(MinecraftServer server, ClanData.Clan winner, ClanData.Clan loser, String who) {
        Component msg = Component.translatable("message.tokyoghoulrpg.territory.captured", winner.name, loser.name, who)
                .withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
        }
    }

    // ------------------------------------------------------------------ защита

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        if (event.getState().is(ModBlocks.CLAN_BANNER.get())) {
            ClanData.Clan owner = flagOwner(level, pos);
            if (owner == null) {
                return;
            }
            if (!allowed(player, owner)) {
                event.setCanceled(true);
                player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.territory.protected", owner.name), true);
                return;
            }
            MinecraftServer server = level.getServer();
            String dim = dim(level);
            // враг во время войны: знамя не ломается, а переходит к его клану, и война выиграна
            if (!owner.members.containsKey(player.getUUID()) && player instanceof ServerPlayer sp) {
                ClanData data = ClanData.get(server);
                ClanData.Clan enemy = data.clanOf(sp.getUUID());
                if (enemy != null && capture(server, data, enemy, owner, dim, pos.asLong())) {
                    event.setCanceled(true);
                    announceCapture(server, enemy, owner, sp.getGameProfile().getName());
                    ClanWars.onFlagBroken(server, owner.id, sp);
                    return;
                }
                removeFlag(server, owner, dim, pos.asLong());
                ClanWars.onFlagBroken(server, owner.id, sp);
            } else {
                removeFlag(server, owner, dim, pos.asLong());
            }
            for (UUID id : owner.members.keySet()) {
                ServerPlayer member = server.getPlayerList().getPlayer(id);
                if (member != null) {
                    member.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.territory.lost", owner.name,
                            player.getGameProfile().getName()).withStyle(ChatFormatting.RED));
                }
            }
            return;
        }
        ClanData.Clan owner = ownerAt(level, pos);
        if (!allowed(player, owner)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.territory.protected", owner.name), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ClanData.Clan owner = ownerAt(level, event.getPos());
        if (!allowed(player, owner)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.tokyoghoulrpg.territory.protected", owner.name), true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> ownerAt(level, pos) != null);
    }

    // ------------------------------------------------------------------ доход

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        ClanWars.tick(server);
        com.tokyoghoulrpg.market.Market.tick(server);
        if (server.getTickCount() % 200 != 0) {
            return;
        }
        long now = System.currentTimeMillis();
        ClanData data = ClanData.get(server);
        for (ClanData.Clan clan : data.all()) {
            if (territoryCount(clan) == 0) {
                continue;
            }
            // знамя убрали командой или иначе — территории нет
            for (String f : flags(clan)) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (dim(level).equals(dimOf(f))) {
                        BlockPos pos = BlockPos.of(posOf(f));
                        if (level.isLoaded(pos) && !level.getBlockState(pos).is(ModBlocks.CLAN_BANNER.get())) {
                            removeFlag(server, clan, dimOf(f), posOf(f));
                        }
                    }
                }
            }
            if (territoryCount(clan) == 0 || now - clan.lastIncome < INCOME_MS) {
                continue;
            }
            boolean online = false;
            for (UUID id : clan.members.keySet()) {
                if (server.getPlayerList().getPlayer(id) != null) {
                    online = true;
                    break;
                }
            }
            clan.lastIncome = now;
            if (online) {
                // своя территория — полный доход, каждая захваченная — половина
                long income = (hasFlag(clan) ? (long) INCOME_PER_LEVEL * clan.level : 0L)
                        + clan.captured.size() * (long) INCOME_PER_LEVEL * clan.level / 2;
                clan.treasury = (int) Math.min(Integer.MAX_VALUE, clan.treasury + income);
            }
            data.setDirty();
        }
    }
}
