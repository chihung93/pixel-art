package com.jaween.pixelart;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jaween.pixelart.tools.FloodFill;
import com.jaween.pixelart.tools.Oval;
import com.jaween.pixelart.tools.Pen;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.ui.ToolboxFragment;

public class DrawingFragment extends Fragment implements
        DrawingSurface.OnClearPanelsListener,
        DrawingSurface.OnDimensionsCalculatedListener {

    private DrawingSurface surface;

    private OnClearPanelsListener onClearPanelsListener = null;
    private OnDimensionsCalculatedListener onDimensionsCalculatedListener = null;

    private Tool selectedTool;

    public DrawingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    public void setTool(Tool tool) {
        selectedTool = tool;
        if (surface != null) {
            surface.setTool(tool);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
                surface.toggleGrid();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        surface = new DrawingSurface(getActivity(), selectedTool);
        surface.setOnClearPanelsListener(this);
        surface.setOnDimensionsCalculatedListener(this);
        return surface;
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
        onDimensionsCalculatedListener.onDimensionsCalculated(width, height);
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
