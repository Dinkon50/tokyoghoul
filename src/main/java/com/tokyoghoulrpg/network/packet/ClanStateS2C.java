package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Состояние меню клана. members: участники (flag — в игре ли), candidates: игроки без клана
 * (flag — уже приглашён), invites: приглашения в кланы (id клана и «название (глава)»).
 */
public class ClanStateS2C {
    public record Member(UUID id, String name, boolean flag) {
    }

    private static final UUID NONE = new UUID(0L, 0L);

    public final String clanName;
    public final UUID leader;
    /** Репутация клана (от -100 до 100). */
    public final int reputation;
    public final List<Member> members;
    public final List<Member> candidates;
    public final List<Member> invites;

    public ClanStateS2C(String clanName, UUID leader, int reputation, List<Member> members, List<Member> candidates,
                        List<Member> invites) {
        this.clanName = clanName;
        this.leader = leader;
        this.reputation = reputation;
        this.members = members;
        this.candidates = candidates;
        this.invites = invites;
    }

    private static void writeList(FriendlyByteBuf buf, List<Member> list) {
        buf.writeVarInt(list.size());
        for (Member m : list) {
            buf.writeUUID(m.id());
            buf.writeUtf(m.name(), 64);
            buf.writeBoolean(m.flag());
        }
    }

    private static List<Member> readList(FriendlyByteBuf buf) {
        int size = Math.min(buf.readVarInt(), 256);
        List<Member> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Member(buf.readUUID(), buf.readUtf(64), buf.readBoolean()));
        }
        return list;
    }

    public static void encode(ClanStateS2C msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.clanName, 32);
        buf.writeUUID(msg.leader == null ? NONE : msg.leader);
        buf.writeVarInt(msg.reputation);
        writeList(buf, msg.members);
        writeList(buf, msg.candidates);
        writeList(buf, msg.invites);
    }

    public static ClanStateS2C decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(32);
        UUID leader = buf.readUUID();
        int reputation = buf.readVarInt();
        List<Member> members = readList(buf);
        List<Member> candidates = readList(buf);
        List<Member> invites = readList(buf);
        return new ClanStateS2C(name, NONE.equals(leader) ? null : leader, reputation, members, candidates, invites);
    }

    public static void handle(ClanStateS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.clanState(msg)));
        context.setPacketHandled(true);
    }
}
