package com.jaween.pixelart.util;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.ui.undo.UndoManager;

import java.util.LinkedList;

/**
 * Worker Fragment which holds onto large objects (such as the layers or undo stack) when the
 * device has a configuration change. The Activity will be recreated, but this fragment will be
 * retained and the newly recreated Fragment can retrieve the object.
 */
public class ConfigChangeFragment extends Fragment {

    public static final String TAG_CONFIG_CHANGE_FRAGMENT = "config_change_fragment";

    private LinkedList<Layer> layers;
    private UndoManager undoManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retains fragment between device configuration changes
        setRetainInstance(true);
    }

    public LinkedList<Layer> getLayers() {
        return layers;
    }

    public void setLayers(LinkedList<Layer> layers) {
        this.layers = layers;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }
}