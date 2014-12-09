package com.jaween.pixelart.ui.undo;

/**
 * Created by ween on 11/29/14.
 */
public class DrawOpUndoData {

    private Integer[] compressedChanges;
    private int frameIndex;
    private int layerIndex;

    public DrawOpUndoData(Integer[] compressedChanges, int frameIndex, int layerIndex) {
        this.compressedChanges = compressedChanges;
        this.frameIndex = frameIndex;
        this.layerIndex = layerIndex;

    }

    public Integer[] getCompressedChanges() {
        return compressedChanges;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public int getLayerIndex() {
        return layerIndex;
    }
}
