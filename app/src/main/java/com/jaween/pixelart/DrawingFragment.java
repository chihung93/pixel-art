package com.jaween.pixelart;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jaween.pixelart.tools.FloodFill;
import com.jaween.pixelart.tools.Pen;
import com.jaween.pixelart.tools.Tool;

public class DrawingFragment extends Fragment {

    private DrawingSurface surface;
    private Tool tool;
    private Pen pen;
    private FloodFill floodFill;

    public DrawingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pen = new Pen(getActivity());
        floodFill = new FloodFill(getActivity());
        tool = pen;

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.drawing_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tool_pen:
                // TODO: Proper tool switching
                surface.setTool(pen);
                Toast.makeText(getActivity(), "Switched to Pen", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_tool_flood_fill:
                surface.setTool(floodFill);
                Toast.makeText(getActivity(), "Switched to Flood Fill", Toast.LENGTH_SHORT).show();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        surface = new DrawingSurface(getActivity(), tool);
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
}
