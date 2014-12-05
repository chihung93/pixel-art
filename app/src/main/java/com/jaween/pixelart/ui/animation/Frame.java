package com.jaween.pixelart.ui.animation;

import com.jaween.pixelart.ui.layer.Layer;

import java.util.LinkedList;

/**
 * Created by ween on 12/6/14.
 */
public class Frame {

    private LinkedList<Layer> layers;
    private int currentLayerIndex;

    public Frame(LinkedList<Layer> layers, int currentLayerIndex) {
        this.layers = layers;
        this.currentLayerIndex = currentLayerIndex;
    }

    public LinkedList<Layer> getLayers() {
        return layers;
    }

    public void setLayers(LinkedList<Layer> layers) {
        this.layers = layers;
    }

    public int getCurrentLayerIndex() {
        return currentLayerIndex;
    }

    public void setCurrentLayerIndex(int currentLayerIndex) {
        this.currentLayerIndex = currentLayerIndex;
    }
}
