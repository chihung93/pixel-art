package com.jaween.pixelart;

import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Display;

/**
 * Decides which ContainerFragment to add.
 */
public final class ContainerActivity extends FragmentActivity {

    private static final int TABLET_LAYOUT_WIDTH_DP = 720;


    private ContainerFragment fragment;
    private static final String TAG_CONTAINER_FRAGMENT = "container_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_activity);

        fragment = (ContainerFragment) getSupportFragmentManager().findFragmentByTag(TAG_CONTAINER_FRAGMENT);
        if (fragment == null) {
            // Screen size details
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            float dp = Resources.getSystem().getDisplayMetrics().density;
            float widthDp = size.x / dp;

            // Picks the Fragment based on the size of the screen
            if (widthDp >= TABLET_LAYOUT_WIDTH_DP) {
                fragment = new ContainerFragment();
            } else {
                fragment = new ContainerFragmentNarrow();
            }

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fl_main_container, fragment, TAG_CONTAINER_FRAGMENT);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (fragment.onBackPressed() == false) {
            super.onBackPressed();
        }
    }
}
