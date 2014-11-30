package com.jaween.pixelart.ui.colourpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.jaween.pixelart.util.Color;

import java.util.ArrayList;

/**
 * Created by ween on 11/21/14.
 */
public class ColourPickerView extends View {

    private Paint bitmapPaint = new Paint();
    private Paint circleBorderPaint = new Paint();
    private Paint selectedPointPaint = new Paint();

    private PointF selectedPoint = new PointF();
    private float selectedPointWidth;

    private Rect bounds = new Rect();
    private float radius;
    private float dp;

    private static float[] hsl = new float[3];
    private float hue = 0f;
    private float saturation = 1f;
    private float lightness = 0.5f;

    private Bitmap colourWheel;
    private boolean colourPickerGenerated = false;

    private OnColourSelectListener onColourSelectListener = null;

    public ColourPickerView(Context context) {
        super(context);
        init(context);
    }

    public ColourPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ColourPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        dp = context.getResources().getDisplayMetrics().density;

        bitmapPaint.setColor(android.graphics.Color.MAGENTA);
        bitmapPaint.setStyle(Paint.Style.STROKE);

        circleBorderPaint.setColor(android.graphics.Color.BLACK);
        circleBorderPaint.setStrokeWidth(dp);
        circleBorderPaint.setStyle(Paint.Style.STROKE);
        circleBorderPaint.setAntiAlias(true);

        selectedPointWidth = 4*dp;
        selectedPointPaint.setColor(android.graphics.Color.WHITE);
        selectedPointPaint.setStrokeWidth(selectedPointWidth);
        selectedPointPaint.setStyle(Paint.Style.STROKE);
        selectedPointPaint.setShadowLayer(selectedPointWidth, 0, selectedPointWidth / 2, android.graphics.Color.DKGRAY);
        selectedPointPaint.setAntiAlias(true);

    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
        invalidateSelectedColour(false);
    }

    public float getHue() {
        return hue;
    }

    public void setHue(float hue) {
        this.hue = hue;
        invalidateSelectedColour(false);
    }

    public float getLightness() {
        return lightness;
    }

    public void setLightness(float lightness) {
        this.lightness = lightness;
        invalidateSelectedColour(false);
    }

    public void setHSL(float hue, float saturation, float lightness, boolean fromPalette) {
        this.hue = hue;
        this.saturation = saturation;
        this.lightness = lightness;

        invalidateSelectedColour(fromPalette);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        bounds.left = getPaddingLeft();
        bounds.top = getPaddingTop();
        bounds.right = w - getPaddingRight() - bounds.left;
        bounds.bottom = h - getPaddingBottom() - bounds.top;

        int smallerDimension = Math.min(bounds.width(), bounds.height());
        radius = smallerDimension / 2f;

        selectedPoint.set(bounds.centerX(), bounds.centerY());

        // Fills out the colour picker
        colourWheel = Bitmap.createBitmap(smallerDimension, smallerDimension, Bitmap.Config.ARGB_8888);
        ArrayList<Object> data = new ArrayList<Object>();
        data.add(colourWheel);
        data.add(radius);
        data.add(saturation);
        data.add(this);
        ColourPickerGenerator colourPickerGenerator = new ColourPickerGenerator();
        colourPickerGenerator.execute(data);
    }

    // Outputs the colour (in HSL) that these coordinates represent on a colour rectangle
    private float[] coordsToRectangularHSL(float x, float y, float z, float width, float height, float[] hsl) {
        float h = 360f * x / width;
        float s = z;
        float l = 1 - y / height;
        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;

        return hsl;
    }

    // Outputs the colour (in HSL) that these coordinates represent on a colour wheel
    private static float[] coordsToCirclularHSL(float x, float y, float z, float centerX, float centerY, float radius, float[] hsl) {
        float angle = (float) Math.atan2(y - centerX, x - centerX);
        float dist = (float) Math.hypot(x - centerY, y - centerY);

        float hPrime = (float) (angle * 180f / Math.PI + 180f);
        float s = z;
        float l = Math.min(dist / radius, 1f);

        if (dist > radius) {
            return null;
        }

        hsl[0] = hPrime;
        hsl[1] = s;
        hsl[2] = l;
        return hsl;
    }

    private void clampWithinCircle(PointF point) {
        float angle = (float) Math.atan2(point.y - bounds.centerY(), point.x - bounds.centerX());
        float dist = (float) Math.hypot(point.x - bounds.centerX(), point.y - bounds.centerY());

        if (dist > radius) {
            point.x = bounds.centerX() + (float) (radius*Math.cos(angle));
            point.y = bounds.centerY() + (float) (radius*Math.sin(angle));
        }
    }

    // Updates the selector reticule
    private void invalidateSelectedColour(boolean fromPalette) {
        int colour = Color.HSLToColor(hue, saturation, lightness);

        float angleRadians = (float) (hue * Math.PI / 180f);
        selectedPoint.x = bounds.centerX() - (float) (radius * lightness * Math.cos(angleRadians));
        selectedPoint.y = bounds.centerY() - (float) (radius * lightness * Math.sin(angleRadians));

        if (!fromPalette) {
            if (onColourSelectListener != null) {
                onColourSelectListener.onColourSelect(colour);
            }
        }

        invalidate();
    }

    private void colourPickerGenerated() {
        colourPickerGenerated = true;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectColour(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                selectColour(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                if (onColourSelectListener != null) {
                    onColourSelectListener.onColourSelectEnd();
                }
            default:
                return false;
        }
        return true;
    }

    // Sets the selected colour to the coordinates passed in
    private void selectColour(float x, float y) {
        selectedPoint.set(x, y);

        // Coordinates outside the circle are brought back inside
        clampWithinCircle(selectedPoint);

        // Converts the coordinates into the corresponding HSL value (that is colour under the user's finger)
        float[] hsl = coordsToCirclularHSL(selectedPoint.x, selectedPoint.y, saturation, bounds.centerX(), bounds.centerY(), radius, this.hsl);
        if (hsl != null) {
            hue = hsl[0];
            saturation = hsl[1];
            lightness = hsl[2];

            // Updates the UI selector
            invalidateSelectedColour(false);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Colour wheel
        if (colourPickerGenerated) {
            canvas.drawBitmap(colourWheel, bounds.centerX() - radius, bounds.centerY() - radius, bitmapPaint);
        }

        // Anti-aliased border
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, circleBorderPaint);

        // Selected colour reticule
        // TODO: On the first draw selectedPoint has the wrong value
        canvas.drawCircle(selectedPoint.x, selectedPoint.y, 8*dp, selectedPointPaint);
    }

    public void setOnColourSelectListener(OnColourSelectListener onColourSelectListener) {
        this.onColourSelectListener = onColourSelectListener;
    }

    public interface OnColourSelectListener {
        public void onColourSelect(int colour);
        public void onColourSelectEnd();
    }

    private static class ColourPickerGenerator extends AsyncTask<ArrayList<Object>, Void, Bitmap> {

        private Bitmap bitmap;
        private Float radius;
        private Float saturation;
        private ColourPickerView colourPickerView;
        private float[] hslTemp = new float[3];

        @Override
        protected Bitmap doInBackground(ArrayList<Object>... arrayLists) {
            ArrayList<Object> data = arrayLists[0];
            bitmap = (Bitmap) data.get(0);
            radius = (Float) data.get(1);
            saturation = (Float) data.get(2);
            colourPickerView = (ColourPickerView) data.get(3);
            makeColourPicker(bitmap);
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            colourPickerView.colourPickerGenerated();
            super.onPostExecute(bitmap);
        }

        private void makeColourPicker(Bitmap bitmap) {
            for (int x = 0; x < radius * 2; x++) {
                for (int y = 0; y < radius * 2; y++) {
                    //float[] hsl = coordsToRectangularHSL(x, y, saturation, radius * 2, radius * 2);
                    float[] hsl = coordsToCirclularHSL(x, y, saturation, radius, radius, radius, hslTemp);

                    int colour;
                    if (hsl == null) {
                        colour = android.graphics.Color.TRANSPARENT;
                    } else {
                        colour = Color.HSLToColor(hsl[0], hsl[1], hsl[2]);
                    }
                    bitmap.setPixel(x, y, colour);
                }
            }
        }


    }
}