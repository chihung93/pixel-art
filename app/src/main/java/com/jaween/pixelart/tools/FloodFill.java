package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jaween.pixelart.tools.attributes.ToolAttributes;

import java.util.Stack;

/**
 * Created by ween on 9/28/14.
 */
public class FloodFill extends Tool {
    private Bitmap floodedBitmap = null;

    private Stack<PointF> pixels = new Stack<PointF>();

    public FloodFill(String name, Drawable icon) {
        super(name, icon);

        toolAttributes = new ToolAttributes();

        // Floods pixel by pixel, so attributes must reflect this
        toolAttributes.getPaint().setStrokeWidth(0);
        toolAttributes.getPaint().setAntiAlias(false);
    }

    // Top to bottom scanline flood fill using a stack
    // TODO: Fix long operation when filling large areas
    // Takes 300~500ms on average and occasionally more than 1000ms on a 180x259px image!!!
    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        performFloodFill(bitmap, event);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        blitBitmap(floodedBitmap, bitmap);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        blitBitmap(floodedBitmap, bitmap);
    }

    private void blitBitmap(Bitmap source, Bitmap destination) {
        if (cancelled == false) {
            canvas.setBitmap(destination);
            canvas.drawBitmap(source, 0, 0, toolAttributes.getPaint());
        }
    }

    private int colour(Bitmap bitmap, float x, float y) {
        return bitmap.getPixel((int) x, (int) y);
    }

    public void setBitmapConfiguration(int width, int height, Bitmap.Config config) {
        floodedBitmap = Bitmap.createBitmap(width, height, config);
    }

    private void performFloodFill(Bitmap bitmap, PointF event) {
        long startTime = System.currentTimeMillis();

        // Out of bounds
        if (!isInBounds(bitmap, event)) {
            return;
        }

        // Colour to be replaced and the colour which will replace it
        int oldColour = bitmap.getPixel((int) event.x, (int) event.y);
        int newColour = toolAttributes.getPaint().getColor();
        toolAttributes.getPaint().setColor(toolAttributes.getPaint().getColor());

        // No bitmap
        if (bitmap == null)
            return;

        // Filling not required
        if (oldColour == newColour) {
            return;
        }

        // Filling not required
        if (colour(bitmap, event.x, event.y) == newColour) {
            return;
        }

        // Clears pixel queue
        pixels.clear();
        pixels.push(event);

        // Four-way flood fill algorithm
        while (!pixels.isEmpty()) {
            PointF pixel = pixels.pop();
            float x = pixel.x;
            float y1 = pixel.y;

            while (y1 >= 0 && colour(bitmap, x, y1) == oldColour) {
                y1--;
            }
            y1++;

            boolean spanLeft = false;
            boolean spanRight = false;

            while (y1 < bitmap.getHeight() && colour(bitmap, x, y1) == oldColour) {
                bitmap.setPixel((int) x, (int) y1, toolAttributes.getPaint().getColor());

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

        blitBitmap(bitmap, floodedBitmap);

        Log.d("FloodFill", "Flooding took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
