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
    /** Твои анимации: ходьба, копание кагуне и блок (поднять щупальца, держать, опустить). */
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("kagune_walk");
    private static final RawAnimation BREAK_BLOCKS = RawAnimation.begin().thenLoop("kagune_break_blocks");
    private static final RawAnimation BLOCK = RawAnimation.begin().thenPlay("kagune_block_start").thenLoop("kagune_block_hold");
    private static final int BLOCK_END_TICKS = 14;
    private static final Map<String, RawAnimation> ONE_SHOTS = new HashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final KaguneVeinsAnimatable veins = new KaguneVeinsAnimatable(this);
    private final Seen mainSeen = new Seen();

    /** Какой по счёту запуск разовой анимации уже обработал контроллер. */
    public static final class Seen {
        private int serial = 0;
    }

    private boolean initialized = false;
    private boolean active = false;
    private boolean rage = false;
    private boolean blocking = false;
    /** Обновляются каждый кадр при отрисовке: идёт ли игрок и копает ли. */
    private boolean moving = false;
    private boolean breaking = false;
    private KaguneVariant variant = KaguneVariant.KANEKI;

    /** Текущая разовая анимация (появление, удар, барраж и так далее) и тик её окончания. */
    private String oneShot = null;
    private long oneShotEnd = 0L;
    /** Растёт при каждом новом запуске разовой анимации: так вены знают, что её надо проиграть заново. */
    private int restartSerial = 0;
    /** Когда началась ярость (игровой тик). Нужно, чтобы вены вспыхнули в начале. */
    private long rageStart = 0L;
    /** Пока идёт убирание, модель ещё видна. */
    private long visibleUntil = 0L;
    /** До этого тика модель скрыта: её щупальца сейчас рисует ульта (KaguneUltimateFx). */
    private long hiddenUntil = 0L;
    /** До этого тика у модели нет одного щупальца (upper_left): оно сейчас тянется к цели захвата (KaguneReach). */
    private long reachHiddenUntil = 0L;

    /** Разовые анимации ударов: от первого лица их не показываем (выглядит плохо). */
    private static final java.util.Set<String> STRIKES = java.util.Set.of("kagune_attack_right_thrust",
            "kagune_attack_left_sweep", "kagune_attack_double", "kagune_barrage", "kagune_grapple", "kagune_grab_pull");

    public KaguneVariant getVariant() {
        return variant;
    }

    public KaguneVeinsAnimatable getVeins() {
        return veins;
    }

    public boolean isRage() {
        return rage;
    }

    /** Сколько тиков идёт ярость. */
    public long rageAge() {
        return Math.max(0L, gameTime() - rageStart);
    }

    /** Нужно ли рисовать кагуне прямо сейчас. */
    public boolean shouldRender() {
        if (gameTime() < hiddenUntil) {
            return false;
        }
        return active || gameTime() < visibleUntil;
    }

    /** Убрать с модели щупальце захвата до этого тика (вместо него тянется длинное щупальце). */
    public void hideReachTentacleUntil(long tick) {
        reachHiddenUntil = tick;
    }

    /** Нет ли сейчас на модели щупальца захвата. */
    public boolean isReachTentacleHidden() {
        return gameTime() < reachHiddenUntil;
    }

    /**
     * Видно ли своё кагуне от первого лица: только когда оно не бьёт — блок, копание, рывок, появление и убирание.
     * Удары, барраж и захваты от первого лица не показываем (захват видно по длинному щупальцу).
     */
    public boolean visibleInFirstPerson() {
        boolean oneShotNow = oneShot != null && gameTime() < oneShotEnd;
        if (oneShotNow) {
            return !STRIKES.contains(oneShot);
        }
        return active && (blocking || breaking);
    }

    /** Скрыть модель до этого тика (щупальца ульты рисуются отдельно). */
    public void hideUntil(long tick) {
        hiddenUntil = tick;
    }

    /**
     * Кагуне сейчас что-то делает: удар, барраж, захват, рывок, блок, копание, появление или убирание.
     * Просто стоит, идёт или горит яростью — false. От первого лица кагуне видно только в действии.
     */
    public boolean isInAction() {
        if (oneShot != null && gameTime() < oneShotEnd) {
            return true;
        }
        return active && (blocking || breaking);
    }

    /** Новое состояние с сервера. Первое сообщение не проигрывает появление: кагуне просто уже включено. */
    public void updateState(boolean newActive, KaguneVariant newVariant, boolean newRage, boolean newBlocking,
                            long now) {
        variant = newVariant;
        // Блок отпущен: щупальца опускаются (разовая анимация), если кагуне не убирают
        if (initialized && blocking && !newBlocking && newActive && active) {
            startOneShot("kagune_block_end", now);
        }
        // Блок поставлен: прерываем разовую анимацию, дальше играет подъём щупалец и удержание
        if (newBlocking && !blocking && oneShot != null && !"kagune_appear".equals(oneShot)) {
            oneShot = null;
        }
        blocking = newBlocking;
        if (newRage && !rage) {
            // Если ярость уже шла, когда мы увидели игрока, вспышку вен не показываем
            rageStart = initialized ? now : now - 1000L;
        }
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

    /** Каждый кадр из слоя отрисовки: идёт ли игрок, копает ли блоки. */
    public void setMotion(boolean moving, boolean breaking) {
        this.moving = moving;
        this.breaking = breaking;
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
        restartSerial++;
    }

    private static int durationTicks(String animation) {
        return switch (animation) {
            case "kagune_appear" -> APPEAR_TICKS;
            case "kagune_retract" -> RETRACT_TICKS;
            case "kagune_block_end" -> BLOCK_END_TICKS;
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
        controllers.add(new AnimationController<>(this, "main", 3, state -> drive(state, mainSeen)));
    }

    /**
     * Выбирает анимацию по состоянию кагуне. Её же вызывает контроллер вен, поэтому обе модели
     * играют одну и ту же анимацию в одно и то же время.
     */
    PlayState drive(AnimationState<?> state, Seen seen) {
        long now = gameTime();
        if (seen.serial != restartSerial) {
            seen.serial = restartSerial;
            state.getController().forceAnimationReset();
        }
        if (oneShot != null && now < oneShotEnd) {
            return state.setAndContinue(oneShotAnimation(oneShot));
        }
        if (!active) {
            return PlayState.STOP;
        }
        if (blocking) {
            return state.setAndContinue(BLOCK);
        }
        if (rage) {
            return state.setAndContinue(RAGE);
        }
        if (breaking) {
            return state.setAndContinue(BREAK_BLOCKS);
        }
        return state.setAndContinue(moving ? WALK : IDLE);
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
