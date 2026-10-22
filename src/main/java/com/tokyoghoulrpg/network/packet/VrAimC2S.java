package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.Aim;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

/**
 * Игрок в VR: где его главная рука (смещение от глаз) и куда она направлена. Шлётся каждый тик, пока он в шлеме.
 * active = false — вышел из VR, целиться снова головой.
 */
public class VrAimC2S {
    private final boolean active;
    private final Vec3 offset;
    private final Vec3 dir;

    public VrAimC2S(boolean active, Vec3 offset, Vec3 dir) {
        this.active = active;
        this.offset = offset;
        this.dir = dir;
    }

    public static VrAimC2S off() {
        return new VrAimC2S(false, Vec3.ZERO, Vec3.ZERO);
    }

    public static void encode(VrAimC2S msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        if (msg.active) {
            buf.writeFloat((float) msg.offset.x);
            buf.writeFloat((float) msg.offset.y);
            buf.writeFloat((float) msg.offset.z);
            buf.writeFloat((float) msg.dir.x);
            buf.writeFloat((float) msg.dir.y);
            buf.writeFloat((float) msg.dir.z);
        }
    }

    public static VrAimC2S decode(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return off();
        }
        Vec3 offset = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        Vec3 dir = new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        return new VrAimC2S(true, offset, dir);
    }

    public static void handle(VrAimC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Aim.update(player, msg.active, msg.offset, msg.dir);
            }
        });
        context.setPacketHandled(true);
    }
}
