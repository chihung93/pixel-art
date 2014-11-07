package com.jaween.pixelart.ui;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.util.ConfigChangeFragment;

public class DrawingFragment extends Fragment implements
        DrawingSurface.OnClearPanelsListener,
        DrawingSurface.OnDimensionsCalculatedListener {

    private DrawingSurface surface;

    private OnClearPanelsListener onClearPanelsListener = null;
    private OnDimensionsCalculatedListener onDimensionsCalculatedListener = null;

    private Tool selectedTool;

    private static final String KEY_CENTER_X = "key_center_x";
    private static final String KEY_CENTER_Y = "key_center_y";
    private static final String KEY_SCALE = "key_scale";

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

        if (savedInstanceState != null) {
            surface.setConfigurationChanged(true);

            // Restores viewport
            float DEFAULT_CENTER_X = 0;
            float DEFAULT_CENTER_Y = 0;
            float centerX = savedInstanceState.getFloat(KEY_CENTER_X, DEFAULT_CENTER_X);
            float centerY = savedInstanceState.getFloat(KEY_CENTER_Y, DEFAULT_CENTER_Y);
            float scale = savedInstanceState.getFloat(KEY_SCALE, DrawingSurface.DEFAULT_SCALE);
            surface.restoreViewport(centerX, centerY, scale);

            boolean DEFAULT_GRID = false;
            boolean gridEnabled = savedInstanceState.getBoolean(KEY_GRID, DEFAULT_GRID);
            surface.setGridEnabled(gridEnabled);
        }

        return surface;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

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
        selectedTool = tool;
        if (surface != null) {
            surface.setTool(tool);
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

    public void setOnDimensionsCalculatedListener(OnDimensionsCalculatedListener onDimensionsCalculatedListener) {
        this.onDimensionsCalculatedListener = onDimensionsCalculatedListener;
    }

    public interface OnDimensionsCalculatedListener {
        public void onDimensionsCalculated(int width, int height);
    }

    public interface OnClearPanelsListener {
        public boolean onClearPanels();
    }
}
