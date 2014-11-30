package com.jaween.pixelart.ui.colourpicker;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.util.AbsVerticalSeekBar;
import com.jaween.pixelart.util.Color;

/**
 * Created by ween on 11/22/14.
 */
public class ColourPickerFragment extends Fragment implements
        ColourPickerView.OnColourSelectListener,
        SeekBar.OnSeekBarChangeListener,
        View.OnLongClickListener {

    private ColourPickerView colourPickerView;
    private TextView colourName;
    private EditText hueText;
    private EditText saturationText;
    private EditText lightnessText;
    private EditText opacityText;
    private SeekBar hueBar;
    private SeekBar saturationBar;
    private SeekBar lightnessBar;
    private AbsVerticalSeekBar opacityBar;

    private int opacity;
    private int colour;
    private float[] hsl = new float[3];

    private boolean hasRestoredColour = false;
    private float[] restoredColour = new float[3];

    private OnColourUpdateListener onColourUpdateListener = null;
    private OnColourPickerAnimationEndListener onColourPickerAnimationEndListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.colour_picker_fragment, null);
        initialiseViews(view);

        return view;
    }

    private void initialiseViews(View v) {
        // Links to XML
        colourPickerView = (ColourPickerView) v.findViewById(R.id.cp_colour_picker);
        colourName = (TextView) v.findViewById(R.id.tv_colour_name);
        hueBar = (SeekBar) v.findViewById(R.id.sb_hue);
        saturationBar = (SeekBar) v.findViewById(R.id.sb_saturation);
        lightnessBar = (SeekBar) v.findViewById(R.id.sb_lightness);
        opacityBar = (AbsVerticalSeekBar) v.findViewById(R.id.sb_opacity);
        hueText = (EditText) v.findViewById(R.id.et_hue);
        saturationText = (EditText) v.findViewById(R.id.et_saturation);
        lightnessText = (EditText) v.findViewById(R.id.et_lightness);
        opacityText = (EditText) v.findViewById(R.id.et_opacity);

        // Handles long clicks which typically trigger a selection CAB
        hueText.setOnLongClickListener(this);
        saturationText.setOnLongClickListener(this);
        lightnessText.setOnLongClickListener(this);
        opacityText.setOnLongClickListener(this);

        colourPickerView.setOnColourSelectListener(this);
        hueBar.setOnSeekBarChangeListener(this);
        saturationBar.setOnSeekBarChangeListener(this);
        lightnessBar.setOnSeekBarChangeListener(this);
        //opacityBar.setOnSeekBarChangeListener(this);

        //addTextWatchers();

        // Sets the initial colours
        opacity = opacityBar.getProgress();

        if (hasRestoredColour) {
            colourPickerView.setHSL(restoredColour[0], restoredColour[1], restoredColour[2], true);
        } else {
            colourPickerView.setHSL(
                    (float) hueBar.getProgress() / (float) hueBar.getMax(),
                    (float) saturationBar.getProgress() / (float) saturationBar.getMax(),
                    (float) lightnessBar.getProgress() / (float) lightnessBar.getMax(), true);
        }
    }

    // Sets the colour of the colour picker from an ARGB value
    public void setColour(int colour, boolean fromPalette) {

        // Converts the RGB value into HSL
        opacity = android.graphics.Color.alpha(colour);
        int r = android.graphics.Color.red(colour);
        int g = android.graphics.Color.green(colour);
        int b = android.graphics.Color.blue(colour);
        Color.colorToHsl(r, g, b, hsl);

        // Sets the selected colour of the ColourPickerView
        if (colourPickerView == null) {
            hasRestoredColour = true;
            restoredColour[0] = hsl[0];
            restoredColour[1] = hsl[1];
            restoredColour[2] = hsl[2];
        } else {
            if (fromPalette) {
                colourPickerView.setHSL(hsl[0], hsl[1], hsl[2], fromPalette);
            }
        }

        this.colour = colour;
    }

    // ColourPickerView has chosen a colour, modifies the palette accordingly
    @Override
    public void onColourSelect(int colour) {
        colour = android.graphics.Color.argb(opacity, android.graphics.Color.red(colour), android.graphics.Color.green(colour), android.graphics.Color.blue(colour));
        colourName.setTextColor(colour);
        //colourName.setText(Integer.toHexString(colour).toUpperCase());

        // Updates progress of the SeekBars
        hueBar.setProgress((int) (colourPickerView.getHue()));
        saturationBar.setProgress((int) (colourPickerView.getSaturation() * saturationBar.getMax()));
        lightnessBar.setProgress((int) (colourPickerView.getLightness() * lightnessBar.getMax()));

        // Notifies the colour palette of the new colour
        if (onColourUpdateListener != null) {
            onColourUpdateListener.onModifyPalette(colour);
        }

        this.colour = colour;
    }

    @Override
    public void onColourSelectEnd() {
        // Updates the text of the EditTexts
        hueText.setText(Integer.valueOf(hueBar.getProgress()).toString());
        saturationText.setText(Integer.valueOf(saturationBar.getProgress()).toString());
        lightnessText.setText(Integer.valueOf(lightnessBar.getProgress()).toString());
        opacityText.setText(Integer.valueOf(opacityBar.getProgress()).toString());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // A touch to the ColourPickerView modified the bars,
        // returning from here avoids a circular call back to it
        if (fromUser == false) {
            return;
        }

        switch (seekBar.getId()) {
            case R.id.sb_hue:
                colourPickerView.setHue((float) seekBar.getProgress());
                //hueText.setText(Integer.toString(seekBar.getProgress()));
                break;
            case R.id.sb_saturation:
                colourPickerView.setSaturation((float) seekBar.getProgress() / (float) seekBar.getMax());
                //saturationText.setText(Integer.toString(seekBar.getProgress()));
                break;
            case R.id.sb_lightness:
                colourPickerView.setLightness((float) seekBar.getProgress() / (float) seekBar.getMax());
                //lightnessText.setText(Integer.toString(seekBar.getProgress()));
                break;
            case R.id.sb_opacity:
                opacity = seekBar.getProgress();
                onColourSelect(colour);
                //opacityText.setText(Integer.toString(seekBar.getProgress()));
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // No implementation
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Updates the text of the EditTexts (only at the end, because it causes a lot of garbage collection)
        switch (seekBar.getId()) {
            case R.id.sb_hue:
                hueText.setText(Integer.valueOf(hueBar.getProgress()).toString());
                break;
            case R.id.sb_saturation:
                saturationText.setText(Integer.toString(seekBar.getProgress()));
                break;
            case R.id.sb_lightness:
                lightnessText.setText(Integer.toString(seekBar.getProgress()));
                break;
            case R.id.sb_opacity:
                opacityText.setText(Integer.toString(seekBar.getProgress()));
                break;
        }
    }

    private void addTextWatchers() {
        saturationText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // No implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence == "") {
                    hueText.setText("0");
                } else {
                    hueBar.setProgress(Integer.parseInt(charSequence.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No implementation
            }
        });

        saturationText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // No implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence == "") {
                    saturationText.setText("0");
                } else {
                    saturationBar.setProgress(Integer.parseInt(charSequence.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No implementation
            }
        });

        lightnessText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // No implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence == "") {
                    lightnessText.setText("0");
                } else {
                    lightnessBar.setProgress(Integer.parseInt(charSequence.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No implementation
            }
        });

        opacityText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // No implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence == "") {
                    opacityText.setText("0");
                } else {
                    opacityBar.setProgress(Integer.parseInt(charSequence.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No implementation
            }
        });
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        // TODO: Rather than redefining the animation, retrieve the enter animation resource
        // Modified version of this StackOverflow answer http://stackoverflow.com/a/23427739/1702627
        // Notifies of when the listener when the enter animation ends (to hide occluded fragments)
        Animation anim = null;
        if (enter) {
            anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // No implementation
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (onColourPickerAnimationEndListener != null) {
                        onColourPickerAnimationEndListener.onColourPickerAnimationEnd();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // No implementation
                }
            });
        } else {
            anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);
        }

        return anim;
    }

    public void setOnColourUpdateListener(OnColourUpdateListener onColourUpdateListener) {
        this.onColourUpdateListener = onColourUpdateListener;
    }

    public void setOnColourPickerAnimationEndListener(OnColourPickerAnimationEndListener onColourPickerAnimationEndListener) {
        this.onColourPickerAnimationEndListener = onColourPickerAnimationEndListener;
    }

    @Override
    public boolean onLongClick(View view) {
        // Consumes long clicks to stop a new CAB from opening and thereby closing the colour picker
        return true;
    }

    public interface OnColourUpdateListener {
        public void onModifyPalette(int colour);
    }

    public interface OnColourPickerAnimationEndListener {
        public void onColourPickerAnimationEnd();
    }
}
