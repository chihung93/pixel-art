package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

import com.jaween.pixelart.R;

/**
 * Created by ween on 10/24/14.
 */
public class ColourButton extends Button {

    private int colour = Color.WHITE;
    private static final float LIGHTEN_FACTOR = 1.3f;
    private static final float DARKEN_FACTOR = 0.7f;

    /*private float[] hsvDarken = new float[3];
    private float[] hsvLighten = new float[3];*/

    private Paint selectedPaint;
    private boolean selected = false;

    /*(private Drawable pressed;
    private Drawable focused;
    private Drawable normal;*/

    //private ColorDrawable pressed;
    //private ColorDrawable focused;
    //private ColorDrawable normal;

    //private int[] statePressed = new int [] { android.R.attr.state_pressed };
    //private int[] stateFocused = new int [] { android.R.attr.state_focused };
    //private int[] stateNormal = new int [] { /* No state attribute */ };

    private float radius;
    private float dp;

    private Rect bounds = new Rect();

    public ColourButton(Context context) {
        super(context);
        init(context);
    }

    public ColourButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColourButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        dp = context.getResources().getDisplayMetrics().density;
        /*pressed = getResources().getDrawable(R.drawable.palette_menu_item);
        focused = getResources().getDrawable(R.drawable.palette_menu_item);
        normal = getResources().getDrawable(R.drawable.palette_menu_item);

        pressed.mutate();
        focused.mutate();
        normal.mutate();*/

        /*pressed = new ColorDrawable(colour);
        focused = new ColorDrawable(colour);
        normal = new ColorDrawable(colour);*/

        selectedPaint = new Paint();
        selectedPaint.setColor(Color.LTGRAY);
        selectedPaint.setAlpha(100);
        selectedPaint.setStyle(Paint.Style.FILL);
    }

    public void setColour(int colour) {
        /*Color.colorToHSV(colour, hsvDarken);
        hsvDarken[2] = hsvDarken[2] * DARKEN_FACTOR;
        int darker = Color.HSVToColor(hsvDarken);

        Color.colorToHSV(colour, hsvLighten);
        hsvLighten[2] = hsvLighten[2] * LIGHTEN_FACTOR;
        int lighter = Color.HSVToColor(hsvLighten);

        // Pre-Honeycomb doesn't have colorDrawable.setColor()
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.HONEYCOMB) {
            pressed.setColorFilter(darker, PorterDuff.Mode.MULTIPLY);
            focused.setColorFilter(lighter, PorterDuff.Mode.MULTIPLY);
            normal.setColorFilter(Color.MAGENTA, PorterDuff.Mode.MULTIPLY);
        } else {
            pressed.setColor(darker);
            focused.setColor(lighter);
            normal.setColor(colour);
        }*/

        /*StateListDrawable states = new StateListDrawable();
        states.addState(statePressed, pressed);
        states.addState(stateFocused, focused);
        states.addState(stateNormal, normal);*/

        // Tints the inner square to the selected colour
        Drawable colouredInner = getResources().getDrawable(R.drawable.palette_colour_button);
        Drawable border = getResources().getDrawable(R.drawable.palette_colour_button_border);
        LayerDrawable layerDrawable = com.jaween.pixelart.util.Color.tintAndLayerDrawable(colouredInner, border, colour);

        // Pre-Jellybean doesn't have setBackground()
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(layerDrawable);
        } else {
            setBackground(layerDrawable);
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        bounds.set(0, 0, w, h);
        radius = 15 * dp;

        //setPadding(padding, padding, padding, padding);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (selected) {
            canvas.drawRect(bounds, selectedPaint);
        }
    }
}
