package com.jaween.pixelart.ui.animation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jaween.pixelart.R;
import com.jaween.pixelart.ui.undo.FrameUndoData;
import com.jaween.pixelart.ui.undo.UndoItem;
import com.jaween.pixelart.ui.undo.UndoManager;
import com.jaween.pixelart.util.ConfigChangeFragment;

import java.util.LinkedList;

/**
 * Created by ween on 12/6/14.
 */
public class AnimationFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemClickListener,
        FrameAdapter.FrameListItemListener {

    private LinkedList<Frame> frames = null;
    private FrameAdapter frameAdapter;

    private GridView frameGrid;
    private LinearLayout previewLayout;
    private ImageView previewImage;
    private EditText framesPerSecond;
    private ImageView expandCollapseButton;

    private BitmapDrawable checkerboardTile;
    private AnimationDrawable previewAnimation;

    private int currentFrameIndex = 0;
    private int millisPerFrame = 33;

    private FrameListener frameListener;

    private int previewHeight;
    private boolean visible = true;

    // Undo system
    private UndoManager undoManager;

    // Instance state
    private ConfigChangeFragment configChangeWorker;
    private final static String KEY_CURRENT_FRAME_INDEX = "key_current_frame_index";

    // Layout animations
    private AnimatorSet gone = new AnimatorSet();
    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Animation disabled until issues resolved
        //setHasOptionsMenu(true);

        previewAnimation = new AnimationDrawable();
        previewAnimation.setOneShot(false);

        // Transparency checkerboard background
        Resources resources = getResources();
        Bitmap checkerboardBitmap = BitmapFactory.decodeResource(resources, R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(resources, checkerboardBitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.animation_fragment, null);
        linkViews(view);
        initialiseViews();
        initialiseConfigChangeFragment();

        onRestoreInstanceState(savedInstanceState);

        return view;
    }

    /** Links the inflated XML layout to Java objects. **/
    private void linkViews(View v) {
        frameGrid = (GridView) v.findViewById(R.id.gv_frame_list);
        previewLayout = (LinearLayout) v.findViewById(R.id.ll_animation_preview);
        previewImage = (ImageView) v.findViewById(R.id.iv_preview_image);
        framesPerSecond = (EditText) v.findViewById(R.id.et_frames_per_second);
        expandCollapseButton = (ImageView) v.findViewById(R.id.iv_preview_collapse);
    }

    /** Sets up the views, and registers listeners. **/
    private void initialiseViews() {
        // Pre-Jellybean doesn't have setBackground()
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            previewImage.setBackgroundDrawable(checkerboardTile);
        } else {
            previewImage.setBackground(checkerboardTile);
        }
        previewImage.setImageDrawable(previewAnimation);

        // Listeners
        frameGrid.setOnItemClickListener(this);
        expandCollapseButton.setOnClickListener(this);
    }

    /** Retrieves (or creates and adds) the Fragment that is retained across config changes. **/
    private void initialiseConfigChangeFragment() {
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        configChangeWorker = (ConfigChangeFragment) fragmentManager.
                findFragmentByTag(ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);

        // Worker doesn't exist, creates new worker
        if (configChangeWorker == null) {
            configChangeWorker = new ConfigChangeFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(configChangeWorker, ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflates and hides the animation menu
        inflater.inflate(R.menu.animation_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_frame:
                Frame frame = requestFrame(false);
                pushAddItem(currentFrameIndex, frame);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_FRAME_INDEX, currentFrameIndex);

        // Retains the large the Frame list across configuration changes
        configChangeWorker.setFrames(frames);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        restoreFrames();

        if (savedInstanceState != null) {
            currentFrameIndex = savedInstanceState.getInt(KEY_CURRENT_FRAME_INDEX, 0);
        }
    }

    private void restoreFrames() {
        // Attempts to restore the frames
        frames = configChangeWorker.getFrames();
        configChangeWorker.setFrames(null);

        // Creates the adaptor only if it's restoring frames, not if we're loading a file
        if (frames != null) {
            createAdaptor();
        }
    }

    /** Creates and sets the frame adapter. **/
    private void createAdaptor() {
        // Sets the adapter now that we have a reference to our list of frames
        frameAdapter = new FrameAdapter(getActivity(), frames, checkerboardTile);
        frameAdapter.setFrameListItemListener(this);
        frameGrid.setAdapter(frameAdapter);
    }

    /** Notifies the ContainerFragment to pass a different set of Layers to the SurfaceView. **/
    public void notifyCurrentFrameChanged() {
        if (frameListener != null) {
            Frame frame = frames.get(currentFrameIndex);
            frameListener.onCurrentFrameChange(frame, currentFrameIndex);
        }
        activate(currentFrameIndex);
    }

    /** Notifies this Fragment when it can safely request the first frame. **/
    public void notifyFragmentsReady() {
        if (frames == null) {
            frames = new LinkedList<Frame>();
            createAdaptor();
        }

        if (frames.isEmpty()) {
            requestFrame(false);
        }
    }

    /** Sets the current layer index of the currently selected frame. **/
    public void setCurrentLayerIndex(int index) {
        frames.get(currentFrameIndex).setCurrentLayerIndex(index);
    }

    /** Retrieves the currently selected Frame. **/
    public Frame getFrame() {
        return frames.get(currentFrameIndex);
    }

    /** Retrieves the list of all the Frames. **/
    public LinkedList<Frame> getFrames() {
        return frames;
    }

    /** Sets the frames and creates the adaptor. **/
    public void setFrames(LinkedList<Frame> frames) {
        this.frames = frames;
        createAdaptor();
    }

    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    private void pushAddItem(int frameIndex, Frame frame) {
        FrameUndoData frameUndoData = new FrameUndoData(
                FrameUndoData.FrameOperation.ADD,
                frameIndex,
                frame);
        UndoItem undoItem = new UndoItem(UndoItem.Type.FRAME, 0, frameUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    private void pushDeleteItem(int frameIndex, Frame frame) {
        FrameUndoData frameUndoData = new FrameUndoData(
                FrameUndoData.FrameOperation.DELETE,
                frameIndex,
                frame);
        UndoItem undoItem = new UndoItem(UndoItem.Type.FRAME, 0, frameUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    /** Deletes and returns the Frame at the given index and updates the UI accordingly **/
    private Frame deleteFrame(int frameIndex) {
        Frame currentFrame = frames.get(currentFrameIndex);
        Frame deleteFrame = frames.get(currentFrameIndex);

        // Updates the data structure and animates out the item
        frames.remove(frameIndex);

        // If the current frame has moved, points the currentFrameIndex at its new position
        if (frameIndex == currentFrameIndex && frameIndex == frames.size()) {
            // Frame deleted was the current frame and it was the final frame
            // New current frame is the one above it
            if (frames.size() > 0) {
                currentFrameIndex--;
                notifyCurrentFrameChanged();
            }
        } else {
            // Finds the position of the new frame
            int newCurrentFrameIndex = frames.indexOf(currentFrame);
            if (newCurrentFrameIndex != -1) {
                if (currentFrameIndex != newCurrentFrameIndex) {
                    currentFrameIndex = newCurrentFrameIndex;
                    notifyCurrentFrameChanged();
                }
            }
        }

        // Updates the UI
        frameAdapter.notifyDataSetChanged();

        return deleteFrame;
    }

    /**
     * Moves a frame in the data set. This does not push/pop the undo stack.
     * @param from The source index from which the frame must move
     * @param to The destination index to which the frame must move
     */
    private void moveFrame(int from, int to) {
        Frame currentFrame = frames.get(currentFrameIndex);

        Frame movingFrame = frames.remove(from);
        frames.add(to, movingFrame);
        frameAdapter.notifyDataSetChanged();

        int newCurrentFrameIndex = frames.indexOf(currentFrame);
        if (currentFrameIndex != newCurrentFrameIndex) {
            currentFrameIndex = newCurrentFrameIndex;
            notifyCurrentFrameChanged();
        }
    }

    public void undo(Object undoData) {
        if (undoData instanceof FrameUndoData) {
            switch (((FrameUndoData) undoData).getType()) {
                case ADD:
                    int frameIndex = ((FrameUndoData) undoData).getFrameIndex();
                    deleteFrame(frameIndex);
                    break;
                case DELETE:
                    frameIndex = ((FrameUndoData) undoData).getFrameIndex();
                    Frame frame = ((FrameUndoData) undoData).getFrame();
                    frames.add(frameIndex, frame);
                    frameAdapter.notifyDataSetChanged();
                    notifyCurrentFrameChanged();
                    break;
                case MOVE:
                    int fromIndex = ((FrameUndoData) undoData).getFromIndex();
                    int toIndex = ((FrameUndoData) undoData).getToIndex();
                    moveFrame(toIndex, fromIndex);
                    break;
            }
        } else {
            String className = "Null";

            if (undoData != null) {
                className = undoData.getClass().getName();
            }
            Log.e("AnimationFragment", "Undo data wasn't of type FrameUndoData, it was of type " + className);
        }
    }

    public void redo(Object redoData) {
        if (redoData instanceof FrameUndoData) {
            switch (((FrameUndoData) redoData).getType()) {
                case ADD:
                    int frameIndex = ((FrameUndoData) redoData).getFrameIndex();
                    Frame frame = ((FrameUndoData) redoData).getFrame();
                    frames.add(frameIndex, frame);
                    frameAdapter.notifyDataSetChanged();
                    break;
                case DELETE:
                    frameIndex = ((FrameUndoData) redoData).getFrameIndex();
                    deleteFrame(frameIndex);
                    break;
                case MOVE:
                    int fromIndex = ((FrameUndoData) redoData).getFromIndex();
                    int toIndex = ((FrameUndoData) redoData).getToIndex();
                    moveFrame(fromIndex, toIndex);
                    break;
            }
        } else {
            String className = "Null";

            if (redoData != null) {
                className = redoData.getClass().getName();
            }
            Log.e("AnimationFragment", "Redo data wasn't of type FrameUndoData, it was of type " + className);
        }
    }

    /**
     * Requests the the FrameListener to add a frame and calls updatePreview().
     * Returns the new Frame or null if there was no listener.
     * **/
    private Frame requestFrame(boolean duplicate) {
        if (frameListener != null) {
            Frame frame = frameListener.requestFrame(duplicate);
            frames.add(frame);
            frameAdapter.notifyDataSetChanged();

            updatePreview();
            return frame;
        }
        return null;
    }

    private void updatePreview() {
        previewAnimation = new AnimationDrawable();

        for (int i = 0; i < frames.size(); i++) {
            Bitmap compositeBitmap = frames.get(i).getCompositeBitmap();
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), compositeBitmap);
            previewAnimation.addFrame(bitmapDrawable, millisPerFrame);
        }
        previewAnimation.start();
    }

    private void activate(int index) {
        frameGrid.setSelection(index);
        frameAdapter.setCurrentFrameIndex(index);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_preview_collapse:
                float fromDegrees, toDegrees;
                int fromHeight, toHeight;
                if (visible) {
                    visible = false;
                    fromDegrees = 0;
                    toDegrees = 180;
                    previewHeight = view.getMeasuredHeight();
                    fromHeight = previewHeight;
                    toHeight = 20;
                } else {
                    visible = true;
                    fromDegrees = 180;
                    toDegrees = 360;
                    fromHeight = 20;
                    toHeight = previewHeight;
                }

                ValueAnimator hide = new ValueAnimator().ofInt(fromHeight, toHeight);
                hide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        int val = (Integer) valueAnimator.getAnimatedValue();
                        ViewGroup.LayoutParams params = previewLayout.getLayoutParams();
                        params.height = val;
                        previewLayout.setLayoutParams(params);
                    }
                });
                hide.setInterpolator(decelerateInterpolator);
                hide.setDuration(300);
                hide.start();

                ObjectAnimator rotation = new ObjectAnimator().ofFloat(view, "rotation", fromDegrees, toDegrees);
                rotation.setInterpolator(decelerateInterpolator);
                rotation.setDuration(300);
                rotation.start();

                gone.play(hide).with(rotation);
                gone.start();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        currentFrameIndex = position;
        activate(currentFrameIndex);
        frameAdapter.notifyDataSetChanged();
        notifyCurrentFrameChanged();
    }

    public void setFrameListener(FrameListener frameListener) {
        this.frameListener = frameListener;
    }

    @Override
    public void onDeleteFrameFromList(int index) {
        Frame frame = deleteFrame(index);
        pushDeleteItem(index, frame);
        frameAdapter.notifyDataSetChanged();

        updatePreview();
    }


    public interface FrameListener {
        public void onCurrentFrameChange(Frame frame, int frameIndex);
        public Frame requestFrame(boolean duplicate);
    }
}
