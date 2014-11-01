package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by ween on 11/1/14.
 */
public class ToolButton extends ImageButton {

    private boolean selected = false;
    private Paint selectedPaint;

    public ToolButton(Context context) {
        super(context);
        initialisePaints();
    }

    public ToolButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialisePaints();
    }

    public ToolButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialisePaints();
    }

    private void initialisePaints() {
        selectedPaint = new Paint();
        selectedPaint.setColor(Color.LTGRAY);
        selectedPaint.setAlpha(100);
        selectedPaint.setStyle(Paint.Style.FILL);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (selected) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), selectedPaint);
        }
        super.onDraw(canvas);
    }
}
