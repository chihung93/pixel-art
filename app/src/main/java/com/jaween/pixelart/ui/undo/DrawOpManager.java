package com.jaween.pixelart.ui.undo;

import android.graphics.Bitmap;

import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.util.BitmapEncoder;

import java.util.List;

/**
 * Used by first constructing the object with the initial state Bitmap. Prior to an undoable
 * drawing occurring you must pass the Bitmap in switchLayer(). After the drawing
 * call add(), this will find the differences between the new frame and the previous frame and store
 * the compressed result. The undo() function will then decompress that result and modify the input
 * to return it to what it was before the drawing operation.
 */
public class DrawOpManager {

    // Bitmaps and compression
    private Bitmap layerBeforeModification = null;
    private final BitmapEncoder bitmapEncoder = new BitmapEncoder();

    // Bitmap pixel arrays
    private int[] lhsPixelArray = null;
    private int[] rhsPixelArray = null;
    private int[] xorArray = null;

    // Layer dimensions
    private int layerWidth;
    private int layerHeight;

    public DrawOpManager(int layerWidth, int layerHeight, Bitmap.Config config) {
        // Used for storing a copy of the previous frame
        layerBeforeModification = Bitmap.createBitmap(layerWidth, layerHeight, config);

        // Holds pixel data (manipulations are faster than individual Bitmap.setPixel() calls)
        lhsPixelArray = new int[layerWidth * layerHeight];
        rhsPixelArray = new int[layerWidth * layerHeight];
        xorArray = new int[layerWidth * layerHeight];

        // Allocates memory for pixel data in the compression function
        bitmapEncoder.setBitmapDimensions(layerWidth, layerHeight);

        // Layer dimensions
        this.layerWidth = layerWidth;
        this.layerHeight = layerHeight;
    }

    /**
     * Keeps a pre-draw snapshot of the layer
     * @param currentLayer The new current layer
     */
    public void switchLayer(Bitmap currentLayer) {
        bitmapCopy(currentLayer, layerBeforeModification);
    }

    public UndoItem add(Bitmap currentLayer, int frameIndex, int layerIndex) {
        // Finds the changes between the bitmaps
        xorArray = xor(layerBeforeModification, currentLayer);

        // Compresses these differences
        Integer[] encodedChanges = bitmapEncoder.encodeRunLength(xorArray);

        // Creates an UndoItem
        DrawOpUndoData undoData = new DrawOpUndoData(encodedChanges, frameIndex, layerIndex);
        UndoItem undoItem = new UndoItem(UndoItem.Type.DRAW_OP, 0, undoData);

        // Keeps a copy of this frame for future undos/redos
        bitmapCopy(currentLayer, layerBeforeModification);

        return undoItem;
    }

    public void undo(List<Frame> frames, int currentFrameIndex, int currentLayerIndex, DrawOpUndoData undoData) {
        // The changes to be undone
        Integer[] previousChanges = undoData.getCompressedChanges();

        // Retrieves the layer which was modified
        int modifiedFrameIndex = undoData.getFrameIndex();
        int modifiedLayerIndex = undoData.getLayerIndex();
        Bitmap modifiedLayer = frames
                .get(modifiedFrameIndex)
                .getLayers()
                .get(modifiedLayerIndex)
                .getImage();

        // Rolls back the change
        performUpdate(previousChanges, modifiedLayer);

        // Keeps a copy of the currently selected layer for future undos/redos
        Bitmap currentLayer = frames
                .get(currentFrameIndex)
                .getLayers()
                .get(currentLayerIndex)
                .getImage();
        bitmapCopy(currentLayer, layerBeforeModification);
    }

    public void redo(List<Frame> frames, int currentFrameIndex, int currentLayerIndex, DrawOpUndoData redoData) {
        // Retrieves the layer which needs to be modified
        int frameToModifyIndex = redoData.getFrameIndex();
        int layerToModifyIndex = redoData.getLayerIndex();
        Bitmap layerToModify = frames
                .get(frameToModifyIndex)
                .getLayers()
                .get(layerToModifyIndex)
                .getImage();

        // Returns this change to the undo stack
        Integer[] nextChanges = redoData.getCompressedChanges();

        // Performs the redo operation to the current frame
        performUpdate(nextChanges, layerToModify);

        // Keeps a copy of the currently selected layer for future undos/redos
        Bitmap currentLayer = frames
                .get(currentFrameIndex)
                .getLayers()
                .get(currentLayerIndex)
                .getImage();
        bitmapCopy(currentLayer, layerBeforeModification);
    }

    /**
     * Updates a bitmap based on an array of compressed changes
     * This can be an undo change or a redo change
     **/
    private void performUpdate(Integer[] compressedChanges, Bitmap destination) {
        // Decodes the uncompressed changes
        bitmapEncoder.decodeRunLength(compressedChanges, xorArray);

        // Retrieves the new frame
        xor(destination, xorArray);

        // Updates the previous frame (for use with the next drawing operation)
        bitmapCopy(destination, layerBeforeModification);
    }

    /**
     * Copies the pixels from a source Bitmap into a destination Bitmap.
     **/
    private void bitmapCopy(Bitmap source, Bitmap destination) {
        source.getPixels(lhsPixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);
        destination.setPixels(lhsPixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);
    }

    /**
     * Finds the bitwise XOR of the two bitmaps (i.e. the differences between them).
     * @param lhs The left hand side Bitmap
     * @param rhs The right hand side Bitmap
     * @return An array containing the XOR'd pixels
     **/
    private int[] xor(Bitmap lhs, Bitmap rhs) {
        // Retrieves the pixel data of both bitmaps into the two pixel arrays
        lhs.getPixels(lhsPixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);
        rhs.getPixels(rhsPixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);

        // XOR's the bitmaps together
        return xor(lhsPixelArray, rhsPixelArray);
    }


    /**
     * Finds the bitwise XOR of the two parameters (i.e. the differences between them) and stores
     * the result in the lhs Bitmap.
     * @param lhs The left hand side Bitmap and destination of the result
     * @param rhs The right hand side's pixels
     **/
    private void xor(Bitmap lhs, int[] rhs) {
        // Retrieves the pixel data of the bitmap
        lhs.getPixels(lhsPixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);

        // XOR's the parameters together
        xorArray = xor(lhsPixelArray, rhs);

        // Loads the result into the Bitmap
        lhs.setPixels(xorArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);
    }

    /**
     * Performs a bitwise XOR on the elements of two arrays.
     * @param lhs The left hand side operand
     * @param rhs The right hand side operand
     * @return An array containing the XOR'd values
     */
    private int[] xor(int[] lhs, int[] rhs) {
        // Iterates over the pixels and XORs them together
        for (int y = 0; y < layerHeight; y++) {
            for (int x = 0; x < layerWidth; x++) {
                int index = x + y * layerWidth;
                xorArray[index] = lhs[index] ^ rhs[index];
            }
        }
        return xorArray;
    }
}