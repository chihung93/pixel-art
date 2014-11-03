package com.jaween.pixelart.tools.attributes;

import android.graphics.Paint;

/**
 * Created by ween on 11/2/14.
 */
public class OvalToolAttributes extends ToolAttributes {

    private boolean circleLocked = false;
    private boolean fill = false;
    private boolean antiAlias = false;
    private int thicknessLevel = 2;

    public OvalToolAttributes() {
        super();
    }

    public boolean isCircleLocked() {
        return circleLocked;
    }

    public void setCircleLocked(boolean circleLocked) {
        this.circleLocked = circleLocked;
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

    public int getThicknessLevel() {
        return thicknessLevel;
    }

    public void setThicknessLevel(int thicknessLevel) {
        paint.setStrokeWidth(thicknessLevel);
        this.thicknessLevel = thicknessLevel;
    }
}
