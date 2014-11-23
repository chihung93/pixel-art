package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by ween on 10/25/14.
 */
public class ColourSelector extends View {

    private Paint selectorPaint;
    private RectF bounds = new RectF();
    private float cornerRadius;

    public ColourSelector(Context context) {
        super(context);
        init(context);
    }

    public ColourSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColourSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        selectorPaint = new Paint();
        selectorPaint.setColor(Color.MAGENTA);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(4);
        selectorPaint.setAntiAlias(true);

        cornerRadius = 2 * context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        bounds.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawCircle(, selectorPaint);
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, selectorPaint);
    }
}
