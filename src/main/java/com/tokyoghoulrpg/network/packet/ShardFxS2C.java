package com.tokyoghoulrpg.network.packet;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Осколки крыльев видно как летящие шипы: сервер сообщает игрокам рядом, что шип вылетел (откуда, куда,
 * с какой скоростью и на сколько блоков), и когда он кончился (попал или упёрся). Клиент сам ведёт его по прямой.
 */
public class ShardFxS2C {
    private final boolean spawn;
    private final int id;
    private final double x, y, z;
    private final float vx, vy, vz, range;
    /** Размер шипа: 0 — крошечный (шквал), 1 — маленький (ЛКМ, рикошет), 2 — средний (ульта), 3 — большой (ПКМ). */
    private final int size;

    private ShardFxS2C(boolean spawn, int id, double x, double y, double z, float vx, float vy, float vz, float range,
            int size) {
        this.spawn = spawn;
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.range = range;
        this.size = size;
    }

    public static ShardFxS2C spawn(int id, double x, double y, double z, double vx, double vy, double vz, double range,
            int size) {
        return new ShardFxS2C(true, id, x, y, z, (float) vx, (float) vy, (float) vz, (float) range, size);
    }

    public static ShardFxS2C end(int id) {
        return new ShardFxS2C(false, id, 0.0, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, 0);
    }

    public static void encode(ShardFxS2C msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.spawn);
        buf.writeVarInt(msg.id);
        if (msg.spawn) {
            buf.writeDouble(msg.x);
            buf.writeDouble(msg.y);
            buf.writeDouble(msg.z);
            buf.writeFloat(msg.vx);
            buf.writeFloat(msg.vy);
            buf.writeFloat(msg.vz);
            buf.writeFloat(msg.range);
            buf.writeByte(msg.size);
        }
    }

    public static ShardFxS2C decode(FriendlyByteBuf buf) {
        boolean spawn = buf.readBoolean();
        int id = buf.readVarInt();
        if (!spawn) {
            return end(id);
        }
        return new ShardFxS2C(true, id, buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readByte());
    }

    public static void handle(ShardFxS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (msg.spawn) {
                com.tokyoghoulrpg.client.ShardSpikes.spawn(msg.id, msg.x, msg.y, msg.z, msg.vx, msg.vy, msg.vz,
                        msg.range, msg.size);
            } else {
                com.tokyoghoulrpg.client.ShardSpikes.end(msg.id);
            }
        }));
        context.setPacketHandled(true);
    }
}
