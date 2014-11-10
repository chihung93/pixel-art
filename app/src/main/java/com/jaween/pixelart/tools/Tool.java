package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 9/28/14.
 */
public abstract class Tool implements Command {

    protected Canvas canvas = new Canvas();
    protected final String name;
    protected final Drawable icon;
    protected boolean cancelled = false;
    protected ToolAttributes toolAttributes;
    protected Path toolPath;

    public Tool(String name, Drawable icon) {
        this.name = name;
        this.icon = icon;

        toolPath = new Path();
    }

    public String getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }

    @Override
    public final void start(Bitmap bitmap, PointF event) {
        cancelled = false;
        toolPath.reset();
        toolPath.moveTo(event.x, event.y);
        onStart(bitmap, event);
    }

    @Override
    public final void move(Bitmap bitmap, PointF event) {
        if (!cancelled) {
            toolPath.lineTo(event.x, event.y);
            onMove(bitmap, event);
        }
    }

    @Override
    public final Path end(Bitmap bitmap, PointF event) {
        if (!cancelled) {
            toolPath.moveTo(event.x, event.y);
            onEnd(bitmap, event);
        }
        return toolPath;
    }

    @Override
    public final void cancel() {
        cancelled = true;
    }

    protected abstract void onStart(Bitmap bitmap, PointF event);

    protected abstract void onMove(Bitmap bitmap, PointF event);

    protected abstract  void onEnd(Bitmap bitmap, PointF event);

    protected static boolean isInBounds(Bitmap bitmap, PointF point) {
        if (point.x >= 0 && point.x < bitmap.getWidth()) {
            if (point.y >= 0 && point.y < bitmap.getHeight()) {
                return true;
            }
        }
        return false;
    }

    public ToolAttributes getToolAttributes() {
        return toolAttributes;
    }
}