package com.jaween.pixelart.util;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.util.LinkedList;

/**
 * Worker Fragment which holds onto large objects (such as the user Bitmaps or Undo stack) when the
 * device has a configuration change. The Activity will be recreated, but this fragment will be
 * retained and the newly recreated Fragment can retrieve the object.
 */
public class ConfigChangeFragment extends Fragment {

    private LinkedList<Bitmap> layers;
    private Bitmap ongoingOperationBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retains fragment between device configuration changes
        setRetainInstance(true);
    }

    public void setLayers(LinkedList<Bitmap> data) {
        this.layers = data;
    }

    public LinkedList<Bitmap> getLayers() {
        return layers;
    }

    public Bitmap getOngoingOperationBitmap() {
        return ongoingOperationBitmap;
    }

    public void setOngoingOperationBitmap(Bitmap ongoingOperationBitmap) {
        this.ongoingOperationBitmap = ongoingOperationBitmap;
    }
}