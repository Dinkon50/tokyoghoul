package com.tokyoghoulrpg.network.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Лоты торговой площадки, которые видит игрок. */
public class MarketStateS2C {
    /**
     * mine — свой лот; ownClan — продавец из твоего клана. secondsLeft: -1 — лот с ценой,
     * иначе аукцион (price — следующая минимальная ставка), leader — кто сейчас лидирует, leading — это ты.
     */
    public record Entry(int id, ItemStack stack, int price, String seller, String clan, boolean clanOnly, boolean mine,
                        boolean ownClan, int secondsLeft, String leader, boolean leading) {
    }

    public final boolean inClan;
    public final List<Entry> entries;

    public MarketStateS2C(boolean inClan, List<Entry> entries) {
        this.inClan = inClan;
        this.entries = entries;
    }

    public static void encode(MarketStateS2C msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.inClan);
        buf.writeVarInt(msg.entries.size());
        for (Entry e : msg.entries) {
            buf.writeVarInt(e.id());
            buf.writeItem(e.stack());
            buf.writeVarInt(e.price());
            buf.writeUtf(e.seller(), 64);
            buf.writeUtf(e.clan(), 32);
            buf.writeBoolean(e.clanOnly());
            buf.writeBoolean(e.mine());
            buf.writeBoolean(e.ownClan());
            buf.writeVarInt(e.secondsLeft() + 1);
            buf.writeUtf(e.leader(), 64);
            buf.writeBoolean(e.leading());
        }
    }

    public static MarketStateS2C decode(FriendlyByteBuf buf) {
        boolean inClan = buf.readBoolean();
        int size = Math.min(buf.readVarInt(), 2048);
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf.readVarInt(), buf.readItem(), buf.readVarInt(), buf.readUtf(64), buf.readUtf(32),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt() - 1, buf.readUtf(64),
                    buf.readBoolean()));
        }
        return new MarketStateS2C(inClan, entries);
    }

    public static void handle(MarketStateS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.tokyoghoulrpg.client.screen.MarketScreen.accept(msg)));
        context.setPacketHandled(true);
    }
}
