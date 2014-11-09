package com.jaween.pixelart;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import java.util.LinkedList;

/**
 * Manages the undo and redo lists.
 *
 * Used by first constructing the object with the initial state Bitmap and the max undo limit.
 * The user draws on the Bitmap and passes that bitmap to modifiedBitmap().
 * It XORs the Bitmap with the previous frame and stores the compressed the result.
 * The undo() function will then decompress that result, XOR it with the given bitmap to retrieve
 * the previous frame. Finally it blits that bitmap back onto the given Bitmap.
 *
 * TODO: Run in separate thread
 */
public class UndoRedoTracker {

    private static final Encoder encoder = new Encoder();

    private static LinkedList<Integer[]> compressedUndo = new LinkedList<Integer[]>();
    private static LinkedList<Integer[]> compressedRedo = new LinkedList<Integer[]>();
    private static int maxUndos;

    private static Bitmap previousFrame;
    private static Bitmap xorBitmap;
    private static Bitmap decompressedChanges;

    private static Canvas sharedCanvas;
    private static Paint xorPaint;
    private static Paint bitmapPaint;

    private static final int BITS_PER_CHANNEL = 8;
    private static final int OPAQUE_ALPHA_CHANNEL = 255 << (3 * BITS_PER_CHANNEL);

    public UndoRedoTracker(Bitmap initialBitmap, int maxUndos) {
        // Used for storing a copy of the previous frame
        previousFrame = Bitmap.createBitmap(
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

        sharedCanvas = new Canvas();
        xorPaint = new Paint();
        xorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
        bitmapPaint = new Paint();

        bitmapCopy(initialBitmap, previousFrame);

        this.maxUndos = maxUndos;
    }

    public void bitmapModified(Bitmap currentFrame) {
        long startTime = System.currentTimeMillis();

        // Loses any future drawing commands that were in the redo stack
        if (compressedRedo.size() > 0) {
            compressedRedo.clear();
        }

        // Holds the changes between the bitmaps
        Bitmap changes = xor(currentFrame, previousFrame);

        // Keeps track of the differences in a compressed form
        Integer[] encodedChanges = encoder.encodeRunLength(changes);
        compressedUndo.push(encodedChanges);

        // Maintains the maximum number of undos (and hence the maximum number of redos)
        if (compressedUndo.size() > maxUndos) {
            compressedUndo.removeLast();
        }

        // Keeps a copy of this frame for future undos/redos
        bitmapCopy(currentFrame, previousFrame);

        Log.d("UndoRedoTracker", "Modification took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public static void undo(Bitmap currentFrame) {
        long startTime = System.currentTimeMillis();

        // There must be a frame to undo
        if (compressedUndo.size() <= 0) {
            return;
        }

        // Pushes the the last change to the redo stack in case the user wants to redo
        Integer[] previousChanges = compressedUndo.pop();
        compressedRedo.push(previousChanges);

        // Performs the undo operation to the previous frame for future undos
        performUpdate(previousChanges, currentFrame);

        Log.d("UndoRedoTracker", "Undo took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    public static void redo(Bitmap current) {
        long startTime = System.currentTimeMillis();

        // There must be a frame to redo
        if (compressedRedo.size() <= 0) {
            return;
        }

        // This frame will become the previous frame since it's as if the user is drawing on screen
        bitmapCopy(current, previousFrame);

        // Returns this change to the undo stack
        Integer[] nextChanges = compressedRedo.pop();
        compressedUndo.push(nextChanges);

        // Performs the redo operation to the current frame
        performUpdate(nextChanges, current);

        Log.d("UndoRedoTracker", "Redo took " + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * Updates a bitmap based on an array of compressed changes
     * This can be an undo change or a redo change
     **/
    private static void performUpdate(Integer[] compressedChanges, Bitmap currentFrame) {
        // Decodes the uncompressed changes
        encoder.decodeRunLength(compressedChanges, decompressedChanges);

        // Retrieves the new frame
        Bitmap newFrameBitmap = xor(currentFrame, decompressedChanges);

        // Updates the current frame
        bitmapCopy(newFrameBitmap, currentFrame);

        // Updates the previous frame (for use with the next drawing operation)
        bitmapCopy(newFrameBitmap, previousFrame);
    }

    /**
     * Returns the bitwise XOR of the lhs and rhs
     * // TODO: Ensure bitmap configurations are also the same.
     **/
    private static Bitmap xor(Bitmap lhs, Bitmap rhs) {
        long startTime = System.currentTimeMillis();

        /*sharedCanvas.setBitmap(xorBitmap);
        //sharedCanvas.drawColor(Color.YELLOW);
        sharedCanvas.drawBitmap(lhs, 0, 0, bitmapPaint);
        sharedCanvas.drawBitmap(rhs, 0, 0, xorPaint);*/

        for (int y = 0; y < xorBitmap.getHeight(); y++) {
            for (int x = 0; x < xorBitmap.getWidth(); x++) {
                int xorColour = (lhs.getPixel(x, y)) ^ (rhs.getPixel(x, y));
                int alphaXorColour = xorColour + OPAQUE_ALPHA_CHANNEL;
                xorBitmap.setPixel(x, y, alphaXorColour);
            }
        }

        Log.d("UndoRedoTracker", "XOR took " + (System.currentTimeMillis() - startTime) + "ms");

        return xorBitmap;
    }

    /**
     * Copies the pixels from a source bitmap onto a destination bitmap
     **/
    private static void bitmapCopy(Bitmap source, Bitmap destination) {
        sharedCanvas.setBitmap(destination);
        sharedCanvas.drawBitmap(source, 0, 0, bitmapPaint);
    }
}
