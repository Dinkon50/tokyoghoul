package com.tokyoghoulrpg.network;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.contract.Contract;
import com.tokyoghoulrpg.contract.ContractManager;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.packet.AbilityC2S;
import com.tokyoghoulrpg.network.packet.BarrageC2S;
import com.tokyoghoulrpg.network.packet.BiteFlashS2C;
import com.tokyoghoulrpg.network.packet.BlockC2S;
import com.tokyoghoulrpg.network.packet.ContractActionC2S;
import com.tokyoghoulrpg.network.packet.DoubleJumpC2S;
import com.tokyoghoulrpg.network.packet.KaguneAnimS2C;
import com.tokyoghoulrpg.network.packet.KaguneStateS2C;
import com.tokyoghoulrpg.network.packet.OpenBloodShopS2C;
import com.tokyoghoulrpg.network.packet.OpenClassSelectionS2C;
import com.tokyoghoulrpg.network.packet.OpenContractsS2C;
import com.tokyoghoulrpg.network.packet.OpenVariantSelectionS2C;
import com.tokyoghoulrpg.network.packet.SelectClassC2S;
import com.tokyoghoulrpg.network.packet.SelectVariantC2S;
import com.tokyoghoulrpg.network.packet.ShopActionC2S;
import com.tokyoghoulrpg.network.packet.SyncDataS2C;
import com.tokyoghoulrpg.network.packet.ToggleKaguneC2S;
import com.tokyoghoulrpg.network.packet.UnlockSkillC2S;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "10";

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
        CHANNEL.registerMessage(id++, BarrageC2S.class,
                BarrageC2S::encode, BarrageC2S::decode, BarrageC2S::handle);
        CHANNEL.registerMessage(id++, BiteFlashS2C.class,
                BiteFlashS2C::encode, BiteFlashS2C::decode, BiteFlashS2C::handle);
        CHANNEL.registerMessage(id++, OpenVariantSelectionS2C.class,
                OpenVariantSelectionS2C::encode, OpenVariantSelectionS2C::decode, OpenVariantSelectionS2C::handle);
        CHANNEL.registerMessage(id++, SelectVariantC2S.class,
                SelectVariantC2S::encode, SelectVariantC2S::decode, SelectVariantC2S::handle);
        CHANNEL.registerMessage(id++, OpenBloodShopS2C.class,
                OpenBloodShopS2C::encode, OpenBloodShopS2C::decode, OpenBloodShopS2C::handle);
        CHANNEL.registerMessage(id++, ShopActionC2S.class,
                ShopActionC2S::encode, ShopActionC2S::decode, ShopActionC2S::handle);
        CHANNEL.registerMessage(id++, OpenContractsS2C.class,
                OpenContractsS2C::encode, OpenContractsS2C::decode, OpenContractsS2C::handle);
        CHANNEL.registerMessage(id++, ContractActionC2S.class,
                ContractActionC2S::encode, ContractActionC2S::decode, ContractActionC2S::handle);
        CHANNEL.registerMessage(id++, DoubleJumpC2S.class,
                DoubleJumpC2S::encode, DoubleJumpC2S::decode, DoubleJumpC2S::handle);
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

    /** Красные края экрана у укусившего. */
    public static void biteFlash(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BiteFlashS2C());
    }

    /** Открыть игроку окно информатора с контрактом на сегодня. */
    public static void openContracts(ServerPlayer player, int informantId) {
        PlayerData data = PlayerData.get(player);
        Contract offer = data == null ? null : ContractManager.todayOffer(player, data);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenContractsS2C(informantId, offer == null ? "" : offer.id(), ContractManager.day(player)));
    }

    /** Открыть игроку лавку торговца. */
    public static void openBloodShop(ServerPlayer player, int traderId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenBloodShopS2C(traderId));
    }

    /** Просит клиент открыть выбор варианта кагуне. */
    public static void openVariantSelection(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenVariantSelectionS2C());
    }

    /** Просит клиент открыть меню выбора класса. */
    public static void openClassSelection(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenClassSelectionS2C());
    }
}
