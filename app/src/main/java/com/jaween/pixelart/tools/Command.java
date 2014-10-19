package com.jaween.pixelart.tools;

import android.graphics.Bitmap;
import android.graphics.PointF;

/**
 * Created by ween on 10/19/14.
 */
public interface Command {

    public void start(Bitmap bitmap, PointF event, Tool.Attributes attributes);

    public void move(Bitmap bitmap, PointF event, Tool.Attributes attributes);

    public void end(Bitmap bitmap, PointF event, Tool.Attributes attributes);

}
