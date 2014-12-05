package com.jaween.pixelart.ui;

import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jaween.pixelart.ContainerActivity;
import com.jaween.pixelart.ContainerFragment;
import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.ui.undo.UndoManager;
import com.jaween.pixelart.util.ConfigChangeFragment;

import java.util.LinkedList;

public class DrawingFragment extends Fragment implements
        DrawingSurface.OnClearPanelsListener,
        DrawingSurface.OnDimensionsCalculatedListener,
        DrawingSurface.OnSelectRegionListener,
        ActionMode.Callback {

    // Main SurfaceView on which to draw
    private DrawingSurface surface;

    // Callbacks
    private OnClearPanelsListener onClearPanelsListener = null;
    private OnDimensionsCalculatedListener onDimensionsCalculatedListener = null;
    private ActionMode.Callback actionModeCallback = this;

    // Regular UI state
    private ActionMode actionMode = null;
    private Tool selectedTool;

    // Viewport save-state
    private static final String KEY_CENTER_X = "key_center_x";
    private static final String KEY_CENTER_Y = "key_center_y";
    private static final String KEY_SCALE = "key_scale";

    // UI save-state
    private static final String KEY_CURRENT_LAYER = "key_current_layer";
    private static final String KEY_GRID = "key_grid";

    // Config change
    private ConfigChangeFragment configChangeWorker;

    public DrawingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        surface = new DrawingSurface(getActivity(), selectedTool);
        surface.setOnClearPanelsListener(this);
        surface.setOnDimensionsCalculatedListener(this);
        surface.setOnSelectRegionListener(this);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        configChangeWorker = (ConfigChangeFragment) fragmentManager.
                findFragmentByTag(ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);

        if (configChangeWorker == null) {
            // Worker doesn't exist, creates new worker
            configChangeWorker = new ConfigChangeFragment();
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(configChangeWorker, ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);
            fragmentTransaction.commit();
        } else {
            // Restores the layers
            LinkedList<Layer> layers = configChangeWorker.getLayers();
            if (layers != null) {
                surface.setLayers(configChangeWorker.getLayers());
            }
        }

        // Retrieves the undo manager for undoing and redoing drawing commands
        UndoManager undoManager = ((ContainerFragment) getParentFragment()).getUndoManager();
        surface.setUndoManager(undoManager);

        onRestoreInstanceState(savedInstanceState);

        return surface;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If state is saved immediately after start up, the surface may not have been created yet
        if (surface.isSurfaceCreated()) {
            // Saves the layers
            configChangeWorker.setLayers(surface.getLayers());

            // Saves the current layer index
            outState.putInt(KEY_CURRENT_LAYER, surface.getCurrentLayerIndex());

            // Saves viewport
            RectF viewport = surface.getViewport();
            outState.putFloat(KEY_CENTER_X, viewport.left + viewport.width() / 2);
            outState.putFloat(KEY_CENTER_Y, viewport.top + viewport.height() / 2);
            outState.putFloat(KEY_SCALE, surface.getScale());

            // Saves the state of the grid
            outState.putBoolean(KEY_GRID, surface.isGridEnabled());
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            surface.setConfigurationChanged(true);

            // Restores the current layer index
            surface.setCurrentLayerIndex(savedInstanceState.getInt(KEY_CURRENT_LAYER, 0));

            // Restores viewport
            float DEFAULT_CENTER_X = 0;
            float DEFAULT_CENTER_Y = 0;
            float centerX = savedInstanceState.getFloat(KEY_CENTER_X, DEFAULT_CENTER_X);
            float centerY = savedInstanceState.getFloat(KEY_CENTER_Y, DEFAULT_CENTER_Y);
            float scale = savedInstanceState.getFloat(KEY_SCALE, DrawingSurface.DEFAULT_SCALE);
            surface.restoreViewport(centerX, centerY, scale);

            // Restores the grid
            boolean DEFAULT_GRID = false;
            boolean gridEnabled = savedInstanceState.getBoolean(KEY_GRID, DEFAULT_GRID);
            surface.setGridEnabled(gridEnabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        surface.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        surface.onPause();
    }

    public void setTool(Tool tool) {
        if (selectedTool != tool) {
            selectedTool = tool;
            if (surface != null) {
                surface.setTool(tool);
            }

            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    public void setLayers(LinkedList<Layer> layers) {
        surface.setLayers(layers);
    }

    public void setCurrentLayer(int i) {
        surface.setCurrentLayerIndex(i);
    }

    public void setUndoManager(UndoManager undoManager) {
        surface.setUndoManager(undoManager);
    }

    public void undo(Object undoData) {
        surface.undo(undoData);
    }

    public void redo(Object redoData) {
        surface.redo(redoData);
    }

    public void invalidate() {
        surface.invalidate();
    }

    public boolean isGridEnabled() {
        return surface.isGridEnabled();
    }

    public void setGridEnabled(boolean enabled) {
        surface.setGridEnabled(enabled);
    }


    public void setOnClearPanelsListener(OnClearPanelsListener onClearPanelsListener) {
        this.onClearPanelsListener = onClearPanelsListener;
    }

    @Override
    public void onClearPanels() {
        if (onClearPanelsListener != null) {
            onClearPanelsListener.onClearPanels();
        }
    }

    @Override
    public void onDimensionsCalculated(int width, int height) {
        if (onDimensionsCalculatedListener != null) {
            onDimensionsCalculatedListener.onDimensionsCalculated(width, height);
        }
    }

    @Override
    public void onSelectRegion(Path selectedRegion) {
        // Starts the Contextual Action Bar if there isn't one already there (i.e. actionMode == null)
        if (actionMode == null) {
            ((ContainerActivity) getActivity()).getToolbar().startActionMode(actionModeCallback);
        }
    }

    public void setOnDimensionsCalculatedListener(OnDimensionsCalculatedListener onDimensionsCalculatedListener) {
        this.onDimensionsCalculatedListener = onDimensionsCalculatedListener;
    }

    // Passed along to the DrawingSurface
    public void setOnDropColourListener(DrawingSurface.OnDropColourListener onDropColourListener) {
        surface.setOnDropColourListener(onDropColourListener);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.selection_menu, menu);
        this.actionMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_select_all:
                surface.selectAll();
                break;
            case R.id.action_cut:
                surface.clearSelection();
                Toast.makeText(getActivity().getApplicationContext(), "TODO: Layer support", Toast.LENGTH_SHORT).show();
                surface.dismissSelection();
                actionMode.finish();
                break;
            case R.id.action_copy:
                surface.clearSelection();
                Toast.makeText(getActivity().getApplicationContext(), "TODO: Layer support", Toast.LENGTH_SHORT).show();
                surface.dismissSelection();
                actionMode.finish();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        surface.dismissSelection();
        this.actionMode = null;
    }

    public interface OnDimensionsCalculatedListener {
        public void onDimensionsCalculated(int width, int height);
    }

    public interface OnClearPanelsListener {
        public boolean onClearPanels();
    }
}
