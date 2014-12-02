package com.jaween.pixelart.ui.undo;

/**
 * Created by ween on 11/29/14.
 */
public class DrawOpUndoData {

    private Integer[] compressedChanges;
    private int layerIndex;

    public DrawOpUndoData(Integer[] compressedChanges, int layerIndex) {
        this.compressedChanges = compressedChanges;
        this.layerIndex = layerIndex;

    }

    public Integer[] getCompressedChanges() {
        return compressedChanges;
    }

    public int getLayerIndex() {
        return layerIndex;
    }
}
