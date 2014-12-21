package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.jaween.pixelart.R;

/**
 * Draws a tiling 32x32dp checkerboard on a Canvas (dp rounded up, so may be larger than 32x32dp).
 */
public class TransparencyCheckerboard {

    private final float length;

    private Bitmap tile;
    private Rect tileSourceRect = new Rect();
    private RectF tileDestinationRect = new RectF();
    private float dp;

    public TransparencyCheckerboard(Context context) {

        tile = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        tileSourceRect.set(0, 0, tile.getWidth(), tile.getHeight());

        // The side length of a tile, made up of four squares
        // Rounds up the value as there is an odd stretching of the tile at the edges of the tiled
        // surface on devices with odd densities (Nexus 7 2012 where 1px is about 1.3dp)
        dp = (float) (Math.ceil(context.getResources().getDisplayMetrics().density));
        length = (32 * dp);
    }

    public void draw(Canvas canvas, RectF bitmapRect, Rect surfaceRect) {
        // Determines the bounds of the viewable area to draw fewer checkerboards tiles

        float left;
        if (bitmapRect.left > surfaceRect.left) {
            left = bitmapRect.left;
        } else {
            // Adds offset to compensate for being locked to the corner of the surface
            float offsetX = (bitmapRect.left) % length;
            left = surfaceRect.left + offsetX;
        }

        float top;
        if (bitmapRect.top > surfaceRect.top) {
            top = bitmapRect.top;
        } else {
            // Adds offset to compensate for being locked to the corner of the surface
            float offsetY = (bitmapRect.top) % length;
            top = surfaceRect.top + offsetY;
        }

        float right = bitmapRect.right < surfaceRect.right ? bitmapRect.right : surfaceRect.right;
        float bottom = bitmapRect.bottom < surfaceRect.bottom ? bitmapRect.bottom : surfaceRect.bottom;

        // Iterates over the viewable area and draws checkerboard tiles
        for (float x = left; x < right; x += length) {
            for (float y = top; y < bottom; y += length) {

                tileDestinationRect.left = x;
                tileDestinationRect.top = y;

                // The rightmost column of tiles may be cut off
                if (x + length > bitmapRect.right) {
                    // Clips the tile
                    // TODO: Wrong source edge on odd-density-screens which leads to tile stretching
                    tileDestinationRect.right = bitmapRect.right;
                    tileSourceRect.right = (int) (bitmapRect.right - x);
                } else {
                    tileDestinationRect.right = x + length;
                    tileSourceRect.right = tile.getWidth();
                }

                // The bottommost row of tiles may be cut off
                if (y + length > bitmapRect.bottom) {
                    // Clips the tile
                    tileDestinationRect.bottom = bitmapRect.bottom;
                    tileSourceRect.bottom = (int) (bitmapRect.bottom - y);
                } else {
                    tileDestinationRect.bottom = y + length;
                    tileSourceRect.bottom = tile.getHeight();
                }

                // Finally, draws the tile to the canvas
                canvas.drawBitmap(tile, tileSourceRect, tileDestinationRect, null);
            }
        }
    }
}
