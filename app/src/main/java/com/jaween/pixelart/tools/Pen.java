package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

/**
 * Created by ween on 9/28/14.
 */
// This is a pen
public class Pen extends Tool {

    private PointF previous = new PointF();
    private Path path = new Path();

    public Pen(String name, Drawable icon) {
        super(name, icon);
    }

    @Override
    public void start(Bitmap bitmap, PointF event, Attributes attributes) {
        cancelled = false;

        path.reset();
        path.moveTo(event.x, event.y);
    }

    @Override
    public void move(Bitmap bitmap, PointF event, Attributes attributes) {
        if (cancelled == false) {
            draw(bitmap, event, attributes);
        }
    }

    @Override
    public void end(Bitmap bitmap, PointF event, Attributes attributes) {
        if (cancelled == false) {
            draw(bitmap, event, attributes);
        }
    }

    private void draw(Bitmap bitmap, PointF event, Attributes attributes) {
        path.lineTo(event.x, event.y);

        canvas.setBitmap(bitmap);
        canvas.drawPath(path, attributes.paint);
    }
}