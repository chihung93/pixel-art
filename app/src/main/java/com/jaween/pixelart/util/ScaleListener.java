package com.jaween.pixelart.util;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by ween on 9/30/14.
 */
public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    private final float horizontalMin;
    private final float horizontalMax;
    private final float verticalMin;
    private final float verticalMax;

    private final float minScale;
    private final float maxScale;

    private PointF viewportFocus = new PointF();
    private RectF viewport = new RectF();
    private RectF contentRect = new RectF();
    private float scale;

    private GestureDetector gestureDetector;
    private boolean isScaling = false;

    public ScaleListener(Context context, float minScale, float maxScale, float initialScale, float containerWidth, float containerHeight) {

        scale = initialScale;

        contentRect.left = 0;
        contentRect.top = 0;
        contentRect.right = containerWidth;
        contentRect.bottom = containerHeight;

        viewport.left = 0;
        viewport.top = 0;
        viewport.right = contentRect.width() / initialScale;
        viewport.bottom = contentRect.height() / initialScale;

        horizontalMin = -containerWidth/2;
        horizontalMax = containerWidth + containerWidth/2;
        verticalMin = -containerHeight/2;
        verticalMax = containerHeight + containerHeight/2;

        gestureDetector = new GestureDetector(context, gestureListener);

        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        scale *= detector.getScaleFactor();

        constrainScale();

        float newWidth = contentRect.width() / scale;
        float newHeight = contentRect.height() / scale;

        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();
        hitTest(focusX, focusY, viewportFocus);

        viewport.set(
                viewportFocus.x - newWidth * (focusX - contentRect.left) / contentRect.width(),
                viewportFocus.y - newHeight * (focusY - contentRect.top) / contentRect.height(),
                0,
                0);

        viewport.right = viewport.left + newWidth;
        viewport.bottom = viewport.top + newHeight;
        //constrainViewport();


        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        isScaling = true;

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isScaling = false;
    }

    private boolean hitTest(float x, float y, PointF destination) {
        if (!contentRect.contains((int) x, (int) y)) {
            return false;
        }

        destination.set(
                viewport.left
                        + viewport.width()
                        * (x - contentRect.left) / contentRect.width(),
                viewport.top
                        + viewport.height()
                        * (y - contentRect.top) / contentRect.height());

        return true;
    }

    private void constrainViewport() {
        /*viewport.left = Math.max(horizontalMin, viewport.left);
        viewport.top = Math.max(verticalMin, viewport.top);
        viewport.bottom = Math.min(verticalMax, viewport.bottom);
        viewport.right = Math.min(horizontalMax, viewport.right);*/

        if (viewport.left / scale < horizontalMin) {
            viewport.right = horizontalMin + viewport.width();
            viewport.left = horizontalMin;
        } else if (viewport.right / scale > horizontalMax) {
            viewport.left = horizontalMax - viewport.width();
            viewport.right = horizontalMax;
        }

        if (viewport.top / scale < verticalMin) {
            viewport.bottom = verticalMin + viewport.height();
            viewport.top = verticalMin;
        } else if (viewport.bottom / scale > verticalMax) {
            viewport.top = verticalMax - viewport.height();
            viewport.bottom = verticalMax;
        }
    }

    // Keeps the scale within a minimum and maximum
    private void constrainScale() {
        if (scale < minScale) {
            scale = minScale;
        } else if (scale > maxScale) {
            scale = maxScale;
        }
    }

    private void setViewportTopLeft(float x, float y) {
        float currentWidth = viewport.width();
        float currentHeight = viewport.height();

        //x = Math.max(horizontalMin, Math.min(x, horizontalMax - currentWidth));
        //y = Math.max(verticalMin, Math.min(y, verticalMax - currentHeight));

        viewport.set(x, y, x + currentWidth, y + currentHeight);
    }

    // TODO: Remove this from being inline
    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return super.onDown(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isScaling) {
                float viewportOffsetX = distanceX * viewport.width() / contentRect.width();
                float viewportOffsetY = distanceY * viewport.height() / contentRect.height();


                setViewportTopLeft(viewport.left + viewportOffsetX, viewport.top + viewportOffsetY);
                return true;
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    };

    public GestureDetector getGestureDetector() {
        return gestureDetector;
    }

    public float getScale() {
        return scale;
    }

    public RectF getViewport() {
        return viewport;
    }
}
