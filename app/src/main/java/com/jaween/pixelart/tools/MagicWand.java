package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jaween.pixelart.tools.attributes.MagicWandToolAttributes;
import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 11/30/14.
 */
public class MagicWand extends Tool {

    private static final int TOOL_ID = 8;
    private int[] bitmapArray;
    private int[] pixelStack;
    private int width, height;

    public MagicWand(String name, Drawable icon) {
        super(name, icon, TOOL_ID);

        toolAttributes = new MagicWandToolAttributes();
        toolAttributes.setSelector(true);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        // No implementation
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        // No implementation
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        //performSelection(bitmap, event);
    }

    public void setBitmapConfiguration(int width, int height) {
        bitmapArray = new int[width * height];
        pixelStack = new int[height * 2];

        this.width = width;
        this.height = height;
    }

    private void performSelection(Bitmap bitmap, PointF event) {
        long startTime = System.currentTimeMillis();

        // Out of bounds
        if (!isInBounds(bitmap, event)) {
            return;
        }

        // Gets an array of the bitmap's colours
        bitmap.getPixels(bitmapArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Colour to be replaced and the colour which will replace it
        int oldColour = bitmapArray[(int) event.x + (int) event.y * width];
        int newColour = Color.BLACK;
        toolAttributes.getPaint().setColor(toolAttributes.getPaint().getColor());

        if (oldColour == newColour) {
            return;
        }

        // No bitmap
        if (bitmap == null) {
            return;
        }

        // Clears the pixelStack
        for (int i = 0; i < pixelStack.length; i++) {
            pixelStack[i] = -1;
        }
        int topOfStackIndex = 0;

        // Sets the touched pixel to the new colour
        bitmapArray[(int) event.x + (int) event.y * width] = Color.BLACK;

        // Clears the stack
        pixelStack[topOfStackIndex] = (int) event.x;
        pixelStack[topOfStackIndex + 1] = (int) event.y;
        topOfStackIndex += 2;

        // Modified four-way flood fill algorithm
        while (topOfStackIndex > 0) {
            // Pops a pixel from the stack
            int x = pixelStack[topOfStackIndex - 2];
            int y1 = pixelStack[topOfStackIndex - 1];
            topOfStackIndex -= 2;

            // Finds the top of the line segment
            while (y1 >= 0 && com.jaween.pixelart.util.Color.colourDistance(bitmapArray[x + y1 * width], oldColour) <= ((MagicWandToolAttributes) toolAttributes).getThreshold()) {
                y1--;
            }
            y1++;

            boolean spanLeft = false;
            boolean spanRight = false;

            // Colours down the line segment
            while (y1 < bitmap.getHeight() && com.jaween.pixelart.util.Color.colourDistance(bitmapArray[x + y1 * width], oldColour) <= ((MagicWandToolAttributes) toolAttributes).getThreshold()) {
                bitmapArray[x + y1 * width] = Color.BLACK;

                if (!spanLeft && x > 0 && com.jaween.pixelart.util.Color.colourDistance(bitmapArray[x - 1 + y1 * width], oldColour) <= ((MagicWandToolAttributes) toolAttributes).getThreshold()) {
                    // Pixel to the left must also be changed, pushes it to the stack
                    pixelStack[topOfStackIndex] = x - 1;
                    pixelStack[topOfStackIndex + 1] = y1;
                    topOfStackIndex += 2;
                    spanLeft = true;
                } else if (spanLeft && x > 0 && com.jaween.pixelart.util.Color.colourDistance(bitmapArray[x - 1 + y1 * width], oldColour) > ((MagicWandToolAttributes) toolAttributes).getThreshold()) {
                    // Pixel to the left has already been changed
                    spanLeft = false;
                }

                if (!spanRight && x < bitmap.getWidth() - 1 && com.jaween.pixelart.util.Color.colourDistance(bitmapArray[x + 1 + y1 * width], oldColour) <= ((MagicWandToolAttributes) toolAttributes).getThreshold()) {
                    // Pixel to the right must also be changed, pushes it to the stack
                    pixelStack[topOfStackIndex] = x + 1;
                    pixelStack[topOfStackIndex + 1] = y1;
                    topOfStackIndex += 2;
                    spanRight = true;
                } else if (spanRight && x < bitmap.getWidth() - 1 && com.jaween.pixelart.util.Color.colourDistance(bitmapArray[x + 1 + y1 * width], oldColour) > ((MagicWandToolAttributes) toolAttributes).getThreshold()) {
                    // Pixel to the right has already been changed
                    spanRight = false;
                }
                y1++;
            }
        }

        bitmap.setPixels(bitmapArray, 0, width, 0, 0, width, height);

        Log.d("MagicWand", "Selecting took " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
