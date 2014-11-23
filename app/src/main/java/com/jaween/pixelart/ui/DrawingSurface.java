package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.UndoRedoTracker;
import com.jaween.pixelart.tools.ToolReport;
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
    public static final float DEFAULT_SCALE = 4;
    private float thumbnailScaleTheshold;
    private Rect displayRect = new Rect();
    private RectF transformedBitmapRect = new RectF();
    private Matrix transformation = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private ScaleListener scaleListener;
    private Paint shadowPaint = new Paint();
    private float shadowWidthDp;

    // Selection
    private static final int SELECTION_BORDER_ALPHA = 240;
    private static final int SELECTION_FILL_ALPHA = 50;
    private Paint selectionBorderPaint = new Paint();
    private Paint selectionInnerPaint = new Paint();

    // Tool and drawing variables
    private Tool tool;
    private Paint toolPaint = new Paint();
    private Paint bitmapPaint;
    private PointF displayTouch = new PointF();
    private float dp;
    private Canvas ongoingOperationCanvas;
    private Bitmap ongoingOperationBitmap;
    private OnSelectRegionListener onSelectRegionListener = null;
    private OnDropColourListener onDropColourListener = null;

    // UI Controls
    private static final int MAX_UNDOS = 50;
    private UndoRedoTracker undoRedoTracker;
    private PixelGrid pixelGrid;
    private Thumbnail thumbnail;
    private OnClearPanelsListener onClearPanelsListener = null;
    private ToolReport toolReport;
    private RectF toolPathBounds = new RectF();
    private final int selectionColour;

    // Layers
    private ArrayList<Bitmap> layers;
    private int currentLayer = 0;
    private int layerWidth;
    private int layerHeight;
    private OnDimensionsCalculatedListener onDimensionsCalculatedListener = null;

    // Touch variables
    private static final int NULL_POINTER_ID = -1;
    private int currentPointerId = NULL_POINTER_ID;
    private static final long CANCEL_DRAWING_MILLIS = 300;
    private long touchDownTime = 0;
    private PointF pixelTouch = new PointF();
    private boolean requiresUndoStackSaving = false;

    // Configuration Change variables
    private boolean configurationChanged = false;
    private RectF viewport = new RectF();
    private Bitmap restoredLayer = null;
    private float restoredCenterX;
    private float restoredCenterY;
    private float restoredScale;
    private boolean restoredGridState;

    // Temporary UI variables
    private Paint tempTextPaint = new Paint();
    private Paint blankOutPaint = new Paint();
    private Path selectedPath = new Path();
    BitmapDrawable checkerboardTile;
    Bitmap bitmap;

    public DrawingSurface(Context context, Tool tool) {
        super(context);

        // Screen density info
        dp = context.getResources().getDisplayMetrics().density;

        holder = getHolder();
        holder.addCallback(this);

        selectionColour = context.getResources().getColor(R.color.tool_selection_colour);

        initialisePaints();

        this.context = context;
        this.tool = tool;
    }

    private void initialisePaints() {
        // Tool paint defaults
        toolPaint.setAntiAlias(false);
        toolPaint.setStyle(Paint.Style.STROKE);
        toolPaint.setColor(Color.DKGRAY);

        // Purely used to blit bitmaps
        bitmapPaint = new Paint();
        bitmapPaint.setStyle(Paint.Style.STROKE);

        // Shadow around canvas and thumbnail
        shadowWidthDp = 4 * dp;
        shadowPaint.setShadowLayer(shadowWidthDp, 0, shadowWidthDp / 2, Color.DKGRAY);

        // Region selection
        selectionBorderPaint.setStrokeWidth(2 * dp);
        selectionBorderPaint.setColor(selectionColour);
        selectionBorderPaint.setAlpha(SELECTION_BORDER_ALPHA);
        selectionBorderPaint.setAntiAlias(true);
        selectionBorderPaint.setPathEffect(new DashPathEffect(new float[]{ 8 * dp, 8 * dp }, 0));
        selectionBorderPaint.setStyle(Paint.Style.STROKE);

        selectionInnerPaint.setColor(selectionColour);
        selectionInnerPaint.setAlpha(SELECTION_FILL_ALPHA);
        selectionInnerPaint.setAntiAlias(true);
        selectionInnerPaint.setStyle(Paint.Style.FILL);

        // Blank out sections
        blankOutPaint.setColor(Color.WHITE);
        blankOutPaint.setStyle(Paint.Style.FILL);
        blankOutPaint.setAntiAlias(false);


        // Temporary UI text
        tempTextPaint.setTextSize(30);
        tempTextPaint.setAntiAlias(true);
        tempTextPaint.setColor(Color.MAGENTA);
        tempTextPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        initialiseLayers();

        initialiseViewport();

        initialiseGestureDetector();

        // Thumbnail
        float thumbnailLeft = getWidth() - layerWidth * dp
                - getResources().getDimension(R.dimen.canvas_margin);
        float thumbnailTop = getHeight() - layerHeight * dp
                - getResources().getDimension(R.dimen.canvas_margin);
        thumbnail = new Thumbnail(thumbnailLeft, thumbnailTop, layerWidth * dp, layerHeight * dp, dp);

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

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(bitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
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
            ongoingOperationCanvas.drawColor(blankOutPaint.getColor());
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
        }

        // Scale limit determining when thumbnail displayed (viewport's smaller dimension can't fit the image)
        if (getWidth() < getHeight()) {
            thumbnailScaleTheshold = getWidth() / layerWidth;
        }  else {
            thumbnailScaleTheshold = getHeight() / layerHeight;
        }

    }

    // Multi-touch zoom and pan
    private void initialiseGestureDetector() {
        // Smallest scale is when 1 pixel of the image equals 1 dp
        final float minScale = dp;

        // Largest scale is when there are only 4 image pixels on screen in the smaller dimension
        final float smallerDimensionPixels = Math.min(getWidth(), getHeight());
        final float pixelsAlongSmallerDimension = 4;
        final float maxScale =  smallerDimensionPixels / pixelsAlongSmallerDimension;

        displayRect.set(0, 0, getWidth(), getHeight());
        scaleListener = new ScaleListener(context, minScale, maxScale, viewport, displayRect);
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

                // TODO Fix similar IllegalArgumentException on app startup
                //11-21 14:56:03.900  26409-26436/com.jaween.pixelart E/AndroidRuntime﹕ FATAL EXCEPTION: Thread-6285
                //java.lang.IllegalArgumentException
                //at android.view.Surface.nativeUnlockCanvasAndPost(Native Method)
                //at android.view.Surface.unlockCanvasAndPost(Surface.java:255)
                //at android.view.SurfaceView$4.unlockCanvasAndPost(SurfaceView.java:844)
                //at com.jaween.pixelart.ui.DrawingSurface.run(DrawingSurface.java:333)
                //at java.lang.Thread.run(Thread.java:841)
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
                    requiresUndoStackSaving = false;
                } else {
                    // Scaling begun long after the initial touch, commits the drawing operation
                    // up until this point, but cancels further drawing
                    toolReport = tool.end(ongoingOperationBitmap, pixelTouch);
                    tool.cancel();

                    commitIfWithinDrawingBounds(toolReport);
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
                    requiresUndoStackSaving = true;
                    toolReport = tool.start(ongoingOperationBitmap, pixelTouch);
                    dropColour(toolReport);
                    break;
                case MotionEvent.ACTION_MOVE:
                    resetOngoingBitmap();
                    toolReport = tool.move(ongoingOperationBitmap, pixelTouch);
                    dropColour(toolReport);
                    break;
                case MotionEvent.ACTION_UP:
                    currentPointerId = NULL_POINTER_ID;
                    resetOngoingBitmap();
                    toolReport = tool.end(ongoingOperationBitmap, pixelTouch);

                    commitIfWithinDrawingBounds(toolReport);
                    selectRegion(toolReport);
                    dropColour(toolReport);
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
                        toolReport = tool.end(ongoingOperationBitmap, pixelTouch);
                        tool.cancel();

                        commitIfWithinDrawingBounds(toolReport);
                        selectRegion(toolReport);
                        dropColour(toolReport);
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
            //canvas.drawBitmap(checkerboardTile.getBitmap(), 0, 0, bitmapPaint);
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

            // Selection
            if (toolReport != null && tool.getToolAttributes().isSelector()) {
                toolReport.getPath().transform(transformation, selectedPath);
                canvas.drawPath(selectedPath, selectionInnerPaint);
                canvas.drawPath(selectedPath, selectionBorderPaint);
            }

            // Grid lines
            if (pixelGrid.isEnabled()) {
                pixelGrid.draw(canvas, viewport, scaleListener.getScale());
            }

            // Thumbnail
            if (thumbnail.isEnabled()) {
                // Draws only when zoomed in
                float scale = scaleListener.getScale();
                if (scale > thumbnailScaleTheshold) {
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

    private void commitIfWithinDrawingBounds(ToolReport toolToolReport) {
        if (requiresUndoStackSaving) {
            requiresUndoStackSaving = false;

            if (tool.getToolAttributes().isMutator()) {
                toolToolReport.getPath().computeBounds(toolPathBounds, false);
                transformation.mapRect(toolPathBounds);
                if (toolPathBounds.intersects(transformedBitmapRect.left, transformedBitmapRect.top, transformedBitmapRect.right, transformedBitmapRect.bottom)) {
                    commitOngoingOperation();
                    undoRedoTracker.bitmapModified(layers.get(currentLayer));
                }
            }
        }
    }

    private void selectRegion(ToolReport toolReport) {
        if (tool.getToolAttributes().isSelector() && onSelectRegionListener != null && toolReport != null) {
            toolReport.getPath().transform(transformation, selectedPath);
            onSelectRegionListener.onSelectRegion(selectedPath);
        }
    }

    private void dropColour(ToolReport toolReport) {
        if (tool.getToolAttributes().isDropper() && onDropColourListener != null && toolReport != null) {
            onDropColourListener.onDropColour(toolReport.getDropColour());
        }
    }

    public void dismissSelection() {
        toolReport = null;
    }

    public void clearSelection() {
        if (toolReport != null) {
            ongoingOperationCanvas.setBitmap(ongoingOperationBitmap);
            ongoingOperationCanvas.drawPath(toolReport.getPath(), blankOutPaint);
            commitOngoingOperation();

            undoRedoTracker.bitmapModified(layers.get(currentLayer));
        } else {
            Log.e("DrawingSurface", "Error: Clearing section but toolReport was null!");
        }

        dismissSelection();
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        toolReport = null;
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

    public void setOnSelectRegionListener(OnSelectRegionListener onSelectRegionListener) {
        this.onSelectRegionListener = onSelectRegionListener;
    }

    public void setOnDropColourListener(OnDropColourListener onDropColourListener) {
        this.onDropColourListener = onDropColourListener;
    }

    public interface OnClearPanelsListener {
        public void onClearPanels();
    }

    public interface OnDimensionsCalculatedListener {
        public void onDimensionsCalculated(int width, int height);
    }

    public interface OnSelectRegionListener {
        public void onSelectRegion(Path selectedRegion);
    }

    public interface OnDropColourListener {
        public void onDropColour(int colour);
    }
}