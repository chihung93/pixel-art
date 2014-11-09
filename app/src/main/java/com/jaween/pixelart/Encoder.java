package com.jaween.pixelart;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Compresses bitmaps
 */
public class Encoder {

    private ArrayList<Integer> encodedBitmapList = new ArrayList<Integer>();

    /**
     * Uses run-length encoding to compress a Bitmap
     * @param source The bitmap to compress
     * @return An array of Integers in pairs of 'run colour' followed by 'run count'
     */
    public Integer[] encodeRunLength(Bitmap source) {
        long startTime = System.currentTimeMillis();

        encodedBitmapList.clear();

        int currentRunColour = source.getPixel(0, 0);
        int currentRunCount = 0;

        // Iterates over all the pixels in the bitmap
        int y = 0;
        while (y < source.getHeight()) {
            int x = 0;
            while (x < source.getWidth()) {
                int currentPixelColour = source.getPixel(x, y);
                if (currentPixelColour == currentRunColour) {
                    // Continues current run
                    currentRunCount += 1;
                } else {
                    // New run, adds previous run to list
                    encodedBitmapList.add(currentRunColour);
                    encodedBitmapList.add(currentRunCount);

                    // Begins new run
                    currentRunColour = currentPixelColour;
                    currentRunCount = 1;
                }
                x++;
            }
            y++;
        }

        // Adds the final run (was not added because the image ended)
        encodedBitmapList.add(currentRunColour);
        encodedBitmapList.add(currentRunCount);

        // Statistics
        float newSize = (float) encodedBitmapList.size();
        float oldSize = (float) (source.getWidth() * source.getHeight());
        float ratioDecreased = 1f - newSize / oldSize;
        float percentage = ratioDecreased * 100f;

        Integer[] encodedBitmapArray = new Integer[encodedBitmapList.size()];
        encodedBitmapList.toArray(encodedBitmapArray);

        Log.d("Encoder", "Compressed by " + percentage + "% (" + (System.currentTimeMillis() - startTime) + "ms)");

        return encodedBitmapArray;
    }

    /**
     * Decodes a run-length encoded array of Integers into a Bitmap
     * @param encodedBitmap The Bitmap to decompress in pairs of 'run colour' followed by 'run count'
     * @param destination The Bitmap in which to store the decompressed data
     */
    public void decodeRunLength(Integer[] encodedBitmap, Bitmap destination) {
        long startTime = System.currentTimeMillis();

        if (encodedBitmap == null || destination == null) {
            return;
        }

        if (encodedBitmap.length <= 0) {
            return;
        }

        int x = 0;
        int y = 0;

        int tempLineFeedCount = 0;
        int tempPixelsCount = 0;

        // Iterates over pairs 'run colour's followed by 'run count's
        for (int i = 0; i < encodedBitmap.length; i += 2) {
            int currentRunColour = encodedBitmap[i];
            int currentRunCount = encodedBitmap[i + 1];

            while (currentRunCount > 0) {
                destination.setPixel(x, y, currentRunColour);
                currentRunCount--;
                tempPixelsCount++;

                // Wraps x when it reaches the edge of the image
                x++;
                if (x >= destination.getWidth()) {
                    tempLineFeedCount++;
                    x = 0;
                    y++;
                }
            }
        }


        Log.d("Encoder", "Decoding took " + (System.currentTimeMillis() - startTime) + "ms, (" + (tempPixelsCount/tempLineFeedCount) + ", " + tempLineFeedCount + ")" );
    }
}