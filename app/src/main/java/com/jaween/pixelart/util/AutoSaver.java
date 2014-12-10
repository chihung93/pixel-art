package com.jaween.pixelart.util;

import android.os.Handler;
import android.os.Looper;

import com.jaween.pixelart.ContainerFragment;

/**
 * // TODO: Make Singleton, use enum method instead? Does it need to be a Singleton?
 * Calls ContainerFragment.save() on the main thread at regular intervals.
 */
public class AutoSaver {

    // 60 seconds
    private int intervalMillis = 1000 * 60;
    private static AutoSaver instance = null;

    private ContainerFragment containerFragment;
    private Handler handler;

    // Singleton
    /*public static AutoSaver getInstance() {
        if (instance == null) {
            instance = new AutoSaver();
        }
        return instance;
    }*/

    public AutoSaver(ContainerFragment containerFragment) {
        this.containerFragment = containerFragment;
        handler = new Handler(Looper.getMainLooper());
    }

    public void setInterval(int intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public void begin() {
        new Thread(runnable).start();
    }

    public void stop() {
        handler.removeCallbacks(runnable);
    }

    private void performSave() {
        containerFragment.save();
        handler.postDelayed(runnable, intervalMillis);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            performSave();
        }
    };

}
