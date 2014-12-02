package com.jaween.pixelart;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
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
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.ui.layer.LayerFragment;
import com.jaween.pixelart.ui.undo.UndoItem;
import com.jaween.pixelart.ui.undo.UndoManager;
import com.jaween.pixelart.util.Color;
import com.jaween.pixelart.util.ConfigChangeFragment;

import java.util.LinkedList;

/**
 * Base container class for the main screen of the app: the drawing canvas and the tool panels.
 */
public class ContainerFragment extends Fragment implements
        PaletteFragment.OnPrimaryColourSelectedListener,
        ToolboxFragment.OnToolSelectListener,
        DrawingFragment.OnDimensionsCalculatedListener,
        DrawingSurface.OnDropColourListener,
        PaletteFragment.OnShowPaletteListener,
        LayerFragment.LayerListener,
        ActionMode.Callback {

    // Child Fragments
    private DrawingFragment drawingFragment;
    private PanelManagerFragment panelManagerFragment;
    private PaletteFragment paletteFragment;
    private ToolboxFragment toolboxFragment;
    private LayerFragment layerFragment;
    private ConfigChangeFragment configChangeFragment;

    // Fragment tags
    private static final String TAG_DRAWING_FRAGMENT = "tag_drawing_fragment";
    private static final String TAG_PANEL_MANAGER_FRAGMENT = "tag_panel_manager_fragment";

    // Undo system
    private static final int MAX_UNDOS = 50;
    private UndoManager undoManager = null;

    // Contextual ActionBar (for selection and the ColourPicker)
    private ActionMode actionMode = null;
    private ActionMode.Callback actionModeCallback = this;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Can't retain nested Fragments so we use the Activity's fragment manager
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        configChangeFragment = (ConfigChangeFragment) fragmentManager.findFragmentByTag(ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);
        if (configChangeFragment == null) {
            configChangeFragment = new ConfigChangeFragment();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(configChangeFragment, ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);
            fragmentTransaction.commit();
        }

        onRestoreInstanceState(savedInstanceState);
    }

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Saves the UndoManager
        configChangeFragment.setUndoManager(undoManager);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restores the UndoManager
        undoManager = configChangeFragment.getUndoManager();
        configChangeFragment.setUndoManager(null);
        if (undoManager == null) {
            undoManager = new UndoManager(MAX_UNDOS);
        }
    }

    // Called after PanelManagerFragment's child Fragments have been created
    @Override
    public void onStart() {
        super.onStart();

        // Gets Fragments from the PanelManagerFragment in order to manage their callbacks
        paletteFragment = panelManagerFragment.getPaletteFragment();
        toolboxFragment = panelManagerFragment.getToolboxFragment();
        layerFragment = panelManagerFragment.getLayerFragment();

        // Fragment callbacks
        drawingFragment.setOnDimensionsCalculatedListener(this);
        drawingFragment.setOnDropColourListener(this);
        drawingFragment.setOnClearPanelsListener(panelManagerFragment);
        paletteFragment.setOnPrimaryColourSelectedListener(this);
        toolboxFragment.setOnToolSelectListener(this);
        layerFragment.setLayerListener(this);

        // Fragments with undo capabilities
        layerFragment.setUndoManager(undoManager);
        drawingFragment.setUndoManager(undoManager);

        // Initial tool
        drawingFragment.setTool(toolboxFragment.getTool());
        toolboxFragment.setColour(paletteFragment.getPrimaryColour());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // TODO: Load state menu items ONLY when dynamic panels are around
        inflater.inflate(R.menu.state_menu, menu);

        // Inflates the main actions menu (undo, layers, settings, etc.)
        inflater.inflate(R.menu.main_actions_menu, menu);

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
            case R.id.action_undo:
                // Unpacks the UndoItem far enough to determine which Fragment knows how to handle it
                UndoItem undoItem = undoManager.popUndoItem();
                if (undoItem != null) {
                    switch (undoItem.getType()) {
                        case DRAW_OP:
                            drawingFragment.undo(undoItem.getData());
                            layerFragment.invalidate();
                            break;
                        case LAYER:
                            layerFragment.undo(undoItem.getData());
                            drawingFragment.invalidate();
                            break;
                    }
                }
                break;
            case R.id.action_redo:
                // Unpacks the UndoItem far enough to determine which Fragment knows how to handle it
                UndoItem redoItem = undoManager.popRedoItem();
                if (redoItem != null) {
                    switch (redoItem.getType()) {
                        case DRAW_OP:
                            drawingFragment.redo(redoItem.getData());
                            drawingFragment.invalidate();
                            break;
                        case LAYER:
                            layerFragment.redo(redoItem.getData());
                            layerFragment.invalidate();
                            break;
                    }
                }
                break;
            case R.id.action_grid:
                // Toggles grid
                Drawable gridIcon;
                if (drawingFragment.isGridEnabled()) {
                    gridIcon = getResources().getDrawable(R.drawable.ic_action_grid_off);
                } else {
                    gridIcon = getResources().getDrawable(R.drawable.ic_action_grid);
                }
                item.setIcon(gridIcon);
                drawingFragment.setGridEnabled(!drawingFragment.isGridEnabled());
                break;
            case R.id.action_tool:
                panelManagerFragment.togglePanel(toolboxFragment);
                break;
            case R.id.action_palette:
                panelManagerFragment.togglePanel(paletteFragment);
                break;
            case R.id.action_layers:
                panelManagerFragment.togglePanel(layerFragment);
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

    public UndoManager getUndoManager() {
        return undoManager;
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

    // TODO: Find a lifecycle callback of ContainerFragment between LayerFragment's onCreate() and SurfaceView's onSurfaceCreated(), place this code in that instead
    @Override
    public void onLayersInitialised(LinkedList<Layer> layers) {
        drawingFragment.setLayers(layers);
    }

    @Override
    public void onCurrentLayerChange(int i) {
        drawingFragment.setCurrentLayer(i);
    }

    @Override
    public void onMergeLayer(int i) {
        // TODO: Implement layer merging
    }
}
