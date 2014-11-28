package com.jaween.pixelart.ui.layer;

import android.graphics.Bitmap;

/**
 * Created by ween on 11/27/14.
 */
public class Layer {

    private Bitmap image;
    private String titile;
    private boolean visible = true;
    private boolean locked = false;

    public Layer(Bitmap image, String title) {
        this.image = image;
        this.titile = title;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getTitile() {
        return titile;
    }

    public void setTitile(String titile) {
        this.titile = titile;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

}
