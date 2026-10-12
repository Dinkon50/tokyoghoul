package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Сервер присылает клиенту данные его персонажа. */
public class SyncDataS2C {
    private final CompoundTag tag;

    public SyncDataS2C(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(SyncDataS2C msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.tag);
    }

    public static SyncDataS2C decode(FriendlyByteBuf buf) {
        return new SyncDataS2C(buf.readNbt());
    }

    public static void handle(SyncDataS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.applySync(msg.tag)));
        context.setPacketHandled(true);
    }
}
