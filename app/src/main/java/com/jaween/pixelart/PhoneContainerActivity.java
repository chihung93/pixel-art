package com.jaween.pixelart;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.ui.PaletteFragment;

/**
 * BaseContainerActivity with specific implementation details for the phone UI (sliding panels
 * different callbacks)
 */
public class PhoneContainerActivity extends BaseContainerActivity implements DrawingFragment.OnClearPanelsListener {

    private Tool selectedTool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            drawingFragment.setOnClearPanelsListener(this);

            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.hide(toolboxFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.phone_drawing_menu, menu);

        MenuItem item = menu.findItem(R.id.action_tool);
        if (selectedTool != null) {
            item.setIcon(selectedTool.getIcon());
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Drawable drawable = getResources().getDrawable(R.drawable.palette_menu_item);
        drawable.setColorFilter(paletteFragment.getColour(), PorterDuff.Mode.MULTIPLY);

        MenuItem paletteItem = menu.findItem(R.id.action_palette);
        paletteItem.setIcon(drawable);
        paletteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tool:
                togglePanel(toolboxFragment);
                break;
            case R.id.action_palette:
                togglePanel(paletteFragment);
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
            hidePanel(paletteFragment);
        }
    }

    private boolean togglePanel(Fragment fragment) {
        // Dismisses any other panels that are in the way
        Fragment in, out;
        if (fragment instanceof PaletteFragment) {
            in = paletteFragment;
            out = toolboxFragment;
        } else {
            in = toolboxFragment;
            out = paletteFragment;
        }
        hidePanel(out);

        // Slides in the new panel
        if (in.isHidden()) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.slide_down, R.animator.slide_up, R.animator.slide_down, R.animator.slide_up);
            fragmentTransaction.show(in);
            fragmentTransaction.commit();
            return true;
        } else {
            // The panel was already being shown, slides it out
            hidePanel(in);
            return false;
        }
    }

    private boolean hidePanel(Fragment fragment) {
        if (!fragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.slide_down, R.animator.slide_up, R.animator.slide_down, R.animator.slide_up);
            fragmentTransaction.hide(fragment);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onClearPanels() {
        boolean didHide = hidePanel(paletteFragment) | hidePanel(toolboxFragment);
        return didHide;
    }

    @Override
    public void onToolSelected(Tool tool, boolean done) {
        super.onToolSelected(tool, done);

        // Updates the tool action on the ActionBar
        selectedTool = tool;
        invalidateOptionsMenu();

        // Dismisses the toolbox panel
        if (done) {
            onClearPanels();
        }
    }
}
