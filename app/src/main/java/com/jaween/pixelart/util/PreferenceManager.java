package com.jaween.pixelart.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the SharedPreferences.
 */
public class PreferenceManager {

    private static final String MAIN_PREFERENCES = "main_preferences";
    private static final String KEY_LAST_USED_FILENAME = "key_last_used_filename";
    private static final String KEY_FILE_COUNT = "key_file_count";

    private static final int INITIAL_FILE_COUNT = 0;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        preferences = context.getSharedPreferences(MAIN_PREFERENCES, 0);
        editor = preferences.edit();
    }

    public void setLastUsedFilename(String filename) {
        editor.putString(KEY_LAST_USED_FILENAME, filename);
        editor.commit();
    }

    public String getLastUsedFilename() {
        return preferences.getString(KEY_LAST_USED_FILENAME, null);
    }

    /** Retrieves and increments the file count **/
    public int getFileCount() {
        int count = preferences.getInt(KEY_FILE_COUNT, INITIAL_FILE_COUNT);

        // Increments and stores the file count
        count++;
        editor.putInt(KEY_FILE_COUNT, count);
        editor.commit();

        return count;
    }

}
