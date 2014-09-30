package com.jaween.pixelart;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.jaween.pixelart.tools.FloodFill;
import com.jaween.pixelart.tools.Pen;
import com.jaween.pixelart.tools.Tool;

import java.util.ArrayList;

/**
 * Created by ween on 9/28/14.
 */
public class DrawingSurface extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    // Android View and threading vairables
    private Context context;
    private SurfaceHolder holder;
    private Thread thread;
    private boolean running = false;
    private boolean surfaceCreated = false;

    // Canvas transformation
    private static final int MIN_SCALE = 1;
    private static final int MAX_SCALE = 16;
    private static final int DEFAULT_SCALE = 4;
    private float scale = DEFAULT_SCALE;
    private Matrix canvasTransformation;

    // Tool and drawing variables
    private Tool tool;
    private Tool.Attributes toolAttributes;
    private Paint toolPaint;
    private Paint bitmapPaint;
    private Paint thumbnailBorderPaint;
    private Point touchEvent = new Point();
    private Canvas drawOperationsCanvas;

    // Layers
    private ArrayList<Bitmap> layers;
    private int currentLayer = 0;

    // Temporary UI variables
    private long downTime = 0;
    private Pen pen;
    private FloodFill floodFill;
    private Paint tempTextPaint;


    public DrawingSurface(Context context, Tool tool) {
        super(context);

        holder = getHolder();
        holder.addCallback(this);

        // Temporary tools
        pen = new Pen(context);
        floodFill = new FloodFill(context);
        canvasTransformation = new Matrix();
        canvasTransformation.postScale(scale, scale);

        initialisePaints();

        // Tool defaults
        int strokeWidth = 2;
        toolAttributes = new Tool.Attributes(toolPaint, strokeWidth);
        toolAttributes.paint = toolPaint;

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

        thumbnailBorderPaint = new Paint();
        thumbnailBorderPaint.setColor(Color.LTGRAY);
        thumbnailBorderPaint.setStrokeWidth(0);
        thumbnailBorderPaint.setAntiAlias(false);
        thumbnailBorderPaint.setStyle(Paint.Style.STROKE);

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
        layers.add(Bitmap.createBitmap((int) (getWidth() / scale), (int) (getHeight() / scale), Bitmap.Config.ARGB_8888));

        // Attaches a canvas to a layer
        drawOperationsCanvas = new Canvas(layers.get(0));
        drawOperationsCanvas.drawColor(Color.WHITE);

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
            touchEvent.x = event.getX() / scale;
            touchEvent.y = event.getY() / scale;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Temporary UI (tool switching) TODO Proper tool switching
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - downTime < 200) {
                        tool = tool instanceof Pen ? floodFill : pen;
                        Toast.makeText(context, "Switched to " + tool.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        tool.beginAction(drawOperationsCanvas, layers.get(currentLayer), touchEvent, toolAttributes);
                    }
                    downTime = currentTime;

                    break;
                case MotionEvent.ACTION_MOVE:
                    tool.beginAction(drawOperationsCanvas, null, touchEvent, toolAttributes);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    tool.endAction(touchEvent);
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
            canvas.drawColor(Color.LTGRAY);
            canvas.drawBitmap(layers.get(currentLayer), canvasTransformation, bitmapPaint);

            canvas.drawText("Double Tap: Tool Switch", 50, 50, tempTextPaint);
            canvas.drawText("Volume Buttons: Zoom", 50, 100, tempTextPaint);

            // Thumbnail
            if (scale >= DEFAULT_SCALE || scale >= DEFAULT_SCALE) {
                float left = getWidth() - getWidth() / DEFAULT_SCALE - getResources().getDimension(R.dimen.activity_horizontal_margin);
                float top = getHeight() - getHeight() / DEFAULT_SCALE - getResources().getDimension(R.dimen.activity_vertical_margin);
                canvas.drawBitmap(layers.get(currentLayer), left, top, bitmapPaint);
                canvas.drawRect(left, top, left + getWidth() / DEFAULT_SCALE, top + getHeight() / DEFAULT_SCALE, thumbnailBorderPaint);
            }

        }
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public void setScale(float scale) {
        if (scale >= MIN_SCALE && scale <= MAX_SCALE) {
            canvasTransformation.setScale(scale, scale);
            this.scale = scale;
        }
    }

    public float getScale() {
        return scale;
    }

}