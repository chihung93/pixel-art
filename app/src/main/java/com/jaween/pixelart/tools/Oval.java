package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Created by ween on 10/19/14.
 */
public class Oval extends Tool {

    public Oval(String name, Drawable icon) {
        super(name, icon);
    }

    private PointF start = new PointF();
    private RectF ovalBounds = new RectF();

    @Override
    public void start(Bitmap bitmap, PointF event, Attributes attributes) {
        cancelled = false;

        start.x = event.x;
        start.y = event.y;

        draw(canvas, bitmap, event, attributes.paint);
    }

    @Override
    public void move(Bitmap bitmap, PointF event, Attributes attributes) {
        if (cancelled == false) {
            draw(canvas, bitmap, event, attributes.paint);
        }
    }

    @Override
    public void end(Bitmap bitmap, PointF event, Attributes attributes) {
        if (cancelled == false) {
            draw(canvas, bitmap, event, attributes.paint);
        }
    }

    // TODO: Allow the coordinates of oval to go less than 0 in both x and y
    private void draw(Canvas canvas, Bitmap bitmap, PointF event, Paint paint) {
        ovalBounds.set(start.x, start.y, event.x, event.y);

        canvas.setBitmap(bitmap);
        canvas.drawOval(ovalBounds, paint);
    }
}
