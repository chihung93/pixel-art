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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.util.UndoRedoTracker;
import com.jaween.pixelart.tools.ToolReport;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.util.ScaleListener;

import java.util.LinkedList;

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
    private int surfaceBackgroundColour;
    private float thumbnailScaleThreshold;
    private Rect displayRect = new Rect();
    private RectF transformedBitmapRectF = new RectF();
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
    private Path selectAllPath = new Path();

    // Tool and drawing variables
    private Tool tool;
    private Paint toolPaint = new Paint();
    private Paint bitmapPaint;
    private PointF displayTouch = new PointF();
    private float dp;
    private Canvas reusableCanvas = new Canvas();
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
    public static final int NULL_CURRENT_LAYER = -1;
    private LinkedList<Bitmap> layers;
    private int currentLayer = NULL_CURRENT_LAYER;
    private int layerWidth;
    private int layerHeight;
    private Bitmap compositeBitmap;
    private LinkedList<Layer> layerItems;
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
    private float restoredCenterX;
    private float restoredCenterY;
    private float restoredScale;
    private boolean restoredGridState;

    // Temporary UI variables
    private Paint tempTextPaint = new Paint();
    private Paint blankOutPaint = new Paint();
    private Path selectedPath = new Path();
    private BitmapDrawable checkerboardTile;
    private Rect transformedBitmapRect = new Rect();

    public DrawingSurface(Context context, Tool tool) {
        super(context);

        // SurfaceView details
        holder = getHolder();
        holder.addCallback(this);

        // Resources
        dp = context.getResources().getDisplayMetrics().density;
        selectionColour = context.getResources().getColor(R.color.tool_selection_colour);
        surfaceBackgroundColour = context.getResources().getColor(R.color.surface_background_colour);

        // Paints
        initialisePaints();

        // Transparency checkerboard background
        Bitmap checkerboardBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(getResources(), checkerboardBitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

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
        blankOutPaint.setColor(Color.TRANSPARENT);
        blankOutPaint.setStyle(Paint.Style.FILL);
        blankOutPaint.setAntiAlias(false);
        blankOutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

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
        // To avoid the pixels lose their aspect ratio, we round the thumbnail scale
        // We round up as we don' want pixels smaller than 1dp
        float thumnbnailScale = (float) Math.ceil(dp);
        float thumbnailLeft = getWidth() - layerWidth * thumnbnailScale
                - getResources().getDimension(R.dimen.canvas_margin);
        float thumbnailTop = getHeight() - layerHeight * thumnbnailScale
                - getResources().getDimension(R.dimen.canvas_margin);
        thumbnail = new Thumbnail(
                thumbnailLeft,
                thumbnailTop,
                layerWidth * thumnbnailScale,
                layerHeight * thumnbnailScale,
                thumnbnailScale);

        // UI Controls
        int maxUndos = MAX_UNDOS;
        undoRedoTracker = new UndoRedoTracker(layers.get(currentLayer), maxUndos);

        // Grid
        int majorPixelSpacing = 8;
        pixelGrid = new PixelGrid(dp, layerWidth, layerHeight, majorPixelSpacing);
        if (configurationChanged) {
            pixelGrid.setEnabled(restoredGridState);
        }

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(bitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        surfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Notifies the owner fragment that the size has been calculated
        if (onDimensionsCalculatedListener != null) {
            onDimensionsCalculatedListener.onDimensionsCalculated(layerWidth, layerHeight);
        }

        // Encompasses the entire layer with path
        selectedPath.moveTo(0, 0);
        selectAllPath.lineTo(layerWidth, 0);
        selectAllPath.lineTo(layerWidth, layerHeight);
        selectAllPath.lineTo(0, layerHeight);
        selectAllPath.close();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
        onDimensionsCalculatedListener = null;
    }

    private void initialiseLayers() {
        if (currentLayer == NULL_CURRENT_LAYER) {
            // New layers
            layers = new LinkedList<Bitmap>();
            layerWidth = 128;//(int) (getWidth() / INITIAL_SCALE);
            layerHeight = 128;//(int) (getHeight() / INITIAL_SCALE);
            currentLayer = 0;

            // Bitmap that is used to temporarily store ongoing drawing operations
            // (e.g. dragging out an oval shape is an ongoing operation that draws many temporary ovals)
            ongoingOperationBitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);

            addLayer(false);
            resetOngoingBitmap();
        } else {
            // Restored layers
            layerWidth = layers.get(currentLayer).getWidth();
            layerHeight = layers.get(currentLayer).getHeight();
        }

        compositeBitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);

        // Attaches the canvas to a layer
        reusableCanvas.setBitmap(layers.get(currentLayer));
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
            thumbnailScaleThreshold = getWidth() / layerWidth;
        }  else {
            thumbnailScaleThreshold = getHeight() / layerHeight;
        }

    }

    // Multi-touch zoom and pan
    private void initialiseGestureDetector() {
        // Smallest scale is when either 1 pixel of the image equals 1 dp rounded up
        // Avoids pixels losing aspect ratio when the device's density is not round by rounding up
        // TODO: Improve, rounding to multiples of 0.5 would be better, right?
        final float minScale = (float) Math.ceil(dp);

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
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException e) {
                        // Fixes occasional crash on Galaxy Nexus during Fragment startup
                        Log.e("DrawingSurface", "unlockCanvasAndPost error caught!");
                        e.printStackTrace();
                    }
                }
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

            // Clears any panels
            if (onClearPanelsListener != null) {
                onClearPanelsListener.onClearPanels();
            }

            // Stops drawing if the current layer is locked
            if (layerItems.get(currentLayer).isLocked()) {
                // Consumes the touch event
                return true;
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
                    // Scaling began soon after the initial touch, rolls back the drawing operation
                    resetOngoingBitmap();
                    requiresUndoStackSaving = false;
                } else {
                    // Scaling begun long after the initial touch, commits the drawing operation
                    // up until this point, but cancels further drawing
                    toolReport = tool.end(ongoingOperationBitmap, pixelTouch);
                    commitIfWithinDrawingBounds(toolReport);
                }
                tool.cancel();
                dismissSelection();
                return true;
            }

            // Single-touch event
            switch (action) {

                case MotionEvent.ACTION_DOWN:
                    touchDownTime = System.currentTimeMillis();
                    requiresUndoStackSaving = true;
                    toolReport = tool.start(ongoingOperationBitmap, pixelTouch);
                    transformRegion(toolReport);
                    dropColour(toolReport);
                    break;
                case MotionEvent.ACTION_MOVE:
                    resetOngoingBitmap();
                    toolReport = tool.move(ongoingOperationBitmap, pixelTouch);
                    transformRegion(toolReport);
                    dropColour(toolReport);
                    break;
                case MotionEvent.ACTION_UP:
                    currentPointerId = NULL_POINTER_ID;
                    resetOngoingBitmap();
                    toolReport = tool.end(ongoingOperationBitmap, pixelTouch);

                    commitIfWithinDrawingBounds(toolReport);
                    transformRegion(toolReport);
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
                        transformRegion(toolReport);
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
            canvas.drawColor(surfaceBackgroundColour);

            // Calculates the zoom and pan transformation
            transformation.setTranslate(-viewport.left, -viewport.top);
            transformation.postScale(scaleListener.getScale(), scaleListener.getScale());

            // Applies the transformation to the border shadow around the drawing
            transformedBitmapRectF.set(0, 0, layerWidth, layerHeight);
            transformation.mapRect(transformedBitmapRectF);

            // Image's border shadow
            canvas.drawRect(transformedBitmapRectF, shadowPaint);

            // Transparency checkerboard
            transformedBitmapRect.set((int) transformedBitmapRectF.left, (int) transformedBitmapRectF.top, (int) transformedBitmapRectF.right, (int) transformedBitmapRectF.bottom);
            checkerboardTile.setBounds(transformedBitmapRect);
            checkerboardTile.draw(canvas);

            // Draws the users image
            compositeLayers();
            canvas.drawBitmap(compositeBitmap, transformation, bitmapPaint);

            // Selection
            if (!selectedPath.isEmpty() && tool.getToolAttributes().isSelector()) {
                //toolReport.getPath().transform(transformation, selectedPath);
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
                if (scale > thumbnailScaleThreshold) {
                    thumbnail.draw(canvas, compositeBitmap, scaleListener.getViewport(), checkerboardTile, bitmapPaint, shadowPaint);
                }
            }
        }
    }

    // Composites layers onto the single compositeBitmap
    private void compositeLayers() {
        // Blanks out composite bitmap
        compositeBitmap.eraseColor(blankOutPaint.getColor());
        reusableCanvas.setBitmap(compositeBitmap);

        // Draws each visible layer of the user's image
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (layerItems.get(i).isVisible()) {
                Bitmap layer;
                if (i == currentLayer) {
                    layer = ongoingOperationBitmap;
                } else {
                    layer = layers.get(i);
                }

                reusableCanvas.drawBitmap(layer, 0, 0, bitmapPaint);
            }
        }
    }

    private void resetOngoingBitmap() {
        // Erases the ongoing operation, then blits the current layer onto it
        ongoingOperationBitmap.eraseColor(blankOutPaint.getColor());
        reusableCanvas.setBitmap(ongoingOperationBitmap);
        reusableCanvas.drawBitmap(layers.get(currentLayer), 0, 0, bitmapPaint);
    }

    private void commitOngoingOperation() {
        // Erases the current layer, then blits the ongoing operation onto it
        layers.get(currentLayer).eraseColor(blankOutPaint.getColor());
        reusableCanvas.setBitmap(layers.get(currentLayer));
        reusableCanvas.drawBitmap(ongoingOperationBitmap, 0, 0, bitmapPaint);
    }

    private void commitIfWithinDrawingBounds(ToolReport toolToolReport) {
        if (requiresUndoStackSaving) {
            requiresUndoStackSaving = false;

            if (tool.getToolAttributes().isMutator()) {
                toolToolReport.getPath().computeBounds(toolPathBounds, false);
                transformation.mapRect(toolPathBounds);
                if (toolPathBounds.intersects(transformedBitmapRectF.left, transformedBitmapRectF.top, transformedBitmapRectF.right, transformedBitmapRectF.bottom)) {
                    commitOngoingOperation();
                    undoRedoTracker.bitmapModified(layers.get(currentLayer));
                }
            }
        }
    }

    private void transformRegion(ToolReport toolReport) {
        if (toolReport != null) {
            toolReport.getPath().transform(transformation, selectedPath);
        }
    }

    // To be used when the path is determined by what the user has drawn
    private void selectRegion(ToolReport toolReport) {
        if (tool.getToolAttributes().isSelector() && onSelectRegionListener != null && toolReport != null) {
            onSelectRegionListener.onSelectRegion(selectedPath);
        }
    }

    public void selectAll() {
        if (toolReport != null) {
            toolReport.getPath().set(selectAllPath);
            transformRegion(toolReport);
            selectRegion(toolReport);
        }
    }

    public void setLayerItems(LinkedList<Layer> layerItems) {
        this.layerItems = layerItems;
    }

    public void setCurrentLayer(int i) {
        currentLayer = i;
        resetOngoingBitmap();
    }

    public int getCurrentLayer() {
        return currentLayer;
    }

    public Bitmap getOngoingOperationBitmap() {
        return ongoingOperationBitmap;
    }

    public void setRestoredOngoingBitmap(Bitmap ongoingOperationBitmap) {
        this.ongoingOperationBitmap = ongoingOperationBitmap;
    }

    public Bitmap addLayer(boolean duplicate) {
        Bitmap newLayer;
        if (duplicate) {
            // Clones the current layer
            newLayer = layers.get(currentLayer).copy(layers.get(currentLayer).getConfig(), true);
        } else {
            newLayer = Bitmap.createBitmap(
                    layerWidth,
                    layerHeight,
                    Bitmap.Config.ARGB_8888);

            // Blanks out the new layer
            reusableCanvas.setBitmap(newLayer);
            newLayer.eraseColor(blankOutPaint.getColor());
        }

        layers.addFirst(newLayer);

        setCurrentLayer(0);

        return newLayer;
    }

    public void deleteLayer(int i) {
        // Current layer must have its index reduced when i is greater than it or if it's at the end of the list
        if (currentLayer > i || currentLayer == layers.size() - 1) {
            currentLayer--;
            setCurrentLayer(currentLayer);
        }

        layers.remove(i);


    }

    public int getLayerWidth() {
        return layerWidth;
    }

    public int getLayerHeight() {
        return layerHeight;
    }

    public void dismissSelection() {
        selectedPath.reset();;
    }

    public void clearSelection() {
        if (toolReport != null) {
            reusableCanvas.setBitmap(ongoingOperationBitmap);
            reusableCanvas.drawPath(toolReport.getPath(), blankOutPaint);
            commitOngoingOperation();

            undoRedoTracker.bitmapModified(layers.get(currentLayer));
        } else {
            Log.e("DrawingSurface", "Error: Clearing section but toolReport was null!");
        }

        dismissSelection();
    }

    private void dropColour(ToolReport toolReport) {
        if (tool.getToolAttributes().isDropper() && onDropColourListener != null && toolReport != null) {
            onDropColourListener.onDropColour(toolReport.getDropColour());
        }
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        toolReport = null;
        selectedPath.reset();
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

    public LinkedList<Bitmap> getLayers() {
        return layers;
    }

    public void setRestoredLayers(LinkedList<Bitmap> restoredLayers) {
        layers = restoredLayers;
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