package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.jaween.pixelart.tools.attributes.RectToolAttributes;

/**
 * Created by ween on 10/19/14.
 */
public class Rect extends Tool {

    private static final int TOOL_ID = 2;
    public Rect(String name, Drawable icon) {
        super(name, icon, TOOL_ID);
        toolAttributes = new RectToolAttributes();
    }

    private PointF start = new PointF();
    private RectF rectBounds = new RectF();

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        start.x = event.x;
        start.y = event.y;

        draw(canvas, bitmap, event);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        // Locks the oval to a circle
        if (((RectToolAttributes) toolAttributes).isSquareLocked()) {
            lockSquare(event);
        }

        draw(canvas, bitmap, event);
    }


    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        // Locks the oval to a circle
        if (((RectToolAttributes) toolAttributes).isSquareLocked()) {
            lockSquare(event);
        }

        draw(canvas, bitmap, event);
    }

    // TODO: Allow the coordinates of rect to go less than 0 in both x and y
    private void draw(Canvas canvas, Bitmap bitmap, PointF end) {
        rectBounds.set(start.x, start.y, end.x, end.y);

        // Flips the bounds of the rect if they are inverted
        roundRectEdgeWorkAround(rectBounds);

        float radius;
        if (((RectToolAttributes) toolAttributes).isRoundedRect()) {
            radius = ((RectToolAttributes) toolAttributes).getRoundnessLevel();
        } else {
            // Regular rectangles can't be drawn outside the top and left boundaries, but rounded
            // rectangles can. We can draw a regular rect by using a rounded rect with corners of radius 0
            radius = 0;
        }

        canvas.setBitmap(bitmap);
        canvas.drawRoundRect(rectBounds, radius, radius, toolAttributes.getPaint());
    }

    // Locks the rect to a square (modifies the input point)
    private void lockSquare(PointF end) {
        float dX = end.x - start.x;
        float dY = end.y - start.y;

        float rectWidth = Math.abs(dX);
        float rectHeight = Math.abs(dY);

        float diameter = Math.max(rectWidth, rectHeight);

        // The diameter of the square is the larger of the rect's width or height
        if (dX > 0 && dY > 0) {
            // Lower right quadrant
            end.y = start.y + diameter;
            end.x = start.x + diameter;
        } else if (dX > 0 && dY < 0) {
            // Upper right quadrant
            end.y = start.y - diameter;
            end.x = start.x + diameter;
        } else if (dX < 0 && dY < 0) {
            // Upper left quadrant
            end.y = start.y - diameter;
            end.x = start.x - diameter;
        } else if (dX < 0 && dY > 0) {
            // Lower left quadrant
            end.y = start.y + diameter;
            end.x = start.x - diameter;
        }
    }

    // Canvas can't draw rounded rectangles with edges inverted, flips the edges to make it work
    private void roundRectEdgeWorkAround(RectF bounds) {
        if (bounds.right < bounds.left) {
            bounds.set(bounds.right, bounds.top, bounds.left, bounds.bottom);
        }
        if (bounds.bottom < bounds.top) {
            bounds.set(bounds.left, bounds.bottom, bounds.right, bounds.top);
        }
    }
}
