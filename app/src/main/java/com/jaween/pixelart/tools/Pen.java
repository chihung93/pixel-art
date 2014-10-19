package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PointF;

/**
 * Created by ween on 9/28/14.
 */
// This is a pen
public class Pen extends Tool {

    private PointF previous = new PointF();
    private Path path = new Path();

    public Pen(String name) {
        super(name);
    }

    @Override
    public void start(Bitmap bitmap, PointF event, Attributes attributes) {
        path.moveTo(event.x, event.y);
    }

    @Override
    public void move(Bitmap bitmap, PointF event, Attributes attributes) {
        path.lineTo(event.x, event.y);

        canvas.setBitmap(bitmap);
        canvas.drawPath(path, attributes.paint);
    }

    @Override
    public void end(Bitmap bitmap, PointF event, Attributes attributes) {
        path.reset();
    }
}
