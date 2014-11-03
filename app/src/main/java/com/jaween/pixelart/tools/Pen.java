package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jaween.pixelart.tools.attributes.PenToolAttributes;

/**
 * Created by ween on 9/28/14.
 */
// This is a pen
public class Pen extends Tool {

    private PointF start = new PointF();
    private Path path = new Path();

    public Pen(String name, Drawable icon) {
        super(name, icon);
        toolAttributes = new PenToolAttributes();
    }

    @Override
    public void start(Bitmap bitmap, PointF event) {
        cancelled = false;

        start.x = event.x;
        start.y = event.y;

        path.reset();
        path.moveTo(event.x, event.y);
    }

    @Override
    public void move(Bitmap bitmap, PointF event) {
        if (cancelled == false) {
            // Straight lines
            if (((PenToolAttributes) toolAttributes).isStraight()) {
                path.reset();
                path.moveTo(start.x, start.y);
            }

            // Common pixel art angles
            if (((PenToolAttributes) toolAttributes).isLockAngles()) {
                lockAngles(event);
            }

            draw(bitmap, event);
        }
    }

    @Override
    public void end(Bitmap bitmap, PointF event) {
        if (cancelled == false) {
            // Straight lines
            if (((PenToolAttributes) toolAttributes).isStraight()) {
                path.reset();
                path.moveTo(start.x, start.y);
            }

            // Common pixel art angles
            if (((PenToolAttributes) toolAttributes).isLockAngles()) {
                lockAngles(event);
            }

            draw(bitmap, event);
        }
    }

    private void draw(Bitmap bitmap, PointF event) {
        path.lineTo(event.x, event.y);

        canvas.setBitmap(bitmap);
        canvas.drawPath(path, toolAttributes.getPaint());
    }

    // Locks the line to a common angle (modifies the input point)
    private void lockAngles(PointF end) {
        float length = (float) Math.sqrt((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y));
        float angleRadians = (float) Math.atan2(end.y - start.y, end.x - start.x);
        float angleDegrees = (float) (180 * angleRadians / Math.PI);

        // East
        if (angleDegrees < 45 && angleDegrees > -45) {
            end.x = start.x + length;
            end.y = start.y;
        }

        // South
        if (angleDegrees >= 45 && angleDegrees < 135) {
            end.x = start.x;
            end.y = start.y + length;
        }

        // West
        if (angleDegrees >= 135 || angleDegrees <= -135) {
            end.x = start.x - length;
            end.y = start.y;
        }

        // North
        if (angleDegrees <= -45 && angleDegrees > -135) {
            end.x = start.x;
            end.y = start.y - length;
        }
    }
}