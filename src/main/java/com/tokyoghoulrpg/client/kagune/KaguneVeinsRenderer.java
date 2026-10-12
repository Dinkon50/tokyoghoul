package com.tokyoghoulrpg.client.kagune;

import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class KaguneVeinsRenderer extends GeoObjectRenderer<KaguneVeinsAnimatable> {
    public KaguneVeinsRenderer() {
        super(new KaguneVeinsModel());
    }
}
