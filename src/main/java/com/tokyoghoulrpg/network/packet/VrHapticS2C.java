package com.tokyoghoulrpg.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Вибрация контроллеров у игрока в VR: сервер сообщает о попадании, уроне по нему, блоке, парировании.
 * Шлётся только тем, кто в VR (см. VrFeedback).
 */
public class VrHapticS2C {
    public static final int HIT = 0;
    public static final int HURT = 1;
    public static final int BLOCKED = 2;
    public static final int PARRY = 3;
    public static final int BLOCK_BROKEN = 4;

    private final int kind;
    private final boolean offhand;
    /** Сила от 0 до 1 (для урона — насколько больно). */
    private final float strength;

    public VrHapticS2C(int kind, boolean offhand, float strength) {
        this.kind = kind;
        this.offhand = offhand;
        this.strength = strength;
    }

    public static void encode(VrHapticS2C msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.kind);
        buf.writeBoolean(msg.offhand);
        buf.writeFloat(msg.strength);
    }

    public static VrHapticS2C decode(FriendlyByteBuf buf) {
        return new VrHapticS2C(buf.readByte(), buf.readBoolean(), buf.readFloat());
    }

    public static void handle(VrHapticS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.tokyoghoulrpg.client.vr.VrClient.feedback(msg.kind, msg.offhand, msg.strength)));
        context.setPacketHandled(true);
    }
}
