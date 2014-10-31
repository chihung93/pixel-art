package com.jaween.pixelart.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import com.jaween.pixelart.R;
import com.jaween.pixelart.util.SlidingLinearLayout;

/**
 * Created by ween on 10/24/14.
 */
public class PaletteFragment extends Fragment implements
        View.OnClickListener,
        ViewTreeObserver.OnPreDrawListener,
        View.OnTouchListener {

    // Allows for a smooth fragment sliding animations
    private SlidingLinearLayout parentLayout;

    private GridLayout gridLayout;
    private static final int ROW_COUNT = 2;
    private static final int COLUMN_COUNT = 8;

    private ColourButton selectedColourButton = null;
    private ColourSelector colourSelector;
    private OnColourSelectedListener colourSelectedCallback;

    private Button customisePaletteButton;
    private Button nextPaletteButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parentLayout = (SlidingLinearLayout) inflater.inflate(R.layout.palette_fragment, null, false);
        parentLayout.setOnTouchListener(this);

        gridLayout = (GridLayout) parentLayout.findViewById(R.id.gl_palette_grid);
        gridLayout.setRowCount(ROW_COUNT);
        gridLayout.setColumnCount(COLUMN_COUNT);

        customisePaletteButton = (Button) parentLayout.findViewById(R.id.bt_customise_palette);
        nextPaletteButton = (Button) parentLayout.findViewById(R.id.bt_next_palette);

        customisePaletteButton.setOnClickListener(this);
        nextPaletteButton.setOnClickListener(this);

        initialisePalette(gridLayout);

        return parentLayout;
    }

    private void initialisePalette(GridLayout gridLayout) {
        ViewTreeObserver viewTreeObserver = gridLayout.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view instanceof ColourButton) {
            ColourButton pressedColourButton = (ColourButton) view;

            // User tapped the selected colour again, hides the panel if possible
            if (selectedColourButton == pressedColourButton) {
                colourSelectedCallback.onColourSelected(pressedColourButton.getColour(), true);
            }

            selectedColourButton.setSelected(false);
            selectedColourButton.invalidate();
            selectedColourButton = pressedColourButton;
            selectedColourButton.setSelected(true);
            colourSelectedCallback.onColourSelected(pressedColourButton.getColour(), false);
        }

        switch (view.getId()) {
            case R.id.bt_customise_palette:
                Toast.makeText(getActivity(), "TODO: Customise palette", Toast.LENGTH_SHORT).show();
                break;
            case R.id.bt_next_palette:
                Toast.makeText(getActivity(), "TODO: Next palette", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            colourSelectedCallback = (OnColourSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnColourSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public boolean onPreDraw() {
        gridLayout.getViewTreeObserver().removeOnPreDrawListener(this);
        int finalWidth = gridLayout.getMeasuredWidth();

        for (int row = 0; row < gridLayout.getRowCount(); row++) {
            for (int column = 0; column < gridLayout.getColumnCount(); column++) {
                ColourButton colourButton = new ColourButton(getActivity());

                int buttonWidth = finalWidth / gridLayout.getColumnCount();
                int buttonHeight = buttonWidth;
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(buttonWidth, buttonHeight);

                colourButton.setLayoutParams(layoutParams);
                colourButton.setOnClickListener(this);

                int[] colours = getResources().getIntArray(R.array.default_palette);
                colourButton.setColour(colours[row * gridLayout.getColumnCount() + column]);
                gridLayout.addView(colourButton);

                // Sets the initial colour
                if (selectedColourButton == null) {
                    selectedColourButton = colourButton;
                    selectedColourButton.setSelected(true);
                    colourSelectedCallback.onColourSelected(selectedColourButton.getColour(), false);

                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(selectedColourButton.getWidth(), selectedColourButton.getHeight());
                    colourSelector = new ColourSelector(getActivity());
                    colourSelector.setLayoutParams(params);
                    parentLayout.addView(colourSelector);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Consumes all touch events on the background so they don't pass through onto
        // a possible canvas below
        return true;
    }

    public interface OnColourSelectedListener {
        public void onColourSelected(int colour, boolean done);
    }
}
