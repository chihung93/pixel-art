package com.jaween.pixelart.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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

    /** Starts running performSave() at regular intervals. **/
    public void begin() {
        new Thread(runnable);
        handler.postDelayed(runnable, intervalMillis);
    }

    /** Stops the automatic saving timer. **/
    public void stop() {
        handler.removeCallbacks(runnable);
    }

    /** Run on the main thread. **/
    private void performSave() {
        // TODO: Crash, " Only the original thread that created a view hierarchy can touch its views."
        // at the save() line. The file didn't load next start and ImportExport couldn't decode it again
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
