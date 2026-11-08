package com.tokyoghoulrpg.client;

import com.tokyoghoulrpg.TokyoGhoulRPG;
import com.tokyoghoulrpg.entity.AncientGhoul;
import com.tokyoghoulrpg.entity.BossMob;
import com.tokyoghoulrpg.entity.CrimsonMonarch;
import com.tokyoghoulrpg.entity.DeepDweller;
import com.tokyoghoulrpg.entity.StyleBossMob;
import com.tokyoghoulrpg.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Музыка боя: когда рядом босс мода, который дерётся (или стоит совсем близко), играет его тема,
 * в Глубинах 24-го района — гул данжа. Обычная музыка Minecraft на это время замолкает.
 * Громкость — ползунок «Музыка» в настройках звука (на нуле музыки нет).
 * Темы: Древний гуль и Алый Монарх — «ancient_ghoul», боссы стилей — «style_duel», остальные боссы — «boss_battle».
 */
@Mod.EventBusSubscriber(modid = TokyoGhoulRPG.MOD_ID, value = Dist.CLIENT)
public final class BossMusic {
    private static final double BOSS_RANGE = 40.0;
    private static final double CLOSE_RANGE = 14.0;
    private static final double DEEP_RANGE = 28.0;
    /** Сколько тиков подряд не было повода для музыки, прежде чем она затихнет (чтобы не дёргалась). */
    private static final int GRACE = 100;

    private static Theme playing = null;
    private static int idleTicks = 0;

    private BossMusic() {
    }

    private static final class Theme extends AbstractTickableSoundInstance {
        private final SoundEvent event;
        private boolean fading = false;

        Theme(SoundEvent event) {
            super(event, SoundSource.MUSIC, RandomSource.create());
            this.event = event;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void fadeOut() {
            fading = true;
        }

        boolean isOf(SoundEvent other) {
            return event == other && !fading;
        }

        @Override
        public void tick() {
            if (fading) {
                volume -= 0.025F;
                if (volume <= 0.0F) {
                    stop();
                }
            } else if (volume < 1.0F) {
                volume = Math.min(1.0F, volume + 0.02F);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (playing != null) {
                mc.getSoundManager().stop(playing);
                playing = null;
            }
            return;
        }
        if (player.tickCount % 10 != 0) {
            return;
        }
        SoundEvent want = choose(player);
        if (want == null) {
            idleTicks += 10;
            if (playing != null && idleTicks >= GRACE) {
                playing.fadeOut();
                playing = null;
            }
            return;
        }
        idleTicks = 0;
        if (playing == null || !playing.isOf(want) || !mc.getSoundManager().isActive(playing)) {
            if (playing != null) {
                playing.fadeOut();
            }
            playing = new Theme(want);
            mc.getSoundManager().play(playing);
        }
        mc.getMusicManager().stopPlaying();
    }

    /** Какая тема нужна сейчас (null — никакая). */
    private static SoundEvent choose(LocalPlayer player) {
        AABB area = player.getBoundingBox().inflate(BOSS_RANGE);
        Mob best = null;
        int bestRank = 0;
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, m -> m instanceof BossMob && m.isAlive())) {
            double dist = mob.distanceTo(player);
            if (!mob.isAggressive() && dist > CLOSE_RANGE) {
                continue;
            }
            int rank = mob instanceof AncientGhoul || mob instanceof CrimsonMonarch ? 3 : mob instanceof StyleBossMob ? 2 : 1;
            if (rank > bestRank) {
                best = mob;
                bestRank = rank;
            }
        }
        if (best != null) {
            return switch (bestRank) {
                case 3 -> ModSounds.MUSIC_ANCIENT_GHOUL.get();
                case 2 -> ModSounds.MUSIC_STYLE_DUEL.get();
                default -> ModSounds.MUSIC_BOSS_BATTLE.get();
            };
        }
        boolean deep = !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(DEEP_RANGE),
                e -> e instanceof DeepDweller && e.isAlive()).isEmpty();
        return deep ? ModSounds.MUSIC_DEEP_WARD.get() : null;
    }
}
