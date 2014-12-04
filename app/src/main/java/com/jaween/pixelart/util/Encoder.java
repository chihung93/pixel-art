package com.jaween.pixelart.util;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;

/**
 * Compresses bitmaps
 */
public class Encoder {

    private static final String LOG_TAG = Encoder.class.getSimpleName();

    private ArrayList<Integer> encodedBitmapList = new ArrayList<Integer>();
    private int[] pixelArray;

    private int width = 0;
    private int height = 0;

    /**
     * Allocates memory for temporary data structures. Must be set prior to calling an encode or
     * decode function.
     * @param width The width of Bitmaps that will be used
     * @param height The height of Bitmaps that will be used
     */
    public void setBitmapDimensions(int width, int height) {
        // Creates a new pixel array if the incoming dimensions are new
        if (this.width != width || this.height != height) {
            pixelArray = new int[width * height];
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Uses run-length encoding to compress a Bitmap.
     * @param source The Bitmap to compress
     * @return An array in pairs of 'run colour' followed by 'run count'
     */
    public Integer[] encodeRunLength(Bitmap source) {
        // Loads the pixels from the bitmap into pixelArray
        source.getPixels(pixelArray, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());
        return encodeRunLength(pixelArray);
    }

    /**
     * Uses run-length encoding to compress an array.
     * @param pixels The array of pixels to compress
     * @return An array in pairs of 'run colour' followed by 'run count'
     */
    public Integer[] encodeRunLength(int[] pixels) {

        encodedBitmapList.clear();

        // Initial run
        int currentRunColour = pixels[0];
        int currentRunCount = 0;

        // Iterates over all the pixels in the bitmap
        int y = 0;
        while (y < height) {
            int x = 0;
            while (x < width) {
                int currentPixelColour = pixels[x + y * width];
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

        // Converts the ArrayList to an array
        Integer[] encodedBitmapArray = new Integer[encodedBitmapList.size()];
        encodedBitmapList.toArray(encodedBitmapArray);

        return encodedBitmapArray;
    }

    /**
     * Decodes a run-length encoded Bitmap.
     * @param encodedBitmap The array to decompress, in pairs of 'run colour' followed by 'run count'
     * @param destination The Bitmap in which to store the decompressed data
     */
    public void decodeRunLength(Integer[] encodedBitmap, Bitmap destination) {
        decodeRunLength(encodedBitmap, pixelArray);
        destination.setPixels(pixelArray, 0, width, 0, 0, width, height);
    }

    /**
     * Decodes a run-length encoded Bitmap.
     * @param encodedBitmap The array to decompress, in pairs of 'run colour' followed by 'run count'
     * @param destination The array in which to store the decompressed data
     */
    public void decodeRunLength(Integer[] encodedBitmap, int[] destination) {
        if (encodedBitmap == null || destination == null) {
            if (encodedBitmap == null) {
                Log.e(LOG_TAG, "Decoding error: Encoded bitmap was null");
            }
            if (destination == null) {
                Log.e(LOG_TAG, "Decoding error: Encoded bitmap was null");
            }
            return;
        }

        if (encodedBitmap.length <= 0) {
            Log.e(LOG_TAG, "Decoding error: Encoded bitmap length was " + encodedBitmap.length);
            return;
        }

        int x = 0;
        int y = 0;

        // Iterates over pairs of run colours followed by run counts
        for (int i = 0; i < encodedBitmap.length; i += 2) {
            int currentRunColour = encodedBitmap[i];
            int currentRunCount = encodedBitmap[i + 1];

            while (currentRunCount > 0) {
                destination[x + y * height] = currentRunColour;
                currentRunCount--;

                // Wraps x when it reaches the edge of the image
                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
            }
        }
    }
}