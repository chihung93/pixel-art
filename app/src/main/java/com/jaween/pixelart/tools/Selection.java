package com.jaween.pixelart.tools;

import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 11/14/14.
 */
abstract class Selection extends Tool {

    protected Selection(String name, Drawable icon, int toolId) {
        super(name, icon, toolId);

        toolAttributes = new ToolAttributes();
        toolAttributes.setMutator(false);
        toolAttributes.setSelector(true);
    }

    protected void roundCoordinates(PointF point) {
        point.x = Math.round(point.x);
        point.y = Math.round(point.y);
    }

    protected void clampPoint(int width, int height, PointF point) {
        if (point.x < 0) {
            point.x = 0;
        } else if (point.x >= width) {
            point.x = width;
        }

        if (point.y < 0) {
            point.y = 0;
        } else if (point.y >= height) {
            point.y = height;
        }
    }
}
