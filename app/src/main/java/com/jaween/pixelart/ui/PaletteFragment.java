package com.jaween.pixelart.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.jaween.pixelart.R;
import com.jaween.pixelart.ui.colourpicker.ColourPickerFragment;

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
    private static final String TAG_COLOUR_PICKER_FRAGMENT = "colour_picker_fragment";

    // Allows for smooth fragment sliding animations
    private LinearLayout parentLayout;

    // Palette grid
    private TableLayout tableLayout;
    private static final int ROW_COUNT = 2;
    private static final int COLUMN_COUNT = 8;

    // Palette items
    private ColourButton selectedColourButton = null;
    private ColourSelector colourSelector;
    private OnPrimaryColourSelectedListener onPrimaryColourSelectedListener;
    private OnShowColourPaletteListener onShowColourPaletteListener;

    // Palette navigation and customisation
    private Button customisePaletteButton;
    private Button nextPaletteButton;

    private ArrayList<ArrayList<ColourButton>> palettes = new ArrayList<ArrayList<ColourButton>>();
    private int numberOfPalettes = 1;
    private int currentPalette = 0;
    private int colour;

    private int[] restoredSelectedColour;
    private static final String KEY_SELECTED_COLOUR = "key_selected_colour";

    // A single use flag used in setting the initial colour button (should be a better way of achieving this)
    private boolean colourSetPriorToInitialDraw = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getChildFragmentManager();
        if (savedInstanceState != null) {
            colourPickerFragment = (ColourPickerFragment) fragmentManager.findFragmentByTag(TAG_COLOUR_PICKER_FRAGMENT);

            restoredSelectedColour = savedInstanceState.getIntArray(KEY_SELECTED_COLOUR);
        }

        if (colourPickerFragment == null) {
            colourPickerFragment = new ColourPickerFragment();
            colourPickerFragment.setOnColourUpdateListener(this);
            colourPickerFragment.setOnColourPickerAnimationEndListener(this);

            // Adds the colour picker
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_container_colour_picker, colourPickerFragment, TAG_COLOUR_PICKER_FRAGMENT);
            fragmentTransaction.commit();
        }

        // Immediately hides the colour picker so that we can show it later when it's needed
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(colourPickerFragment);
        fragmentTransaction.commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        parentLayout = (LinearLayout) inflater.inflate(R.layout.palette_fragment, null, false);
        parentLayout.setOnTouchListener(this);

        customisePaletteButton = (Button) parentLayout.findViewById(R.id.bt_customise_palette);
        nextPaletteButton = (Button) parentLayout.findViewById(R.id.bt_next_palette);

        customisePaletteButton.setOnClickListener(this);
        nextPaletteButton.setOnClickListener(this);

        tableLayout = (TableLayout) parentLayout.findViewById(R.id.tl_palette_grid);
        initialisePalette(tableLayout);

        return parentLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // TODO: Ponder, if we don't remove the fragment here it automatically shows itself on config change, why?
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(colourPickerFragment);
        fragmentTransaction.commit();
    }

    public int getColour() {
        return colour;
    }

    public void setColourButton(int colour) {
        colourSetPriorToInitialDraw = true;

        // Updates the colour picker to this colour
        colourPickerFragment.setColour(colour);

        // Iterates through all the ColourButtons of all the palettes until it finds one with our new colour
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

        // New colour didn't belong to any ColourButton, so we must nullify the selected colour button
        invalidateSelectedColourButton(null);

        this.colour = colour;
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

        // Sets the initial colour
        if (restoredSelectedColour == null) {
            selectedColourButton = palettes.get(0).get(0);
            selectedColourButton.setSelected(true);
            //onPrimaryColourSelectedListener.onPrimaryColourSelected(selectedColourButton.getColour(), false, true);
            colour = selectedColourButton.getColour();
        } else {
            setColourButton(palettes.get(restoredSelectedColour[0]).get(restoredSelectedColour[1]).getColour());
        }

        // TODO: This is too hacky, there should be a better way to achieve this
        // In the case that the colour was set before the ColourButtons were created we must update the ColourSelector
        if (colourSetPriorToInitialDraw) {
            setColourButton(colour);
        }
    }

    @Override
    public void onClick(View view) {
        if (view instanceof ColourButton) {
            ColourButton clickedColourButton = (ColourButton) view;

            // Hides the panel when the user taps the same colour twice (only when there is no colour picker around)
            boolean dismissPanel = false;
            if (selectedColourButton == clickedColourButton && colourPickerFragment.isHidden()) {
                dismissPanel = true;
            }

            // Sets the Palette's own colour
            colour = clickedColourButton.getColour();

            // Notifies the selected button
            invalidateSelectedColourButton(clickedColourButton);

            // Sets the menu item colour
            onPrimaryColourSelectedListener.onPrimaryColourSelected(colour, dismissPanel, true);

            // Sets the colour of the colour picker
            colourPickerFragment.setColour(colour);

            return;
        }

        switch (view.getId()) {
            case R.id.bt_customise_palette:
                if (onShowColourPaletteListener != null) {
                    onShowColourPaletteListener.onToggleColourPalette(true);
                    if (colourPickerFragment.isHidden()) {
                        FragmentManager fragmentManager = getChildFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.setCustomAnimations(R.anim.slide_down, R.anim.slide_up);
                        fragmentTransaction.show(colourPickerFragment);
                        fragmentTransaction.commit();

                        // Updates the colour picker currently selected colour
                        colourPickerFragment.setColour(colour);
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

        // A null ColourButton means the colour doesn't exist in any palette
        if (newColourButton != null) {
            selectedColourButton = newColourButton;
            selectedColourButton.setSelected(true);
            selectedColourButton.invalidate();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onPrimaryColourSelectedListener = (OnPrimaryColourSelectedListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnColourSelectedListener");
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

    public void setOnShowColourPaletteListener(OnShowColourPaletteListener onShowColourPaletteListener) {
        this.onShowColourPaletteListener = onShowColourPaletteListener;
    }

    @Override
    public void onColorUpdate(int colour) {
        this.colour = colour;

        // Updates the UI ColourButton
        if (selectedColourButton != null) {
            selectedColourButton.setColour(colour);
        }

        // Notifies the selected colour menu item to update
        if (onPrimaryColourSelectedListener != null) {
            onPrimaryColourSelectedListener.onPrimaryColourSelected(colour, false, true);
        }
    }

    @Override
    public void onColourPickerAnimationEnd() {
        // DrawingFragment is being drawn even though it is completely occluded, hides it
        onShowColourPaletteListener.onColourPaletteAnimationEnd();
    }

    public interface OnPrimaryColourSelectedListener {
        public void onPrimaryColourSelected(int colour, boolean done, boolean fromPalette);
    }

    public interface OnShowColourPaletteListener {
        public void onToggleColourPalette(boolean visible);
        public void onColourPaletteAnimationEnd();
    }
}
