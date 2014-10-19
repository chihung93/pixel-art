package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;

import java.util.Stack;

/**
 * Created by ween on 9/28/14.
 */
public class FloodFill extends Tool {

    private static Paint floodPaint = new Paint();
    private Stack<PointF> pixels = new Stack<PointF>();

    public FloodFill(String name) {
        super(name);

        // Attributes required for flooding (must only touch a single pixel at a time)
        floodPaint.setStrokeWidth(0);
        floodPaint.setAntiAlias(false);
    }

    // Top to bottom scanline flood fill using a stack TODO Fix long operation when filling large areas
    @Override
    public void start(Bitmap bitmap, PointF event, Attributes attributes) {
        long startTime = System.currentTimeMillis();

        floodPaint.setColor(attributes.paint.getColor());

        int oldColour = attributes.tempTouchedColour;
        int newColour = attributes.paint.getColor();

        // No bitmap
        if (bitmap == null)
            return;

        // Out of bounds
        if (event.x < 0 || event.x >= bitmap.getWidth() || event.y < 0 || event.y >= bitmap.getHeight())
            return;

        // Filling not required
        if (oldColour == newColour)
            return;

        // Filling not required
        if (colour(bitmap, event.x, event.y) == newColour)
            return;

        pixels.clear();
        pixels.push(event);

        while (!pixels.isEmpty()) {
            PointF pixel = pixels.pop();
            float x = pixel.x;
            float y1 = pixel.y;

            while (y1 >= 0 && colour(bitmap, x, y1) == oldColour)
                y1--;
            y1++;

            boolean spanLeft = false;
            boolean spanRight = false;

            while (y1 < bitmap.getHeight() && colour(bitmap, x, y1) == oldColour) {
                bitmap.setPixel((int) x, (int) y1, floodPaint.getColor());

                if (!spanLeft && x > 0 && colour(bitmap, x - 1, y1) == oldColour) {
                    pixels.push(new PointF(x - 1, y1));
                    spanLeft = true;
                } else if (spanLeft && x > 0 && colour(bitmap, x - 1, y1) != oldColour) {
                    spanLeft = false;
                }

                if (!spanRight && x < bitmap.getWidth() - 1 && colour(bitmap, x + 1, y1) == oldColour) {
                    pixels.push(new PointF(x + 1, y1));
                    spanRight = true;
                } else if (spanRight && x < bitmap.getWidth() - 1 && colour(bitmap, x + 1, y1) != oldColour) {
                    spanRight = false;
                }
                y1++;
            }
        }

        Log.d("FloodFill", "Flooding took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    @Override
    public void move(Bitmap bitmap, PointF event, Attributes attributes) {
        // No implementation
    }

    @Override
    public void end(Bitmap bitmap, PointF event, Attributes attributes) {
        // No implementation
    }

    private int colour(Bitmap bitmap, float x, float y) {
        return bitmap.getPixel((int) x, (int) y);
    }
}
