package com.jaween.pixelart.tools.options;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.attributes.RectToolAttributes;

/**
 * Created by ween on 11/2/14.
 */
public class RectOptionsView extends ToolOptionsView implements
        CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {

    private SeekBar roundnessSeekBar;
    private ImageView roundnessLeft;
    private ImageView roundnessRight;

    public RectOptionsView(Context context) {
        super(context);
        initialiseViews(context);
    }

    public RectOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseViews(context);
    }

    public RectOptionsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialiseViews(context);
    }

    private void initialiseViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.tool_options_rect, null);

        CheckBox squareLockCheckBox = (CheckBox) view.findViewById(R.id.cb_option_square_lock);
        squareLockCheckBox.setOnCheckedChangeListener(this);

        CheckBox fillCheckbox = (CheckBox) view.findViewById(R.id.cb_option_inner_fill);
        fillCheckbox.setOnCheckedChangeListener(this);

        CheckBox antiAliasCheckBox = (CheckBox) view.findViewById(R.id.cb_option_aa);
        antiAliasCheckBox.setOnCheckedChangeListener(this);

        CheckBox roundedCheckbox = (CheckBox) view.findViewById(R.id.cb_option_rounded);
        roundedCheckbox.setOnCheckedChangeListener(this);

        SeekBar thicknessSeekBar = (SeekBar) view.findViewById(R.id.sb_option_thickness);
        thicknessSeekBar.setOnSeekBarChangeListener(this);

        roundnessSeekBar = (SeekBar) view.findViewById(R.id.sb_option_roundness);
        roundnessSeekBar.setOnSeekBarChangeListener(this);

        roundnessLeft = (ImageView) view.findViewById(R.id.iv_option_roundness_left);
        roundnessRight = (ImageView) view.findViewById(R.id.iv_option_roundness_right);

        addView(view);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_option_square_lock:
                ((RectToolAttributes) toolAttributes).setSquareLocked(isChecked);
                break;
            case R.id.cb_option_inner_fill:
                ((RectToolAttributes) toolAttributes).setFill(isChecked);
                break;
            case R.id.cb_option_aa:
                ((RectToolAttributes) toolAttributes).setAntiAlias(isChecked);
                break;
            case R.id.cb_option_rounded:
                // Shows/Hides the roundness seekbar and its roundness indicators
                if (isChecked == false) {
                    roundnessSeekBar.setVisibility(GONE);
                    roundnessLeft.setVisibility(GONE);
                    roundnessRight.setVisibility(GONE);
                } else {
                    roundnessSeekBar.setVisibility(VISIBLE);
                    roundnessLeft.setVisibility(VISIBLE);
                    roundnessRight.setVisibility(VISIBLE);
                }
                ((RectToolAttributes) toolAttributes).setRoundedRect(isChecked);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            // Must offset the progress by 1 as seekbars begins at 0
            case R.id.sb_option_thickness:
                ((RectToolAttributes) toolAttributes).setThicknessLevel(progress + 1);
                break;
            case R.id.sb_option_roundness:
                ((RectToolAttributes) toolAttributes).setRoundnessLevel(progress + 1);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
