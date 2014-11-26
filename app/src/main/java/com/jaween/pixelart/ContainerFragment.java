package com.jaween.pixelart;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
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
import com.jaween.pixelart.ui.ToolboxFragment;
import com.jaween.pixelart.ui.colourpicker.Color;

/**
 * Base container class for the main screen of the app: the drawing canvas and the tool panels.
 */
public class ContainerFragment extends Fragment implements
        PaletteFragment.OnPrimaryColourSelectedListener,
        ToolboxFragment.OnToolSelectListener,
        DrawingFragment.OnDimensionsCalculatedListener,
        DrawingSurface.OnDropColourListener,
        PaletteFragment.OnShowPaletteListener,
        ActionMode.Callback {

    // Child Fragments
    private DrawingFragment drawingFragment;
    private PanelManagerFragment panelManagerFragment;
    private PaletteFragment paletteFragment;
    private ToolboxFragment toolboxFragment;

    // Fragment tags
    private static final String TAG_DRAWING_FRAGMENT = "tag_drawing_fragment";
    private static final String TAG_PANEL_MANAGER_FRAGMENT = "tag_panel_manager_fragment";

    // Contextual ActionBar (for selection and the ColourPicker)
    private ActionMode actionMode = null;
    private ActionMode.Callback actionModeCallback = this;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.container_fragment, null);

        FragmentManager fragmentManager = getChildFragmentManager();
        drawingFragment = (DrawingFragment) fragmentManager.findFragmentByTag(TAG_DRAWING_FRAGMENT);
        panelManagerFragment = (PanelManagerFragment) fragmentManager.findFragmentByTag(TAG_PANEL_MANAGER_FRAGMENT);

        if (drawingFragment == null | panelManagerFragment == null) {
            // Fragments don't yet exist, creates them
            drawingFragment = new DrawingFragment();
            panelManagerFragment = new PanelManagerFragment();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_container_drawing, drawingFragment, TAG_DRAWING_FRAGMENT);
            fragmentTransaction.add(R.id.fl_container_panels, panelManagerFragment, TAG_PANEL_MANAGER_FRAGMENT);

            fragmentTransaction.commit();
        }

        return view;
    }

    // Called after PanelManagerFragments's child Fragments have been created
    @Override
    public void onStart() {
        super.onStart();

        // Gets fragments from the PanelManagerFragment in order to manage callbacks
        paletteFragment = panelManagerFragment.getPaletteFragment();
        toolboxFragment = panelManagerFragment.getToolboxFragment();

        // Fragment callbacks
        drawingFragment.setOnDimensionsCalculatedListener(this);
        drawingFragment.setOnDropColourListener(this);
        drawingFragment.setOnClearPanelsListener(panelManagerFragment);
        paletteFragment.setOnPrimaryColourSelectedListener(this);
        toolboxFragment.setOnToolSelectListener(this);

        // Initial tool
        drawingFragment.setTool(toolboxFragment.getTool());
        toolboxFragment.setColour(paletteFragment.getPrimaryColour());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflates the base menu (undo, grid, settings, etc.)
        inflater.inflate(R.menu.drawing_menu_narrow, menu);

        // Sets the icon of the tool menu item
        MenuItem item = menu.findItem(R.id.action_tool);
        item.setIcon(toolboxFragment.getTool().getIcon());
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Tints the inner square to the selected colour
        Drawable colouredInner = getResources().getDrawable(R.drawable.palette_menu_item);
        Drawable border = getResources().getDrawable(R.drawable.palette_menu_item_border);
        int colour = paletteFragment.getPrimaryColour();
        LayerDrawable layerDrawable = Color.tintAndLayerDrawable(colouredInner, border, colour);

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
                panelManagerFragment.togglePanel(toolboxFragment);
                break;
            case R.id.action_palette:
                panelManagerFragment.togglePanel(paletteFragment);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onPrimaryColourSelected(int colour, boolean done, boolean fromPalette) {
        // The tool must be notified of the colour change in order to have any effect
        toolboxFragment.setColour(colour);

        // Notifies the palette if the colour change originated from a tool
        // (avoids a circular call cycle if the palette notified itself)
        if (!fromPalette) {
            paletteFragment.setColourButton(colour);
        }

        // Hides the panels on narrow and wide layouts and updates the menu item
        getActivity().supportInvalidateOptionsMenu();
        if (done) {
            panelManagerFragment.hidePanel(paletteFragment);
        }
    }

    @Override
    public void onToolSelected(Tool tool, boolean done) {
        drawingFragment.setTool(tool);

        // When we switch tools, we must inform it of the current colour
        toolboxFragment.setColour(paletteFragment.getPrimaryColour());

        getActivity().supportInvalidateOptionsMenu();

        // Dismisses the toolbox panel
        if (done) {
            panelManagerFragment.onClearPanels();
        }
    }

    @Override
    public void onDimensionsCalculated(int width, int height) {
        toolboxFragment.setDimensions(width, height);
    }

    @Override
    public void onDropColour(int colour) {
        onPrimaryColourSelected(colour, false, false);
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

    // Returns true if a panels was made hidden, false otherwise
    public boolean onBackPressed() {
        if (actionMode == null) {
            return panelManagerFragment.onClearPanels();
        } else {
            return false;
        }
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
