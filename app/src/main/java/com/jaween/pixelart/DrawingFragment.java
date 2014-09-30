package com.jaween.pixelart;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    public void zoom(float relativeZoom) {
        surface.setScale(surface.getScale() + relativeZoom);
    }
}
