package com.jaween.pixelart.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

public class Thumbnail {

    // Thumbnail View
    private boolean enabled = true;

    // Thumbnail Metrics
    private RectF thumbnailRectF = new RectF();
    private Rect thumbnailRect = new Rect();
    private float dp;

    // View window
    private boolean viewWindowEnabled = true;
    private RectF viewWindowRect = new RectF();
    private RectF scaledViewport = new RectF();

    // Paints
    private Paint viewWindowPaint;

    // Colour preferences
    private static final int VIEW_WINDOW_COLOUR = Color.LTGRAY;
    private static final int VIEW_WINDOW_TRANSPARENCY = 130;
    
    public Thumbnail(float left, float top, float width, float height, float dp) {
        thumbnailRectF.left = left;
        thumbnailRectF.top = top;
        thumbnailRectF.right = left + width;
        thumbnailRectF.bottom = top + height;

        this.dp = dp;

        initialisePaints();
    }

    private void initialisePaints() {
        // Thumbnail window
        viewWindowPaint = new Paint();
        viewWindowPaint.setStyle(Paint.Style.STROKE);
        viewWindowPaint.setColor(VIEW_WINDOW_COLOUR);
        viewWindowPaint.setAlpha(VIEW_WINDOW_TRANSPARENCY);
        viewWindowPaint.setStyle(Paint.Style.FILL);
        viewWindowPaint.setAntiAlias(false);
    }

    // Draws thumbnail given a canvas to draw on to, the unscaled bitmap and the unscaled viewport
    public void draw(Canvas canvas, Bitmap bitmap, RectF viewport, BitmapDrawable checkerboardTile, Paint bitmapPaint, Paint shadowPaint) {
        // Border
        canvas.drawRect(thumbnailRectF, shadowPaint);

        // Transparency checkerboard
        thumbnailRectF.round(thumbnailRect);
        checkerboardTile.setBounds(thumbnailRect);
        checkerboardTile.draw(canvas);

        // Thumbnail
        canvas.drawBitmap(bitmap, null, thumbnailRectF, bitmapPaint);

        // View Window (portion of the image being viewed)
        if (viewWindowEnabled) {
            scaledViewport.set(viewport.left * dp, viewport.top * dp, viewport.right * dp, viewport.bottom * dp);
            constrainViewWindow(scaledViewport);
            drawViewWindow(canvas);
        }
    }

    // Uses four semi-transparent rectangles around the view window to indicate viewing region,
    // visually similar to letterboxing and pillarboxing
    private void drawViewWindow(Canvas canvas) {
        // Top semi-transparent overlay
        canvas.drawRect(
                thumbnailRectF.left,
                thumbnailRectF.top,
                thumbnailRectF.right,
                viewWindowRect.top,
                viewWindowPaint);

        // Bottom semi-transparent overlay
        canvas.drawRect(
                thumbnailRectF.left,
                viewWindowRect.bottom,
                thumbnailRectF.right,
                thumbnailRectF.bottom,
                viewWindowPaint);

        // Left semi-transparent overlay
        canvas.drawRect(
                thumbnailRectF.left,
                viewWindowRect.top,
                viewWindowRect.left,
                viewWindowRect.bottom,
                viewWindowPaint);

        // Right semi-transparent overlay
        canvas.drawRect(
                viewWindowRect.right,
                viewWindowRect.top,
                thumbnailRectF.right,
                viewWindowRect.bottom,
                viewWindowPaint);
    }

    // Determines the viewing area on the thumbnail (viewWindowRect) based on the area displayed on
    // the screen (viewportRect) and the location of the thumbnail (thumbnailRectF)
    private void constrainViewWindow(RectF viewport) {
        // Left
        if (viewport.left < 0) {
            viewWindowRect.left = thumbnailRectF.left;
        } else if (viewport.left > thumbnailRectF.width()) {
            viewWindowRect.left = thumbnailRectF.right;
        } else {
            viewWindowRect.left = thumbnailRectF.left + viewport.left;
        }

        // Top
        if (viewport.top < 0) {
            viewWindowRect.top = thumbnailRectF.top;
        } else if (viewport.top > thumbnailRectF.height()) {
            viewWindowRect.top = thumbnailRectF.bottom;
        } else if (viewport.top < thumbnailRectF.height()) {
            viewWindowRect.top = thumbnailRectF.top + viewport.top;
        }

        // Right
        if (viewport.right > thumbnailRectF.width()) {
            viewWindowRect.right = thumbnailRectF.right;
        } else if (viewport.right < 0) {
            viewWindowRect.right = thumbnailRectF.left;
        } else {
            viewWindowRect.right = thumbnailRectF.left + viewport.right;
        }

        // Bottom
        if (viewport.bottom > thumbnailRectF.height()) {
            viewWindowRect.bottom = thumbnailRectF.bottom;
        } else if (viewport.bottom < 0) {
            viewWindowRect.bottom = thumbnailRectF.top;
        } else {
            viewWindowRect.bottom = thumbnailRectF.top + viewport.bottom;
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
