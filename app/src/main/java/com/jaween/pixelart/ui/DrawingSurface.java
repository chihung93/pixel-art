package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.UndoRedoTracker;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.util.ScaleListener;

import java.util.ArrayList;

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

    // Drawing canvas
    private static final float MIN_SCALE = 1;
    private static final float MAX_SCALE = 48;
    public static final float DEFAULT_SCALE = 4;
    private float initialScale;
    private Rect displayRect = new Rect();
    private Matrix transformation = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private ScaleListener scaleListener;
    private Paint shadowPaint;
    private RectF transformedBitmapRect = new RectF();
    private float shadowWidthDp;

    // Tool and drawing variables
    private Tool tool;
    private Paint toolPaint;
    private Paint bitmapPaint;
    private PointF displayTouch = new PointF();
    private Canvas ongoingOperationCanvas;
    private Bitmap ongoingOperationBitmap;
    private OnDimensionsCalculatedListener onDimensionsCalculatedListener = null;
    private float dp;

    // UI Controls
    private static final int MAX_UNDOS = 50;
    private UndoRedoTracker undoRedoTracker;
    private PixelGrid pixelGrid;
    private Thumbnail thumbnail;
    private OnClearPanelsListener onClearPanelsListener = null;
    private Path toolPath;
    private RectF toolPathBounds = new RectF();

    // Layers
    private ArrayList<Bitmap> layers;
    private int currentLayer = 0;
    private int layerWidth;
    private int layerHeight;

    // Touch variables
    private static final int NULL_POINTER_ID = -1;
    private int currentPointerId = NULL_POINTER_ID;
    private long touchDownTime = 0;
    private static final long CANCEL_DRAWING_MILLIS = 300;
    private PointF pixelTouch = new PointF();
    private boolean hasCommittedOperation = false;

    // Configuration Change variables
    private boolean configurationChanged = false;
    private RectF viewport = new RectF();
    private Bitmap restoredLayer = null;
    private float restoredCenterX;
    private float restoredCenterY;
    private float restoredScale;
    private boolean restoredGridState;

    // Temporary UI variables
    private Paint tempTextPaint;

    public DrawingSurface(Context context, Tool tool) {
        super(context);

        // Screen density info
        dp = context.getResources().getDisplayMetrics().density;

        holder = getHolder();
        holder.addCallback(this);

        initialisePaints();

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
        bitmapPaint.setStyle(Paint.Style.STROKE);

        // Shadow around canvas and thumbnail
        shadowWidthDp = 4 * dp;
        shadowPaint = new Paint();
        shadowPaint.setShadowLayer(shadowWidthDp, 0, shadowWidthDp / 2, Color.DKGRAY);

        // Temporary UI text
        tempTextPaint = new Paint();
        tempTextPaint.setTextSize(30);
        tempTextPaint.setAntiAlias(true);
        tempTextPaint.setColor(Color.MAGENTA);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        initialiseLayers();

        initialiseViewport();

        initialiseGestureDetector();

        // Thumbnail
        float thumbnailLeft = getWidth() - layerWidth
                - getResources().getDimension(R.dimen.canvas_margin);
        float thumbnailTop = getHeight() - layerHeight
                - getResources().getDimension(R.dimen.canvas_margin);
        thumbnail = new Thumbnail(thumbnailLeft, thumbnailTop, layerWidth, layerHeight, dp);

        // UI Controls
        int maxUndos = MAX_UNDOS;
        undoRedoTracker = new UndoRedoTracker(layers.get(currentLayer), maxUndos);

        // Grid
        int majorPixelSpacing = 8;
        pixelGrid = new PixelGrid(dp, layerWidth, layerHeight, majorPixelSpacing);
        if (configurationChanged) {
            pixelGrid.setEnabled(restoredGridState);
        }

        surfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Notifies the owner fragment that the size has been calculated
        if (onDimensionsCalculatedListener != null) {
            onDimensionsCalculatedListener.onDimensionsCalculated(layerWidth, layerHeight);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
        onDimensionsCalculatedListener = null;
    }

    private void initialiseLayers() {
        layers = new ArrayList<Bitmap>();
        if (restoredLayer != null) {
            // Restored layers
            layerWidth = restoredLayer.getWidth();
            layerHeight = restoredLayer.getHeight();
            layers.add(restoredLayer);

            ongoingOperationBitmap = restoredLayer.copy(Bitmap.Config.ARGB_8888, true);

            // Attaches a canvas to a layer
            ongoingOperationCanvas = new Canvas(layers.get(0));
        } else {
            // New layers
            layerWidth = 128;//(int) (getWidth() / INITIAL_SCALE);
            layerHeight = 128;//(int) (getHeight() / INITIAL_SCALE);
            layers.add(Bitmap.createBitmap(
                    layerWidth,
                    layerHeight,
                    Bitmap.Config.ARGB_8888));

            // Bitmap that is used to temporarily store ongoing drawing operations
            // (e.g. dragging out an oval shape is an ongoing operation that draws many temporary ovals)
            ongoingOperationBitmap = Bitmap.createBitmap(
                    layerWidth,
                    layerHeight,
                    Bitmap.Config.ARGB_8888);

            // Attaches a canvas to a layer
            ongoingOperationCanvas = new Canvas(layers.get(0));

            // Blanks out canvas
            ongoingOperationCanvas.drawColor(Color.WHITE);
            resetOngoingBitmap();
        }
    }

    private void initialiseViewport() {
        if (configurationChanged) {
            // Restores the viewport (based on previous center)
            viewport.left = restoredCenterX - (getWidth() / 2) / restoredScale;
            viewport.right = restoredCenterX + (getWidth() / 2) / restoredScale;
            viewport.top = restoredCenterY - (getHeight() / 2) / restoredScale;
            viewport.bottom = restoredCenterY + (getHeight() / 2) / restoredScale;
        } else {
            float shadowPadding = shadowWidthDp;

            // Fits the initial viewport so the drawing touches the edges of the fragment
            if (getWidth() < getHeight()) {
                // Tall aspect ratio
                float surfaceAspectRatio = (float) getHeight() / (float) getWidth();
                viewport.left = -shadowPadding;
                viewport.right = layerWidth + shadowPadding;
                viewport.top = (layerHeight - layerHeight * surfaceAspectRatio) / 2 - shadowPadding;
                viewport.bottom = (layerHeight + layerHeight * surfaceAspectRatio) / 2 + shadowPadding;
            } else {
                // Wide aspect ratio (or square)

                // TODO Bug: lower shadow appears off screen, touch point offset, fixes itself on first zoom
                // Careful! Viewport  must maintain original aspect ratio otherwise hiccup on first zoom
                float surfaceAspectRatio = (float) getWidth() / (float) getHeight();
                viewport.left = (layerWidth - layerWidth * surfaceAspectRatio) / 2 - shadowPadding;
                viewport.right = (layerWidth + layerWidth * surfaceAspectRatio) / 2 + shadowPadding;
                viewport.top = -shadowPadding;
                viewport.bottom = layerHeight + shadowPadding;
            }
            initialScale = getWidth() / viewport.width();
        }
    }

    private void initialiseGestureDetector() {
        // Multi-touch zoom and pan
        displayRect.set(0, 0, getWidth(), getHeight());
        scaleListener = new ScaleListener(context, MIN_SCALE, MAX_SCALE, viewport, displayRect);
        scaleGestureDetector = new ScaleGestureDetector(context, scaleListener);
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

            int action = event.getActionMasked();
            int index = event.getActionIndex();

            // Ignores drawing operations that change pointer halfway through
            if (currentPointerId == NULL_POINTER_ID) {
                currentPointerId = event.getPointerId(index);
            }

            // Adjusts the viewport and gets the coordinates of the touch on the drawing
            viewport = scaleListener.getViewport();
            displayTouch.set(event.getX(), event.getY());
            pixelTouch.set(
                    viewport.left + displayTouch.x / getWidth() * viewport.width(),
                    viewport.top + displayTouch.y / getHeight() * viewport.height());

            // Stops drawing if we're performing a gesture
            if (scaleGestureDetector.isInProgress()) {
                if (System.currentTimeMillis() - touchDownTime < CANCEL_DRAWING_MILLIS) {
                    // Scaling began soon after the initial touch, cancels the drawing operation
                    tool.cancel();
                    resetOngoingBitmap();
                } else {
                    // Scaling begun long after the initial touch, commits the drawing operation
                    // up until this point, but cancels further drawing
                        toolPath = tool.end(ongoingOperationBitmap, pixelTouch);
                        tool.cancel();

                    if (hasCommittedOperation == false) {
                        hasCommittedOperation = true;
                        commitIfWithinDrawingBounds(toolPath);
                    }
                }
                return true;
            }

            // Single-touch event
            switch (action) {
                case MotionEvent.ACTION_DOWN:

                    if (onClearPanelsListener != null) {
                        onClearPanelsListener.onClearPanels();
                    }

                    touchDownTime = System.currentTimeMillis();

                    tool.start(ongoingOperationBitmap, pixelTouch);
                    break;
                case MotionEvent.ACTION_MOVE:
                    resetOngoingBitmap();
                    tool.move(ongoingOperationBitmap, pixelTouch);
                    break;
                case MotionEvent.ACTION_UP:
                    currentPointerId = NULL_POINTER_ID;
                    resetOngoingBitmap();
                    toolPath = tool.end(ongoingOperationBitmap, pixelTouch);
                    commitIfWithinDrawingBounds(toolPath);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    currentPointerId = NULL_POINTER_ID;
                    tool.cancel();
                    resetOngoingBitmap();
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    // Ends drawing operation if the main pointer left the screen
                    if (event.getPointerId(index) == currentPointerId) {
                        currentPointerId = NULL_POINTER_ID;

                        resetOngoingBitmap();
                        toolPath = tool.end(ongoingOperationBitmap, pixelTouch);
                        tool.cancel();
                       commitIfWithinDrawingBounds(toolPath);
                    }
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
            // Background
            canvas.drawColor(Color.LTGRAY);

            // Calculates the zoom and pan transformation
            transformation.setTranslate(-viewport.left, -viewport.top);
            transformation.postScale(scaleListener.getScale(), scaleListener.getScale());

            // Applies the transformation to the border shadow around the drawing
            transformedBitmapRect.set(0, 0, layerWidth, layerHeight);
            transformation.mapRect(transformedBitmapRect);

            // User drawn image and border shadow
            canvas.drawRect(transformedBitmapRect, shadowPaint);
            canvas.drawBitmap(ongoingOperationBitmap, transformation, bitmapPaint);

            // Grid lines
            if (pixelGrid.isEnabled()) {
                pixelGrid.draw(canvas, viewport, scaleListener.getScale());
            }

            // Thumbnail
            if (thumbnail.isEnabled()) {
                // Draws only when zoomed in
                float scale = scaleListener.getScale();
                if (scale > initialScale || scale > initialScale) {
                    thumbnail.draw(canvas, ongoingOperationBitmap, scaleListener.getViewport(), bitmapPaint, shadowPaint);
                }
            }
        }
    }

    private void resetOngoingBitmap() {
        ongoingOperationCanvas.setBitmap(ongoingOperationBitmap);
        ongoingOperationCanvas.drawBitmap(layers.get(currentLayer), 0, 0, bitmapPaint);
    }

    private void commitOngoingOperation() {
        ongoingOperationCanvas.setBitmap(layers.get(currentLayer));
        ongoingOperationCanvas.drawBitmap(ongoingOperationBitmap, 0, 0, bitmapPaint);
    }

    private void commitIfWithinDrawingBounds(Path toolPath) {
        toolPath.computeBounds(toolPathBounds, false);
        transformation.mapRect(toolPathBounds);
        if (toolPathBounds.intersects(transformedBitmapRect.left, transformedBitmapRect.top, transformedBitmapRect.right, transformedBitmapRect.bottom)) {
            commitOngoingOperation();
            undoRedoTracker.bitmapModified(layers.get(currentLayer));
        }
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public void undo() {
        undoRedoTracker.undo(layers.get(currentLayer));
        resetOngoingBitmap();
    }

    public void redo() {
        undoRedoTracker.redo(layers.get(currentLayer));
        resetOngoingBitmap();
    }

    public void setConfigurationChanged(boolean configurationChanged) {
        this.configurationChanged = configurationChanged;
    }

    public boolean isSurfaceCreated() {
        return surfaceCreated;
    }

    public boolean isGridEnabled() {
        return pixelGrid.isEnabled();
    }

    public void setGridEnabled(boolean enabled) {
        if (pixelGrid == null) {
            restoredGridState = enabled;
        } else {
            pixelGrid.setEnabled(enabled);
        }
    }

    public RectF getViewport() {
        return scaleListener.getViewport();
    }

    public void restoreViewport(float centerX, float centerY, float scale) {
        restoredCenterX = centerX;
        restoredCenterY = centerY;
        restoredScale = scale;
    }

    public Bitmap getLayers() {
        return ongoingOperationBitmap;
    }

    public void setRestoredLayers(Bitmap bitmap) {
        restoredLayer = bitmap;
    }

    public float getScale() {
        return scaleListener.getScale();
    }

    public void setOnClearPanelsListener(OnClearPanelsListener onClearPanelsListener) {
        this.onClearPanelsListener = onClearPanelsListener;
    }

    public void setOnDimensionsCalculatedListener(OnDimensionsCalculatedListener onDimensionsCalculatedListener) {
        this.onDimensionsCalculatedListener = onDimensionsCalculatedListener;
    }

    public interface OnClearPanelsListener {
        public void onClearPanels();
    }

    public interface OnDimensionsCalculatedListener {
        public void onDimensionsCalculated(int width, int height);
    }
}