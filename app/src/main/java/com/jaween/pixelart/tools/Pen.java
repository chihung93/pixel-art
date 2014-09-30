package com.jaween.pixelart.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.jaween.pixelart.Point;
import com.jaween.pixelart.R;

/**
 * Created by ween on 9/28/14.
 */
// This is a pen
public class Pen extends Tool {

    private final String name;

    private Point previous = new Point(0, 0);
    private boolean hasPrevious = false;

    public Pen(Context context) {
        super(context);

        name = context.getString(R.string.tool_pen);
    }

    @Override
    public void beginAction(Canvas canvas, Bitmap bitmap, Point event, Attributes attributes) {
        if (hasPrevious == false) {
            previous.x = event.x;
            previous.y = event.y;
            hasPrevious = true;
        }

        canvas.drawLine(previous.x, previous.y, event.x, event.y, attributes.paint);
        previous.x = event.x;
        previous.y = event.y;
    }

    @Override
    public void endAction(Point event) {
        hasPrevious = false;
    }

    @Override
    public String getName() {
        return name;
    }
}
