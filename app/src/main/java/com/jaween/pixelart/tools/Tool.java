package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

/**
 * Created by ween on 9/28/14.
 */
abstract public class Tool implements Command {

    protected Canvas canvas = new Canvas();
    protected final String name;
    protected boolean cancelled = false;

    public Tool(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
