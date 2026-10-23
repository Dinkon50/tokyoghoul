package com.tokyoghoulrpg.network.packet;

import com.tokyoghoulrpg.combat.AbilityManager;
import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.data.KaguneVariant;
import com.tokyoghoulrpg.data.PlayerClass;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент сообщает серверу, какой класс и вариант кагуне выбрал игрок. */
public class SelectClassC2S {
    private final PlayerClass playerClass;
    private final KaguneVariant variant;
    /** Взять скин класса из мода (гуль, полугуль, агент CCG) вместо своего. */
    private final boolean modSkin;

    public SelectClassC2S(PlayerClass playerClass, KaguneVariant variant, boolean modSkin) {
        this.playerClass = playerClass;
        this.variant = variant;
        this.modSkin = modSkin;
    }

    public static void encode(SelectClassC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.playerClass);
        buf.writeEnum(msg.variant);
        buf.writeBoolean(msg.modSkin);
    }

    public static SelectClassC2S decode(FriendlyByteBuf buf) {
        return new SelectClassC2S(buf.readEnum(PlayerClass.class), buf.readEnum(KaguneVariant.class), buf.readBoolean());
    }

    /** Стартовые очки крови: один раз на персонажа, любому классу. */
    public static void giveStartBlood(PlayerData data) {
        if (!data.isStartBloodGiven()) {
            data.setStartBloodGiven(true);
            data.addBloodPoints(Balance.BLOOD_START);
        }
    }

    public static void handle(SelectClassC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            player.getCapability(PlayerData.CAP).ifPresent(data -> {
                // Класс выбирается один раз. Сбросить выбор можно командой /tgrpg reset.
                if (data.hasChosenClass() || msg.playerClass == PlayerClass.NONE) {
                    return;
                }
                KaguneVariant variant = KaguneVariant.NONE;
                if (msg.playerClass.hasKagune()) {
                    if (!msg.playerClass.variants().contains(msg.variant)) {
                        return;
                    }
                    variant = msg.variant;
                }
                data.setPlayerClass(msg.playerClass);
                data.setKaguneVariant(variant);
                data.setModSkin(PlayerData.hasModSkin(msg.playerClass) && msg.modSkin);
                data.setKaguneActive(false);
                data.setRage(0);
                giveStartBlood(data);
                // Гуль и полугуль сразу получают двойное здоровье, заполненное до конца
                AbilityManager.applyClassStats(player, data, true);
                NetworkHandler.syncTo(player);
                player.sendSystemMessage(Component.translatable(
                        "message.tokyoghoulrpg.class_chosen",
                        Component.translatable(msg.playerClass.translationKey())));
            });
        });
        context.setPacketHandled(true);
    }
}
