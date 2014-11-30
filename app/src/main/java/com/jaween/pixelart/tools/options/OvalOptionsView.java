package com.jaween.pixelart.tools.options;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.attributes.OvalToolAttributes;

/**
 * Created by ween on 11/2/14.
 */
public class OvalOptionsView extends ToolOptionsView implements
        CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {


    public OvalOptionsView(Context context) {
        super(context);
        initialiseViews(context);
    }

    public OvalOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseViews(context);
    }

    public OvalOptionsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialiseViews(context);
    }

    private void initialiseViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.tool_options_oval, null);

        CheckBox circleLockCheckBox = (CheckBox) view.findViewById(R.id.cb_option_circle_lock);
        circleLockCheckBox.setOnCheckedChangeListener(this);

        CheckBox fillCheckbox = (CheckBox) view.findViewById(R.id.cb_option_inner_fill);
        fillCheckbox.setOnCheckedChangeListener(this);

        CheckBox antiAliasCheckBox = (CheckBox) view.findViewById(R.id.cb_option_aa);
        antiAliasCheckBox.setOnCheckedChangeListener(this);

        SeekBar thicknessSeekBar = (SeekBar) view.findViewById(R.id.sb_option_thickness);
        thicknessSeekBar.setOnSeekBarChangeListener(this);

        addView(view);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_option_circle_lock:
                ((OvalToolAttributes) toolAttributes).setCircleLocked(isChecked);
                break;
            case R.id.cb_option_inner_fill:
                ((OvalToolAttributes) toolAttributes).setFill(isChecked);
                break;
            case R.id.cb_option_aa:
                ((OvalToolAttributes) toolAttributes).setAntiAlias(isChecked);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sb_option_thickness:
                // Must offset the progress by 1 as seekbar begins at 0
                ((OvalToolAttributes) toolAttributes).setThicknessLevel(progress + 1);
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
