package com.jaween.pixelart;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.jaween.pixelart.ui.PaletteFragment;

/**
 * Parent container class for the main screen of the app: the drawing canvas and the tool panels.
 */
public class BaseContainerActivity extends Activity implements PaletteFragment.OnColourSelectedListener {

    protected DrawingFragment drawingFragment;
    protected PaletteFragment paletteFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.container_activity);

        drawingFragment = new DrawingFragment();
        paletteFragment = new PaletteFragment();

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.container_content, drawingFragment);
        fragmentTransaction.add(R.id.container_panel, paletteFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onColourSelected(int colour, boolean done) {
        drawingFragment.setColour(colour);
    }
}
