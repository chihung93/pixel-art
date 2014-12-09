package com.jaween.pixelart.util;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.ui.undo.UndoManager;

import java.util.LinkedList;

/**
 * Worker Fragment which holds onto large objects (such as the Frames or undo stack) when the
 * device has a configuration change. The Activity will be recreated, but this fragment will be
 * retained and the newly recreated Fragment can retrieve the object.
 */
public class ConfigChangeFragment extends Fragment {

    public static final String TAG_CONFIG_CHANGE_FRAGMENT = "config_change_fragment";

    private LinkedList<Frame> frames;
    private UndoManager undoManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retains fragment between device configuration changes
        setRetainInstance(true);
    }

    public LinkedList<Frame> getFrames() {
        return frames;
    }

    public void setFrames(LinkedList<Frame> frames) {
        this.frames= frames;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }
}