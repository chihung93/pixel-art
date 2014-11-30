package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jaween.pixelart.tools.attributes.PenToolAttributes;
import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 9/28/14.
 */
// This is a pen
public class Pen extends Tool {

    private static final int TOOL_ID = 0;
    private PointF start = new PointF();

    public Pen(String name, Drawable icon) {
        super(name, icon, TOOL_ID);
        toolAttributes = new PenToolAttributes();
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        start.x = event.x;
        start.y = event.y;

        draw(bitmap, event);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        // Straight lines
        if (((PenToolAttributes) toolAttributes).isStraight()) {
            toolReport.getPath().reset();
            toolReport.getPath().moveTo(start.x, start.y);
        }

        // Common pixel art angles
        if (((PenToolAttributes) toolAttributes).isLockAngles()) {
            lockAngles(event);
        }

        draw(bitmap, event);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        // Straight lines
        if (((PenToolAttributes) toolAttributes).isStraight()) {
            toolReport.getPath().reset();
            toolReport.getPath().moveTo(start.x, start.y);
        }

        // Common pixel art angles
        if (((PenToolAttributes) toolAttributes).isLockAngles()) {
            lockAngles(event);
        }

        draw(bitmap, event);
    }

    private void draw(Bitmap bitmap, PointF event) {
        toolReport.getPath().lineTo(event.x, event.y);
        canvas.setBitmap(bitmap);

        // Work around for drawing individual pixels, which Canvas.drawPath() doesn't do well
        if (((PenToolAttributes) toolAttributes).getThicknessLevel() == ToolAttributes.MIN_THICKNESS
                && !((PenToolAttributes) toolAttributes).isStraight()) {
            canvas.drawPoint(start.x, start.y, toolAttributes.getPaint());
            canvas.drawPath(toolReport.getPath(), toolAttributes.getPaint());
        } else if (start.x == event.x && start.y == event.y) {
            canvas.drawPoint(start.x, start.y, toolAttributes.getPaint());
        } else {
            // Regular drawing
            canvas.drawPath(toolReport.getPath(), toolAttributes.getPaint());
        }
    }

    // Locks the line in 11.25 degree (PI/8 radians) increments, so we can have 16 different angles
    private void lockAngles(PointF end) {
        float length = (float) Math.sqrt((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y));
        float angleRadians = (float) Math.atan2(end.y - start.y, end.x - start.x);
        float angleDegrees = (float) (180 * angleRadians / Math.PI);

        final float interval = 11.25f;

        // Vertical and horizontals

        // North
        if (angleDegrees < -7 * interval && angleDegrees >= -9 * interval) {
            end.x = start.x;
            end.y = start.y - length;
        }

        // South
        if (angleDegrees > 7 * interval && angleDegrees <= 9 * interval) {
            end.x = start.x;
            end.y = start.y + length;
        }

        // East
        if (angleDegrees <= 1 * interval && angleDegrees > -1 * interval) {
            end.x = start.x + length;
            end.y = start.y;
        }

        // West (uses or because atan2 wraps is between [-pi and pi]
        if (angleDegrees >= 15 * interval || angleDegrees <= -15 * interval) {
            end.x = start.x - length;
            end.y = start.y;
        }

        // Diagonals

        // Saves us from recalculating cosine of 45 degrees
        final float cos45 = (float) (Math.sqrt(2) / 2f);

        // North-East
        if (angleDegrees < -3 * interval && angleDegrees >= -5 * interval) {
            end.x = start.x + cos45 * length;
            end.y = start.y - cos45 * length;
        }

        // South-East
        if (angleDegrees > 3 * interval && angleDegrees <= 5 * interval) {
            end.x = start.x + cos45 * length;
            end.y = start.y + cos45 * length;
        }

        // North-West
        if (angleDegrees >= -13 * interval && angleDegrees < -11 * interval) {
            end.x = start.x - cos45 * length;
            end.y = start.y - cos45 * length;
        }

        // South-West
        if (angleDegrees <= 13 * interval && angleDegrees > 11 * interval) {
            end.x = start.x - cos45 * length;
            end.y = start.y + cos45 * length;
        }


        // These aren't exactly NNE, ENE, etc., but at 1x2 pixel steps (65.43 degrees)
        // and 2x1 pixel steps (26.57 degrees)

        // Saves us from recalculating sine and cosine of 63.43 degrees
        final float cos63_43 = (float) (1 / Math.sqrt(5));
        final float sin63_43 = (float) (2 / Math.sqrt(5));

        // North-North-East
        if (angleDegrees >= -7 * interval && angleDegrees < -5 * interval) {
            end.x = start.x + cos63_43 * length;
            end.y = start.y - sin63_43 * length;
        }

        // East-North-East
        if (angleDegrees >= -3 * interval && angleDegrees < -1 * interval) {
            end.x = start.x + sin63_43 * length;
            end.y = start.y - cos63_43 * length;
        }

        // East-South-East
        if (angleDegrees <= 3 * interval && angleDegrees > 1 * interval) {
            end.x = start.x + sin63_43 * length;
            end.y = start.y + cos63_43 * length;
        }

        // South-South-East
        if (angleDegrees <= 7 * interval && angleDegrees > 5 * interval) {
            end.x = start.x + cos63_43 * length;
            end.y = start.y + sin63_43 * length;
        }

        // North-North-West
        if (angleDegrees >= -11 * interval && angleDegrees < -9 * interval) {
            end.x = start.x - cos63_43 * length;
            end.y = start.y - sin63_43 * length;
        }

        // West-North-West
        if (angleDegrees >= -15 * interval && angleDegrees < -13 * interval) {
            end.x = start.x - sin63_43 * length;
            end.y = start.y - cos63_43 * length;
        }

        // West-South-West
        if (angleDegrees <= 15 * interval && angleDegrees > 13 * interval) {
            end.x = start.x - sin63_43 * length;
            end.y = start.y + cos63_43 * length;
        }

        // South-South-West
        if (angleDegrees <= 11 * interval && angleDegrees > 9 * interval) {
            end.x = start.x - cos63_43 * length;
            end.y = start.y + sin63_43 * length;
        }
    }
}