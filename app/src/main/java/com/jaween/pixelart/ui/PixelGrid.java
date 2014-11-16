package com.jaween.pixelart.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Created by ween on 10/19/14.
 */
public class PixelGrid {

    // PixelGrid View
    private boolean enabled = false;

    // Visuals
    private float dp;
    private Paint gridPaint;

    // Grid lines
    private float imageWidth;
    private float imageHeight;
    private float[] majorGridLines;
    private float majorPixelSpacing;

    public PixelGrid(float dp, float imageWidth, float imageHeight, int majorPixelSpacing) {

        initialisePaints(dp);

        // Grid line memory allocation
        int extraGridLines = 2; // Right most and bottom most lines only visible at precise zoom levels
        int gridLineCount = (int) ((imageHeight/majorPixelSpacing) + (imageWidth/majorPixelSpacing) + extraGridLines);
        int floatsPerLine = 4;
        majorGridLines = new float[gridLineCount * floatsPerLine];

        this.dp = dp;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.majorPixelSpacing = majorPixelSpacing;
    }

    public void draw(Canvas canvas, RectF viewport, float scale) {
        float majorScaledSpacing = majorPixelSpacing * scale;
        int i = 0;

        // Horizontal lines
        for (float y = 0; y < imageHeight * scale; y += majorScaledSpacing) {
            majorGridLines[i++] = (int) (-viewport.left * scale);
            majorGridLines[i++] = (int) (-viewport.top * scale + y);
            majorGridLines[i++] = (int) ((-viewport.left + imageWidth) * scale);
            majorGridLines[i++] = (int) (-viewport.top * scale + y);
        }

        // Vertical lines
        for (float x = 0; x < imageWidth * scale; x += majorScaledSpacing) {
            majorGridLines[i++] = (int) (-viewport.left * scale + x);
            majorGridLines[i++] = (int) (-viewport.top * scale);
            majorGridLines[i++] = (int) (-viewport.left * scale + x);
            majorGridLines[i++] = (int) ((-viewport.top + imageHeight) * scale);
        }

        canvas.drawLines(majorGridLines, 0, i, gridPaint);
    }

    private void initialisePaints(float dp) {
        gridPaint = new Paint();
        gridPaint.setStrokeWidth(dp);
        gridPaint.setAntiAlias(false);
        gridPaint.setColor(Color.DKGRAY);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
