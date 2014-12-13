package com.jaween.pixelart.util;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;

/**
 * Created by ween on 11/21/14.
 */
public class Color extends android.graphics.Color {

    private static final int ALPHA_CHANNEL_SHIFT = 24;
    private static final int ALPHA_CHANNEL = 255 << ALPHA_CHANNEL_SHIFT;
    private static final int RED_CHANNEL_SHIFT = 16;
    private static final int GREEN_CHANNEL_SHIFT = 8;

    private static final Drawable[] layers = new Drawable[2];

    public static int HSLToColor(float h, float s, float l) {
        float chroma = (1 - Math.abs(2*l - 1)) * s;

        float hPrime = (h % 360) / 60f;
        float X = chroma * (1f - Math.abs(hPrime % 2f - 1f));
        
        float red, green, blue;
        if (hPrime < 1) {
            red = chroma;
            green = X;
            blue = 0;
        } else if (hPrime < 2) {
            red = X;
            green = chroma;
            blue = 0;
        } else if (hPrime < 3) {
            red = 0;
            green = chroma;
            blue = X;
        } else if (hPrime < 4) {
            red = 0;
            green = X;
            blue = chroma;
        } else if (hPrime < 5) {
            red = X;
            green = 0;
            blue = chroma;
        } else if (hPrime <= 6) {
            red = chroma;
            green = 0;
            blue = X;
        } else {
            //Log.e("Color", "hPrime = " + hPrime);
            return android.graphics.Color.MAGENTA;
        }

        float m = l - chroma / 2f;
        red += m;
        green += m;
        blue += m;

        //Scaled to 8-bit colour
        red = Math.max(0, Math.min( (int) (red * 255), 255));
        green = Math.max(0, Math.min( (int) (green * 255), 255));
        blue = Math.max(0, Math.min( (int) (blue * 255), 255));

        // Packs components into a single int as per the android.graphics.Color Class Overview
        int colour = ALPHA_CHANNEL | ((int) red << RED_CHANNEL_SHIFT) | ((int) green << GREEN_CHANNEL_SHIFT) | (int) blue;

        return colour;
    }

    // As per http://www.niwa.nu/2013/05/math-behind-colorspace-conversions-rgb-hsl/
    public static void colorToHsl(int r, int g, int b, float[] hsl) {
        // Normalises the input
        float rNorm = (float) r / (255f);
        float gNorm = (float) g / (255f);
        float bNorm = (float) b / (255f);

        // Min and max of the input values
        float min = Math.min(rNorm, Math.min(gNorm, bNorm));
        float max = Math.max(rNorm, Math.max(gNorm, bNorm));

        // Lightness
        float l = (min + max) / 2f;

        // Saturation and hue
        float s, h;
        if (min == max) {
           // No saturation
           s = 0;
           h = 0;
        } else {
            // There is saturation, it's calculated based on the lightness value
            if (l < 0.5f) {
                s = (max - min) / (max + min);
            } else {
                s = (max - min) / (2 - max - min);
            }

            if (max == rNorm) {
                h = (gNorm - bNorm) / (max - min);
            } else if (max == gNorm) {
                h = 2f + (bNorm - rNorm) / (max - min);
            } else {
                h = 4f + (rNorm - gNorm) / (max - min);
            }

            // Convert to degrees
            h = (h * 60) % 360;
        }

        hsl[0] = h;
        hsl[1] = s;
        hsl[2] = l;
    }

    /**
     * Work in progress. Finds the perceptual difference between two colours and returns a
     * normalised value between 0.0 and 1.0. Uses the algorithm found here:
     * http://www.compuphase.com/cmetric.htm
     * @param colourA The first colour to compare in packed ARGB format
     * @param colourB The second colour to compare in packed ARGB format
     * @return The colour distance from 0.0 to 1.0
     */
    public static double colourDistance(int colourA, int colourB) {
        double averageRed = ((Color.red(colourA) + Color.red(colourB)) / 2)/256.0;

        double deltaRed = Color.red(colourA)/256.0 - Color.red(colourB)/256.0;
        double deltaGreen = Color.green(colourA)/256.0 - Color.green(colourB)/256.0;
        double deltaBlue = Color.blue(colourA)/256.0 - Color.blue(colourB)/256.0;

        double weightRed = 2/256.0 + averageRed;
        double weightGreen = 4.0/256.0;
        double weightBlue = 2/256.0 + (1.0 - averageRed);

        double componentRed = weightRed * deltaRed * deltaRed;
        double componentGreen = weightGreen * deltaGreen * deltaGreen;
        double componentBlue = weightBlue * deltaBlue * deltaBlue;

        double distance = Math.sqrt(componentRed + componentGreen + componentBlue);

        return distance;
    }

    /**
     * Finds the RGB distance between two colours and returns a normalised value between 0.0 and
     * 1.0.
     * @param colourA The first colour to compare in packed ARGB format
     * @param colourB The second colour to compare in packed ARGB format
     * @return The colour distance from 0.0 to 1.0
     */
    public static double colourDistanceRGB(int colourA, int colourB) {
        double deltaA = (Color.alpha(colourA) - Color.alpha(colourB));
        double deltaR = (Color.red(colourA) - Color.red(colourB));
        double deltaG = (Color.green(colourA) - Color.green(colourB));
        double deltaB = (Color.blue(colourA) - Color.blue(colourB));

        double distA =  deltaA * deltaA;
        double distR = deltaR * deltaR;
        double distG = deltaG * deltaG;
        double distB = deltaB * deltaB;

        return Math.sqrt(distA + distR + distG + distB) / (2 * 255f);
    }

    public static LayerDrawable tintAndLayerDrawable(Drawable colouredInnner, Drawable border, int colour) {
        // Tints the inner square to the selected colour
        colouredInnner.mutate();
        colouredInnner.setColorFilter(colour, PorterDuff.Mode.MULTIPLY);

        // The white border
        if (colour == android.graphics.Color.WHITE) {
            // The selected colour is white, darkens the border slightly
            border.setColorFilter(android.graphics.Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
        }

        // Layers the two elements
        layers[0] = border;
        layers[1] = colouredInnner;
        LayerDrawable layerDrawable = new LayerDrawable(layers);

        return layerDrawable;
    }
}
