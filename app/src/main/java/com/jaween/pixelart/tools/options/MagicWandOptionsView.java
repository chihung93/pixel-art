package com.jaween.pixelart.tools.options;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.attributes.MagicWandToolAttributes;
import com.jaween.pixelart.tools.attributes.ToolAttributes;

public class MagicWandOptionsView extends ToolOptionsView implements
        SeekBar.OnSeekBarChangeListener {

    private EditText thresholdEditText;
    private SeekBar thresholdSeekBar;

    private int initialThreshold;

    public MagicWandOptionsView(Context context) {
        super(context);
        initialiseViews(context);
    }

    public MagicWandOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseViews(context);
    }

    public MagicWandOptionsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialiseViews(context);
    }

    private void initialiseViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewGroup = (ViewGroup) inflater.inflate(R.layout.tool_options_magic_wand, null);

        initialThreshold = context.getResources().getInteger(R.integer.tool_magic_wand_initial_threshold);

        thresholdEditText = (EditText) viewGroup.findViewById(R.id.et_threshold);
        thresholdEditText.setText(Integer.toString(initialThreshold));
        addTextWatchers();

        thresholdSeekBar = (SeekBar) viewGroup.findViewById(R.id.sb_threshold);
        thresholdSeekBar.setProgress(initialThreshold);
        thresholdSeekBar.setOnSeekBarChangeListener(this);

        addView(viewGroup);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            // Must offset the progress by 1 as seekbars begin at 0
            case R.id.sb_threshold:
                ((MagicWandToolAttributes) toolAttributes).setThreshold(progress + 1);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // No implementation
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.sb_threshold:
                thresholdEditText.setText(Integer.valueOf(seekBar.getProgress()).toString());
                break;
        }
    }

    private void addTextWatchers() {
        thresholdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // No implementation
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.toString().isEmpty()) {
                    thresholdEditText.setText("0");
                } else {
                    thresholdSeekBar.setProgress(Integer.parseInt(charSequence.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No implementation
            }
        });
    }

    @Override
    public void setToolAttributes(ToolAttributes toolToolAttributes) {
        super.setToolAttributes(toolToolAttributes);
        ((MagicWandToolAttributes) toolToolAttributes).setThreshold(initialThreshold);
    }
}
