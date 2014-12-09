package com.jaween.pixelart.ui.layer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.ui.undo.LayerUndoData;
import com.jaween.pixelart.ui.undo.UndoItem;
import com.jaween.pixelart.ui.undo.UndoManager;
import com.jaween.pixelart.util.ConfigChangeFragment;
import com.mobeta.android.dslv.DragSortListView;

import java.util.LinkedList;

/**
 * Created by ween on 11/27/14.
 */
public class LayerFragment extends Fragment implements
        View.OnClickListener,
        ListView.OnItemClickListener,
        LayerAdapter.LayerListItemListener,
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {

    // ListView data
    private LinkedList<Frame> frames = null;
    private LayerAdapter layerAdapter;

    // Views
    private DragSortListView layerDragSortListView;
    private ImageButton layerAdd;
    private ImageButton layerDuplicate;

    // Layer properties
    private static final int MAX_LAYER_COUNT = 20;
    private int currentFrameIndex = 0;
    private int currentLayerIndex = 0;
    private int layerWidth;
    private int layerHeight;
    private Bitmap.Config config = Bitmap.Config.ARGB_8888;

    // Layer callbacks
    private LayerListener layerListener;

    // Undo management
    private UndoManager undoManager;

    // Fragment save state
    private static final String KEY_SCROLL_INDEX = "key_scroll_index";
    private static final String KEY_SCROLL_OFFSET = "key_scroll_offset";
    private static final String KEY_LAYER_INDEX = "key_layer_index";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SCROLL_INDEX, layerDragSortListView.getFirstVisiblePosition());

        // Offset of top-most visible child
        View item = layerDragSortListView.getChildAt(0);
        int offset;
        if (item != null) {
            offset = item.getTop();
        } else {
            offset = 0;
        }
        outState.putInt(KEY_SCROLL_OFFSET, offset);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // TODO: Perhaps remove scroll pos restore if it's already in the DSLV
            // Restores scroll position
            int scrollIndex = savedInstanceState.getInt(KEY_SCROLL_INDEX, 0);
            int scrollOffset = savedInstanceState.getInt(KEY_SCROLL_OFFSET, 0);
            // TODO: Fix, surely this wasn't added in API 21, it was fine in other lists
            layerDragSortListView.setSelectionFromTop(scrollIndex, scrollOffset);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layer_fragment, null);

        layerDragSortListView = (DragSortListView) view.findViewById(R.id.dslv_layer_list);
        layerAdd = (ImageButton) view.findViewById(R.id.ib_layer_add);
        layerDuplicate = (ImageButton) view.findViewById(R.id.ib_layer_duplicate);

        onRestoreInstanceState(savedInstanceState);


        layerDragSortListView.setOnItemClickListener(this);
        layerDragSortListView.setRemoveListener(this);
        layerDragSortListView.setDropListener(this);
        layerAdd.setOnClickListener(this);
        layerDuplicate.setOnClickListener(this);

        return view;
    }

    // Sets the undo manager for undoing and redoing layer commands
    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    /** Creates and returns an animation Frame without storing it here, used for future frame
     * changes. The composite Bitmap is blank. **/
    public Frame requestFrame(boolean duplicate) {
        LinkedList<Layer> layers = new LinkedList<Layer>();

        Layer layer = createLayer(duplicate);
        layers.add(layer);
        Bitmap compositeBitmap = Bitmap.createBitmap(layerWidth, layerHeight, config);
        int index = 0;
        Frame frame = new Frame(layers, compositeBitmap, index);

        return frame;
    }

    /** Changes the working set of layers, updates the list adapter. **/
    public void setFrames(LinkedList<Frame> frames) {
        this.frames = frames;

        setListAdapter();
    }

    /** Initialises the drawing dimensions. Must be called before requesting a Frame **/
    public void setDimensions(int layerWidth, int layerHeight) {
        this.layerWidth = layerWidth;
        this.layerHeight = layerHeight;
    }

    public void setCurrentFrameIndex(int index) {
        currentFrameIndex = index;
        setListAdapter();
    }

    /*private void setCurrentLayerIndex(int index) {
        currentLayerIndex = index;
    }*/

    public void setListAdapter() {
        // TODO: Is there a better way? (test case, new image > add layer > add frame > switch to that frame : should have only a single layer)
        // New list adapter every time the user changes frames
        layerAdapter = new LayerAdapter(getActivity(), frames.get(currentFrameIndex).getLayers());
        layerAdapter.setLayerListItemListener(this);
        layerDragSortListView.setAdapter(layerAdapter);
    }

    /** Creates and returns a new Layer instance **/
    private Layer createLayer(boolean duplicate) {
        // Creates the bitmap that belongs to the new layer
        Bitmap image;
        if (frames != null) {
            LinkedList<Layer> layers = frames.get(currentFrameIndex).getLayers();
            if (duplicate) {
                image = layers.get(currentLayerIndex).getImage().copy(config, true);
            } else {
                image = Bitmap.createBitmap(layerWidth, layerHeight, config);
            }
        } else {
            image = Bitmap.createBitmap(layerWidth, layerHeight, config);
        }

        // Creates the new layer object
        int size = frames == null ? 0 : frames.get(currentFrameIndex).getLayers().size();
        String title = getString(R.string.layer) + " " + (size + 1);
        return new Layer(image, title);
    }

    /** Creates and adds a new Layer to the data structure, updates the UI accordingly. **/
    private Layer addLayer(int frameIndex, int layerIndex, boolean duplicate) {
        // Creates a new layer and updates the data structure
        LinkedList<Layer> layers = frames.get(frameIndex).getLayers();
        Layer layer = createLayer(duplicate);
        layers.add(layerIndex, layer);

        // If the current layer has moved, points the currentLayerIndex at it's new position
        currentLayerIndex = layerIndex;
        if (layers.size() > 1) {
            notifyCurrentLayerChanged();
        }

        // Updates the UI
        layerAdapter.notifyDataSetChanged();

        // Scrolls to the new item in the list. This is similar to using the 'Transcript mode', but
        // it also can maintain the scroll position on config change
        layerDragSortListView.smoothScrollToPosition(layerIndex);

        Log.d("LayerFragment", "Layer added, size is now " + layers.size() + ", frame index is " + frameIndex);

        return layer;
    }

    /** Deletes and returns the Layer at the given index and updates the UI accordingly **/
    private Layer deleteLayer(int frameIndex, int layerIndex) {
        LinkedList<Layer> layers = frames.get(frameIndex).getLayers();
        Layer currentLayer = layers.get(currentLayerIndex);
        Layer deleteLayer = layers.get(layerIndex);

        // Updates the data structure and animates out the item
        layers.remove(layerIndex);

        // If the current layer has moved, points the currentLayerIndex at it's new position
        if (layerIndex == currentLayerIndex && layerIndex == layers.size()) {
            // Layer deleted was the current layer and it was the bottom most layer
            // New current layer is the one above it
            if (layers.size() > 0) {
                currentLayerIndex--;
                notifyCurrentLayerChanged();
            }
        } else {
            // Finds the position of the new layer
            int newCurrentLayerIndex = layers.indexOf(currentLayer);
            if (newCurrentLayerIndex != -1) {
                if (currentLayerIndex != newCurrentLayerIndex) {
                    currentLayerIndex = newCurrentLayerIndex;
                    notifyCurrentLayerChanged();
                }
            }
        }

        // Updates the UI
        layerAdapter.notifyDataSetChanged();

        return deleteLayer;
    }

    /**
     * Moves a layer in the data set. This does not push/pop the undo stack.
     * @param frameIndex The index of the Frame in which this operation is taking place
     * @param from The source index from which the layer must move
     * @param to The destination index to which the layer must move
     */
    private void moveLayer(int frameIndex, int from, int to) {
        LinkedList<Layer> layers = frames.get(frameIndex).getLayers();
        Layer currentLayer = layers.get(currentLayerIndex);

        Layer movingLayer = layers.remove(from);
        layers.add(to, movingLayer);
        layerAdapter.notifyDataSetChanged();

        int newCurrentLayerIndex = layers.indexOf(currentLayer);
        if (currentLayerIndex != newCurrentLayerIndex) {
            currentLayerIndex = newCurrentLayerIndex;
            notifyCurrentLayerChanged();
        }
    }

    private void pushAddItem(int frameIndex, int layerIndex, Layer layer) {
        LayerUndoData layerUndoData = new LayerUndoData(
                LayerUndoData.LayerOperation.ADD,
                frameIndex,
                layerIndex,
                layer);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 0, layerUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    private void pushDeleteItem(int frameIndex, int layerIndex, Layer layer) {
        LayerUndoData layerUndoData = new LayerUndoData(
                LayerUndoData.LayerOperation.DELETE,
                frameIndex,
                layerIndex,
                layer);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 0, layerUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    /**
     * Pushes an undo item stating that a layer has moved places.
     * @param from The source index from which the layer has already moved
     * @param to The destination index to which the layer has already moved
     */
    private void pushMoveItem(int from, int to, int frameindex) {
        LayerUndoData layerUndoData = new LayerUndoData(from, to, frameindex);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 3, layerUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    public void undo(Object undoData) {
        if (undoData instanceof LayerUndoData) {
            switch (((LayerUndoData) undoData).getType()) {
                case ADD:
                    int frameIndex = ((LayerUndoData) undoData).getFrameIndex();
                    int layerIndex = ((LayerUndoData) undoData).getLayerIndex();
                    deleteLayer(frameIndex, layerIndex);
                    break;
                case DELETE:
                    frameIndex = ((LayerUndoData) undoData).getFrameIndex();
                    layerIndex = ((LayerUndoData) undoData).getLayerIndex();
                    Layer layer = ((LayerUndoData) undoData).getLayer();
                    LinkedList<Layer> layers = frames.get(frameIndex).getLayers();
                    layers.add(layerIndex, layer);
                    layerAdapter.notifyDataSetChanged();
                    notifyCurrentLayerChanged();
                    break;
                case MOVE:
                    frameIndex = ((LayerUndoData) undoData).getFrameIndex();
                    int fromIndex = ((LayerUndoData) undoData).getFromIndex();
                    int toIndex = ((LayerUndoData) undoData).getToIndex();
                    moveLayer(frameIndex, toIndex, fromIndex);
                    break;
                case MERGE:
                    //TODO: Undo merge layers
                    break;
            }
        } else {
            String className = "Null";

            if (undoData != null) {
                className = undoData.getClass().getName();
            }
            Log.e("DrawingSurface", "Undo data wasn't of type DrawOpUndoData, it was of type " + className);
        }
    }

    public void redo(Object redoData) {
        if (redoData instanceof LayerUndoData) {
            switch (((LayerUndoData) redoData).getType()) {
                case ADD:
                    int frameIndex = ((LayerUndoData) redoData).getFrameIndex();
                    int layerIndex = ((LayerUndoData) redoData).getLayerIndex();
                    Layer layer = ((LayerUndoData) redoData).getLayer();
                    LinkedList<Layer> layers = frames.get(frameIndex).getLayers();
                    layers.add(layerIndex, layer);
                    layerAdapter.notifyDataSetChanged();
                    break;
                case DELETE:
                    frameIndex = ((LayerUndoData) redoData).getFrameIndex();
                    layerIndex = ((LayerUndoData) redoData).getLayerIndex();
                    deleteLayer(frameIndex, layerIndex);
                    break;
                case MOVE:
                    frameIndex = ((LayerUndoData) redoData).getFrameIndex();
                    int fromIndex = ((LayerUndoData) redoData).getFromIndex();
                    int toIndex = ((LayerUndoData) redoData).getToIndex();
                    moveLayer(frameIndex, fromIndex, toIndex);
                    break;
                case MERGE:
                    //TODO: Redo merge layers
                    break;
            }
        } else {
            String className = "Null";

            if (redoData != null) {
                className = redoData.getClass().getName();
            }
            Log.e("DrawingSurface", "Redo data wasn't of type DrawOpUndoData, it was of type " + className);
        }
    }

    public void invalidate() {
        layerAdapter.notifyDataSetChanged();
    }

    /** Notifies the SurfaceView to perform future draw operations on the new current layer **/
    public void notifyCurrentLayerChanged() {
        if (layerListener != null) {
            layerListener.onCurrentLayerChange(currentLayerIndex);
        }
        activate(currentLayerIndex);
    }

    @Override
    public void onClick(View view) {
        LinkedList<Layer> layers = frames.get(currentFrameIndex).getLayers();
        switch (view.getId()) {
            case R.id.ib_layer_add:
                if (layers.size() < MAX_LAYER_COUNT) {
                    int frameIndex = currentFrameIndex;
                    int layerIndex = currentLayerIndex;
                    boolean duplicate = false;
                    Layer layer = addLayer(frameIndex, layerIndex, duplicate);
                    pushAddItem(frameIndex, layerIndex, layer);
                }
                break;
            case R.id.ib_layer_duplicate:
                if (layers.size() < MAX_LAYER_COUNT) {
                    int frameIndex = currentFrameIndex;
                    int layerIndex = currentLayerIndex;
                    boolean duplicate = true;
                    Layer layer = addLayer(frameIndex, layerIndex, duplicate);
                    pushAddItem(frameIndex, layerIndex, layer);
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        currentLayerIndex = i;
        activate(currentLayerIndex);
        layerAdapter.notifyDataSetChanged();
        notifyCurrentLayerChanged();
    }

    public void setLayerListener(LayerListener layerListener) {
        this.layerListener = layerListener;
    }

    @Override
    public void onDeleteLayerFromList(int index) {
        layerDragSortListView.removeItem(index);
    }

    @Override
    public void drop(int from, int to) {
        moveLayer(currentFrameIndex, from, to);
        pushMoveItem(from, to, currentFrameIndex);
    }

    @Override
    public void remove(int layerIndex) {
        Layer layer = deleteLayer(currentFrameIndex, layerIndex);
        pushDeleteItem(currentFrameIndex, layerIndex, layer);
        notifyCurrentLayerChanged();
    }

    private void activate(int layerIndex) {
        layerDragSortListView.setSelection(layerIndex);
        layerAdapter.setCurrentLayerIndex(layerIndex);
    }

    public interface LayerListener {
        public void onCurrentLayerChange(int index);
        public void onMergeLayer(int index);
    }
}
