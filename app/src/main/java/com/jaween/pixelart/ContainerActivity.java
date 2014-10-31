package com.jaween.pixelart;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.jaween.pixelart.ui.PaletteFragment;

public class ContainerActivity extends Activity implements PaletteFragment.OnColourSelectedListener, DrawingFragment.OnClearPanelsListener {

    private PaletteFragment paletteFragment;
    private DrawingFragment drawingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_activity);
        if (savedInstanceState == null) {
            paletteFragment = new PaletteFragment();
            drawingFragment = new DrawingFragment();

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.container_panel, paletteFragment);
            fragmentTransaction.add(R.id.container_content, drawingFragment);
            fragmentTransaction.commit();

            drawingFragment.setOnClearPanelsListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Drawable drawable = getResources().getDrawable(R.drawable.palette_menu_item);
        drawable.setColorFilter(drawingFragment.getColour(), PorterDuff.Mode.MULTIPLY);

        MenuItem paletteItem = menu.findItem(R.id.action_palette);
        paletteItem.setIcon(drawable);
        paletteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_palette:
                togglePaletteFragment();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!onClearPanels()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onColourSelected(int colour, boolean done) {
        drawingFragment.setColour(colour);
        invalidateOptionsMenu();

        if (done) {
            hidePaletteFragment();
        }
    }

    private void togglePaletteFragment() {
        if (paletteFragment.isHidden()) {
            showPaletteFragment();
        } else {
            hidePaletteFragment();
        }
    }

    private boolean showPaletteFragment() {
        if (paletteFragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.slide_down, R.animator.slide_up, R.animator.slide_down, R.animator.slide_up);
            fragmentTransaction.show(paletteFragment);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }

    private boolean hidePaletteFragment() {
        if (!paletteFragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.slide_down, R.animator.slide_up, R.animator.slide_down, R.animator.slide_up);
            fragmentTransaction.hide(paletteFragment);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onClearPanels() {
        return hidePaletteFragment();
    }
}
