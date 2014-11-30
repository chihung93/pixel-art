package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 11/15/14.
 */
public class Dropper extends Tool {

    private static final int TOOL_ID = 6;

    public Dropper(String name, Drawable icon) {
        super(name, icon, TOOL_ID);

        toolAttributes = new ToolAttributes();
        toolAttributes.setMutator(false);
        toolAttributes.setDropper(true);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        pickColour(bitmap, event);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        pickColour(bitmap, event);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        pickColour(bitmap, event);
    }

    // Selects colour under the user's finger
    private void pickColour(Bitmap bitmap, PointF point) {
        int pickedColour;
        if (isInBounds(bitmap, point)) {
            // In bounds, picks the colour under the user's finger
            pickedColour = bitmap.getPixel((int) point.x, (int) point.y);
        } else {
            // Out of bounds, picks the current drawing colour
            pickedColour = toolAttributes.getPaint().getColor();
        }
        toolReport.setDropColour(pickedColour);
    }
}
