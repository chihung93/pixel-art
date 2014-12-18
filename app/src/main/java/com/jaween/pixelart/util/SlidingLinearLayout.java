package com.jaween.pixelart.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by ween on 12/18/14.
 */
public class SlidingLinearLayout extends LinearLayout {

    public SlidingLinearLayout(Context context) {
        super(context);
    }

    public SlidingLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setYFraction(final float fraction) {
        float translationY = getHeight() * fraction - getHeight();
        setTranslationY(translationY);
    }

    public float getYFraction() {
        if (getHeight() == 0) {
            return 0;
        }
        return (getTranslationY() + getHeight()) / getHeight();
    }
}
