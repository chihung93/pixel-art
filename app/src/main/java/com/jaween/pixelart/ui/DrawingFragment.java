package com.jaween.pixelart.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jaween.pixelart.ContainerActivity;
import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.util.ConfigChangeFragment;

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
    private static final String KEY_GRID = "key_grid";
    private static final String TAG_CONFIG_CHANGE_FRAGMENT = "config_change_fragment";
    private ConfigChangeFragment configChangeWorker;

    public DrawingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        surface = new DrawingSurface(getActivity(), selectedTool);
        surface.setOnClearPanelsListener(this);
        surface.setOnDimensionsCalculatedListener(this);
        surface.setOnSelectRegionListener(this);

        // Worker fragment to save data across device configuration changes
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        configChangeWorker = (ConfigChangeFragment) fragmentManager.findFragmentByTag(TAG_CONFIG_CHANGE_FRAGMENT);

        if (configChangeWorker == null) {
            // Worker doesn't exist, creates new worker
            configChangeWorker = new ConfigChangeFragment();
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(configChangeWorker, TAG_CONFIG_CHANGE_FRAGMENT);
            fragmentTransaction.commit();
        } else {
            // Restores user drawn layers
            Bitmap restoredBitmap = configChangeWorker.getLayers();
            surface.setRestoredLayers(restoredBitmap);
            configChangeWorker.setLayers(null);
        }

        onRestoreInstanceState(savedInstanceState);

        return surface;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If state is saved immediately after start up, the surface may not have been created yet
        if (surface.isSurfaceCreated()) {
            // Saves user's drawing
            configChangeWorker.setLayers(surface.getLayers());

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Empties menu of all items (e.g. from before a rotation)
        //menu.clear();

        inflater.inflate(R.menu.drawing_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo:
                surface.undo();
                break;
            case R.id.action_redo:
                surface.redo();
                break;
            case R.id.action_grid:
                // Toggles grid
                Drawable gridIcon;
                if (surface.isGridEnabled()) {
                    gridIcon = getResources().getDrawable(R.drawable.ic_action_grid_off);
                } else {
                    gridIcon = getResources().getDrawable(R.drawable.ic_action_grid);
                }
                item.setIcon(gridIcon);
                surface.setGridEnabled(!surface.isGridEnabled());
                break;
            default:
                return false;
        }
        return true;
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
            ((ActionBarActivity) getActivity()).startSupportActionMode(actionModeCallback);
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
            case R.id.action_copy:
                surface.clearSelection();
                break;
            case R.id.action_cut:
                surface.clearSelection();
                break;
            default:
                return false;
        }
        Toast.makeText(getActivity().getApplicationContext(), "TODO: Layer support", Toast.LENGTH_SHORT).show();
        surface.dismissSelection();
        actionMode.finish();
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
