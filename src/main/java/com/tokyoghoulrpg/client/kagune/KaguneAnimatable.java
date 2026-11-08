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
    /** Длительности из файлов анимаций: появление и убирание у каждого кагуне свои (Канеки 2,4 и 1,9 с). */
    private static final int APPEAR_TICKS = 48;
    private static final int RETRACT_TICKS = 38;
    private static final int WINGS_APPEAR_TICKS = 30;
    private static final int WINGS_RETRACT_TICKS = 22;
    private static final int GOURMET_APPEAR_TICKS = 24;
    private static final int GOURMET_RETRACT_TICKS = 18;
    /**
     * Разовые анимации, которые сервер продлевает, пока действие идёт (стрельба крыльями, град): повторный
     * запуск той же анимации не начинает её заново, а только сдвигает конец. Сами они зациклены.
     */
    private static final java.util.Set<String> SUSTAINED = java.util.Set.of("wings_fire", "wings_hail");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("kagune_idle");
    private static final RawAnimation RAGE = RawAnimation.begin().thenLoop("kagune_rage");
    /** Твои анимации: ходьба, копание кагуне и блок (поднять щупальца, держать, опустить). */
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("kagune_walk");
    private static final RawAnimation BREAK_BLOCKS = RawAnimation.begin().thenLoop("kagune_break_blocks");
    private static final RawAnimation BLOCK = RawAnimation.begin().thenPlay("kagune_block_start").thenLoop("kagune_block_hold");
    private static final int BLOCK_END_TICKS = 14;
    private static final Map<String, RawAnimation> ONE_SHOTS = new HashMap<>();
    /** Плавный переход между анимациями: обычно 3 тика, у рывка 1 (он сам длится 3-5 тиков и иначе опаздывает). */
    private static final int TRANSITION = 3;
    private static final int FAST_TRANSITION = 1;
    /** Поле длины перехода в контроллере GeckoLib (через отражение: если его нет, переход просто остаётся 3 тика). */
    private static java.lang.reflect.Field transitionField;
    private static boolean transitionLookedUp = false;

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
    /** Вид кагуне, который станет текущим, когда закончится убирание (см. updateState). */
    private KaguneVariant pendingVariant = null;
    /** До этого тика модель скрыта: её щупальца сейчас рисует ульта (KaguneUltimateFx). */
    private long hiddenUntil = 0L;
    /** До этого тика у модели нет одного щупальца (upper_left): оно сейчас тянется к цели захвата (KaguneReach). */
    private long reachHiddenUntil = 0L;

    /** Разовые анимации ударов: от первого лица их не показываем (выглядит плохо). */
    private static final java.util.Set<String> STRIKES = java.util.Set.of("kagune_attack_right_thrust",
            "kagune_attack_left_sweep", "kagune_attack_double", "kagune_barrage", "kagune_grapple", "kagune_grab_pull",
            "wings_fire", "wings_big", "wings_hail", "wings_storm");

    public KaguneVariant getVariant() {
        return variant;
    }

    public KaguneVeinsAnimatable getVeins() {
        return veins;
    }

    /** Вены ярости есть только у модели Канеки (её же использует «Авторский»). */
    public boolean hasVeins() {
        return variant != KaguneVariant.WINGS && variant != KaguneVariant.GOURMET;
    }

    public boolean isRage() {
        return rage;
    }

    /** Держит ли игрок блок (у агента CCG — блок квинке). */
    public boolean isBlocking() {
        return blocking;
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
        if (active || gameTime() < visibleUntil) {
            return true;
        }
        if (pendingVariant != null) {
            variant = pendingVariant;
            pendingVariant = null;
        }
        return false;
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
        // Кагуне убирают и тут же меняется вид (инъекция: класс без кагуне, вид «нет»): пока идёт убирание,
        // играет анимация твоего кагуне, а не обычного. Новый вид — после того как модель скрылась.
        boolean retractingNow = initialized && active && !newActive;
        boolean stillRetracting = initialized && !active && !newActive && now < visibleUntil;
        if (newVariant != variant && (retractingNow || stillRetracting) && variant != KaguneVariant.NONE) {
            pendingVariant = newVariant;
        } else {
            variant = newVariant;
            pendingVariant = null;
        }
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
        if (SUSTAINED.contains(animation) && animation.equals(oneShot) && now < oneShotEnd) {
            oneShotEnd = now + durationTicks(animation, variant);
            return;
        }
        startOneShot(animation, now);
    }

    private void startOneShot(String animation, long now) {
        oneShot = animation;
        oneShotEnd = now + durationTicks(animation, variant);
        // Та же анимация подряд должна проиграться заново
        restartSerial++;
    }

    private static int durationTicks(String animation, KaguneVariant variant) {
        return switch (animation) {
            case "kagune_appear" -> variant == KaguneVariant.WINGS ? WINGS_APPEAR_TICKS
                    : variant == KaguneVariant.GOURMET ? GOURMET_APPEAR_TICKS : APPEAR_TICKS;
            case "kagune_retract" -> variant == KaguneVariant.WINGS ? WINGS_RETRACT_TICKS
                    : variant == KaguneVariant.GOURMET ? GOURMET_RETRACT_TICKS : RETRACT_TICKS;
            case "kagune_block_end" -> BLOCK_END_TICKS;
            default -> Math.max(1, AbilitySync.get(animation).duration());
        };
    }

    private static RawAnimation oneShotAnimation(String name) {
        return ONE_SHOTS.computeIfAbsent(name, n -> SUSTAINED.contains(n)
                ? RawAnimation.begin().thenLoop(n) : RawAnimation.begin().thenPlay(n));
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
        boolean oneShotNow = oneShot != null && now < oneShotEnd;
        setTransition(state.getController(), oneShotNow && "kagune_dash".equals(oneShot) ? FAST_TRANSITION : TRANSITION);
        if (oneShotNow) {
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

    private static void setTransition(AnimationController<?> controller, int ticks) {
        if (!transitionLookedUp) {
            transitionLookedUp = true;
            try {
                transitionField = AnimationController.class.getDeclaredField("transitionLength");
                transitionField.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                transitionField = null;
            }
        }
        if (transitionField == null) {
            return;
        }
        try {
            // в разных версиях GeckoLib поле целое или дробное
            if (transitionField.getType() == double.class) {
                if (transitionField.getDouble(controller) != ticks) {
                    transitionField.setDouble(controller, ticks);
                }
            } else if (transitionField.getInt(controller) != ticks) {
                transitionField.setInt(controller, ticks);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            transitionField = null;
        }
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
