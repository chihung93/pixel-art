package com.jaween.pixelart.util;

import android.graphics.DashPathEffect;

import java.util.ArrayList;

/**
 * Created by ween on 12/14/14.
 */
public class MarchingAnts {

    private ArrayList<DashPathEffect> bees = new ArrayList<>();
    private int currentFrame = 0;

    public MarchingAnts(int dashOn, int dashOff, float dp) {
        for (int i = 0; i < dashOn + dashOff; i++) {
            int offset = (int) (i * dp);
            bees.add(new DashPathEffect(new float[]{ dashOn * dp, dashOff * dp }, offset));
        }
    }

    public DashPathEffect getNextPathEffect() {
        currentFrame = (currentFrame + 1) % bees.size();
        return bees.get(currentFrame);
    }

}
