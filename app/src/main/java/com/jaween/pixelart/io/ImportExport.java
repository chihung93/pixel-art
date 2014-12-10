package com.jaween.pixelart.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.util.AnimatedGifEncoder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by ween on 12/9/14.
 */
public class ImportExport {

    private static final String LOG_TAG = "ImportExport";
    private static final String PIXEL_ART_DIRECTORY = "Pixel Art";
    public enum Format {
        GIF, PNG;
    }

    private static BitmapFactory.Options options;

    public ImportExport() {
        options = new BitmapFactory.Options();
        options.inMutable = true;
    }

    /** Saves a drawing to the external storage **/
    public static boolean export(Bitmap bitmap, String filename, int fps, Format format) {
        if (isExternalStorageWritable()){
            String albumName = PIXEL_ART_DIRECTORY;
            File path = getAlbumStorageDir(albumName);
            File output = new File(path, filename);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(output);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            if (fos != null) {

                int millisPerFrame = (int) (1000f / (float) fps);

                switch (format) {
                    case GIF:
                        saveGif(fos, bitmap, millisPerFrame);
                        break;
                    case PNG:
                        savePng(fos, bitmap);
                }
            }
            return true;
        }
        return false;
    }

    /** Saves a drawing to the internal storage **/
    public static boolean save(Context context, Bitmap bitmap, String filename) {
        FileOutputStream fos;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            savePng(fos, bitmap);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Loads all decodable drawings from the internal storage **/
    public static ArrayList<AnimationFile> load(Context context) {
        ArrayList<AnimationFile> animationFiles = new ArrayList();

        File dir = context.getFilesDir();
        String[] filenames = dir.list();
        for (int i = 0; i < filenames.length; i++) {
            String filename = filenames[i];
            String path = dir.getPath() + "/" + filename;
            Bitmap bitmap = decodeFile(path);
            if (bitmap != null) {
                AnimationFile animationFile = createAnimationFile(filenames[i], bitmap);
                animationFiles.add(animationFile);
            } else {
                Log.e(LOG_TAG, "Could not decode " + path);
            }
        }

        return animationFiles;
    }

    /** Loads a single decodable drawing from the internal storage. **/
    public static AnimationFile load(Context context, String filename) {
        File dir = context.getFilesDir();
        String path = dir.getPath() + "/" + filename;
        Bitmap bitmap = decodeFile(path);
        if (bitmap != null) {
            return createAnimationFile(filename, bitmap);
        } else {
            Log.d(LOG_TAG, "Could not decode " + filename);
            return null;
        }
    }

    /** Deletes a file specified by 'filename'. Returns true on deletion. **/
    public static boolean delete(Context context, String filename) {
        File dir = context.getFilesDir();
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(filename)) {
                files[i].delete();
                return true;
            }
        }
        return false;
    }

    private static void saveGif(FileOutputStream fos, Bitmap bitmap, int millisPerFrame) {
        // Loads the pixels of the Bitmap into an array
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] bitmapArray = new int[width * height];
        bitmap.getPixels(bitmapArray, 0, width, 0, 0, width, height);

        // Performs the export
        AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
        gifEncoder.start(fos);
        gifEncoder.setDelay(millisPerFrame);
        gifEncoder.setTransparent(Color.TRANSPARENT);
        gifEncoder.addFrame(bitmapArray, width, height);
        gifEncoder.finish();
    }

    private static void savePng(FileOutputStream fos, Bitmap bitmap) {
        // PNG is lossless, ignores the quality
        int quality = 100;
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, fos);
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Returns the path to the user's public pictures directory **/
    private static File getAlbumStorageDir(String albumName) {
        File path = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!path.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created!");
        }
        return path;
    }

    // TODO: Why is BitmapFactory.Options.inMutable being ignored? We shouldn't need this.
    /** Returns a mutable copy of the file specified by 'path'. **/
    private static Bitmap decodeFile(String path) {
        Bitmap mutableBitmap = BitmapFactory.decodeFile(path, options);
        if (mutableBitmap != null && !mutableBitmap.isMutable()) {
            boolean mutable = true;
            Bitmap defintelyMutable = mutableBitmap.copy(mutableBitmap.getConfig(), mutable);
            mutableBitmap.recycle();
            return defintelyMutable;
        }
        return mutableBitmap;
    }

    /** Checks if external storage is available for read and write **/
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /** Checks if external storage is available to at least read **/
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /** Creates an AnimationFile from a Bitmap strip. **/
    private static AnimationFile createAnimationFile(String filename, Bitmap strip) {
        LinkedList<Frame> frames = sliceBitmap(strip, strip.getWidth(), strip.getHeight(), 0, 0, 0, 0);
        AnimationFile animationFile = new AnimationFile(filename, frames);
        return animationFile;
    }

    // TODO: Implement bitmap strip slicing
    /** Creates an list of Frames given a strip of rows of frames made up of columns of layers. **/
    private static LinkedList<Frame> sliceBitmap(Bitmap bitmap, int layerWidth, int layerHeight,
                                                int offsetX, int offsetY, int spacingX, int spacingY) {
        LinkedList<Frame> frames = new LinkedList<Frame>();
        LinkedList<Layer> layers = new LinkedList<Layer>();

        String layerTitle = "TestLayer";
        Layer layer = new Layer(bitmap, layerTitle);
        layers.add(layer);

        Bitmap compositeBitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);
        int currentLayerIndex = 0;

        Frame frame = new Frame(layers, compositeBitmap, currentLayerIndex);
        frames.add(frame);

        return frames;
    }
}
