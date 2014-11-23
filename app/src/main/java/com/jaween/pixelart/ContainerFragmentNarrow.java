package com.jaween.pixelart;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.ui.DrawingFragment;
import com.jaween.pixelart.ui.DrawingSurface;
import com.jaween.pixelart.ui.PaletteFragment;

/**
 * BaseContainerFragment with specific implementation details for a narrow UI (sliding panels,
 * different callbacks)
 */
public class ContainerFragmentNarrow extends ContainerFragment implements
        DrawingFragment.OnClearPanelsListener,
        DrawingSurface.OnDropColourListener,
        PaletteFragment.OnShowColourPaletteListener,
        ActionMode.Callback {

    private Tool selectedTool;
    private Drawable[] paletteMenuItemLayers = new Drawable[2];

    private ActionMode actionMode = null;
    private ActionMode.Callback actionModeCallback = this;

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
        drawingFragment.setOnDropColourListener(this);
        paletteFragment.setOnShowColourPaletteListener(this);

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

        // Tints the inner square to the selected colour
        Drawable colouredDrawable = getResources().getDrawable(R.drawable.palette_menu_item);
        colouredDrawable.setColorFilter(paletteFragment.getColour(), PorterDuff.Mode.MULTIPLY);

        // The white border
        Drawable borderDrawable;
        borderDrawable = getResources().getDrawable(R.drawable.palette_menu_item_border);
        if (paletteFragment.getColour() == Color.WHITE) {
            // The selected colour is white, darkens the border slightly
            borderDrawable.setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        }

        // Layers the two elements
        paletteMenuItemLayers[0] = borderDrawable;
        paletteMenuItemLayers[1] = colouredDrawable;
        LayerDrawable layerDrawable = new LayerDrawable(paletteMenuItemLayers);

        // Sets the menu item
        MenuItem paletteItem = menu.findItem(R.id.action_palette);
        paletteItem.setIcon(layerDrawable);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Consumes items clicks as the CAB is animating in
        if (actionMode != null) {
            return true;
        }

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
        if (actionMode == null) {
            return onClearPanels();
        } else {
            return false;
        }
    }

    @Override
    public void onPrimaryColourSelected(int colour, boolean done, boolean fromPalette) {
        super.onPrimaryColourSelected(colour, done, fromPalette);
        
        getActivity().supportInvalidateOptionsMenu();

        if (done) {
            hidePanel(paletteFragment);
        }
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
        getActivity().supportInvalidateOptionsMenu();

        // Dismisses the toolbox panel
        if (done) {
            onClearPanels();
        }
    }

    @Override
    public void onDropColour(int colour) {
        onPrimaryColourSelected(colour, false, false);
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
    public void onToggleColourPalette(boolean visible) {
        if (visible) {
            if (actionMode == null) {
                ((ActionBarActivity) getActivity()).startSupportActionMode(actionModeCallback);
            }
        } else {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.show(drawingFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onColourPaletteAnimationEnd() {
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.hide(drawingFragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.colour_picker_menu, menu);
        this.actionMode = actionMode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_add:
                // TODO: Adding palettes
                break;
            case R.id.action_delete:
                // TODO: Deleting palettes
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        paletteFragment.hideColourPicker();
        onToggleColourPalette(false);
        this.actionMode = null;
    }
}
