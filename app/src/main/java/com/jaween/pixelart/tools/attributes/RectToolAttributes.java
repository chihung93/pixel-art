package com.jaween.pixelart.tools.attributes;

import android.graphics.Paint;

/**
 * Created by ween on 11/2/14.
 */
public class RectToolAttributes extends ToolAttributes {

    private static final int MIN_ROUNDNESS = 1;

    private boolean squareLocked = false;
    private boolean fill = false;
    private boolean antiAlias = false;
    private boolean roundedRect = false;
    private int thicknessLevel = MIN_THICKNESS;
    private int roundnessLevel = MIN_ROUNDNESS;

    public RectToolAttributes() {
        super();
    }

    public boolean isSquareLocked() {
        return squareLocked;
    }

    public void setSquareLocked(boolean squareLocked) {
        this.squareLocked = squareLocked;
    }

    public boolean isFill() {
        return fill;
    }

    public void setFill(boolean fill) {
        if (fill) {
            paint.setStyle(Paint.Style.FILL);
        } else {
            paint.setStyle(Paint.Style.STROKE);
        }
        this.fill = fill;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        paint.setAntiAlias(antiAlias);
        this.antiAlias = antiAlias;
    }

    public boolean isRoundedRect() {
        return roundedRect;
    }

    public void setRoundedRect(boolean roundedRect) {
        this.roundedRect = roundedRect;
    }

    public int getThicknessLevel() {
        return thicknessLevel;
    }

    public void setThicknessLevel(int thicknessLevel) {
        paint.setStrokeWidth(thicknessLevel);
        this.thicknessLevel = thicknessLevel;
    }

    public int getRoundnessLevel() {
        return roundnessLevel;
    }

    public void setRoundnessLevel(int roundnessLevel) {
        this.roundnessLevel = roundnessLevel;
    }
}
