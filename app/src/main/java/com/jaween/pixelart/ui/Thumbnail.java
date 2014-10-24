package com.jaween.pixelart.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Thumbnail {

    // Thumbnail View
    private boolean enabled = true;

    // Thumbnail Metrics
    private RectF thumbnailRect = new RectF();
    private float scale;
    private float dp;

    // View window
    private boolean viewWindowEnabled = true;
    private RectF viewWindowRect = new RectF();

    // Paints
    private Paint viewWindowPaint;
    private Paint borderPaint;
    private Paint bitmapPaint;

    // Colour preferences
    private static final int THUMBNAIL_BORDER_COLOUR = Color.DKGRAY;
    private static final int VIEW_WINDOW_COLOUR = Color.LTGRAY;
    private static final int VIEW_WINDOW_TRANSPARENCY = 130;
    
    public Thumbnail(float left, float top, float width, float height, float dp) {
        thumbnailRect.left = left;
        thumbnailRect.top = top;
        thumbnailRect.right = left + width;
        thumbnailRect.bottom = top + height;

        this.dp = dp;

        initialisePaints();
    }

    private void initialisePaints() {
        // Thumbnail border
        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(THUMBNAIL_BORDER_COLOUR);
        borderPaint.setStrokeWidth(dp);
        borderPaint.setAntiAlias(false);

        // Thumbnail window
        viewWindowPaint = new Paint();
        viewWindowPaint.setStyle(Paint.Style.STROKE);
        viewWindowPaint.setColor(VIEW_WINDOW_COLOUR);
        viewWindowPaint.setAlpha(VIEW_WINDOW_TRANSPARENCY);
        viewWindowPaint.setStyle(Paint.Style.FILL);
        viewWindowPaint.setAntiAlias(false);

        // Purely used to blit bitmaps
        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(false);
    }

    // Draws thumbnail given a canvas to draw on to, the unscaled bitmap and the unscaled viewport
    public void draw(Canvas canvas, Bitmap bitmap, RectF viewport) {
        // Thumbnail
        canvas.drawBitmap(bitmap, null, thumbnailRect, bitmapPaint);

        // Border
        canvas.drawRect(thumbnailRect, borderPaint);

        // View Window (portion of the image being viewed)
        if (viewWindowEnabled) {
           constrainViewWindow(viewport);
           drawViewWindow(canvas);
        }
    }

    // Uses four semi-transparent rectangles around the view window to indicate viewing region,
    // visually similar to letterboxing and pillarboxing
    private void drawViewWindow(Canvas canvas) {
        // Top semi-transparent overlay
        canvas.drawRect(
                thumbnailRect.left,
                thumbnailRect.top,
                thumbnailRect.right,
                viewWindowRect.top,
                viewWindowPaint);

        // Bottom semi-transparent overlay
        canvas.drawRect(
                thumbnailRect.left,
                viewWindowRect.bottom,
                thumbnailRect.right,
                thumbnailRect.bottom,
                viewWindowPaint);

        // Left semi-transparent overlay
        canvas.drawRect(
                thumbnailRect.left,
                viewWindowRect.top,
                viewWindowRect.left,
                viewWindowRect.bottom,
                viewWindowPaint);

        // Right semi-transparent overlay
        canvas.drawRect(
                viewWindowRect.right,
                viewWindowRect.top,
                thumbnailRect.right,
                viewWindowRect.bottom,
                viewWindowPaint);
    }

    // Takes a viewport rect i
    private void constrainViewWindow(RectF viewport) {
        // Left
        if (viewport.left < 0) {
            viewWindowRect.left = thumbnailRect.left;
        } else if (viewport.left > thumbnailRect.width()) {
            viewWindowRect.left = thumbnailRect.right;
        } else {
            viewWindowRect.left = thumbnailRect.left + viewport.left;
        }

        // Top
        if (viewport.top < 0) {
            viewWindowRect.top = thumbnailRect.top;
        } else if (viewport.top > thumbnailRect.height()) {
            viewWindowRect.top = thumbnailRect.bottom;
        } else if (viewport.top < thumbnailRect.height()) {
            viewWindowRect.top = thumbnailRect.top + viewport.top;
        }

        // Right
        if (viewport.right > thumbnailRect.width()) {
            viewWindowRect.right = thumbnailRect.right;
        } else if (viewport.right < 0) {
            viewWindowRect.right = thumbnailRect.left;
        } else {
            viewWindowRect.right = thumbnailRect.left + viewport.right;
        }

        // Bottom
        if (viewport.bottom > thumbnailRect.height()) {
            viewWindowRect.bottom = thumbnailRect.bottom;
        } else if (viewport.bottom < 0) {
            viewWindowRect.bottom = thumbnailRect.top;
        } else {
            viewWindowRect.bottom = thumbnailRect.top + viewport.bottom;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setViewWindowEnable(boolean enabled) {
        viewWindowEnabled = enabled;
    }

    public boolean isViewWindowEnabled() {
        return viewWindowEnabled;
    }
}
