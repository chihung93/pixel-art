package com.jaween.pixelart.tools.attributes;

/**
 * Created by ween on 11/2/14.
 */
public class PenToolAttributes extends ToolAttributes {

    boolean straight = false;
    boolean lockAngles = false;
    boolean antiAlias = false;
    int thicknessLevel = 2;

    public PenToolAttributes() {
        super();
    }

    public boolean isStraight() {
        return straight;
    }

    public void setStraight(boolean straight) {
        this.straight = straight;
    }

    public boolean isLockAngles() {
        return lockAngles;
    }

    public void setLockAngles(boolean lockAngles) {
        this.lockAngles = lockAngles;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        paint.setAntiAlias(antiAlias);
        this.antiAlias = antiAlias;
    }

    public void setThicknessLevel(int thicknessLevel) {
        paint.setStrokeWidth(thicknessLevel);
        this.thicknessLevel = thicknessLevel;
    }

    public int getThicknessLevel() {
        return thicknessLevel;
    }


}
