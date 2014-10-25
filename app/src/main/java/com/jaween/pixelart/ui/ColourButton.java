package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by ween on 10/24/14.
 */
public class ColourButton extends Button {

    private int colour = Color.WHITE;
    private static final float LIGHTEN_FACTOR = 1.3f;
    private static final float DARKEN_FACTOR = 0.7f;

    private float[] hsvDarken = new float[3];
    private float[] hsvLighten = new float[3];

    private Paint selectedPaint;
    private boolean selected = false;

    /*private Drawable pressed;
    private Drawable focused;
    private Drawable normal;*/

    private ColorDrawable pressed;
    private ColorDrawable focused;
    private ColorDrawable normal;

    public ColourButton(Context context) {
        super(context);

        /*pressed = getResources().getDrawable(R.drawable.palette_colour_button);
        focused = getResources().getDrawable(R.drawable.palette_colour_button);
        normal = getResources().getDrawable(R.drawable.palette_colour_button);

        pressed.mutate();
        focused.mutate();
        normal.mutate();*/

        pressed = new ColorDrawable(colour);
        focused = new ColorDrawable(colour);
        normal = new ColorDrawable(colour);

        selectedPaint = new Paint();
        selectedPaint.setColor(Color.BLACK);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(8);
    }

    public ColourButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColourButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setColour(int colour) {
        Color.colorToHSV(colour, hsvDarken);
        hsvDarken[2] = hsvDarken[2] * DARKEN_FACTOR;
        int darker = Color.HSVToColor(hsvDarken);

        Color.colorToHSV(colour, hsvLighten);
        hsvLighten[2] = hsvLighten[2] * LIGHTEN_FACTOR;
        int lighter = Color.HSVToColor(hsvLighten);

        /*pressed.setColorFilter(darker, PorterDuff.Mode.MULTIPLY);
        focused.setColorFilter(lighter, PorterDuff.Mode.MULTIPLY);
        normal.setColorFilter(Color.MAGENTA, PorterDuff.Mode.MULTIPLY);*/

        pressed.setColor(darker);
        focused.setColor(lighter);
        normal.setColor(colour);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);

        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(states);
        } else {
            setBackground(states);
        }

        this.colour = colour;
    }

    public int getColour() {
        return colour;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (selected) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), selectedPaint);
        }
    }
}
