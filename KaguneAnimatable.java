package com.tokyoghoulrpg.client.kagune;

import com.tokyoghoulrpg.combat.AbilitySync;
import com.tokyoghoulrpg.data.KaguneVariant;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Кагуне одного игрока на клиенте: что сейчас показывать и какую анимацию играть.
 * Состояние приходит с сервера, поэтому кагуне видно и у других игроков.
 */
public class KaguneAnimatable implements GeoAnimatable {
    /** Длительности из файла анимаций: появление 2,4 с и убирание 1,9 с. */
    private static final int APPEAR_TICKS = 48;
    private static final int RETRACT_TICKS = 38;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("kagune_idle");
    private static final RawAnimation RAGE = RawAnimation.begin().thenLoop("kagune_rage");
    private static final Map<String, RawAnimation> ONE_SHOTS = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean initialized = false;
    private boolean active = false;
    private boolean rage = false;
    private KaguneVariant variant = KaguneVariant.KANEKI;

    /** Текущая разовая анимация (появление, удар, барраж и так далее) и тик её окончания. */
    private String oneShot = null;
    private long oneShotEnd = 0L;
    private boolean restart = false;
    /** Пока идёт убирание, модель ещё видна. */
    private long visibleUntil = 0L;

    public KaguneVariant getVariant() {
        return variant;
    }

    /** Нужно ли рисовать кагуне прямо сейчас. */
    public boolean shouldRender() {
        return active || gameTime() < visibleUntil;
    }

    /** Новое состояние с сервера. Первое сообщение не проигрывает появление: кагуне просто уже включено. */
    public void updateState(boolean newActive, KaguneVariant newVariant, boolean newRage, long now) {
        variant = newVariant;
        rage = newRage;
        if (!initialized) {
            initialized = true;
            active = newActive;
            return;
        }
        if (newActive && !active) {
            startOneShot("kagune_appear", now);
        } else if (!newActive && active) {
            startOneShot("kagune_retract", now);
            visibleUntil = oneShotEnd;
        }
        active = newActive;
    }

    /** Разовая анимация способности. Пустое имя прерывает текущую. */
    public void playOneShot(String animation, long now) {
        if (animation == null || animation.isEmpty()) {
            oneShot = null;
            return;
        }
        startOneShot(animation, now);
    }

    private void startOneShot(String animation, long now) {
        oneShot = animation;
        oneShotEnd = now + durationTicks(animation);
        // Та же анимация подряд должна проиграться заново
        restart = true;
    }

    private static int durationTicks(String animation) {
        return switch (animation) {
            case "kagune_appear" -> APPEAR_TICKS;
            case "kagune_retract" -> RETRACT_TICKS;
            default -> Math.max(1, AbilitySync.get(animation).duration());
        };
    }

    private static RawAnimation oneShotAnimation(String name) {
        return ONE_SHOTS.computeIfAbsent(name, n -> RawAnimation.begin().thenPlay(n));
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, this::predicate));
    }

    private PlayState predicate(AnimationState<KaguneAnimatable> state) {
        long now = gameTime();
        if (restart) {
            state.getController().forceAnimationReset();
            restart = false;
        }
        if (oneShot != null && now < oneShotEnd) {
            return state.setAndContinue(oneShotAnimation(oneShot));
        }
        if (!active) {
            return PlayState.STOP;
        }
        return state.setAndContinue(rage ? RAGE : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return Util.getMillis() / 50.0;
    }
}
