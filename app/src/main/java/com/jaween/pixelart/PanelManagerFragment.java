package com.jaween.pixelart;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import com.jaween.pixelart.ui.DrawingFragment;
import com.jaween.pixelart.ui.PaletteFragment;
import com.jaween.pixelart.ui.ToolboxFragment;

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

    // Fragment tags
    private static final String TAG_PALETTE_FRAGMENT = "tag_palette_fragment";
    private static final String TAG_TOOLBOX_FRAGMENT = "tag_toolbox_fragment";

    // Save state
    private static final String KEY_PALETTE_VISIBILITY = "key_palette_visibility";
    private static final String KEY_TOOLBOX_VISIBILITY = "key_toolbox_visibility";

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

        if (paletteFragment == null | toolboxFragment == null) {
            paletteFragment = new PaletteFragment();
            toolboxFragment = new ToolboxFragment();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_container_palette, paletteFragment, TAG_PALETTE_FRAGMENT);
            fragmentTransaction.add(R.id.fl_container_toolbox, toolboxFragment, TAG_TOOLBOX_FRAGMENT);
            fragmentTransaction.commit();
        }

        if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
            combinedPanel = (RelativeLayout) view.findViewById(R.id.rl_combined_panel);
            combinedPanel.setVisibility(View.VISIBLE);
        } else if (layoutWidthDp == NARROW_LAYOUT_WIDTH_DP || layoutWidthDp == WIDE_LAYOUT_WIDTH_DP) {
            // Sliding panel and menu item callbacks for not static layouts (wide and narrow layouts)
            paletteFragment.setOnShowPaletteListener((ContainerFragment) getParentFragment());

            // The panels are initially hidden in the narraw and wide layouts
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.hide(paletteFragment);
            fragmentTransaction.hide(toolboxFragment);
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
            outState.putBoolean(KEY_PALETTE_VISIBILITY, paletteFragment.isVisible());
            outState.putBoolean(KEY_TOOLBOX_VISIBILITY, toolboxFragment.isVisible());
        } else {
            // Tall layout
            outState.putBoolean(KEY_PALETTE_VISIBILITY, combinedPanelVisible);
            outState.putBoolean(KEY_TOOLBOX_VISIBILITY, combinedPanelVisible);
        }
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restores the visibility of panels
            boolean paletteVisible = savedInstanceState.getBoolean(KEY_PALETTE_VISIBILITY, false);
            boolean toolboxVisible = savedInstanceState.getBoolean(KEY_TOOLBOX_VISIBILITY, false);

            if (layoutWidthDp == NARROW_LAYOUT_WIDTH_DP || layoutWidthDp == WIDE_LAYOUT_WIDTH_DP) {
                // The narrow and wide layouts have independent panels that can be shown
                FragmentManager fragmentManager = getChildFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                // Palette visibility
                if (paletteVisible) {
                    fragmentTransaction.show(paletteFragment);
                } else {
                    fragmentTransaction.hide(paletteFragment);
                }

                // Toolbox visibility
                if (toolboxVisible) {
                    fragmentTransaction.show(toolboxFragment);
                } else {
                    fragmentTransaction.hide(toolboxFragment);
                }

                fragmentTransaction.commit();
            } else if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
                // The tall layout has a single combined panel that can be shown if either panel was visible prior
                if (paletteVisible || toolboxVisible) {
                    combinedPanelVisible = true;
                    toggleCombinedPanel(true);
                }
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

    public boolean togglePanel(Fragment fragment) {
        // Palette and Toolbox fragments are combined into a single panel in the tall layout
        if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
            return toggleCombinedPanel(!combinedPanelVisible);
        }

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
            fragmentTransaction.setCustomAnimations(slideInAnimation, slideOutAnimation, slideInAnimation, slideOutAnimation);
            fragmentTransaction.show(in);
            fragmentTransaction.commit();
            return true;
        } else {
            // The panel was already being shown, slides it out
            hidePanel(in);
            return false;
        }
    }

    public boolean hidePanel(Fragment fragment) {
        // Large layouts have only static panels (for now)
        if (layoutWidthDp == LARGE_LAYOUT_WIDTH_DP) {
            return false;
        }

        // Requests to hide the combined panel
        if (layoutHeightDp == TALL_LAYOUT_HEIGHT_DP) {
            return toggleCombinedPanel(false);
        }

        if (!fragment.isHidden()) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(slideInAnimation, slideOutAnimation, slideInAnimation, slideOutAnimation);
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

    // The variable 'show' is the state you request the panel to be in (true means request to be visible)
    private boolean toggleCombinedPanel(boolean show) {
        // Performs an animation if the panel is not already in the requested state
        if (show != combinedPanelVisible) {
            Animation slidePaletteToolboxPanel;
            if (show) {
                combinedPanelVisible = true;
                slidePaletteToolboxPanel = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), slideInAnimation);
                slidePaletteToolboxPanel.setAnimationListener(this);
            } else {
                combinedPanelVisible = false;
                slidePaletteToolboxPanel = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), slideOutAnimation);
                slidePaletteToolboxPanel.setAnimationListener(this);
            }
            combinedPanel.startAnimation(slidePaletteToolboxPanel);
        }

        return combinedPanelVisible;
    }


    public PaletteFragment getPaletteFragment() {
        return paletteFragment;
    }

    public ToolboxFragment getToolboxFragment() {
        return toolboxFragment;
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // If te
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
