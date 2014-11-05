package com.jaween.pixelart.util;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;

/**
 * Worker Fragment which holds onto large objects (such as the user Bitmaps or Undo stack) when the
 * device has a configuration change. The Activity will be recreated, but this fragment will be
 * retained and the newly recreated Activity can retrieve the object.
 */
public class ConfigChangeFragment extends Fragment {

    private Bitmap layers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retains fragment between device configuration changes
        setRetainInstance(true);
    }

    public void setLayers(Bitmap data) {
        this.layers = data;
    }

    public Bitmap getLayers() {
        return layers;
    }
}