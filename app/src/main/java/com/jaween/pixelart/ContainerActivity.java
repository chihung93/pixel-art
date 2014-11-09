package com.jaween.pixelart;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;

/**
 * Decides which ContainerFragment to add.
 */
public final class ContainerActivity extends ActionBarActivity {

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
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);

            float density = getResources().getDisplayMetrics().density;
            float widthDp = metrics.widthPixels / density;

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
