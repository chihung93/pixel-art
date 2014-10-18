package com.jaween.pixelart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.util.ScaleListener;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by ween on 9/28/14.
 */
public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // Android View and threading variables
    private Context context;
    private SurfaceHolder holder;
    private Thread thread;
    private boolean running = false;
    private boolean surfaceCreated = false;

    // Canvas transformation
    private static final float MIN_SCALE = 1;
    private static final float MAX_SCALE = 48;
    private static final float INITIAL_SCALE = 4;
    private float scale = INITIAL_SCALE;
    private Rect displayRect = new Rect();
    private Rect viewportRect = new Rect();
    private ScaleGestureDetector scaleGestureDetector;
    private ScaleListener scaleListener;
    private Thumbnail thumbnail;

    // Tool and drawing variables
    private Tool tool;
    private Tool.Attributes toolAttributes;
    private Paint toolPaint;
    private Paint bitmapPaint;
    private PointF touch = new PointF();
    private Canvas drawOperationsCanvas;
    private float dp;

    // Layers
    private ArrayList<Bitmap> layers;
    private int currentLayer = 0;
    private int layerWidth;
    private int layerHeight;

    // Temporary UI variables
    private Paint tempTextPaint;
    private Random random;


    public DrawingSurface(Context context, Tool tool) {
        super(context);

        // Screen density info
        dp = context.getResources().getDisplayMetrics().density;

        holder = getHolder();
        holder.addCallback(this);

        initialisePaints();

        // Tool defaults
        int strokeWidth = 2;
        toolAttributes = new Tool.Attributes(toolPaint, strokeWidth);
        toolAttributes.paint = toolPaint;

        // Temporary variables
        random = new Random();

        this.context = context;
        this.tool = tool;
    }

    private void initialisePaints() {
        // Tool paint defaults
        toolPaint = new Paint();
        toolPaint.setAntiAlias(false);
        toolPaint.setStrokeWidth(6);
        toolPaint.setStyle(Paint.Style.STROKE);
        toolPaint.setColor(Color.DKGRAY);

        // Purely used to blit bitmaps
        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(false);

        // Temporary UI text
        tempTextPaint = new Paint();
        tempTextPaint.setTextSize(30);
        tempTextPaint.setAntiAlias(true);
        tempTextPaint.setColor(Color.MAGENTA);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Initialises layers
        layers = new ArrayList<Bitmap>();
        layerWidth = (int) (getWidth() / INITIAL_SCALE);
        layerHeight = (int) (getHeight() / INITIAL_SCALE);
        layers.add(Bitmap.createBitmap(
                layerWidth,
                layerHeight,
                Bitmap.Config.ARGB_8888));

        // Attaches a canvas to a layer
        drawOperationsCanvas = new Canvas(layers.get(0));
        drawOperationsCanvas.drawColor(Color.WHITE);

        // Thumbnail
        float thumbnailLeft = getWidth() - layerWidth
                - getResources().getDimension(R.dimen.activity_horizontal_margin);
        float thumbnailTop = getHeight() - layerHeight
                - getResources().getDimension(R.dimen.activity_vertical_margin);
        thumbnail = new Thumbnail(thumbnailLeft, thumbnailTop, layerWidth, layerHeight, dp);

        // Multi-touch zoom and pan
        scaleListener = new ScaleListener(context, MIN_SCALE, MAX_SCALE, INITIAL_SCALE, getWidth(), getHeight());
        scaleGestureDetector = new ScaleGestureDetector(context, scaleListener);
        displayRect.set(0, 0, getWidth(), getHeight());

        surfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // No implementation
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
    }

    @Override
    public void run() {

        // Performs the draw loop
        while (running) {
            if (!holder.getSurface().isValid())
                continue;

            Canvas canvas = null;

            try {
                canvas = holder.lockCanvas(null);
                synchronized (holder) {
                    draw(canvas);
                }
            } finally {
                // TODO Fix IllegalArgumentException at the unlockCanvasAndPost line on app startup
                //E/IMGSRV﹕ :0: gralloc_unregister_buffer: Cannot unregister a locked buffer (ID=27812)
                //W/GraphicBufferMapper﹕ unregisterBuffer(0x417f7360) failed -22 (Invalid argument)
                //E/Surface﹕ Surface::unlockAndPost failed, no locked buffer
                //W/dalvikvm﹕ threadid=11: thread exiting with uncaught exception (group=0x41900700)
                //E/AndroidRuntime﹕ FATAL EXCEPTION: Thread-65118
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    // Called from parent's onResume()
    public void onResume() {
        // Begins the drawing loop
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    // Called from parent's onPause()
    public void onPause() {
        joinThread();
    }

    private void joinThread() {
        running = false;
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
        thread = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        synchronized (holder) {
            scaleGestureDetector.onTouchEvent(event);
            scaleListener.getGestureDetector().onTouchEvent(event);

            // Stops if we're performing a gesture
            if (scaleGestureDetector.isInProgress()) {
                return false;
            }

            int action = event.getActionMasked();
            int index = event.getActionIndex();

            RectF viewport = scaleListener.getViewport();
            touch.x = viewport.left + event.getX(index)/getWidth()*viewport.width();
            touch.y = viewport.top + event.getY(index)/getHeight()*viewport.height();

            // Single-touch event
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Replace-colour for the flood fill tool
                    if (isInBounds(layers.get(currentLayer), touch)){
                        toolAttributes.tempTouchedColour = layers.get(currentLayer).getPixel((int) touch.x, (int) touch.y);
                    }

                    // A temporary random colour for the tool
                    toolAttributes.paint.setColor(Color.rgb(random.nextInt(255) + 15, random.nextInt(230) + 15, random.nextInt(230) + 15));
                    tool.beginAction(drawOperationsCanvas, layers.get(currentLayer), touch, toolAttributes);
                    break;
                case MotionEvent.ACTION_MOVE:
                    tool.beginAction(drawOperationsCanvas, null, touch, toolAttributes);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    tool.endAction(touch);

                    // TODO: Undo/redo system
                    //undoRedoTracker.bitmapModified(layers.get(currentLayer));
                    break;
                default:
                    return false;
            }
            return true;
            }
    }

    @Override
    public void draw(Canvas canvas) {
        if (surfaceCreated) {
            // Draws the user's image
            canvas.drawColor(Color.LTGRAY);

            RectF floatViewport = scaleListener.getViewport();
            floatViewport.round(viewportRect);
            canvas.drawBitmap(layers.get(currentLayer), viewportRect, displayRect, bitmapPaint);

            // Draws the thumbnail
            if (thumbnail.isEnabled()) {
                float scale = scaleListener.getScale();
                //if (scale >= INITIAL_SCALE || scale >= INITIAL_SCALE) {
                    thumbnail.draw(canvas, layers.get(currentLayer), scaleListener.getViewport());
                //}
            }
        }
    }

    private boolean isInBounds(Bitmap bitmap, PointF point) {
        if (point.x >= 0 && point.x < bitmap.getWidth()) {
            if (point.y >= 0 && point.y < bitmap.getHeight()) {
                return true;
            }
        }
        return false;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }
}