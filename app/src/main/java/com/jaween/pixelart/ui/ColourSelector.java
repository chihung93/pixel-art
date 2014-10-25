package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by ween on 10/25/14.
 */
public class ColourSelector extends View {

    private Paint selectorPaint;

    public ColourSelector(Context context) {
        super(context);
        selectorPaint = new Paint();
        selectorPaint.setColor(Color.MAGENTA);
        selectorPaint.setStyle(Paint.Style.STROKE);
    }

    public ColourSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColourSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(getWidth()/2, getHeight()/2, Math.min(getWidth(), getHeight()), selectorPaint);
        canvas.drawRect(0, 0, getWidth(), getHeight(), selectorPaint);
    }
}
