package com.jaween.pixelart;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Decides which ContainerFragment to add.
 */
public final class ContainerActivity extends ActionBarActivity {

    private ContainerFragment containerFragment;
    private static final String TAG_CONTAINER_FRAGMENT = "tag_container_fragment";

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_activity);

        // Removes a layer of overdraw (though the background is needed during the starting window)
        getWindow().setBackgroundDrawable(null);

        // Retrieves our Toolbar and sets it to be our ActionBar
        toolbar = (Toolbar) findViewById(R.id.tb_toolbar);
        toolbar.setLogo(R.drawable.ic_logo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // TODO: Animation disabled until issues resolved
        /*getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);*/

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // TODO: Animation disabled until issues resolved
        //containerFragment.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (containerFragment.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (containerFragment.onBackPressed() == false) {
            super.onBackPressed();
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }
}
