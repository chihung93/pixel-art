package com.jaween.pixelart.tools.attributes;

/**
 * Created by ween on 11/2/14.
 */
public class EraserToolAttributes extends ToolAttributes {

    private boolean antiAlias = false;
    private int thicknessLevel = MIN_THICKNESS;

    public EraserToolAttributes() {
        super();
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
