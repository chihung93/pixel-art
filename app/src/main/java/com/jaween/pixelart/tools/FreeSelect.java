package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

/**
 * Created by ween on 11/14/14.
 */
public class FreeSelect extends Selection {

    private static final int TOOL_ID = 9;

    // Once we call path.close(), for subsequent lineTo() calls our path would look odd
    // We avoid this by saving the tentative path, copying it to toolRegion.path, then calling close
    private Path tentativePath = new Path();
    private Path tentativePathInt = new Path();
    private Path inverse;

    public FreeSelect(String name, Drawable icon) {
        super(name, icon, TOOL_ID);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        // The selected path without the closing line
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);
        tentativePath.reset();
        tentativePath.moveTo(event.x, event.y);

        inverse = new Path();
        inverse.setFillType(Path.FillType.INVERSE_WINDING);

        // The selected path aligned to the pixels of the image
        roundCoordinates(event);
        setPath(tentativePathInt, inverse);
        pathReset();
        pathMoveTo(event.x, event.y);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);
        tentativePath.lineTo(event.x, event.y);

        roundCoordinates(event);
        pathLineTo(event.x, event.y);

        closePathRegion();
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        clampPoint(bitmap.getWidth(), bitmap.getHeight(), event);
        roundCoordinates(event);
        pathLineTo(event.x, event.y);
        tentativePath.set(tentativePathInt);

        closePathRegion();

        toolReport.getInversePath().set(inverse);
    }

    private void closePathRegion() {
        // Copies the tentative path and closes it
        toolReport.getPath().set(tentativePath);
        toolReport.getPath().close();
    }

}