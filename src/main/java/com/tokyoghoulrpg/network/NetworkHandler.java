package com.tokyoghoulrpg.network;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.packet.AbilityC2S;
import com.tokyoghoulrpg.network.packet.BlockC2S;
import com.tokyoghoulrpg.network.packet.KaguneAnimS2C;
import com.tokyoghoulrpg.network.packet.KaguneStateS2C;
import com.tokyoghoulrpg.network.packet.OpenClassSelectionS2C;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.network.packet.SyncDataS2C;
import com.tokyoghoulrpg.network.packet.ToggleKaguneC2S;
import com.tokyoghoulrpg.network.packet.UnlockSkillC2S;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TokyoGhoulRPG.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private NetworkHandler() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SelectClassC2S.class,
                SelectClassC2S::encode, SelectClassC2S::decode, SelectClassC2S::handle);
        CHANNEL.registerMessage(id++, ToggleKaguneC2S.class,
                ToggleKaguneC2S::encode, ToggleKaguneC2S::decode, ToggleKaguneC2S::handle);
        CHANNEL.registerMessage(id++, SyncDataS2C.class,
                SyncDataS2C::encode, SyncDataS2C::decode, SyncDataS2C::handle);
        CHANNEL.registerMessage(id++, OpenClassSelectionS2C.class,
                OpenClassSelectionS2C::encode, OpenClassSelectionS2C::decode, OpenClassSelectionS2C::handle);
        CHANNEL.registerMessage(id++, AbilityC2S.class,
                AbilityC2S::encode, AbilityC2S::decode, AbilityC2S::handle);
        CHANNEL.registerMessage(id++, BlockC2S.class,
                BlockC2S::encode, BlockC2S::decode, BlockC2S::handle);
        CHANNEL.registerMessage(id++, KaguneStateS2C.class,
                KaguneStateS2C::encode, KaguneStateS2C::decode, KaguneStateS2C::handle);
        CHANNEL.registerMessage(id++, KaguneAnimS2C.class,
                KaguneAnimS2C::encode, KaguneAnimS2C::decode, KaguneAnimS2C::handle);
        CHANNEL.registerMessage(id++, UnlockSkillC2S.class,
                UnlockSkillC2S::encode, UnlockSkillC2S::decode, UnlockSkillC2S::handle);
    }

    /** Отправляет игроку актуальные данные его персонажа (с текущим состоянием ярости, блока и оглушения). */
    public static void syncTo(ServerPlayer player) {
        player.getCapability(PlayerData.CAP).ifPresent(data ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncDataS2C(data.toSyncTag())));
        broadcastKagune(player);
    }

    /** Сообщает игроку и всем, кто его видит, включено ли кагуне и действует ли ярость. */
    public static void broadcastKagune(ServerPlayer player) {
        PlayerData data = PlayerData.get(player);
        if (data != null) {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), kagunePacket(player, data));
        }
    }

    /** Когда receiver начинает видеть target, отправляем ему состояние кагуне target. */
    public static void sendKaguneTo(ServerPlayer receiver, ServerPlayer target) {
        PlayerData data = PlayerData.get(target);
        if (data != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), kagunePacket(target, data));
        }
    }

    /** Просит всех, кто видит игрока, проиграть анимацию кагуне. Пустое имя прерывает текущую. */
    public static void playAnimation(ServerPlayer player, String animation) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new KaguneAnimS2C(player.getId(), animation));
    }

    private static KaguneStateS2C kagunePacket(ServerPlayer player, PlayerData data) {
        boolean visible = data.getPlayerClass().hasKagune() && data.isKaguneActive();
        return new KaguneStateS2C(player.getId(), visible, data.getKaguneVariant(), data.isRageActive());
    }

    /** Просит клиент открыть меню выбора класса. */
    public static void openClassSelection(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenClassSelectionS2C());
    }
}
