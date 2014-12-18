package com.jaween.pixelart.ui;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.jaween.pixelart.PanelManagerFragment;
import com.jaween.pixelart.R;
import com.jaween.pixelart.util.Color;
import com.jaween.pixelart.ui.colourpicker.ColourPickerFragment;
import com.jaween.pixelart.util.SlideAnimator;
import com.jaween.pixelart.util.SlidingLinearLayout;

import java.util.ArrayList;

/**
 * Created by ween on 10/24/14.
 */
public class PaletteFragment extends Fragment implements
        View.OnClickListener,
        View.OnTouchListener,
        ColourPickerFragment.OnColourUpdateListener,
        ColourPickerFragment.OnColourPickerAnimationEndListener {

    // Child Fragments
    private ColourPickerFragment colourPickerFragment;
    private static final String TAG_COLOUR_PICKER_FRAGMENT = "tag_colour_picker_fragment";

    // Palette grid
    private TableLayout paletteTable;
    private static final int ROW_COUNT = 2;
    private static final int COLUMN_COUNT = 8;

    // Palette items
    private ColourButton selectedColourButton = null;
    private ColourSelector colourSelector;
    private OnPrimaryColourSelectedListener onPrimaryColourSelectedListener;
    private OnShowPaletteListener onShowPaletteListener;

    // Palette navigation and customisation
    private Button customisePaletteButton;
    private Button nextPaletteButton;

    private ArrayList<ArrayList<ColourButton>> palettes = new ArrayList<ArrayList<ColourButton>>();
    private int numberOfPalettes = 1;
    private int currentPalette = 0;
    private int primaryColour;

    // Saved instance state
    private static final String KEY_PRIMARY_COLOUR = "key_primary_colour";

    // A single use flag used in setting the initial primaryColour button (should be a better way of achieving this)
    private boolean colourSetPriorToInitialDraw = false;

    // Static sidebar specific variables
    private View primaryColourView;
    private Drawable[] primaryColourViewLayers = new Drawable[2];

    // Animation
    private SlideAnimator slideAnimator;
    private SlidingLinearLayout container;
    private LinearLayout buttonBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getChildFragmentManager();
        if (getArguments() != null) {
            colourPickerFragment = (ColourPickerFragment) fragmentManager.findFragmentByTag(TAG_COLOUR_PICKER_FRAGMENT);
        }

        if (colourPickerFragment == null) {
            colourPickerFragment = new ColourPickerFragment();
            colourPickerFragment.setOnColourUpdateListener(this);
            colourPickerFragment.setOnColourPickerAnimationEndListener(this);

            // Adds the primaryColour picker
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_container_colour_picker, colourPickerFragment, TAG_COLOUR_PICKER_FRAGMENT);
            fragmentTransaction.commit();
        }

        // Immediately hides the primaryColour picker so that we can show it later when it's needed
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(colourPickerFragment);
        fragmentTransaction.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.palette_fragment, null, false);
        initialiseViews(view);

        onRestoreInstanceState(savedInstanceState);

        if (this.container != null) {
            slideAnimator = new SlideAnimator(this.container, paletteTable, buttonBar, ((PanelManagerFragment) getParentFragment()), this);
        }

        return view;
    }

    private void initialiseViews(View v) {
        customisePaletteButton = (Button) v.findViewById(R.id.bt_customise_palette);
        nextPaletteButton = (Button) v.findViewById(R.id.bt_next_palette);
        primaryColourView = v.findViewById(R.id.vw_primary_colour);
        container = (SlidingLinearLayout) v.findViewById(R.id.sll_palette_content);
        paletteTable = (TableLayout) v.findViewById(R.id.tl_palette_grid);
        buttonBar = (LinearLayout) v.findViewById(R.id.ll_button_bar);

        v.setOnTouchListener(this);
        customisePaletteButton.setOnClickListener(this);
        nextPaletteButton.setOnClickListener(this);

        initialisePalette(paletteTable);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // TODO: Ponder, if we don't remove the fragment here it automatically shows itself on config change, why?
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(colourPickerFragment);
        fragmentTransaction.commit();

        // Saves the current primaryColour
        outState.putInt(KEY_PRIMARY_COLOUR, primaryColour);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restores the primary colour
        if (savedInstanceState != null) {
            primaryColour = savedInstanceState.getInt(KEY_PRIMARY_COLOUR);
        } else {
            primaryColour = palettes.get(0).get(0).getColour();
        }
        setColourButton(primaryColour);

        if (onPrimaryColourSelectedListener != null) {
            onPrimaryColourSelectedListener.onPrimaryColourSelected(primaryColour, false, true);
        }
    }

    public int getPrimaryColour() {
        return primaryColour;
    }

    // Sets the selected ColourButton if the given primaryColour exists in the palette, otherwise sets it to null
    public void setColourButton(int colour) {
        // TODO: Fix, remove this in whatever way possible
        colourSetPriorToInitialDraw = true;

        primaryColour = colour;

        updatePrimaryColourView();

        // Iterates through all the ColourButtons of all the palettes until it finds one with our new primaryColour
        for (int i = 0; i < palettes.size(); i++) {
            for (int j = 0; j < palettes.get(i).size(); j++) {
                ColourButton colourButton = palettes.get(i).get(j);
                if (colourButton.getColour() == colour) {
                    // ColourButton found, makes it the newly selected button
                    invalidateSelectedColourButton(colourButton);
                    return;
                }
            }
        }

        // New primaryColour didn't belong to any ColourButton, so we must nullify the selected primaryColour button
        invalidateSelectedColourButton(null);

        // Updates the primaryColour picker to this primaryColour
        colourPickerFragment.setColour(colour, true);
    }

    private void initialisePalette(TableLayout tableLayout) {
        // Sets up the storage for the palettes
        for (int i = 0; i < numberOfPalettes; i++) {
            palettes.add(new ArrayList<ColourButton>());
        }

        // Sets up the table UI and the ColourButtons for each of the palettes
        for (int palette = 0; palette < numberOfPalettes; palette++) {
            for (int row = 0; row < ROW_COUNT; row++) {
                TableRow tableRow = new TableRow(getActivity().getApplicationContext());
                for (int column = 0; column < COLUMN_COUNT; column++) {

                    // Gives each ColourButton an equal width
                    TableRow.LayoutParams buttonParams = new TableRow.LayoutParams();
                    buttonParams.weight = 1;

                    // Sets up the properties of the ColourButton
                    int[] colours = getResources().getIntArray(R.array.default_palette);
                    ColourButton colourButton = new ColourButton(getActivity().getApplicationContext());
                    colourButton.setLayoutParams(buttonParams);
                    colourButton.setColour(colours[row * COLUMN_COUNT + column]);
                    colourButton.setOnClickListener(this);

                    // Adds the ColourButton to the row (row is not yet in the UI)
                    tableRow.addView(colourButton);
                    palettes.get(palette).add(colourButton);
                }
                // Adds the row to the UI
                tableLayout.addView(tableRow);
            }
        }

        // Colour selector
        FrameLayout.LayoutParams selectorParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        selectorParams.width = 90;
        selectorParams.height = 90;
        colourSelector = new ColourSelector(getActivity());
        colourSelector.setLayoutParams(selectorParams);
        //parentLayout.addView(colourSelector);

        // TODO: This is too hacky, there should be a better way to achieve this
        // In the case that the primaryColour was set before the ColourButtons were created we must update the ColourSelector
        if (colourSetPriorToInitialDraw) {
            setColourButton(primaryColour);
        }
    }

    /**
     * UI indicator of the primary colour when there is no menu item (used in static panel layouts for tablets)
     */
    private void updatePrimaryColourView() {
        if (primaryColourView != null) {
            Drawable colouredInner = getResources().getDrawable(R.drawable.palette_colour_button);
            Drawable border = getResources().getDrawable(R.drawable.palette_colour_button_border);
            LayerDrawable background = Color.tintAndLayerDrawable(colouredInner, border, primaryColour);

            // Pre-Jellybean doesn't have setBackground()
            int sdk = Build.VERSION.SDK_INT;
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                primaryColourView.setBackgroundDrawable(background);
            } else {
                primaryColourView.setBackground(background);
            }
        }
    }

    public int getHeight() {
        return container.getHeight();
    }

    @Override
    public void onClick(View view) {
        if (view instanceof ColourButton) {
            ColourButton clickedColourButton = (ColourButton) view;

            // Hides the panel when the user taps the same primaryColour twice (only when there is no primaryColour picker around)
            boolean dismissPanel = false;
            if (selectedColourButton == clickedColourButton && colourPickerFragment.isHidden()) {
                dismissPanel = true;
            }

            // Sets the Palette's own primaryColour
            primaryColour = clickedColourButton.getColour();
            updatePrimaryColourView();

            // Notifies the selected button
            invalidateSelectedColourButton(clickedColourButton);

            // Sets the menu item primaryColour
            onPrimaryColourSelectedListener.onPrimaryColourSelected(primaryColour, dismissPanel, true);

            // Sets the primaryColour of the primaryColour picker
            colourPickerFragment.setColour(primaryColour, true);

            return;
        }

        switch (view.getId()) {
            case R.id.bt_customise_palette:
                if (onShowPaletteListener != null) {
                    onShowPaletteListener.onToggleColourPalette(true);
                    if (colourPickerFragment.isHidden()) {
                        FragmentManager fragmentManager = getChildFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.setCustomAnimations(R.anim.slide_down, R.anim.slide_up);
                        fragmentTransaction.show(colourPickerFragment);
                        fragmentTransaction.commit();
                    }
                }
                break;
            case R.id.bt_next_palette:
                Toast.makeText(getActivity(), "TODO: Next palette", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void invalidateSelectedColourButton(ColourButton newColourButton) {
        // Can be null on a configuration change
        if (selectedColourButton != null) {
            selectedColourButton.setSelected(false);
            selectedColourButton.invalidate();
        }

        // A null ColourButton means the primaryColour doesn't exist in any palette
        if (newColourButton != null) {
            selectedColourButton = newColourButton;
            selectedColourButton.setSelected(true);
            selectedColourButton.invalidate();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Consumes all touch events on the background so they don't pass through onto
        // a possible canvas below
        return true;
    }

    public void hideColourPicker() {
        if (!colourPickerFragment.isHidden()) {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);

            fragmentTransaction.hide(colourPickerFragment);
            fragmentTransaction.commit();
        }
    }

    public void setOnShowPaletteListener(OnShowPaletteListener onShowPaletteListener) {
        this.onShowPaletteListener = onShowPaletteListener;
    }

    @Override
    public void onModifyPalette(int colour) {
        primaryColour = colour;

        updatePrimaryColourView();

        // Updates the UI ColourButton
        if (selectedColourButton != null) {
            selectedColourButton.setColour(colour);
        }

        // Notifies the selected primaryColour menu item to update
        if (onPrimaryColourSelectedListener != null) {
            onPrimaryColourSelectedListener.onPrimaryColourSelected(colour, false, true);
        }
    }

    public void setOnPrimaryColourSelectedListener(OnPrimaryColourSelectedListener onPrimaryColourSelectedListener) {
        this.onPrimaryColourSelectedListener = onPrimaryColourSelectedListener;
    }

    @Override
    public void onColourPickerAnimationEnd() {
        // DrawingFragment is being drawn even though it is completely occluded, hides it
        onShowPaletteListener.onColourPaletteAnimationEnd();
    }

    public interface OnPrimaryColourSelectedListener {
        public void onPrimaryColourSelected(int colour, boolean done, boolean fromPalette);
    }

    public interface OnShowPaletteListener {
        public void onToggleColourPalette(boolean visible);

        public void onColourPaletteAnimationEnd();
    }

    public void startAnimation(boolean forward, int height) {
        if (forward) {
            slideAnimator.start(height);
        } else {
            slideAnimator.reverse(height);
        }
    }
}
