package com.tokyoghoulrpg.client.kagune;

import net.minecraft.Util;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Вены ярости: отдельная модель из `01_rage.bbmodel`, те же кости, что у кагуне.
 * Не хранит своего состояния: берёт его у кагуне и играет ту же анимацию, поэтому вены
 * всегда лежат точно на щупальцах.
 */
public class KaguneVeinsAnimatable implements GeoAnimatable {
    private final KaguneAnimatable main;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final KaguneAnimatable.Seen seen = new KaguneAnimatable.Seen();

    public KaguneVeinsAnimatable(KaguneAnimatable main) {
        this.main = main;
    }

    public KaguneAnimatable getMain() {
        return main;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> main.drive(state, seen)));
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
