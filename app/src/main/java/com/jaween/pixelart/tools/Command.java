package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PointF;

/**
 * Created by ween on 10/19/14.
 */
public interface Command {

    /**
     * This method is called when the user begins a drawing with this tool. Begin the drawing
     * operation here.
     * @param bitmap The bitmap to draw upon
     * @param event The coordinates of the drawing event
     */
    public void start(Bitmap bitmap, PointF event);

    /**
     * This method is called when the user moves their finger across the screen. Update the drawing
     * operation if necessary. The bitmap will be clear of previous drawings from during the
     * lifecycle of this operation.
     * @param bitmap The bitmap to draw upon
     * @param event The coordinates of the drawing event
     */
    public void move(Bitmap bitmap, PointF event);

    /**
     * This method is called when the drawing operation has been successfully completed. Finish
     * any drawing operations here. The bitmap will be clear of previous drawings from during the
     * lifecycle of this operation.
     * @param bitmap The bitmap to draw upon
     * @param event The coordinates of the drawing event
     * @return The path which this tool took over the lifecycle of this drawing operation
     */
    public Path end(Bitmap bitmap, PointF event);

    /**
     * This method is called when a drawing operation has is to be cancelled (e.g. due to a
     * zoom occurring). Any ongoing drawing operation is to be stopped.
     */
    public void cancel();

}
