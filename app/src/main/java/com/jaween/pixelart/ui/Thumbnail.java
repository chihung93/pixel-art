package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class Thumbnail {

    // Thumbnail View
    private boolean enabled = true;

    // Thumbnail Metrics
    private RectF thumbnailRectF = new RectF();

    // View window
    private boolean viewWindowEnabled = true;
    private RectF viewWindowRect = new RectF();

    // Checkerboard
    private TransparencyCheckerboard transparencyCheckerboard;

    // Paints
    private Paint viewWindowPaint;

    // Colour preferences
    private static final int VIEW_WINDOW_COLOUR = Color.LTGRAY;
    private static final int VIEW_WINDOW_OPACITY = 170;
    
    public Thumbnail(Context context, RectF thumbnailRectF) {
        transparencyCheckerboard = new TransparencyCheckerboard(context);
        initialisePaints();
        this.thumbnailRectF = thumbnailRectF;
    }

    private void initialisePaints() {
        // Thumbnail window
        viewWindowPaint = new Paint();
        viewWindowPaint.setStyle(Paint.Style.STROKE);
        viewWindowPaint.setColor(VIEW_WINDOW_COLOUR);
        viewWindowPaint.setAlpha(VIEW_WINDOW_OPACITY);
        viewWindowPaint.setStyle(Paint.Style.FILL);
        viewWindowPaint.setAntiAlias(false);
    }

    /**
     * Draws thumbnail given a canvas to draw on to, the unscaled bitmap and the unscaled viewport
     * @param canvas The canvas to draw into
     * @param bitmap The bitmap to be drawn as a thumbnail
     * @param viewport The viewing area of the bitmap
     * @param shadowPaint The paint to draw the shadow around the thumbnail
     */
    public void draw(Canvas canvas, Bitmap bitmap, RectF viewport, Paint shadowPaint) {
        // Border shadow
        canvas.drawRect(thumbnailRectF, shadowPaint);

        // Transparency checkerboard
        transparencyCheckerboard.draw(canvas, thumbnailRectF);

        // Thumbnail
        canvas.drawBitmap(bitmap, null, thumbnailRectF, null);

        // View Window (portion of the image being viewed)
        if (viewWindowEnabled) {
            calculateViewWindow(viewport, bitmap.getWidth(), bitmap.getHeight());
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

    // Determines the edges of the scaled viewport, the viewWindowRect
    private void calculateViewWindow(RectF viewport, int bitmapWidth, int bitmapHeight) {
        // Left
        if (viewport.left < 0) {
            viewWindowRect.left = thumbnailRectF.left;
        } else if (viewport.left > bitmapWidth) {
            viewWindowRect.left = thumbnailRectF.right;
        } else {
            // In
            viewWindowRect.left = thumbnailRectF.left + (viewport.left / bitmapWidth)
                    * thumbnailRectF.width();
        }

        // Top
        if (viewport.top < 0) {
            viewWindowRect.top = thumbnailRectF.top;
        } else if (viewport.top > bitmapHeight) {
            viewWindowRect.top = thumbnailRectF.bottom;
        } else {
            viewWindowRect.top = thumbnailRectF.top + (viewport.top / bitmapHeight)
                    * thumbnailRectF.height();
        }

        // Right
        if (viewport.right > bitmapWidth) {
            viewWindowRect.right = thumbnailRectF.right;
        } else if (viewport.right < 0) {
            viewWindowRect.right = thumbnailRectF.left;
        } else {
            viewWindowRect.right = thumbnailRectF.left + (viewport.right / bitmapWidth)
                    * thumbnailRectF.width();
        }

        // Bottom
        if (viewport.bottom > bitmapHeight) {
            viewWindowRect.bottom = thumbnailRectF.bottom;
        } else if (viewport.bottom < 0) {
            viewWindowRect.bottom = thumbnailRectF.top;
        } else {
            viewWindowRect.bottom = thumbnailRectF.top + (viewport.bottom / bitmapHeight)
                    * thumbnailRectF.height();
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
