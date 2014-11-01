package com.jaween.pixelart;

import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.jaween.pixelart.util.SlidingLinearLayout;

/**
 * BaseContainerActivity with specific implementation details for the phone UI (sliding panels
 * different callbacks)
 */
public class PhoneContainerActivity extends BaseContainerActivity implements DrawingFragment.OnClearPanelsListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            drawingFragment.setOnClearPanelsListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phone_drawing_menu, menu);
        return true;
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
        super.onColourSelected(colour, done);

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
