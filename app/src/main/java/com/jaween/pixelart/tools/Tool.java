package com.jaween.pixelart.tools;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by ween on 9/28/14.
 */
abstract public class Tool implements Command {

    protected Canvas canvas = new Canvas();
    protected final String name;

    public Tool(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // TODO Fleshout and improve attributes
    public static class Attributes {
        public Paint paint;
        public int tempTouchedColour  = 0;
        public int radius;

        public Attributes(Paint paint, int radius) {
            this.paint = paint;
            this.radius = radius;
        }
    }
}
