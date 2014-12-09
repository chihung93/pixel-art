package com.jaween.pixelart;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
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
import com.jaween.pixelart.ui.animation.AnimationFragment;
import com.jaween.pixelart.ui.animation.Frame;
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
        AnimationFragment.FrameListener,
        ActionMode.Callback {

    // Child Fragments
    private DrawingFragment drawingFragment;
    private PaletteFragment paletteFragment;
    private ToolboxFragment toolboxFragment;
    private LayerFragment layerFragment;
    private AnimationFragment animationFragment;
    private PanelManagerFragment panelManagerFragment;
    private ConfigChangeFragment configChangeFragment;

    // Fragment tags
    private static final String TAG_DRAWING_FRAGMENT = "tag_drawing_fragment";
    private static final String TAG_ANIMATION_FRAGMENT = "tag_animation_fragment";
    private static final String TAG_PANEL_MANAGER_FRAGMENT = "tag_panel_manager_fragment";

    // Drawing dimensions
    private static final int layerWidth = 128;
    private static final int layerHeight = 128;

    // Undo system
    private static final int MAX_UNDOS = 200;
    private UndoManager undoManager = null;

    // Toolbar and ActionMode
    private ActionBarDrawerToggle drawerToggle;
    private ActionMode actionMode = null;
    private ActionMode.Callback actionModeCallback = this;

    // Animation drawer
    private View animationDrawerView;
    private DrawerLayout drawerLayout;

    // Colour menu item
    private LayerDrawable layerDrawable;

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
        animationFragment = (AnimationFragment) fragmentManager.findFragmentByTag(TAG_ANIMATION_FRAGMENT);
        panelManagerFragment = (PanelManagerFragment) fragmentManager.findFragmentByTag(TAG_PANEL_MANAGER_FRAGMENT);

        if (drawingFragment == null | animationFragment == null | panelManagerFragment == null) {
            // Fragments don't yet exist, creates them
            drawingFragment = new DrawingFragment();
            animationFragment = new AnimationFragment();
            panelManagerFragment = new PanelManagerFragment();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_container_drawing, drawingFragment, TAG_DRAWING_FRAGMENT);
            fragmentTransaction.add(R.id.fl_container_animation, animationFragment, TAG_ANIMATION_FRAGMENT);
            fragmentTransaction.add(R.id.fl_container_panels, panelManagerFragment, TAG_PANEL_MANAGER_FRAGMENT);

            fragmentTransaction.commit();
        }

        animationDrawerView = view.findViewById(R.id.fl_container_animation);

        setupDrawer(view);

        return view;
    }

    private void setupDrawer(View v) {
        // Sets up the animation drawer
        Toolbar toolbar = ((ContainerActivity) getActivity()).getToolbar();
        drawerLayout = (DrawerLayout) v.findViewById(R.id.drawer_layout);
        drawerLayout.setStatusBarBackground(R.color.primary_dark);
        drawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, 0, 0) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActivity().supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActivity().supportInvalidateOptionsMenu();
            }
        };


        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.setDrawerListener(drawerToggle);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
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
        animationFragment.setFrameListener(this);

        // Fragments with undo capabilities
        drawingFragment.setUndoManager(undoManager);
        layerFragment.setUndoManager(undoManager);
        animationFragment.setUndoManager(undoManager);

        // Initial tool
        drawingFragment.setTool(toolboxFragment.getTool());
        toolboxFragment.setColour(paletteFragment.getPrimaryColour());

        // Allocates memory
        drawingFragment.setDimensions(layerWidth, layerHeight);
        layerFragment.setDimensions(layerWidth, layerHeight);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Initiates the process of creating the first animation frame
        animationFragment.notifyFragmentsReady();

        // TODO: Pass the restored frame and layer index to the Drawing and Layer Fragments
        // (ideally done in the AnimationFragment via a call to AnimationFragment.onCurrentFrameChange())

        // Passes the initial frame to the DrawingFragment and LayerFragment
        LinkedList<Frame> frames = animationFragment.getFrames();
        drawingFragment.setFrames(frames);
        layerFragment.setFrames(frames);

        // Allocates memory for some Tools here, as onResume() is called after they are initiated
        toolboxFragment.setDimensions(layerWidth, layerHeight);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // TODO: Load state menu items ONLY when dynamic panels are around
        inflater.inflate(R.menu.state_menu, menu);

        // Inflates the main actions menu (undo, layers, settings, etc.)
        inflater.inflate(R.menu.main_actions_menu, menu);

        updateStateMenuIcons(menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (drawerLayout.isDrawerOpen(animationDrawerView)) {
            menu.setGroupVisible(R.id.menu_group_state, false);
            menu.setGroupVisible(R.id.menu_group_main, false);
        } else {
            menu.setGroupVisible(R.id.menu_group_animation, false);
        }

        updateStateMenuIcons(menu);
    }

    private void updateStateMenuIcons(Menu menu) {
        // Updates the state icons
        MenuItem item = menu.findItem(R.id.action_tool);
        MenuItem paletteItem = menu.findItem(R.id.action_palette);
        item.setIcon(toolboxFragment.getTool().getIcon());
        paletteItem.setIcon(layerDrawable);
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
                        case FRAME:
                            animationFragment.undo(undoItem.getData());
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
                        case FRAME:
                            animationFragment.redo(redoItem.getData());
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

    public void syncState() {
        drawerToggle.syncState();
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

        // Recolours the palette menu item
        updateColourMenuItem();

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

    // TODO: Maybe I don't need this callback, when is this called? If fragments are ready here it's useful for animationFragment.notifyFragmentsReady()
    @Override
    public void onSurfaceCreated(int width, int height) {
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

    private void updateColourMenuItem() {
            // Tints the inner square to the selected colour
            Drawable colouredInner = getResources().getDrawable(R.drawable.palette_menu_item);
            Drawable border = getResources().getDrawable(R.drawable.palette_menu_item_border);
            int colour = paletteFragment.getPrimaryColour();
            layerDrawable = Color.tintAndLayerDrawable(colouredInner, border, colour);

            getActivity().supportInvalidateOptionsMenu();
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

    @Override
    public void onCurrentLayerChange(int index) {
        drawingFragment.setCurrentLayerIndex(index);
        animationFragment.setCurrentLayerIndex(index);
    }

    @Override
    public void onMergeLayer(int i) {
        // TODO: Implement layer merging
    }

    @Override
    public void onCurrentFrameChange(Frame frame, int frameIndex) {
        drawingFragment.setCurrentFrameIndex(frameIndex);
        layerFragment.setCurrentFrameIndex(frameIndex);
    }

    @Override
    public Frame requestFrame(boolean duplicate) {
        // LayerFragment constructs an Animation frame
        return layerFragment.requestFrame(duplicate);
    }
}
