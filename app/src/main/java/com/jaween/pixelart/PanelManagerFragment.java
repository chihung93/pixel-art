package com.jaween.pixelart;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import com.jaween.pixelart.ui.DrawingFragment;
import com.jaween.pixelart.ui.PaletteFragment;
import com.jaween.pixelart.ui.PanelFragment;
import com.jaween.pixelart.ui.ToolboxFragment;
import com.jaween.pixelart.ui.layer.LayerFragment;

/**
 * Manages the fragments that slide-in and out of the UI ('panels')
 * Doesn't manage palette/tool callbacks (that's ContainerFragment's job)
 */
public class PanelManagerFragment extends Fragment implements
        DrawingFragment.OnClearPanelsListener,
        Animation.AnimationListener {

    // Child Fragments
    private PaletteFragment paletteFragment;
    private ToolboxFragment toolboxFragment;
    private LayerFragment layerFragment;

    // Fragment tags
    private static final String TAG_PALETTE_FRAGMENT = "tag_palette_fragment";
    private static final String TAG_TOOLBOX_FRAGMENT = "tag_toolbox_fragment";
    private static final String TAG_LAYER_FRAGMENT = "tag_layer_fragment";

    // Save state
    private static final String KEY_PALETTE_VISIBILITY = "key_palette_visibility";
    private static final String KEY_TOOLBOX_VISIBILITY = "key_toolbox_visibility";
    private static final String KEY_LAYER_VISIBILITY = "key_layer_visibility";
    private boolean paletteRestoredVisiblilty = false;
    private boolean toolboxRestoredVisiblilty = false;
    private boolean layerRestoredVisiblilty = false;

    // Layout dimensions
    // TODO: Ensure these are same as layout directories before publishing
    private static final int LARGE_LAYOUT_WIDTH_DP = 720;
    private static final int WIDE_LAYOUT_WIDTH_DP = 400;
    private static final int NARROW_LAYOUT_WIDTH_DP = 300;
    private static final int TALL_LAYOUT_HEIGHT_DP = 600;
    private int layoutWidthDp, layoutHeightDp;

    // Panel slide animations
    private int slideInAnimation = R.anim.slide_down;
    private int slideOutAnimation = R.anim.slide_up;
    private RelativeLayout combinedPanel;
    private boolean combinedPanelVisible = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        determineLayoutType();

        if (layoutWidthDp != LARGE_LAYOUT_WIDTH_DP) {
            getParentFragment().setHasOptionsMenu(true);
        }

        // Panels slide in from end when using the wide layout
        if (layoutWidthDp == WIDE_LAYOUT_WIDTH_DP) {
            slideInAnimation = R.anim.slide_left;
            slideOutAnimation = R.anim.slide_right;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.panels_fragment, null);

        FragmentManager fragmentManager = getChildFragmentManager();
        paletteFragment = (PaletteFragment) fragmentManager.findFragmentByTag(TAG_PALETTE_FRAGMENT);
        toolboxFragment = (ToolboxFragment) fragmentManager.findFragmentByTag(TAG_TOOLBOX_FRAGMENT);
        layerFragment = (LayerFragment) fragmentManager.findFragmentByTag(TAG_LAYER_FRAGMENT);

        if (paletteFragment == null | toolboxFragment == null | layerFragment == null) {
            paletteFragment = new PaletteFragment();
            toolboxFragment = new ToolboxFragment();
            layerFragment = new LayerFragment();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_container_palette, paletteFragment, TAG_PALETTE_FRAGMENT);
            fragmentTransaction.add(R.id.fl_container_toolbox, toolboxFragment, TAG_TOOLBOX_FRAGMENT);
            fragmentTransaction.add(R.id.fl_container_layers, layerFragment, TAG_LAYER_FRAGMENT);
            fragmentTransaction.commit();
        }

        if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
            combinedPanel = (RelativeLayout) view.findViewById(R.id.rl_combined_panel);
            combinedPanel.setVisibility(View.VISIBLE);
        } else if (layoutWidthDp == NARROW_LAYOUT_WIDTH_DP || layoutWidthDp == WIDE_LAYOUT_WIDTH_DP) {
            // Sliding panel and menu item callbacks for not static layouts (wide and narrow layouts)
            paletteFragment.setOnShowPaletteListener((ContainerFragment) getParentFragment());

            // TODO: Layers disabled until issues resolved
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.hide(layerFragment);
            fragmentTransaction.commit();
        }

        onRestoreInstanceState(savedInstanceState);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Saves the visibility of the panels
        if (layoutWidthDp == NARROW_LAYOUT_WIDTH_DP || layoutWidthDp == WIDE_LAYOUT_WIDTH_DP) {
            outState.putBoolean(KEY_PALETTE_VISIBILITY, paletteFragment.getView().getVisibility() == View.VISIBLE);
            outState.putBoolean(KEY_TOOLBOX_VISIBILITY, toolboxFragment.getView().getVisibility() == View.VISIBLE);
            outState.putBoolean(KEY_LAYER_VISIBILITY, layerFragment.getView().getVisibility() == View.VISIBLE);
        } else {
            // Tall layout
            outState.putBoolean(KEY_PALETTE_VISIBILITY, combinedPanelVisible);
            outState.putBoolean(KEY_TOOLBOX_VISIBILITY, combinedPanelVisible);
            outState.putBoolean(KEY_LAYER_VISIBILITY, combinedPanelVisible);
        }
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restores the visibility of panels
            paletteRestoredVisiblilty = savedInstanceState.getBoolean(KEY_PALETTE_VISIBILITY, false);
            toolboxRestoredVisiblilty = savedInstanceState.getBoolean(KEY_TOOLBOX_VISIBILITY, false);
            layerRestoredVisiblilty = savedInstanceState.getBoolean(KEY_LAYER_VISIBILITY, false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Restores the visibility of the panels (done after restoreInstanceState as Fragment
        // views are now initialised)
        if (layoutWidthDp == NARROW_LAYOUT_WIDTH_DP || layoutWidthDp == WIDE_LAYOUT_WIDTH_DP) {
            // The narrow and wide layouts have independent panels that can be shown

            // Palette visibility
            if (paletteRestoredVisiblilty) {
                paletteFragment.getView().setVisibility(View.VISIBLE);
            } else {
                paletteFragment.getView().setVisibility(View.INVISIBLE);
            }

            // Toolbox visibility
            if (toolboxRestoredVisiblilty) {
                toolboxFragment.getView().setVisibility(View.VISIBLE);
            } else {
                toolboxFragment.getView().setVisibility(View.INVISIBLE);
            }

            // Layer visibility
            if (layerRestoredVisiblilty) {
                layerFragment.getView().setVisibility(View.VISIBLE);
            } else {
                layerFragment.getView().setVisibility(View.INVISIBLE);
            }
        } else if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
            // The tall layout has a single combined panel that can be shown if a panel was visible prior
            if (paletteRestoredVisiblilty || toolboxRestoredVisiblilty || layerRestoredVisiblilty) {
                combinedPanelVisible = true;
                toggleCombinedPanel(true);
            }
        }
    }

    private void determineLayoutType() {
        // Screen size details
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        float density = getResources().getDisplayMetrics().density;
        float widthDp = metrics.widthPixels / density;
        float heightDP = metrics.heightPixels / density;

        // Picks the layout width bucket
        if (widthDp >= LARGE_LAYOUT_WIDTH_DP) {
            layoutWidthDp = LARGE_LAYOUT_WIDTH_DP;
        } else if (widthDp >= WIDE_LAYOUT_WIDTH_DP) {
            layoutWidthDp = WIDE_LAYOUT_WIDTH_DP;
        } else {
            layoutWidthDp = NARROW_LAYOUT_WIDTH_DP;
        }

        // Picks the layout height bucket
        if (heightDP >= TALL_LAYOUT_HEIGHT_DP) {
            layoutHeightDp = TALL_LAYOUT_HEIGHT_DP;
        } else {
            layoutHeightDp = NARROW_LAYOUT_WIDTH_DP;
        }
    }

    public boolean togglePanel(PanelFragment panel) {
        // Palette and Toolbox fragments are combined into a single panel in the tall layout
        if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
            return toggleCombinedPanel(!combinedPanelVisible);
        }

        // Panel is already visible, slides it out
        if (panel.getView().getVisibility() == View.VISIBLE) {
            slidePanel(panel, false);
            return false;
        }

        // No other panels in the way, slides fragment in
        PanelFragment outPanel = getVisiblePanel();
        if (outPanel == null) {
            slidePanel(panel, true);
            return true;
        }

        slidePanels(panel, outPanel);
        return true;
    }

    private void slidePanel(PanelFragment panel, boolean forward) {
        if (animationStarted()) {
            return;
        }

        int height = 0;
        panel.slide(forward, height);
    }

    private void slidePanels(PanelFragment inPanel, PanelFragment outPanel) {
        if (animationStarted()) {
            return;
        }

        boolean forward = true;
        inPanel.slide(forward, outPanel.getView().getHeight());

        boolean backward = false;
        outPanel.slide(backward, inPanel.getView().getHeight());
    }

    private boolean animationStarted() {
        return paletteFragment.animationStarted() ||
                toolboxFragment.animationStarted();/* ||
                layerFragment.animationStarted();*/
    }

    private PanelFragment getVisiblePanel() {
        if (toolboxFragment.getView().getVisibility() == View.VISIBLE) {
            return toolboxFragment;
        } else if (paletteFragment.getView().getVisibility() == View.VISIBLE) {
            return paletteFragment;
        /*} else if (layerFragment.getView().getVisibility() == View.VISIBLE) {
            return layerFragment;*/
        } else {
            return null;
        }
    }

    @Override
    public boolean onClearPanels() {
        PanelFragment outPanel = getVisiblePanel();
        if (outPanel != null) {
            slidePanel(outPanel, false);
            return true;
        }
        return false;
    }

    // The variable 'show' is the state you request the panel to be in (true means request to be visible)
    private boolean toggleCombinedPanel(boolean show) {
        // Performs an animation if the panel is not already in the requested state
        if (show != combinedPanelVisible) {
            Animation slideCombinedPanel;
            if (show) {
                combinedPanelVisible = true;
                slideCombinedPanel = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), slideInAnimation);
                slideCombinedPanel.setAnimationListener(this);
            } else {
                combinedPanelVisible = false;
                slideCombinedPanel = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), slideOutAnimation);
                slideCombinedPanel.setAnimationListener(this);
            }
            combinedPanel.startAnimation(slideCombinedPanel);
        }

        return combinedPanelVisible;
    }

    public PaletteFragment getPaletteFragment() {
        return paletteFragment;
    }

    public ToolboxFragment getToolboxFragment() {
        return toolboxFragment;
    }

    public LayerFragment getLayerFragment() {
        return layerFragment;
    }

    @Override
    public void onAnimationStart(Animation animation) {
        if (combinedPanelVisible) {
            combinedPanel.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (combinedPanelVisible == false) {
            combinedPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}
