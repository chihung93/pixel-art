package com.jaween.pixelart.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.jaween.pixelart.Point;

/**
 * Created by ween on 9/28/14.
 */
abstract public class Tool {

    protected Context context;

    public Tool(Context context) {
        this.context = context;
    }

    // TODO Some tools use data from the image (e.g. flood fill), so this a muddled interface
    abstract public void beginAction(Canvas canvas, Bitmap bitmap, Point event, Attributes attributes);

    abstract public void endAction(Point event);

    public abstract String getName();

    // TODO Fleshout attributes
    public static class Attributes {
        public Paint paint;
        public int radius;

        public Attributes(Paint paint, int radius) {
            this.paint = paint;
            this.radius = radius;
        }
    }
}
