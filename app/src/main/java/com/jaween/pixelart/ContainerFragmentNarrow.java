package com.jaween.pixelart;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.ui.DrawingFragment;
import com.jaween.pixelart.ui.PaletteFragment;

/**
 * BaseContainerFragment with specific implementation details for a narrow UI (sliding panels,
 * different callbacks)
 */
public class ContainerFragmentNarrow extends ContainerFragment implements
        DrawingFragment.OnClearPanelsListener {

    private Tool selectedTool;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        drawingFragment.setOnClearPanelsListener(this);

        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        //fragmentTransaction.hide(paletteFragment);
        fragmentTransaction.hide(toolboxFragment);
        fragmentTransaction.commit();

        return view;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Empties menu of all items (e.g. from before a rotation)
        menu.clear();
        inflater.inflate(R.menu.drawing_menu_narrow, menu);

        // Sets the icon of the tool menu item
        MenuItem item = menu.findItem(R.id.action_tool);
        if (selectedTool != null) {
            item.setIcon(selectedTool.getIcon());
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Drawable drawable = getResources().getDrawable(R.drawable.palette_menu_item);
        drawable.setColorFilter(paletteFragment.getColour(), PorterDuff.Mode.MULTIPLY);

        MenuItem paletteItem = menu.findItem(R.id.action_palette);
        paletteItem.setIcon(drawable);
        paletteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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

    // Returns true if a panels was made hidden, false otherwise
    @Override
    public boolean onBackPressed() {
        return onClearPanels();
    }

    @Override
    public void onColourSelected(int colour, boolean done) {
        super.onColourSelected(colour, done);

        getActivity().invalidateOptionsMenu();

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
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_down, R.anim.slide_up, R.anim.slide_down, R.anim.slide_up);
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
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_down, R.anim.slide_up, R.anim.slide_down, R.anim.slide_up);
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
        getActivity().invalidateOptionsMenu();

        // Dismisses the toolbox panel
        if (done) {
            onClearPanels();
        }
    }
}
