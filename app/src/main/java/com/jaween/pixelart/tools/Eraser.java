package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;

import com.jaween.pixelart.tools.attributes.EraserToolAttributes;
import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 9/28/14.
 */
public class Eraser extends Tool {

    private static final int TOOL_ID = 1;
    private PointF start = new PointF();

    public Eraser(String name, Drawable icon) {
        super(name, icon, TOOL_ID);
        toolAttributes = new EraserToolAttributes();

        // Needed to erase (draws transparent lines)
        toolAttributes.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        start.x = event.x;
        start.y = event.y;

        draw(bitmap, event);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        draw(bitmap, event);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        draw(bitmap, event);
    }

    private void draw(Bitmap bitmap, PointF event) {
        toolReport.getPath().lineTo(event.x, event.y);
        canvas.setBitmap(bitmap);

        // Work around for drawing individual pixels, which Canvas.drawPath() doesn't do well
        if (((EraserToolAttributes) toolAttributes).getThicknessLevel() == ToolAttributes.MIN_THICKNESS) {
            canvas.drawPoint(start.x, start.y, toolAttributes.getPaint());
            canvas.drawPath(toolReport.getPath(), toolAttributes.getPaint());
        } else if (start.x == event.x && start.y == event.y) {
            canvas.drawPoint(start.x, start.y, toolAttributes.getPaint());
        } else {
            // Regular drawing
            canvas.drawPath(toolReport.getPath(), toolAttributes.getPaint());
        }
    }
}