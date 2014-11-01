package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

/**
 * Created by ween on 9/28/14.
 */
abstract public class Tool implements Command {

    protected Canvas canvas = new Canvas();
    protected final String name;
    protected final Drawable icon;
    protected boolean cancelled = false;

    public Tool(String name, Drawable icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }

    @Override
    public final void cancel() {
        cancelled = true;
    }

    protected static boolean isInBounds(Bitmap bitmap, PointF point) {
        if (point.x >= 0 && point.x < bitmap.getWidth()) {
            if (point.y >= 0 && point.y < bitmap.getHeight()) {
                return true;
            }
        }
        return false;
    }

    // TODO Fleshout and improve attributes
    public static class Attributes {
        public Paint paint;
        public int radius;

        public Attributes(Paint paint, int radius) {
            this.paint = paint;
            this.radius = radius;
        }
    }
}
