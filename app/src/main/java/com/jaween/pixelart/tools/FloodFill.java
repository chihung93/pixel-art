package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 9/28/14.
 */
public class FloodFill extends Tool {

    private static final int TOOL_ID = 5;
    private int[] bitmapArray;
    private int[] pixelStack;

    public FloodFill(String name, Drawable icon) {
        super(name, icon, TOOL_ID);

        toolAttributes = new ToolAttributes();

        // Floods pixel by pixel, so attributes must reflect this
        toolAttributes.getPaint().setStrokeWidth(0);
        toolAttributes.getPaint().setAntiAlias(false);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        performFloodFill(bitmap, event);
        setPixels(bitmapArray, bitmap);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        setPixels(bitmapArray, bitmap);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        setPixels(bitmapArray, bitmap);
    }

    private void setPixels(int[] source, Bitmap destination) {
            destination.setPixels(
                    source,
                    0,
                    destination.getWidth(),
                    0,
                    0,
                    destination.getWidth(),
                    destination.getHeight());
    }

    private int colour(Bitmap bitmap, int x, int y) {
        return bitmapArray[x + y * bitmap.getWidth()];
    }

    public void setBitmapConfiguration(int width, int height) {
        bitmapArray = new int[width * height];
        pixelStack = new int[height * 2];
    }

    // Vertical scanline stack based four-way flood fill algorithm
    private void performFloodFill(Bitmap bitmap, PointF event) {
        long startTime = System.currentTimeMillis();

        // Out of bounds
        if (!isInBounds(bitmap, event)) {
            return;
        }

        // No bitmap
        if (bitmap == null)
            return;

        // Gets an array of the bitmap's colours
        bitmap.getPixels(bitmapArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Colour to be replaced and the colour which will replace it
        int oldColour = colour(bitmap, (int) event.x, (int) event.y);
        int newColour = toolAttributes.getPaint().getColor();
        toolAttributes.getPaint().setColor(toolAttributes.getPaint().getColor());

        // Filling not required
        if (oldColour == newColour) {
            return;
        }

        // Clears the pixelStack
        for (int i = 0; i < pixelStack.length; i++) {
            pixelStack[i] = -1;
        }
        int topOfStackIndex = 0;

        // Sets the touched pixel to the new colour
        bitmapArray[(int) event.x + (int) event.y * bitmap.getWidth()] = newColour;

        // Clears the stack
        pixelStack[topOfStackIndex] = (int) event.x;
        pixelStack[topOfStackIndex + 1] = (int) event.y;
        topOfStackIndex += 2;


        // Four-way flood fill algorithm
        while (topOfStackIndex > 0) {
            // Pops a pixel from the stack
            int x = pixelStack[topOfStackIndex - 2];
            int y1 = pixelStack[topOfStackIndex - 1];
            topOfStackIndex -= 2;

            while (y1 >= 0 && colour(bitmap, x, y1) == oldColour) {
                y1--;
            }
            y1++;

            boolean spanLeft = false;
            boolean spanRight = false;

            while (y1 < bitmap.getHeight() && colour(bitmap, x, y1) == oldColour) {
                bitmapArray[x + y1 * bitmap.getWidth()] = toolAttributes.getPaint().getColor();

                if (!spanLeft && x > 0 && colour(bitmap, x - 1, y1) == oldColour) {
                    // Pixel to the left must also be changed, pushes it to the stack
                    pixelStack[topOfStackIndex] = x - 1;
                    pixelStack[topOfStackIndex + 1] = y1;
                    topOfStackIndex += 2;
                    spanLeft = true;
                } else if (spanLeft && x > 0 && colour(bitmap, x - 1, y1) != oldColour) {
                    // Pixel to the left has already been changed
                    spanLeft = false;
                }

                if (!spanRight && x < bitmap.getWidth() - 1 && colour(bitmap, x + 1, y1) == oldColour) {
                    // Pixel to the right must also be changed, pushes it to the stack
                    pixelStack[topOfStackIndex] = x + 1;
                    pixelStack[topOfStackIndex + 1] = y1;
                    topOfStackIndex += 2;
                    spanRight = true;
                } else if (spanRight && x < bitmap.getWidth() - 1 && colour(bitmap, x + 1, y1) != oldColour) {
                    // Pixel to the right has already been changed
                    spanRight = false;
                }
                y1++;
            }
        }

        Log.d("FloodFill", "Flooding took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
