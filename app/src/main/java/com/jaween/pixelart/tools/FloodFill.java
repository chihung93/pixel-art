package com.jaween.pixelart.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.jaween.pixelart.Point;
import com.jaween.pixelart.R;

import java.util.Stack;

/**
 * Created by ween on 9/28/14.
 */
public class FloodFill extends Tool {

    private static Paint floodPaint = new Paint();
    private final String name;
    private Stack<Point> pixels = new Stack<Point>();

    public FloodFill(Context context) {
        super(context);

        // Attributes required for flooding (must only touch a single pixel at a time)
        floodPaint.setStrokeWidth(0);
        floodPaint.setAntiAlias(false);

        name = context.getString(R.string.tool_flood_fill);
    }

    // Top to bottom scanline flood fill using a stack TODO Fix long operation when filling large areas
    @Override
    public void beginAction(Canvas canvas, Bitmap bitmap, Point event, Attributes attributes) {
        long startTime = System.currentTimeMillis();

        floodPaint.setColor(attributes.paint.getColor());

        int oldColour = Color.WHITE;
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
            Point pixel = pixels.pop();
            float x = pixel.x;
            float y1 = pixel.y;

            while (y1 >= 0 && colour(bitmap, x, y1) == oldColour)
                y1--;
            y1++;

            boolean spanLeft = false;
            boolean spanRight = false;

            while (y1 < bitmap.getHeight() && colour(bitmap, x, y1) == oldColour) {
                canvas.drawPoint((int) x, (int) y1, floodPaint);

                if (!spanLeft && x > 0 && colour(bitmap, x - 1, y1) == oldColour) {
                    pixels.push(new Point(x - 1, y1));
                    spanLeft = true;
                } else if (spanLeft && x > 0 && colour(bitmap, x - 1, y1) != oldColour) {
                    spanLeft = false;
                }

                if (!spanRight && x < bitmap.getWidth() - 1 && colour(bitmap, x + 1, y1) == oldColour) {
                    pixels.push(new Point(x + 1, y1));
                    spanRight = true;
                } else if (spanRight && x < bitmap.getWidth() - 1 && colour(bitmap, x + 1, y1) != oldColour) {
                    spanRight = false;
                }
                y1++;
            }
        }

        Log.d("FloodFill", "Flooding took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    private int colour(Bitmap bitmap, float x, float y) {
        return bitmap.getPixel((int) x, (int) y);
    }

    @Override
    public void endAction(Point event) {
        // No implementation
    }

    @Override
    public String getName() {
        return name;
    }
}
