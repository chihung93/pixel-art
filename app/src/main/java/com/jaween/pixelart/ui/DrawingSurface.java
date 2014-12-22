package com.jaween.pixelart.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.ui.layer.Layer;
import com.jaween.pixelart.ui.undo.DrawOpManager;
import com.jaween.pixelart.ui.undo.DrawOpUndoData;
import com.jaween.pixelart.ui.undo.UndoItem;
import com.jaween.pixelart.ui.undo.UndoManager;
import com.jaween.pixelart.tools.ToolReport;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.util.MarchingAnts;
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
    private static final int MILLIS_PER_FRAME = 1000 / 30;
    public static final float DEFAULT_SCALE = 4;
    private int surfaceBackgroundColour;
    private float thumbnailScaleThreshold;
    private Rect surfaceRect = new Rect();
    private RectF transformedBitmapRectF = new RectF();
    private Matrix transformation = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private ScaleListener scaleListener;
    private Paint shadowPaint = new Paint();
    private Paint thumbnailShadowPaint = new Paint();
    private float shadowWidthDp;
    private float thumbnailShadowWidthDp;
    private TransparencyCheckerboard transparencyCheckerboard;

    // Selection
    private static final int SELECTION_BORDER_ALPHA = 240;
    private static final int SELECTION_FILL_ALPHA = 50;
    private Paint selectionBorderPaint = new Paint();
    private Paint selectionInnerPaint = new Paint();
    private Path selectAllPath = new Path();
    private Path selectAllPathInverse = new Path();

    // Tool and drawing variables
    private Tool tool;
    private PointF displayTouch = new PointF();
    private float dp;
    private Canvas reusableCanvas = new Canvas();
    private Bitmap ongoingOperationBitmap;
    private OnSelectRegionListener onSelectRegionListener = null;
    private OnDropColourListener onDropColourListener = null;
    private Matrix regionMatrix = new Matrix();
    private float selectionDragOffsetX = 0;
    private float selectionDragOffsetY = 0;
    private float selectionRotation = 0;
    private Matrix selectionRotationMatrix = new Matrix();

    // UI Controls
    private PixelGrid pixelGrid;
    private Thumbnail thumbnail;
    private OnClearPanelsListener onClearPanelsListener = null;
    private ToolReport toolReport;
    private RectF toolPathBounds = new RectF();
    private final int selectionColour;
    private Paint blankOutPaint = new Paint();
    private Path selectionPath = new Path();
    private MarchingAnts marchingAnts;

    private LinkedList<Frame> frames;
    private Bitmap compositeBitmap;
    private int currentFrameIndex;
    private int currentLayerIndex;
    private int layerWidth;
    private int layerHeight;

    // Undo system
    private UndoManager undoManager = null;
    private DrawOpManager drawOpManager;

    // Touch variables
    private static final int NULL_POINTER_ID = -1;
    private int currentPointerId = NULL_POINTER_ID;
    private static final long CANCEL_DRAWING_MILLIS = 300;
    private long touchDownTime = 0;
    private PointF pixelTouch = new PointF();
    private boolean requiresUndoStackSaving = false;

    // Configuration change variables
    private boolean configurationChanged = false;
    private RectF viewport = new RectF();
    private float restoredCenterX;
    private float restoredCenterY;
    private float restoredScale;
    private boolean restoredGridState;

    // Temporary UI variables
    private Paint tempTextPaint = new Paint();
    private BitmapDrawable checkerboardTile;
    private boolean compositeDirty = true;
    private Bitmap selectionBitmap;
    private int[] pixelArray;
    private PointF dragPoint = new PointF();
    private boolean draggingSelection = false;



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
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(getResources(), bitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        transparencyCheckerboard = new TransparencyCheckerboard(context);

        this.context = context;
        this.tool = tool;
    }

    private void initialisePaints() {
        // Shadow around canvas and thumbnail
        shadowWidthDp = 4 * dp;
        thumbnailShadowWidthDp = 8 * dp;
        shadowPaint.setShadowLayer(shadowWidthDp, 0, shadowWidthDp / 2, Color.DKGRAY);
        thumbnailShadowPaint.setShadowLayer(thumbnailShadowWidthDp, 0,
                thumbnailShadowWidthDp / 2, Color.DKGRAY);

        // Region selection outer 'marching ants' dashed border (path effect added on each frame)
        selectionBorderPaint.setStrokeWidth(2 * dp);
        selectionBorderPaint.setColor(selectionColour);
        selectionBorderPaint.setAlpha(SELECTION_BORDER_ALPHA);
        selectionBorderPaint.setAntiAlias(true);
        selectionBorderPaint.setStyle(Paint.Style.STROKE);

        // Region selection inner semi-transparent solid colour
        selectionInnerPaint.setColor(selectionColour);
        selectionInnerPaint.setAlpha(SELECTION_FILL_ALPHA);
        selectionInnerPaint.setAntiAlias(true);
        selectionInnerPaint.setStyle(Paint.Style.FILL);

        // Marching ants around the selection
        int dashOn = 8;
        int dashOff = 8;
        marchingAnts = new MarchingAnts(dashOn, dashOff, dp);

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
        surfaceRect.set(0, 0, getWidth(), getHeight());

        initialiseViewport();

        initialiseGestureDetector();

        // Thumbnail
        // To avoid the pixels lose their aspect ratio, we round the thumbnail scale
        // We round up as we don' want pixels smaller than 1dp
        float thumbnailDp = (float) Math.ceil(dp);
        float thumbnailLength = 160 * thumbnailDp;
        float margin = getResources().getDimension(R.dimen.canvas_margin);

        RectF thumbnailRectF = new RectF();
        thumbnailRectF.left = margin;
        thumbnailRectF.top = getHeight() - thumbnailLength - getResources().getDimension(R.dimen.canvas_margin);
        thumbnailRectF.right = thumbnailRectF.left + thumbnailLength;
        thumbnailRectF.bottom = thumbnailRectF.top + thumbnailLength;
        thumbnail = new Thumbnail(context, thumbnailRectF);

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
        // Encompasses the entire layer with a path used to select all
        selectAllPath.moveTo(0, 0);
        selectAllPath.lineTo(layerWidth, 0);
        selectAllPath.lineTo(layerWidth, layerHeight);
        selectAllPath.lineTo(0, layerHeight);
        selectAllPath.close();

        // Encompasses the entire layer with an inverse fill
        selectAllPathInverse.setFillType(Path.FillType.INVERSE_WINDING);
        selectAllPathInverse.moveTo(0, 0);
        selectAllPathInverse.lineTo(layerWidth, 0);
        selectAllPathInverse.lineTo(layerWidth, layerHeight);
        selectAllPathInverse.lineTo(0, layerHeight);
        selectAllPathInverse.close();

        surfaceRect.set(0, 0, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
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

        scaleListener = new ScaleListener(context, minScale, maxScale, viewport, surfaceRect);
        scaleGestureDetector = new ScaleGestureDetector(context, scaleListener);
    }

    int count = 100;
    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        long runningTime = System.currentTimeMillis();

        // Performs the draw loop
        while (running) {
            // Maintains frame rate
            /*long elapsedTime = System.currentTimeMillis() - lastTime;
            if (elapsedTime < MILLIS_PER_FRAME) {
                try {
                    Thread.sleep(MILLIS_PER_FRAME - elapsedTime);
                } catch (InterruptedException e) {
                    // TODO: Handle exception
                }
            }*/

            if (!holder.getSurface().isValid()) {
                continue;
            }
            Canvas canvas = null;

            try {
                canvas = holder.lockCanvas(null);
                synchronized (holder) {
                    draw(canvas);
                }
            } finally {
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

            /*lastTime = System.currentTimeMillis();
            count--;
            if (count == 0) {
                count = 30;
                Log.d("DrawingSurface", "FPS is " + (1000f / ((System.currentTimeMillis() - runningTime) / (float) count)));
                runningTime = System.currentTimeMillis();
            }*/
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

            // Choosing to ignore drawing operations that use multiple fingers
            if (currentPointerId == NULL_POINTER_ID) {
                currentPointerId = event.getPointerId(index);
            }

            // Clears any panels
            if (onClearPanelsListener != null) {
                onClearPanelsListener.onClearPanels();
            }

            // Stops drawing if the current layer is locked
            Layer layer = frames.get(currentFrameIndex).getLayers().get(currentLayerIndex);
            if (layer.isLocked()) {
                // Consumes the touch event
                return true;
            }

            // The composite bitmap will need to be re-composited
            compositeDirty = true;

            // Adjusts the viewport and gets the coordinates of the touch on the drawing
            viewport = scaleListener.getViewport();
            displayTouch.set(event.getX(), event.getY());
            pixelTouch.set(
                    viewport.left + displayTouch.x / getWidth() * viewport.width(),
                    viewport.top + displayTouch.y / getHeight() * viewport.height());

            // Transforms the selected region
            if (!selectionPath.isEmpty()) {
                selectionPath.computeBounds(toolPathBounds, false);
            }

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

                    if (!selectionPath.isEmpty() && toolPathBounds.contains(displayTouch.x, displayTouch.y)) {
                        dragPoint.x = pixelTouch.x;
                        dragPoint.y = pixelTouch.y;

                        transformPath(toolReport.getPath(), selectionPath);
                        draggingSelection = true;
                        break;
                    } else {
                        if (!selectionPath.isEmpty()) {
                            commitSelection();
                        }
                        draggingSelection = false;
                        selectionDragOffsetX = 0;
                        selectionDragOffsetY = 0;

                        resetOngoingBitmap();
                        toolReport = tool.start(ongoingOperationBitmap, pixelTouch);
                        // Changes the Palette's primary colour
                        if (tool.getToolAttributes().isDropper()) {
                            dropColour(toolReport);
                        }
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (draggingSelection) {
                        selectionDragOffsetX += (pixelTouch.x  - dragPoint.x);
                        selectionDragOffsetY += (pixelTouch.y - dragPoint.y);

                        dragPoint.x = pixelTouch.x;
                        dragPoint.y = pixelTouch.y;

                        transformPath(toolReport.getPath(), selectionPath);
                        break;
                    } else {
                        resetOngoingBitmap();
                        toolReport = tool.move(ongoingOperationBitmap, pixelTouch);

                        // Changes the Palette's primary colour
                        if (tool.getToolAttributes().isDropper()) {
                            dropColour(toolReport);
                        }
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    toolEnd(pixelTouch);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    toolCancel();
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    // Ends drawing operation only if the main pointer left the screen
                    if (event.getPointerId(index) == currentPointerId) {
                        toolEnd(pixelTouch);

                        // Stops the tool from jumping to another finger
                        tool.cancel();
                    }
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    private void toolEnd(PointF pixelTouch) {
        currentPointerId = NULL_POINTER_ID;

        // Draws the final frame of the drawing
        if (!draggingSelection) {
            resetOngoingBitmap();
            toolReport = tool.end(ongoingOperationBitmap, pixelTouch);
        }

        // Commits the modifications to the drawing
        if (tool.getToolAttributes().isMutator()) {
            commitIfWithinDrawingBounds(toolReport);
        }

        // Begins a selection or commits an ongoing selection
        if (tool.getToolAttributes().isSelector()) {
            if (!draggingSelection) {
                selectRegion(toolReport.getPath(), toolReport.getInversePath(), false);
            }
        }

        // Changes the Palette's primary colour
        if (tool.getToolAttributes().isDropper()) {
            dropColour(toolReport);
        }

        draggingSelection = false;
    }

    private void toolCancel() {
        currentPointerId = NULL_POINTER_ID;

        // Stops the tool from performing further drawing
        tool.cancel();

        // Undoes any drawings
        resetOngoingBitmap();
        draggingSelection = false;
    }

    @Override
    public void draw(Canvas canvas) {
        if (surfaceCreated) {
            // Background
            //canvas.drawBitmap(checkerboardTile.getFrames(), 0, 0, null);
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
            transparencyCheckerboard.draw(canvas, transformedBitmapRectF, surfaceRect);

            // Draws the user's image (no selection to be drawn)
            compositeLayers();
            canvas.drawBitmap(compositeBitmap, transformation, null);

            // Selection
            if (toolReport != null && !toolReport.getPath().isEmpty() && tool.getToolAttributes().isSelector()) {
                // Applies the transformation to the selected region
                transformPath(toolReport.getPath(), selectionPath);
                transformPath(toolReport.getInversePath(), toolReport.getInversePath());

                // Since the user can drag the clip over non pixel boundaries, we can't simply blit
                // the selectionBitmap onto the ongoingOperationBitmap during onTouch(). We instead
                // draw the clip here onto this canvas. For improved UX, we also draw onto the
                // ongoingOperationBitmap here so it can be seen on the thumbnail.
                regionMatrix.set(selectionRotationMatrix);
                regionMatrix.postTranslate(-viewport.left + selectionDragOffsetX, -viewport.top + selectionDragOffsetY);
                regionMatrix.postScale(scaleListener.getScale(), scaleListener.getScale());
                canvas.drawBitmap(selectionBitmap, regionMatrix, null);

                // Draws the clip on the thumbnail
                /*reusableCanvas.setBitmap(ongoingOperationBitmap);
                reusableCanvas.drawBitmap(selectionBitmap, selectionDragOffsetX, selectionDragOffsetY, null);
                compositeLayers();
                resetOngoingBitmap();*/

                // Draws the animated marching ants dashed outline
                selectionBorderPaint.setPathEffect(marchingAnts.getNextPathEffect());
                canvas.drawPath(selectionPath, selectionInnerPaint);
                canvas.drawPath(selectionPath, selectionBorderPaint);
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
                    thumbnail.draw(canvas, compositeBitmap, scaleListener.getViewport()
                            , thumbnailShadowPaint);
                }
            }
        }
    }

    /** Sets the layer dimensions and allocates memory. **/
    public void setDimensions(int layerWidth, int layerHeight) {

        drawOpManager = new DrawOpManager(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);

        // Used for drawing operations that have progress state
        ongoingOperationBitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);

        // Used for selecting regions
        selectionBitmap = Bitmap.createBitmap(layerWidth, layerHeight, Bitmap.Config.ARGB_8888);
        pixelArray = new int[layerWidth * layerHeight];

        this.layerWidth = layerWidth;
        this.layerHeight = layerHeight;
    }

    // TODO: Since our Fragments are sharing Layer data synchronisation problems could occur
    // 'compositeDirty' reduces the likelihood of the 'layers' list changing during a composition,
    // since we can't switch Frames while drawing
    /** Composites the layers list onto the single compositeBitmap **/
    private void compositeLayers() {
        if (compositeDirty) {
            // Blanks out composite bitmap
            compositeBitmap.eraseColor(blankOutPaint.getColor());

            // Draws each visible layer of the user's image (bottom to top)
            reusableCanvas.setBitmap(compositeBitmap);
            LinkedList<Layer> layers = frames.get(currentFrameIndex).getLayers();
            for (int i = layers.size() - 1; i >= 0; i--) {
                if (layers.get(i).isVisible()) {
                    Bitmap layer;
                    if (i == currentLayerIndex) {
                        layer = ongoingOperationBitmap;
                    } else {
                        layer = layers.get(i).getImage();
                    }
                    reusableCanvas.drawBitmap(layer, 0, 0, null);
                }
            }

            compositeDirty = false;
        }
    }

    private void resetOngoingBitmap() {
        // Erases the ongoing operation, then blits the current layer onto it
        ongoingOperationBitmap.eraseColor(blankOutPaint.getColor());
        reusableCanvas.setBitmap(ongoingOperationBitmap);
        reusableCanvas.drawBitmap(frames.get(currentFrameIndex).getLayers().get(currentLayerIndex).getImage(), 0, 0, null);

        compositeDirty = true;
    }

    // Updates the current layer
    private void commitOngoingOperation() {
        // Erases the current layer, then blits the ongoing operation onto it
        frames.get(currentFrameIndex).getLayers().get(currentLayerIndex).getImage().eraseColor(blankOutPaint.getColor());
        reusableCanvas.setBitmap(frames.get(currentFrameIndex).getLayers().get(currentLayerIndex).getImage());
        reusableCanvas.drawBitmap(ongoingOperationBitmap, 0, 0, null);

        compositeDirty = true;
    }

    private void commitIfWithinDrawingBounds(ToolReport toolReport) {
        if (requiresUndoStackSaving) {
            requiresUndoStackSaving = false;

            if (tool.getToolAttributes().isMutator()) {
                toolReport.getPath().computeBounds(toolPathBounds, false);
                transformation.mapRect(toolPathBounds);
                if (toolPathBounds.intersects(
                        transformedBitmapRectF.left,
                        transformedBitmapRectF.top,
                        transformedBitmapRectF.right,
                        transformedBitmapRectF.bottom)) {
                    commitOngoingOperation();

                    UndoItem undoItem = drawOpManager.add(frames.get(currentFrameIndex).getLayers().get(currentLayerIndex).getImage(), currentFrameIndex, currentLayerIndex);
                    undoManager.pushUndoItem(undoItem);
                }
            }
        }
    }

    private void transformPath(Path path, Path destination) {
        path.offset(selectionDragOffsetX, selectionDragOffsetY, destination);
        destination.transform(transformation);
    }

    /** Clips the selected area and calls the Fragment to start the Contextual ActionBar. **/
    private void selectRegion(Path path, Path inversePath, boolean reuseSelection) {
        if (path.isEmpty()) {
            return;
        }

        // Copies the current bitmap to the selected region bitmap
        Bitmap currentLayerBitmap = frames.
                get(currentFrameIndex).
                getLayers().
                get(currentLayerIndex).
                getImage();
        currentLayerBitmap.getPixels(pixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);
        selectionBitmap.setPixels(pixelArray, 0, layerWidth, 0, 0, layerWidth, layerHeight);

        // Blanks out the clipped area in the ongoing bitmap
        reusableCanvas.setBitmap(ongoingOperationBitmap);
        reusableCanvas.drawPath(path, blankOutPaint);
        commitOngoingOperation();

        if (!reuseSelection) {
            // Clips out the selected area onto the selectionBitmap
            reusableCanvas.setBitmap(selectionBitmap);
            reusableCanvas.drawPath(inversePath, blankOutPaint);
        }

        if (onSelectRegionListener != null) {
            onSelectRegionListener.onSelectRegion(selectionPath);
        }
    }

    // TODO: Rotating selection
    public void rotateSelection() {
        selectionRotation -= 90;
        selectionRotationMatrix.setRotate(selectionRotation, toolPathBounds.centerX(), toolPathBounds.centerY());
        selectionPath.transform(selectionRotationMatrix);
    }

    // TODO: Copying selections
    public void copySelection() {
        commitSelection();
        selectRegion(toolReport.getPath(), toolReport.getInversePath(), true);
    }

    public void selectAll() {
        commitSelection();
        toolReport.getPath().set(selectAllPath);
        toolReport.getInversePath().set(selectAllPathInverse);
        selectRegion(toolReport.getPath(), toolReport.getInversePath(), false);
    }

    public void dismissSelection() {
        if (toolReport != null) {
            toolReport.getPath().reset();
        }

        selectionBitmap.eraseColor(blankOutPaint.getColor());
        selectionPath.reset();

        selectionDragOffsetX = 0;
        selectionDragOffsetY = 0;

        selectionRotation = 0;
        selectionRotationMatrix.reset();
    }

    public void commitSelection() {
        // TODO: May not be necessary
        resetOngoingBitmap();
        reusableCanvas.setBitmap(ongoingOperationBitmap);
        reusableCanvas.drawBitmap(selectionBitmap, selectionDragOffsetX, selectionDragOffsetY, null);
        commitOngoingOperation();

        Layer layer = frames.get(currentFrameIndex).getLayers().get(currentLayerIndex);
        UndoItem undoItem = drawOpManager.add(layer.getImage(), currentFrameIndex, currentLayerIndex);
        undoManager.pushUndoItem(undoItem);

        dismissSelection();
    }

    /** Sets the layer and the composite bitmap. Calls setCurrentFrameIndex(). **/
    public void setFrames(LinkedList<Frame> frames) {
        this.frames = frames;
        setCurrentFrameIndex(currentFrameIndex);
    }

    /** Sets the current frame index, the composite bitmap and calls setCurrentLayerIndex(). **/
    public void setCurrentFrameIndex(int index) {
        currentFrameIndex = index;
        Frame frame = frames.get(currentFrameIndex);
        compositeBitmap = frame.getCompositeBitmap();

        setCurrentLayerIndex(frame.getCurrentLayerIndex());
    }

    /** Sets the current layer index, resets local bitmaps. **/
    public void setCurrentLayerIndex(int index) {
        currentLayerIndex = index;
        resetOngoingBitmap();

        // TODO: Fix: Draw, then switch files and undo => Applies the other file's differences!
        // Notifies the undo manager to change its 'layerBeforeModification'
        Layer layer = frames.get(currentFrameIndex).getLayers().get(currentLayerIndex);
        drawOpManager.switchLayer(layer.getImage());
        reusableCanvas.setBitmap(layer.getImage());

    }

    private void dropColour(ToolReport toolReport) {
        if (tool.getToolAttributes().isDropper() && onDropColourListener != null && toolReport != null) {
            onDropColourListener.onDropColour(toolReport.getDropColour());
        }
    }

    public void setTool(Tool tool) {
        this.tool = tool;
        toolReport = null;
        selectionPath.reset();
    }

    public void setUndoManager(UndoManager undoManager){
        this.undoManager = undoManager;
    }

    public void undo(Object undoData) {
        if (undoData instanceof DrawOpUndoData) {
            drawOpManager.undo(frames, currentFrameIndex, currentLayerIndex, (DrawOpUndoData) undoData);
            resetOngoingBitmap();
        } else {
            String className = "Null";

            if (undoData != null) {
                className = undoData.getClass().getName();
            }
            Log.e("DrawingSurface", "Undo data wasn't of type DrawOpUndoData, it was of type " + className);
        }
    }

    public void redo(Object redoData) {
        if (redoData instanceof DrawOpUndoData) {
            drawOpManager.redo(frames, currentFrameIndex, currentLayerIndex, ((DrawOpUndoData) redoData));
            resetOngoingBitmap();
        } else {
            String className = "Null";

            if (redoData != null) {
                className = redoData.getClass().getName();
            }
            Log.e("DrawingSurface", "Redo data wasn't of type DrawOpUndoData, it was of type " + className);
        }
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

    public float getScale() {
        return scaleListener.getScale();
    }

    public void setOnClearPanelsListener(OnClearPanelsListener onClearPanelsListener) {
        this.onClearPanelsListener = onClearPanelsListener;
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

    public interface OnSelectRegionListener {
        public void onSelectRegion(Path selectedPath);
    }

    public interface OnDropColourListener {
        public void onDropColour(int colour);
    }
}