package com.jaween.pixelart.ui.undo;

import android.graphics.Bitmap;

import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.util.Encoder;

import java.util.LinkedList;

/**
 * Created by ween on 11/29/14.
 */
public class FrameUndoData {

    public static enum FrameOperation {
        ADD, DELETE, MOVE
    }

    public static final int NULL_INDEX = -1;
    private static Encoder encoder = new Encoder();

    private FrameOperation type = null;
    private int frameIndex = NULL_INDEX;
    private int fromIndex = NULL_INDEX;
    private int toIndex = NULL_INDEX;

    private LinkedList<Integer[]> compressedLayers = null;
    private Integer[] compressedCompositeBitmap = null;
    private int currentLayerIndex;
    private LinkedList<String> titles = null;
    private LinkedList<Boolean> visibilities;
    private LinkedList<Boolean> lockList;

    private int layerWidth;
    private int layerHeight;
    private Bitmap.Config config;

    /**
     * Used when adding or deleting a Frame. Stores the Frame's Layers in compressed form.
     * @param type Use either FrameOperaiton.ADD or FrameOperation.DELETE
     * @param frameIndex The index of the frame
     * @param frame The Frame being added or deleted
     */
    public FrameUndoData(FrameOperation type, int frameIndex, Frame frame) {
        this.type = type;
        this.frameIndex = frameIndex;

        // Gets the properties of the layers
        layerWidth = frame.getLayers().get(0).getImage().getWidth();
        layerHeight = frame.getLayers().get(0).getImage().getHeight();
        config = frame.getLayers().get(0).getImage().getConfig();

        compressedLayers = new LinkedList<>();
        titles = new LinkedList<>();
        visibilities = new LinkedList<>();
        lockList = new LinkedList<>();
        encoder.setBitmapDimensions(layerWidth, layerHeight);

        // Decomposes the layers and compresses the layer images
        for (int i = 0; i < frame.getLayers().size(); i++) {
            Layer layer = frame.getLayers().get(i);
            Integer[] compressedBitmap = encoder.encodeRunLength(layer.getImage());
            compressedLayers.add(compressedBitmap);
            titles.add(layer.getTitile());
            visibilities.add(layer.isVisible());
            lockList.add(layer.isLocked());
        }
        compressedCompositeBitmap = encoder.encodeRunLength(frame.getCompositeBitmap());
        currentLayerIndex = frame.getCurrentLayerIndex();
    }

    /**
     * Used when repositioning a Frame in the Frame list. Implicitly of type FrameOperation.MOVE.
     * @param fromIndex The index in the list that the Frame is coming from
     * @param toIndex The index in the list that the Frame is moving to
     */
    public FrameUndoData(int fromIndex, int toIndex) {
        this.type = FrameOperation.MOVE;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public FrameOperation getType() {
        return type;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    /**
     * Retrieves the Frame object with the Layers in their decompressed state.
     * @return The recomposed Frame instance
     */
    public Frame getFrame() {
        LinkedList<Layer> layers = new LinkedList<Layer>();
        for (int i = 0; i < compressedLayers.size(); i++) {
            Bitmap layerImage = Bitmap.createBitmap(layerWidth, layerHeight, config);
            encoder.decodeRunLength(compressedLayers.get(i), layerImage);

            String title = titles.get(i);
            boolean visibility = visibilities.get(i);
            boolean locked = lockList.get(i);

            // Recreates the layer object
            Layer layer = new Layer(layerImage, title);
            layer.setVisible(visibility);
            layer.setLocked(locked);

            layers.add(layer);
        }
        Bitmap compositeBitmap = Bitmap.createBitmap(layerWidth, layerHeight, config);
        encoder.decodeRunLength(compressedCompositeBitmap, compositeBitmap);

        Frame frame = new Frame(layers, compositeBitmap, currentLayerIndex);
        return frame;
    }
}