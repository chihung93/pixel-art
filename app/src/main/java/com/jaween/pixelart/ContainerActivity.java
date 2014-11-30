package com.jaween.pixelart;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

/**
 * Decides which ContainerFragment to add.
 */
public final class ContainerActivity extends ActionBarActivity {

    private ContainerFragment containerFragment;
    private static final String TAG_CONTAINER_FRAGMENT = "tag_container_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Removes a layer of overdraw (though the background is needed during the starting window)
        getWindow().setBackgroundDrawable(null);

        // Displays the app icon instead of the title text
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        setContentView(R.layout.container_activity);

        FragmentManager fragmentManager = getSupportFragmentManager();
        containerFragment = (ContainerFragment) fragmentManager.findFragmentByTag(TAG_CONTAINER_FRAGMENT);

        // Fragment doesn't exist, creates it and adds it to the UI
        if (containerFragment == null) {
            containerFragment = new ContainerFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fl_main_container, containerFragment, TAG_CONTAINER_FRAGMENT);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (containerFragment.onBackPressed() == false) {
            super.onBackPressed();
        }
    }
}
