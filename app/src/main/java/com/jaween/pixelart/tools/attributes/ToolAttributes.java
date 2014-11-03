package com.jaween.pixelart.tools.attributes;

import android.graphics.Paint;

/**
 * Created by ween on 11/2/14.
 */
public abstract class ToolAttributes {
    protected Paint paint;

    public ToolAttributes() {
        paint = new Paint();
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.STROKE);
    }

    public Paint getPaint() {
        return paint;
    }
}
