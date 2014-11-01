package com.jaween.pixelart;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

/**
 * Decides which BaseContainerActivity to launch.
 */
public final class DriverActivity extends Activity {

    private static final int TABLET_LAYOUT_WIDTH_DP = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Screen size details
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        float dp = Resources.getSystem().getDisplayMetrics().density;
        float widthDp = size.x / dp;

        // Picks the Activity based on the size of the screen and starts it up
        Intent intent;
        if (widthDp >= TABLET_LAYOUT_WIDTH_DP) {
            intent = new Intent(this, BaseContainerActivity.class);
        } else {
            intent = new Intent(this, PhoneContainerActivity.class);
        }
        startActivity(intent);

    }
}
