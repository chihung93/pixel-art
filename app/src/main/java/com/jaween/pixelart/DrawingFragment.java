package com.jaween.pixelart;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
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

public class DrawingFragment extends Fragment implements DrawingSurface.OnClearPanelsListener {

    private DrawingSurface surface;
    private Tool tool;
    private Pen pen;
    private FloodFill floodFill;
    private Oval oval;

    private OnClearPanelsListener onClearPanelsListener = null;

    public DrawingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pen = new Pen(getString(R.string.tool_pen));
        floodFill = new FloodFill(getString(R.string.tool_flood_fill));
        oval = new Oval(getString(R.string.tool_oval));
        tool = pen;

        setHasOptionsMenu(true);
    }

    public void setColour(int colour) {
        surface.setColour(colour);
    }

    public int getColour() {
        return surface.getColour();
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
            case R.id.action_tool_pen:
                // TODO: Proper tool switching
                surface.setTool(pen);
                Toast.makeText(getActivity(), "Switched to Pen", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_tool_flood_fill:
                surface.setTool(floodFill);
                Toast.makeText(getActivity(), "Switched to Flood Fill", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_tool_oval:
                surface.setTool(oval);
                Toast.makeText(getActivity(), "Switched to Oval", Toast.LENGTH_SHORT).show();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        surface = new DrawingSurface(getActivity(), tool);
        surface.setOnClearPanelsListener(this);
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

    public interface OnClearPanelsListener {
        public boolean onClearPanels();
    }
}
