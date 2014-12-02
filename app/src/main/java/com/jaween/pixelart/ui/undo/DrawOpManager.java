package com.jaween.pixelart.ui.undo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.util.Encoder;

import java.util.List;

/**
 * Created by ween on 11/29/14.
 */
public class DrawOpManager {

    private static final int BITS_PER_CHANNEL = 8;
    private static final int ALPHA_CHANNEL_SHIFT = 3 * BITS_PER_CHANNEL;
    private static final int OPAQUE_ALPHA_CHANNEL = 255 << ALPHA_CHANNEL_SHIFT;

    private static final Encoder encoder = new Encoder();

    private static Bitmap layerBeforeModification;
    private static Bitmap xorBitmap;
    private static Bitmap decompressedChanges;
    
    private static int[] lhsPixelsArray;
    private static int[] rhsPixelsArray;

    private static Canvas sharedCanvas;
    private static Paint xorPaint;
    private static Paint bitmapPaint;

    public DrawOpManager(Bitmap initialBitmap) {
        // Used for storing a copy of the previous frame
        layerBeforeModification = Bitmap.createBitmap(
                initialBitmap.getWidth(),
                initialBitmap.getHeight(),
                initialBitmap.getConfig()
        );

        // Used for XOR'ing bitmaps together
        xorBitmap = Bitmap.createBitmap(
                initialBitmap.getWidth(),
                initialBitmap.getHeight(),
                initialBitmap.getConfig()
        );

        // Used for temporarily storing decompressed changes from last frame or to next frame
        decompressedChanges = Bitmap.createBitmap(
                initialBitmap.getWidth(),
                initialBitmap.getHeight(),
                initialBitmap.getConfig()
        );

        // Used to retrieve and set pixel data in the xor() function (faster than Bitmap.setPixel())
        lhsPixelsArray = new int[initialBitmap.getWidth() * initialBitmap.getHeight()];
        rhsPixelsArray = new int[initialBitmap.getWidth() * initialBitmap.getHeight()];

        // Allocates memory for pixel data in the compression function
        encoder.setBitmapDimensions(initialBitmap.getWidth(), initialBitmap.getHeight());

        sharedCanvas = new Canvas();

        initialisePaints();

        bitmapCopy(initialBitmap, layerBeforeModification);
    }

    private void initialisePaints() {
        bitmapPaint = new Paint();

        xorPaint = new Paint();
        xorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
    }

    public void setLayerBeforeModification(Bitmap layerBeforeModification) {
        bitmapCopy(layerBeforeModification, this.layerBeforeModification);
    }

    public UndoItem add(Bitmap currentFrame, int layerIndex) {
        // Finds the changes between the bitmaps
        Bitmap changes = xor(currentFrame, layerBeforeModification);

        // Compresses these differences
        Integer[] encodedChanges = encoder.encodeRunLength(changes);

        // Keeps a copy of this frame for future undos/redos
        bitmapCopy(currentFrame, layerBeforeModification);

        DrawOpUndoData undoData = new DrawOpUndoData(encodedChanges, layerIndex);
        return new UndoItem(UndoItem.Type.DRAW_OP, 0, undoData);
    }

    public void undo(List<Layer> layers, DrawOpUndoData undoData) {
        // The changes to be undone
        Integer[] previousChanges = undoData.getCompressedChanges();

        // The layer which was modified
        int layerIndex = undoData.getLayerIndex();
        Bitmap modifiedLayer = layers.get(layerIndex).getImage();

        // Rolls back the change
        performUpdate(previousChanges, modifiedLayer);
    }

    public void redo(List<Layer> layers, DrawOpUndoData redoData) {
        // The layer to be redone
        int layerIndex = redoData.getLayerIndex();
        Bitmap layerBeforeRollforward = layers.get(layerIndex).getImage();

        // This frame will become the previous frame since it's as if the user is drawing on screen
        bitmapCopy(layerBeforeRollforward, layerBeforeModification);

        // Returns this change to the undo stack
        Integer[] nextChanges = redoData.getCompressedChanges();

        // Performs the redo operation to the current frame
        performUpdate(nextChanges, layerBeforeRollforward);
    }

    /**
     * Updates a bitmap based on an array of compressed changes
     * This can be an undo change or a redo change
     **/
    private static void performUpdate(Integer[] compressedChanges, Bitmap destination) {
        // Decodes the uncompressed changes
        encoder.decodeRunLength(compressedChanges, decompressedChanges);

        // Retrieves the new frame
        Bitmap newFrameBitmap = xor(destination, decompressedChanges);

        // Updates the current frame
        bitmapCopy(newFrameBitmap, destination);

        // Updates the previous frame (for use with the next drawing operation)
        bitmapCopy(newFrameBitmap, layerBeforeModification);
    }

    /**
     * Copies the pixels from a source bitmap onto a destination bitmap
     **/
    private static void bitmapCopy(Bitmap source, Bitmap destination) {
        sharedCanvas.setBitmap(destination);
        sharedCanvas.drawBitmap(source, 0, 0, bitmapPaint);
    }

    /**
     * Returns the bitwise XOR of the lhs and rhs (i.e. the differences between the two bitmaps)
     * // TODO: Ensure bitmap configurations are also the same.
     **/
    private static Bitmap xor(Bitmap lhs, Bitmap rhs) {
        long startTime = System.currentTimeMillis();

        // Doesn't seem to work due to special case with non-opaque pixels (see PorterDuff docs)
        /*xorBitmap.eraseColor(Color.TRANSPARENT);
        sharedCanvas.setBitmap(xorBitmap);
        sharedCanvas.drawBitmap(lhs, 0, 0, bitmapPaint);
        sharedCanvas.drawBitmap(rhs, 0, 0, xorPaint);*/

        //int alphaXor = Color.alpha(lhs.getPixel(0, 0)) ^ Color.alpha(rhs.getPixel(0, 0));
        //Log.d("DrawOpManager", "top left alpha was " + Color.alpha(rhs.getPixel(0, 0)) + ", is now " + Color.alpha(lhs.getPixel(0, 0)) + ", alphaXor is " + alphaXor);

        // Retrieves the pixel data of both bitmaps into the two pixel arrays
        lhs.getPixels(lhsPixelsArray, 0, lhs.getWidth(), 0, 0, lhs.getWidth(), lhs.getHeight());
        rhs.getPixels(rhsPixelsArray, 0, rhs.getWidth(), 0, 0, rhs.getWidth(), rhs.getHeight());

        /*int leftPixel = lhsPixelsArray[0];
        int rightPixel = rhsPixelsArray[0];

        int leftAlpha = Color.alpha(leftPixel);
        int rightAlpha = Color.alpha(rightPixel);

        int xor = leftPixel ^ rightPixel;
        int xorAlpha = leftAlpha ^ rightAlpha;

        int xorShift = xor >>> 24;

        Log.d("DrawOpManager", "xor is " + xor + ", rightAlpha is " + rightAlpha + ", leftAlpha is " + leftAlpha + ", xorAlpha is " + xorAlpha + ", xorShift is " + xorShift);*/

        for (int y = 0; y < xorBitmap.getHeight(); y++) {
            for (int x = 0; x < xorBitmap.getWidth(); x++) {
                // XORs the two pixels together
                int lhsPixel = lhsPixelsArray[x + y * lhs.getWidth()];
                int rhsPixel = rhsPixelsArray[x + y * rhs.getWidth()];
                int xorColour = lhsPixel ^ rhsPixel;

                int alphaXor = Color.alpha(lhsPixel) ^ Color.alpha(rhsPixel);
                int alphaXorColour = xorColour + OPAQUE_ALPHA_CHANNEL;

                // Sets the pixel in the array to the XOR'd result of the pixels from the two bitmaps
                lhsPixelsArray[x + y * lhs.getWidth()] = alphaXorColour;
            }
        }

        // Finally we load the pixels into the resultant bitmap
        xorBitmap.setPixels(lhsPixelsArray, 0, lhs.getWidth(), 0, 0, lhs.getWidth(), lhs.getHeight());

        Log.d("UndoRedoTracker", "XOR took " + (System.currentTimeMillis() - startTime) + "ms");

        return xorBitmap;
    }
}
