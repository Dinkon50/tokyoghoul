package com.tokyoghoulrpg.clan;

import com.tokyoghoulrpg.combat.Balance;
import com.tokyoghoulrpg.combat.Effects;
import com.tokyoghoulrpg.data.PlayerData;
import com.tokyoghoulrpg.network.NetworkHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.joml.Vector3f;

/**
 * Передача силы в клане (плюсик у участника в меню клана).
 * <ul>
 *   <li>Первый даритель сразу отдаёт половину своего здоровья и сам бьёт на 50% слабее (и удары, и способности).
 *       Получатель сразу получает это здоровье (сверх максимума) и бьёт на 25% сильнее. Всё на 30 секунд.</li>
 *   <li>Пока 30 секунд не прошли, силу этому же игроку может передать ещё один участник: он отдаёт четверть
 *       здоровья и слабеет на 25%, получатель получает это здоровье и ещё +12,5% силы, таймер снова 30 секунд.
 *       Больше двух дарителей нельзя.</li>
 *   <li>Когда время вышло, у получателя и у обоих дарителей 2 минуты отката: никому не передают силу
 *       и ни от кого её не получают.</li>
 *   <li>Пока связь идёт, дарители и получатель тоже никому силу не передают.</li>
 * </ul>
 * Состояние живёт только на сервере, пока он работает (после перезапуска связей нет).
 */
public final class ClanBond {
    private static final UUID DONOR_HP = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e70");
    private static final UUID RECEIVER_HP = UUID.fromString("5b1f2c0e-7a41-4c0a-9d51-0a1b2c3d4e71");
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.2F), 1.2F);

    /** Одна связь: кто получил силу, от кого, сколько здоровья и силы, до какого тика. */
    private static final class Link {
        final UUID receiver;
        final List<UUID> donors = new ArrayList<>();
        float health = 0.0F;
        double bonus = 0.0;
        long end = 0L;

        Link(UUID receiver) {
            this.receiver = receiver;
        }
    }

    /** Получатель → связь. */
    private static final Map<UUID, Link> LINKS = new HashMap<>();
    /** Даритель → насколько он ослаблен (0,5 или 0,25). */
    private static final Map<UUID, Double> WEAKENED = new HashMap<>();
    /** Даритель → кому отдал. */
    private static final Map<UUID, UUID> DONATING = new HashMap<>();
    /** Игрок → до какого тика откат (не отдаёт и не получает). */
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private ClanBond() {
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static boolean busy(UUID player) {
        return LINKS.containsKey(player) || DONATING.containsKey(player);
    }

    private static long cooldownLeft(UUID player, long now) {
        return Math.max(0L, COOLDOWN.getOrDefault(player, 0L) - now);
    }

    /** Передать силу. Проверка клана — в ClanManager. */
    public static void give(ServerPlayer donor, ServerPlayer receiver) {
        MinecraftServer server = donor.getServer();
        if (server == null || donor == receiver) {
            return;
        }
        long now = now(server);
        UUID from = donor.getUUID();
        UUID to = receiver.getUUID();
        if (!donor.isAlive() || !receiver.isAlive()) {
            tell(donor, "dead");
            return;
        }
        long cd = cooldownLeft(from, now);
        if (cd > 0) {
            tell(donor, "cooldown", seconds(cd));
            return;
        }
        if (busy(from)) {
            tell(donor, "busy");
            return;
        }
        long targetCd = cooldownLeft(to, now);
        if (targetCd > 0) {
            tell(donor, "target_cooldown", receiver.getGameProfile().getName(), seconds(targetCd));
            return;
        }
        if (DONATING.containsKey(to)) {
            tell(donor, "target_busy", receiver.getGameProfile().getName());
            return;
        }
        Link link = LINKS.get(to);
        if (link != null && link.donors.size() >= Balance.BOND_MAX_DONORS) {
            tell(donor, "full", receiver.getGameProfile().getName());
            return;
        }
        boolean second = link != null;
        float amount = (float) (donor.getHealth() * (second ? Balance.BOND_HP_SECOND : Balance.BOND_HP_FIRST));
        if (amount < 1.0F) {
            tell(donor, "weak");
            return;
        }
        double weaken = second ? Balance.BOND_WEAKEN_SECOND : Balance.BOND_WEAKEN_FIRST;
        // «Узы силы» (умение клана): бонус получателя больше
        int bondSkill = ClanPerks.skill(donor, ClanPerks.Skill.BOND);
        double bonus = (second ? Balance.BOND_BONUS_SECOND : Balance.BOND_BONUS_FIRST)
                * (1.0 + ClanPerks.Skill.BOND.value(bondSkill));

        // даритель: здоровье уходит сразу, и максимум ниже, чтобы оно не восстановилось за эти 30 секунд
        donor.setHealth(Math.max(1.0F, donor.getHealth() - amount));
        setMaxHealthBonus(donor, DONOR_HP, -amount);
        WEAKENED.put(from, weaken);
        DONATING.put(from, to);

        // получатель: здоровье сразу, сверх максимума; сила; таймер снова 30 секунд
        if (link == null) {
            link = new Link(to);
            LINKS.put(to, link);
        }
        link.donors.add(from);
        // получателю на 25% больше, чем отдал даритель, и ещё +10% за уровень «Уз силы»
        float gained = (float) (amount * (Balance.BOND_HP_GAIN + ClanPerks.BOND_HP_PER_LEVEL * bondSkill));
        link.health += gained;
        link.bonus += bonus;
        link.end = now + Balance.BOND_TICKS;
        setMaxHealthBonus(receiver, RECEIVER_HP, link.health);
        receiver.setHealth(Math.min(receiver.getMaxHealth(), receiver.getHealth() + gained));

        updateDisplay(server, link);
        Effects.particles(donor, ParticleTypes.SMOKE, 20, 0.5, 0.02);
        Effects.particles(receiver, GOLD, 40, 0.6, 0.05);
        Effects.particles(receiver, ParticleTypes.HEART, 6, 0.6, 0.05);
        Effects.sound(receiver, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
        Effects.sound(donor, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.2F);
        String hp = String.format(java.util.Locale.ROOT, "%.1f", amount / 2.0F);
        tell(donor, "given", receiver.getGameProfile().getName(), hp, Math.round(weaken * 100));
        tell(receiver, second ? "received_more" : "received", donor.getGameProfile().getName(),
                String.format(java.util.Locale.ROOT, "%.1f", gained / 2.0F),
                Math.round(link.bonus * 100));
    }

    /** Множитель урона игрока: получатель сильнее, дарители слабее. */
    public static double damageFactor(ServerPlayer player) {
        UUID id = player.getUUID();
        Link link = LINKS.get(id);
        double factor = 1.0;
        if (link != null) {
            factor += link.bonus;
        }
        Double weaken = WEAKENED.get(id);
        if (weaken != null) {
            factor -= weaken;
        }
        return Math.max(0.0, factor);
    }

    /** Раз в полсекунды: закончившиеся связи снимаются, у всех участников начинается откат. */
    public static void tick(MinecraftServer server) {
        long now = now(server);
        if (now % 10 != 0) {
            return;
        }
        if (!LINKS.isEmpty()) {
            Iterator<Link> it = LINKS.values().iterator();
            while (it.hasNext()) {
                Link link = it.next();
                if (now >= link.end) {
                    it.remove();
                    finish(server, link, now);
                }
            }
        }
        COOLDOWN.values().removeIf(until -> until <= now);
    }

    /** Связь закончилась: снять здоровье и силу, всем участникам 2 минуты отката. */
    private static void finish(MinecraftServer server, Link link, long now) {
        // откат короче с «Узами силы» (клан у всех участников связи один)
        ServerPlayer receiverPlayer = server.getPlayerList().getPlayer(link.receiver);
        int bondSkill = receiverPlayer == null ? 0 : ClanPerks.skill(receiverPlayer, ClanPerks.Skill.BOND);
        int cooldown = ClanPerks.BOND_COOLDOWN[Math.max(0, Math.min(ClanPerks.SKILL_MAX, bondSkill))];
        long until = now + cooldown;
        List<UUID> all = new ArrayList<>(link.donors);
        all.add(link.receiver);
        for (UUID id : all) {
            WEAKENED.remove(id);
            DONATING.remove(id);
            COOLDOWN.put(id, until);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                continue;
            }
            setMaxHealthBonus(player, DONOR_HP, 0.0F);
            setMaxHealthBonus(player, RECEIVER_HP, 0.0F);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            PlayerData data = PlayerData.get(player);
            if (data != null) {
                data.setBond(0L, 0.0, until);
                NetworkHandler.syncTo(player);
            }
            tell(player, "ended", cooldown / 20);
        }
    }

    /** Получатель погиб или вышел — связь заканчивается сразу. */
    public static void onLeave(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        Link link = LINKS.remove(player.getUUID());
        if (server != null && link != null) {
            finish(server, link, now(server));
        }
    }

    /** После возрождения: показать на экране, что связь или откат ещё идут. */
    public static void refreshDisplay(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        PlayerData data = PlayerData.get(player);
        if (server == null || data == null) {
            return;
        }
        UUID id = player.getUUID();
        UUID receiver = LINKS.containsKey(id) ? id : DONATING.get(id);
        Link link = receiver == null ? null : LINKS.get(receiver);
        if (link != null) {
            double power = id.equals(link.receiver) ? link.bonus : -WEAKENED.getOrDefault(id, 0.0);
            data.setBond(link.end, power, 0L);
        } else {
            data.setBond(0L, 0.0, COOLDOWN.getOrDefault(id, 0L));
        }
        NetworkHandler.syncTo(player);
    }

    public static void clear() {
        LINKS.clear();
        WEAKENED.clear();
        DONATING.clear();
        COOLDOWN.clear();
    }

    // ------------------------------------------------------------------

    /** Таймер на экране у получателя и у всех его дарителей. */
    private static void updateDisplay(MinecraftServer server, Link link) {
        List<UUID> all = new ArrayList<>(link.donors);
        all.add(link.receiver);
        for (UUID id : all) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            PlayerData data = player == null ? null : PlayerData.get(player);
            if (data == null) {
                continue;
            }
            double power = id.equals(link.receiver) ? link.bonus : -WEAKENED.getOrDefault(id, 0.0);
            data.setBond(link.end, power, 0L);
            NetworkHandler.syncTo(player);
        }
    }

    /** Временная прибавка (или убавка) к максимуму здоровья. 0 — убрать. Не сохраняется в мире. */
    private static void setMaxHealthBonus(ServerPlayer player, UUID id, float amount) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(id);
        if (Math.abs(amount) > 1.0E-3F) {
            health.addTransientModifier(new AttributeModifier(id, "tg_clan_bond", amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static int seconds(long ticks) {
        return (int) Math.ceil(ticks / 20.0);
    }

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.tokyoghoulrpg.bond." + key, args));
    }
}
