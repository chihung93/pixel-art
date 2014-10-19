package com.jaween.pixelart.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by ween on 10/19/14.
 */
public class PixelGrid {

    private float dp;

    private Paint gridPaint;
    private boolean enabled = true;

    public PixelGrid(float dp) {
        this.dp = dp;

        gridPaint = new Paint();
        gridPaint.setStrokeWidth(dp);
        gridPaint.setAntiAlias(false);
        gridPaint.setColor(Color.DKGRAY);
    }

    public void draw(Canvas canvas, float imageWidth, float imageHeight, Rect viewport, float scale, int majorPixelSpacing) {
        float majorScaledSpacing = majorPixelSpacing * scale;

        // Horizontal lines
        for (int y = 0; y < imageHeight * scale; y += majorScaledSpacing) {
            canvas.drawLine(
                    (int) + (-viewport.left * scale),
                    (int) + (-viewport.top * scale + y),
                    (int) + ((-viewport.left + imageWidth) * scale),
                    (int) + (-viewport.top * scale + y),
                    gridPaint
            );
        }

        // Vertical lines
        for (int x = 0; x < imageWidth * scale; x += majorScaledSpacing) {
            canvas.drawLine(
                    (int) (-viewport.left * scale + x),
                    (int) (-viewport.top * scale),
                    (int) (-viewport.left * scale + x),
                    (int) ((-viewport.top + imageHeight) * scale),
                    gridPaint
            );
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
