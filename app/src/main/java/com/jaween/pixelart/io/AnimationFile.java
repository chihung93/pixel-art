package com.jaween.pixelart.io;

import com.jaween.pixelart.ui.animation.Frame;

import java.util.LinkedList;

/**
 * Created by ween on 12/9/14.
 */
public class AnimationFile {

    private String filename;
    private LinkedList<Frame> frames;

    public AnimationFile(String filename, LinkedList<Frame> frames) {
        this.filename = filename;
        this.frames = frames;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public LinkedList<Frame> getFrames() {
        return frames;
    }

    public void setFrames(LinkedList<Frame> frames) {
        this.frames = frames;
    }
}
