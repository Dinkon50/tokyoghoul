package com.tokyoghoulrpg.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Эффект особой атаки босса (см. combat/BossFx и client/BossFxRenderer): тип, точка, вторая точка (конец луча
 * или линии), размер (радиус или ширина), длительность в тиках, цвет 0xRRGGBB и доп. число (угол, количество, сила).
 */
public class BossFxS2C {
    public final int type;
    public final double x, y, z;
    public final double x2, y2, z2;
    public final float size;
    public final int duration;
    public final int color;
    public final float extra;

    public BossFxS2C(int type, double x, double y, double z, double x2, double y2, double z2, float size, int duration,
            int color, float extra) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.size = size;
        this.duration = duration;
        this.color = color;
        this.extra = extra;
    }

    public static void encode(BossFxS2C msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.type);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeDouble(msg.x2);
        buf.writeDouble(msg.y2);
        buf.writeDouble(msg.z2);
        buf.writeFloat(msg.size);
        buf.writeShort(msg.duration);
        buf.writeInt(msg.color);
        buf.writeFloat(msg.extra);
    }

    public static BossFxS2C decode(FriendlyByteBuf buf) {
        return new BossFxS2C(buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readShort(), buf.readInt(), buf.readFloat());
    }

    public static void handle(BossFxS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.tokyoghoulrpg.client.BossFxRenderer.add(msg)));
        context.setPacketHandled(true);
    }
}
