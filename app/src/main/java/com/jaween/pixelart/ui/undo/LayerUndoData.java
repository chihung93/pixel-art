package com.jaween.pixelart.ui.undo;

import android.graphics.Bitmap;

import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.util.Encoder;

/**
 * Created by ween on 11/29/14.
 */
public class LayerUndoData {

    public static enum LayerOperation {
        ADD, DELETE, MOVE, MERGE
    }

    public static final int NULL_INDEX = -1;
    private static Encoder encoder = new Encoder();

    private LayerOperation type = null;
    private int layerIndex = NULL_INDEX;
    private int fromIndex = NULL_INDEX;
    private int toIndex = NULL_INDEX;

    private Integer[] compressedLayer = null;
    private String title = null;
    private boolean visibility;
    private boolean locked;

    private int layerWidth;
    private int layerHeight;
    private Bitmap.Config config;

    /**
     * Used when adding or deleting a layer
     * @param layerIndex The index of the layer
     * @param layer The layer being added or deleted
     */
    public LayerUndoData(LayerOperation type, int layerIndex, Layer layer) {
        this.type = type;
        this.layerIndex = layerIndex;

        // Gets the properties of the layer image
        layerWidth = layer.getImage().getWidth();
        layerHeight = layer.getImage().getHeight();
        config = layer.getImage().getConfig();

        // Compresses the layer image and decomposes the rest of the layer
        encoder.setBitmapDimensions(layerWidth, layerHeight);
        compressedLayer = encoder.encodeRunLength(layer.getImage());
        title = layer.getTitile();
        visibility = layer.isVisible();
        locked = layer.isLocked();
    }

    /**
     * Used when repositioning a layer in the layer list
     * @param fromIndex The index in the list that the layer is coming from
     * @param toIndex The index in the list that the layer is moving to
     */
    public LayerUndoData(int fromIndex, int toIndex) {
        this.type = LayerOperation.MOVE;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public LayerOperation getType() {
        return type;
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    public Layer getLayer() {
        // Decompresses the bitmap
        Bitmap layerImage = Bitmap.createBitmap(layerWidth, layerHeight, config);
        encoder.decodeRunLength(compressedLayer, layerImage);

        // Recreates the layer object
        Layer layer = new Layer(layerImage, title);
        layer.setVisible(visibility);
        layer.setLocked(locked);

        return layer;
    }

}
