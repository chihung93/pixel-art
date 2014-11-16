package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

/**
 * Created by ween on 11/10/14.
 */
public class RectSelect extends Selection {

    private PointF start = new PointF();

    public RectSelect(String name, Drawable icon) {
        super(name, icon);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        roundCoordinates(event);
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);

        start.x = event.x;
        start.y = event.y;

        rectPathRegion(event);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        // Aligns the selected region the image pixels and creates a rectangle out of the path
        roundCoordinates(event);
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);
        rectPathRegion(event);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        // Aligns the selected region the image pixels and creates a rectangle out of the path
        roundCoordinates(event);
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);
        rectPathRegion(event);
    }

    // Creates a rectangular path
    private void rectPathRegion(PointF event) {
        toolReport.getPath().reset();
        toolReport.getPath().moveTo(start.x, start.y);
        toolReport.getPath().lineTo(event.x, start.y);
        toolReport.getPath().lineTo(event.x, event.y);
        toolReport.getPath().lineTo(start.x, event.y);
        toolReport.getPath().close();
    }
}
