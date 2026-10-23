package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.Aim;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Игрок в VR: где его руки (смещение от глаз) и куда они направлены. Шлётся каждый тик, пока он в шлеме.
 * active = false — вышел из VR, целиться снова головой.
 */
public class VrAimC2S {
    private final boolean active;
    private final Vec3 offset;
    private final Vec3 dir;
    private final Vec3 offOffset;
    private final Vec3 offDir;

    public VrAimC2S(boolean active, Vec3 offset, Vec3 dir, Vec3 offOffset, Vec3 offDir) {
        this.active = active;
        this.offset = offset;
        this.dir = dir;
        this.offOffset = offOffset;
        this.offDir = offDir;
    }

    public static VrAimC2S off() {
        return new VrAimC2S(false, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO);
    }

    private static void write(FriendlyByteBuf buf, Vec3 v) {
        buf.writeFloat((float) v.x);
        buf.writeFloat((float) v.y);
        buf.writeFloat((float) v.z);
    }

    private static Vec3 read(FriendlyByteBuf buf) {
        return new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void encode(VrAimC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        if (msg.active) {
            write(buf, msg.offset);
            write(buf, msg.dir);
            write(buf, msg.offOffset);
            write(buf, msg.offDir);
        }
    }

    public static VrAimC2S decode(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return off();
        }
        Vec3 offset = read(buf);
        Vec3 dir = read(buf);
        Vec3 offOffset = read(buf);
        Vec3 offDir = read(buf);
        return new VrAimC2S(true, offset, dir, offOffset, offDir);
    }

    public static void handle(VrAimC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Aim.update(player, msg.active, msg.offset, msg.dir, msg.offOffset, msg.offDir);
            }
        });
        context.setPacketHandled(true);
    }
}
