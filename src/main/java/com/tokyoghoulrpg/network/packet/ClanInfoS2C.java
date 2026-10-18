package com.tokyoghoulrpg.network.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Дополнительные сведения для меню развития клана: задания, войны (кому можно объявить), рейтинг,
 * территория и клановый босс. Приходит вместе с ClanStateS2C.
 */
public class ClanInfoS2C {
    /** Задание: вид (номер ClanQuests.Kind), прогресс, цель, ежедневное ли. */
    public record Quest(int kind, int progress, int target, boolean daily) {
    }

    /** Другой клан для вкладки «Войны»: reason — пусто, если можно объявить войну, иначе ключ причины. */
    public record WarTarget(UUID id, String name, int level, int members, String reason, long protectedMinutes,
                            int allyState) {
    }

    /** Своя территория: ключ, где стоит, основная ли, цена (0 — не продаётся). */
    public record Land(String key, String place, boolean main, int price, int size, int upgradeCost, int guards) {
    }

    /** Территория другого клана на продаже. */
    public record LandOffer(UUID clan, String clanName, String key, String place, int price) {
    }

    /** Строка рейтинга. */
    public record Rating(String name, int level, int members, int wins, int score, boolean own) {
    }

    public final List<Quest> quests;
    public final List<WarTarget> targets;
    public final List<Rating> rating;
    /** Своя война: враг (пусто — войны нет), счёт, минут до конца. */
    public final String warEnemy;
    public final int warOur;
    public final int warTheir;
    public final int warWins;
    public final int warLosses;
    public final long protectedMinutes;
    /** Территория: есть ли знамя и где, доход за 10 минут; минут до кланового босса. */
    public final String territory;
    public final int income;
    public final long bossMinutes;
    public final List<Land> lands;
    public final List<LandOffer> offers;

    public ClanInfoS2C(List<Quest> quests, List<WarTarget> targets, List<Rating> rating, String warEnemy, int warOur,
                       int warTheir, int warWins, int warLosses, long protectedMinutes, String territory, int income,
                       long bossMinutes, List<Land> lands, List<LandOffer> offers) {
        this.lands = lands;
        this.offers = offers;
        this.quests = quests;
        this.targets = targets;
        this.rating = rating;
        this.warEnemy = warEnemy;
        this.warOur = warOur;
        this.warTheir = warTheir;
        this.warWins = warWins;
        this.warLosses = warLosses;
        this.protectedMinutes = protectedMinutes;
        this.territory = territory;
        this.income = income;
        this.bossMinutes = bossMinutes;
    }

    public static void encode(ClanInfoS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.quests.size());
        for (Quest q : msg.quests) {
            buf.writeVarInt(q.kind());
            buf.writeVarInt(q.progress());
            buf.writeVarInt(q.target());
            buf.writeBoolean(q.daily());
        }
        buf.writeVarInt(msg.targets.size());
        for (WarTarget t : msg.targets) {
            buf.writeUUID(t.id());
            buf.writeUtf(t.name(), 32);
            buf.writeVarInt(t.level());
            buf.writeVarInt(t.members());
            buf.writeUtf(t.reason(), 64);
            buf.writeVarLong(t.protectedMinutes());
            buf.writeVarInt(t.allyState());
        }
        buf.writeVarInt(msg.rating.size());
        for (Rating r : msg.rating) {
            buf.writeUtf(r.name(), 32);
            buf.writeVarInt(r.level());
            buf.writeVarInt(r.members());
            buf.writeVarInt(r.wins());
            buf.writeVarInt(r.score());
            buf.writeBoolean(r.own());
        }
        buf.writeUtf(msg.warEnemy, 32);
        buf.writeVarInt(msg.warOur);
        buf.writeVarInt(msg.warTheir);
        buf.writeVarInt(msg.warWins);
        buf.writeVarInt(msg.warLosses);
        buf.writeVarLong(msg.protectedMinutes);
        buf.writeUtf(msg.territory, 64);
        buf.writeVarInt(msg.income);
        buf.writeVarLong(msg.bossMinutes);
        buf.writeVarInt(msg.lands.size());
        for (Land l : msg.lands) {
            buf.writeUtf(l.key(), 80);
            buf.writeUtf(l.place(), 40);
            buf.writeBoolean(l.main());
            buf.writeVarInt(l.price());
            buf.writeVarInt(l.size());
            buf.writeVarInt(l.upgradeCost());
            buf.writeVarInt(l.guards());
        }
        buf.writeVarInt(msg.offers.size());
        for (LandOffer o : msg.offers) {
            buf.writeUUID(o.clan());
            buf.writeUtf(o.clanName(), 32);
            buf.writeUtf(o.key(), 80);
            buf.writeUtf(o.place(), 40);
            buf.writeVarInt(o.price());
        }
    }

    public static ClanInfoS2C decode(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 16);
        List<Quest> quests = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            quests.add(new Quest(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        }
        n = Math.min(buf.readVarInt(), 256);
        List<WarTarget> targets = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            targets.add(new WarTarget(buf.readUUID(), buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64),
                    buf.readVarLong(), buf.readVarInt()));
        }
        n = Math.min(buf.readVarInt(), 64);
        List<Rating> rating = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rating.add(new Rating(buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean()));
        }
        String warEnemy = buf.readUtf(32);
        int warOur = buf.readVarInt();
        int warTheir = buf.readVarInt();
        int warWins = buf.readVarInt();
        int warLosses = buf.readVarInt();
        long protectedMinutes = buf.readVarLong();
        String territory = buf.readUtf(64);
        int income = buf.readVarInt();
        long bossMinutes = buf.readVarLong();
        n = Math.min(buf.readVarInt(), 16);
        List<Land> lands = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lands.add(new Land(buf.readUtf(80), buf.readUtf(40), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt()));
        }
        n = Math.min(buf.readVarInt(), 256);
        List<LandOffer> offers = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            offers.add(new LandOffer(buf.readUUID(), buf.readUtf(32), buf.readUtf(80), buf.readUtf(40), buf.readVarInt()));
        }
        return new ClanInfoS2C(quests, targets, rating, warEnemy, warOur, warTheir, warWins, warLosses, protectedMinutes,
                territory, income, bossMinutes, lands, offers);
    }

    public static void handle(ClanInfoS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.tokyoghoulrpg.client.screen.ClanScreen.acceptInfo(msg)));
        context.setPacketHandled(true);
    }
}
