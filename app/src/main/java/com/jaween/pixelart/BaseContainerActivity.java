package com.jaween.pixelart;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.ui.PaletteFragment;
import com.jaween.pixelart.ui.ToolboxFragment;

/**
 * Parent container class for the main screen of the app: the drawing canvas and the tool panels.
 */
public class BaseContainerActivity extends Activity implements
        PaletteFragment.OnColourSelectedListener,
        ToolboxFragment.OnToolSelectListener,
        DrawingFragment.OnDimensionsCalculatedListener {

    protected DrawingFragment drawingFragment;
    protected PaletteFragment paletteFragment;
    protected ToolboxFragment toolboxFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_activity);

        drawingFragment = new DrawingFragment();
        paletteFragment = new PaletteFragment();
        toolboxFragment = new ToolboxFragment();

        drawingFragment.setOnDimensionsCalculatedListener(this);
        toolboxFragment.setOnToolSelectListener(this);

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.fl_container_content, drawingFragment);
        fragmentTransaction.add(R.id.fl_container_palette, paletteFragment);
        fragmentTransaction.add(R.id.fl_container_toolbox, toolboxFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onColourSelected(int colour, boolean done) {
        drawingFragment.setColour(colour);
    }

    @Override
    public void onToolSelected(Tool tool, boolean done) {
        drawingFragment.setTool(tool);
    }

    @Override
    public void onDimensionsCalculated(int width, int height) {
        toolboxFragment.setDimensions(width, height);
    }
}
