package com.jaween.pixelart;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;

public class ContainerActivity extends Activity {

    DrawingFragment drawingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_activity);
        if (savedInstanceState == null) {
            drawingFragment = new DrawingFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.container, drawingFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawing_menu, menu);
        return true;
    }

    // Temporary zooming UI TODO Proper zooming UI
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                drawingFragment.zoom(1f);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                drawingFragment.zoom(-1f);
                break;
            default:
                return false;
        }
        return true;
    }
}
