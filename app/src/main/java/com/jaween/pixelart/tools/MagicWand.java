package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.jaween.pixelart.tools.attributes.MagicWandToolAttributes;
import com.jaween.pixelart.util.Color;
import com.jaween.pixelart.util.Debug;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by ween on 11/30/14.
 */
public class MagicWand extends Selection {

    private static final int TOOL_ID = 8;
    private int[] bitmapArray;
    private boolean[] mask;
    private int[] maskStack;
    private int width, height;
    private float threshold;

    // Hashmap with chaining, maps points to adjacent line segments
    private Map<Point, LinkedList<Point>> dictionary;
    private LinkedList<Point>[] linkedListStack;

    private LinkedList<Point> segments = new LinkedList<>();
    private Point[] segmentPointStack;

    int pointStackIndex = 0;
    int linkedListStackIndex = 0;

    private Path selectedPath = new Path();
    private Path selectedPathInverse = new Path();
    private int previouslyTouchedColour;

    public MagicWand(String name, Drawable icon) {
        super(name, icon, TOOL_ID);

        toolAttributes = new MagicWandToolAttributes();
        toolAttributes.setMutator(false);
        toolAttributes.setSelector(true);
        selectedPath.setFillType(Path.FillType.EVEN_ODD);
        selectedPathInverse.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        setPath(selectedPath, selectedPathInverse);
    }

    @Override
    protected void onStart(Bitmap bitmap, PointF event) {
        pathReset();

        if (isInBounds(bitmap, event)) {
            previouslyTouchedColour = colour((int) event.x, (int) event.y);
            performSelection(bitmap, event);
        }
        toolReport.getPath().set(selectedPath);
        toolReport.getInversePath().set(selectedPathInverse);
    }

    @Override
    protected void onMove(Bitmap bitmap, PointF event) {
        // Avoids recalculating identical selections
        if (isInBounds(bitmap, event)) {
            int touchedColour = colour((int) event.x, (int) event.y);
            if (touchedColour != previouslyTouchedColour) {
                previouslyTouchedColour = touchedColour;
                performSelection(bitmap, event);
            }
        }
        toolReport.getPath().set(selectedPath);
        toolReport.getInversePath().set(selectedPathInverse);
    }

    @Override
    protected void onEnd(Bitmap bitmap, PointF event) {
        toolReport.getPath().set(selectedPath);
        toolReport.getInversePath().set(selectedPathInverse);
    }

    private int colour(int x, int y) {
        return bitmapArray[x + y * width];
    }

    private boolean similar(int x, int y, int colour) {
        if (Color.colourDistanceRGB(colour(x, y), colour) <= threshold) {
            return true;
        }
        return false;
    }

    public void setBitmapConfiguration(int width, int height) {
        bitmapArray = new int[width * height];
        mask = new boolean[width * height];
        maskStack = new int[height * 2];

        // TODO: Determine the minimum amount of allocation required
        segmentPointStack = new Point[width * height];
        for (int i = 0; i < segmentPointStack.length; i++) {
            segmentPointStack[i] = new Point();
        }

        // TODO: Determine the minimum amount of allocation required
        dictionary = new HashMap<>();
        linkedListStack = new LinkedList[width * height];
        for (int i = 0; i < linkedListStack.length; i++) {
            linkedListStack[i] = new LinkedList<>();
        }

        this.width = width;
        this.height = height;
    }

    private void performSelection(Bitmap bitmap, PointF event) {
        long startTime = System.currentTimeMillis();

        createMask(bitmap, event);
        generateLineSegmentsMap(mask);
        generatePathMap(dictionary);

        // Debug log
        if (Debug.ON) {
            Log.d("MagicWand", "Selection took " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }

    /** Uses a modified flood-fill algorithm to create a mask of pixels to be selected. **/
    private void createMask(Bitmap bitmap, PointF event) {
        // No bitmap
        if (bitmap == null) {
            return;
        }

        // Out of bounds
        if (!isInBounds(bitmap, event)) {
            return;
        }

        // Gets an array of the bitmap's colours
        bitmap.getPixels(bitmapArray, 0, width, 0, 0, width, height);

        // Resets the mask
        for (int i = 0; i < mask.length; i++) {
            mask[i] = false;
        }

        // Colour to be replaced and the colour which will replace it
        threshold = ((MagicWandToolAttributes) toolAttributes).getThreshold() / 100f;
        int oldColour = colour((int) event.x, (int) event.y);
        toolAttributes.getPaint().setColor(toolAttributes.getPaint().getColor());

        // Resets the maskStack
        int maskStackIndex = 0;

        // Pushes the touched pixel onto the stack
        maskStack[maskStackIndex] = (int) event.x;
        maskStack[maskStackIndex + 1] = (int) event.y;
        maskStackIndex += 2;

        // Four-way flood fill algorithm
        while (maskStackIndex > 0) {

            // Pops a pixel from the stack
            int x = maskStack[maskStackIndex - 2];
            int y1 = maskStack[maskStackIndex - 1];
            maskStackIndex -= 2;

            while (y1 >= 0 && similar(x, y1, oldColour)) {
                y1--;
            }
            y1++;

            boolean spanLeft = false;
            boolean spanRight = false;

            while (y1 < height && similar(x, y1, oldColour)) {
                mask[x + y1 * width] = true;

                if (!spanLeft && x > 0 && similar(x - 1, y1, oldColour) && !mask[(x - 1) + y1 * width]) {
                    // Pixel to the left must also be changed, pushes it to the stack
                    // TODO: Had ArrayIndexOutOfBoundsException on the following line
                    maskStack[maskStackIndex] = x - 1;
                    maskStack[maskStackIndex + 1] = y1;
                    maskStackIndex += 2;
                    spanLeft = true;
                } else if (spanLeft && x > 0 && !similar(x - 1, y1, oldColour)) {
                    // Pixel to the left has already been changed
                    spanLeft = false;
                }

                if (!spanRight && x < width - 1 && similar(x + 1, y1, oldColour) && !mask[(x + 1) + y1 * width]) {
                    // Pixel to the right must also be changed, pushes it to the stack
                    maskStack[maskStackIndex] = x + 1;
                    maskStack[maskStackIndex + 1] = y1;
                    maskStackIndex += 2;
                    spanRight = true;
                } else if (spanRight && x < width - 1 && !similar(x + 1, y1, oldColour)) {
                    // Pixel to the right has already been changed
                    spanRight = false;
                }
                y1++;
            }
        }
    }

    private void generateLineSegmentsMap(boolean[] mask) {
        // Clears the data structures
        pointStackIndex = 0;
        linkedListStackIndex = 0;
        segments.clear();
        dictionary.clear();

        int x = 0;
        int y = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                // Sets adjacent directions true if their pixels are also set in the mask,
                // out of bounds pixels set false
                boolean left = x == 0 ? false : mask[(x - 1) + y * width];
                boolean top = y == 0 ? false : mask[x + (y - 1) * width];
                boolean right = x == width - 1 ? false : mask[(x + 1) + y * width];
                boolean bottom = y == height - 1 ? false : mask[x + (y + 1) * width];

                // Adds line segments of where this masked pixel meets unmasked pixels
                if (left == false) {
                    addSegmentToDictionary(x, y, x, y + 1);
                }

                if (top == false) {
                    addSegmentToDictionary(x, y, x + 1, y);
                }

                if (right == false) {
                    addSegmentToDictionary(x + 1, y, x + 1, y + 1);
                }

                if (bottom == false) {
                    addSegmentToDictionary(x, y + 1, x + 1, y + 1);
                }
            }

            // Sets the x and y values for the next pixel in the mask array
            x++;
            if (x >= width) {
                x = 0;
                y++;
            }
        }
    }

    /** Creates a path around the selected area using the line segments in the dictionary. **/
    private void generatePathMap(Map<Point, LinkedList<Point>> dictionary) {
        pathReset();

        // Begins the path at some point on the mask
        Point current = segments.pollFirst();
        pathMoveTo(current.x, current.y);
        while(true) {
            // Retrieves a point that is adjacent to this one (two points that form a line segment)
            LinkedList<Point> points = dictionary.get(current);
            if (!points.isEmpty()) {
                // Typical case, an adjacent point exists
                Point previous = current;
                current = points.pollFirst();

                // Since the values in the dictionary are points that are adjacent to the key, each
                // line segment exists in the dictionary twice (Key: PointA, Value: PointB as well
                // as Key: PointB, Value: PointA). We must remove this duplication here in order to
                // not cycle back on ourselves.
                dictionary.get(current).remove(previous);

                // This edge can now be marked
                pathLineTo(current.x, current.y);
                pathLineTo(current.x, current.y);
            } else {
                // Closes off the area being selected
                pathClose();

                // Since the selection may have holes in it, we must outline those holes too. This
                // ensures that we use all the remaining line segments.
                while (!segments.isEmpty()) {
                    current = segments.pollFirst();

                    if (!dictionary.get(current).isEmpty()) {
                        // Next closed region to outline
                        pathMoveTo(current.x, current.y);
                        break;
                    }
                }

                if (segments.isEmpty()) {
                    return;
                }
            }
        }
    }

    /** Pushes a line segment to the dictionary and to the segments stack. **/
    private void addSegmentToDictionary(int startX, int startY, int endX, int endY) {
        Point segmentStart = segmentPointStack[pointStackIndex];
        Point segmentEnd = segmentPointStack[pointStackIndex + 1];
        pointStackIndex += 2;

        segmentStart.set(startX, startY);
        segmentEnd.set(endX, endY);
        segments.push(segmentStart);
        segments.push(segmentEnd);

        // Places
        LinkedList<Point> segmentStartList = getChain(segmentStart);
        if (!segmentStart.equals(segmentEnd)) {
            segmentStartList.push(segmentEnd);
        }

        LinkedList<Point> segmentEndList = getChain(segmentEnd);
        if (!segmentStart.equals(segmentEnd)) {
            segmentEndList.push(segmentStart);
        }
    }

    /** Returns the chain of points associated with the key 'point' in the dictionary. **/
    private LinkedList<Point> getChain(Point point) {
        LinkedList<Point> list = dictionary.get(point);
        if (list == null) {
            // Point doesn't have a list, retrieves one from the stack
            list = linkedListStack[linkedListStackIndex];
            linkedListStackIndex++;
            list.clear();

            dictionary.put(point, list);
        }
        return list;
    }
}
