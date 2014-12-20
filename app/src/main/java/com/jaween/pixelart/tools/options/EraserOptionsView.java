package com.jaween.pixelart.tools.options;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.attributes.EraserToolAttributes;

/**
 * Created by ween on 11/2/14.
 */
public class EraserOptionsView extends ToolOptionsView implements
        CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {

    public EraserOptionsView(Context context) {
        super(context);
        initialiseViews(context);
    }

    public EraserOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseViews(context);
    }

    public EraserOptionsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialiseViews(context);
    }

    private void initialiseViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewGroup = (ViewGroup) inflater.inflate(R.layout.tool_options_eraser, null);

        CheckBox antiAliasCheckBox = (CheckBox) viewGroup.findViewById(R.id.cb_option_aa);
        antiAliasCheckBox.setOnCheckedChangeListener(this);

        SeekBar thicknessSeekBar = (SeekBar) viewGroup.findViewById(R.id.sb_option_thickness);
        thicknessSeekBar.setOnSeekBarChangeListener(this);

        addView(viewGroup);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cb_option_aa:
                ((EraserToolAttributes) toolAttributes).setAntiAlias(isChecked);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sb_option_thickness:
                // Must offset the progress by 1 as seekbar begins at 0
                ((EraserToolAttributes) toolAttributes).setThicknessLevel(progress + 1);
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
