package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

/**
 * Created by ween on 11/10/14.
 */
public class RectSelect extends Selection {

    private static final int TOOL_ID = 4;
    private PointF start = new PointF();
    private Path inversePath = new Path();

    public RectSelect(String name, Drawable icon) {
        super(name, icon, TOOL_ID);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        roundCoordinates(event);
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);

        start.x = event.x;
        start.y = event.y;
        toolReport.getPath().setFillType(Path.FillType.WINDING);
        inversePath.setFillType(Path.FillType.INVERSE_WINDING);
        setPath(toolReport.getPath(), inversePath);

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
        toolReport.getInversePath().set(inversePath);
    }

    // Creates a rectangular path
    private void rectPathRegion(PointF event) {
        pathReset();
        pathMoveTo(start.x, start.y);
        pathLineTo(event.x, start.y);
        pathLineTo(event.x, event.y);
        pathLineTo(start.x, event.y);
        pathClose();
    }
}
