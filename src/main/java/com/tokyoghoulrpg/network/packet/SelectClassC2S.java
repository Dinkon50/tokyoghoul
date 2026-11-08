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
    /** 0 — свой скин, 1 — скин класса из мода (гуль, полугуль, агент CCG), 2 — второй скин гуля. */
    private final int skin;

    public SelectClassC2S(PlayerClass playerClass, KaguneVariant variant, int skin) {
        this.playerClass = playerClass;
        this.variant = variant;
        this.skin = skin;
    }

    public static void encode(SelectClassC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.playerClass);
        buf.writeEnum(msg.variant);
        buf.writeByte(msg.skin);
    }

    public static SelectClassC2S decode(FriendlyByteBuf buf) {
        return new SelectClassC2S(buf.readEnum(PlayerClass.class), buf.readEnum(KaguneVariant.class), buf.readByte());
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
                // При создании персонажа можно стать только гулем или человеком (полугуль и агент CCG — инъекциями)
                if (msg.playerClass != PlayerClass.GHOUL && msg.playerClass != PlayerClass.HUMAN) {
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
                com.tokyoghoulrpg.rep.ModAdvancements.grant(player, "choose_class");
                data.setKaguneVariant(variant);
                data.setSkinChoice(PlayerData.hasModSkin(msg.playerClass) && msg.skin > 0
                        && msg.skin < PlayerData.skinCount(msg.playerClass) ? msg.skin : 0);
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
