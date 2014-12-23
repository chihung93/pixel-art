package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

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

    private Rect tileRegionRect = new Rect();
    private BitmapDrawable checkerboardTile;

    public TransparencyCheckerboard(Context context) {

        tile = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        tileSourceRect.set(0, 0, tile.getWidth(), tile.getHeight());

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(context.getResources(), bitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        // The side length of a tile, made up of four squares
        // Rounds up the value as there is an odd stretching of the tile at the edges of the tiled
        // surface on devices with odd densities (Nexus 7 2012 where 1px is about 1.3dp)
        dp = (float) (Math.ceil(context.getResources().getDisplayMetrics().density));
        length = (32 * dp);
    }

    /**
     * Draws checkerboard tiles in within the bitmap rectangle.
     * @param canvas The canvas to draw into
     * @param tileRegionRectF The area to be tiled
     */
    public void drawTile(Canvas canvas, RectF tileRegionRectF) {
        tileRegionRectF.round(tileRegionRect);
        checkerboardTile.setBounds(tileRegionRect);
        checkerboardTile.draw(canvas);
    }

    /**
     * Draws checkerboard tiles within the intersection of two rectangles.
     * @param canvas The canvas to draw into
     * @param tileRegion The area to be tiled
     * @param visibleRegion The user visible area
     */
    public void draw(Canvas canvas, RectF tileRegion, Rect visibleRegion) {
        // Determines the bounds of the viewable area to draw fewer checkerboards tiles

        float left;
        if (tileRegion.left > visibleRegion.left) {
            left = tileRegion.left;
        } else {
            // Adds offset to compensate for being locked to the corner of the surface
            float offsetX = (tileRegion.left) % length;
            left = visibleRegion.left + offsetX;
        }

        float top;
        if (tileRegion.top > visibleRegion.top) {
            top = tileRegion.top;
        } else {
            // Adds offset to compensate for being locked to the corner of the surface
            float offsetY = (tileRegion.top) % length;
            top = visibleRegion.top + offsetY;
        }

        float right = tileRegion.right < visibleRegion.right ? tileRegion.right : visibleRegion.right;
        float bottom = tileRegion.bottom < visibleRegion.bottom ? tileRegion.bottom : visibleRegion.bottom;

        tile(canvas, left, top, right, bottom);
    }

    /**
     * Draws checkerboard tiles within a specified rectangle.
     * @param canvas The canvas to draw into
     * @param tileRegion The area to be tiled
     */
    public void draw(Canvas canvas, RectF tileRegion) {
        tile(canvas, tileRegion.left, tileRegion.top, tileRegion.right, tileRegion.bottom);
    }

    /**
     * Performs the tiling, edges are clipped.
     * @param canvas The canvas to draw into
     * @param left The position of the left edge of the tiling area
     * @param top The position of the top edge of the tiling area
     * @param right The position of the right edge of the tiling area
     * @param bottom The position of the bottom edge of the tiling area
     */
    private void tile(Canvas canvas, float left, float top, float right, float bottom) {
        // Iterates over the viewable area and draws checkerboard tiles
        for (float x = left; x < right; x += length) {
            for (float y = top; y < bottom; y += length) {

                tileDestinationRect.left = x;
                tileDestinationRect.top = y;

                // The rightmost column of tiles may be cut off
                if (x + length > right) {
                    // Clips the tile
                    // TODO: Wrong source edge on odd-density-screens which leads to tile stretching
                    tileDestinationRect.right = right;
                    tileSourceRect.right = (int) (right - x);
                } else {
                    tileDestinationRect.right = x + length;
                    tileSourceRect.right = tile.getWidth();
                }

                // The bottommost row of tiles may be cut off
                if (y + length > bottom) {
                    // Clips the tile
                    tileDestinationRect.bottom = bottom;
                    tileSourceRect.bottom = (int) (bottom - y);
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
