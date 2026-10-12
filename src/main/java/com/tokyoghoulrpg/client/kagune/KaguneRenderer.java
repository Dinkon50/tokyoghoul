package com.tokyoghoulrpg.client.kagune;

import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class KaguneRenderer extends GeoObjectRenderer<KaguneAnimatable> {
    public KaguneRenderer() {
        super(new KaguneModel());
    }
}
