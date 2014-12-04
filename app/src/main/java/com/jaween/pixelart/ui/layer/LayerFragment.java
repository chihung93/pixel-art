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
    private LinkedList<Layer> layers = null;
    private LayerAdapter layerAdapter;

    // Views
    private DragSortListView layerDragSortListView;
    private ImageButton layerAdd;
    private ImageButton layerDuplicate;

    // Layer properties
    private static final int MAX_LAYER_COUNT = 20;
    private int currentLayerIndex = 0;
    private int layerWidth = 128;
    private int layerHeight = 128;
    private Bitmap.Config config = Bitmap.Config.ARGB_8888;

    // Layer callbacks
    private LayerListener layerListener;

    // Undo management
    private UndoManager undoManager;

    // Fragment save state
    private ConfigChangeFragment configChangeWorker;
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

        // Current layer index
        outState.putInt(KEY_LAYER_INDEX, currentLayerIndex);

        // To save the large layers object, we store it in the retained worker Fragment
        configChangeWorker.setLayers(layers);
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        // Worker fragment to save data across device configuration changes
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        configChangeWorker = (ConfigChangeFragment) fragmentManager.
                findFragmentByTag(ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);

        if (configChangeWorker == null) {
            // Worker doesn't exist, creates new worker
            configChangeWorker = new ConfigChangeFragment();
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(configChangeWorker, ConfigChangeFragment.TAG_CONFIG_CHANGE_FRAGMENT);
            fragmentTransaction.commit();
        } else {
            // TODO: Determine if this is a legitimate way of both the DrawingSurface and LayerFragment referencing the layers
            // Restores the layers
            layers = configChangeWorker.getLayers();
            //configChangeWorker.setLayers(null);

            // Sets the adapter now that we a non-null list
            layerAdapter = new LayerAdapter(getActivity(), layers);
        }

        // No layers to be restored, creates the initial layer
        if (layers == null) {
            // Creates the list and sets the adapter now that it's no longer null
            layers = new LinkedList<Layer>();
            layerAdapter = new LayerAdapter(getActivity(), layers);

            // Adds the initial layer
            int index = 0;
            boolean duplicate = false;
            addLayer(index, duplicate);
        }

        if (savedInstanceState != null) {
            // TODO: Perhaps remove scroll pos restore if it's already in the DSLV
            // Restores scroll position
            int scrollIndex = savedInstanceState.getInt(KEY_SCROLL_INDEX, 0);
            int scrollOffset = savedInstanceState.getInt(KEY_SCROLL_OFFSET, 0);
            // TODO: Fix, surely this wasn't added in API 21, it was fine in other lists
            layerDragSortListView.setSelectionFromTop(scrollIndex, scrollOffset);

            // Restores selected item
            currentLayerIndex = savedInstanceState.getInt(KEY_LAYER_INDEX, 0);
            layerAdapter.setCurrentLayerIndex(currentLayerIndex);
        }

        if (layerListener != null) {
            layerListener.onLayersInitialised(layers);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layer_fragment, null);

        layerDragSortListView = (DragSortListView) view.findViewById(R.id.dslv_layer_list);
        layerAdd = (ImageButton) view.findViewById(R.id.ib_layer_add);
        layerDuplicate = (ImageButton) view.findViewById(R.id.ib_layer_duplicate);

        onRestoreInstanceState(savedInstanceState);

        layerAdapter.setLayerListItemListener(this);
        layerDragSortListView.setAdapter(layerAdapter);
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

    private Layer addLayer(int index, boolean duplicate) {
        // Creates the bitmap that belongs to the new layer
        Bitmap image;
        if (duplicate) {
            image = layers.get(currentLayerIndex).getImage().copy(config, true);
        } else {
            image = Bitmap.createBitmap(layerWidth, layerHeight, config);
        }

        // Creates the new layer object
        String title = getString(R.string.layer) + " " + (layers.size() + 1);
        Layer layer = new Layer(image, title);

        // Updates the data structure
        layers.add(index, layer);

        // If the current layer has moved, points the currentLayerIndex at it's new position
        currentLayerIndex = index;
        if (layers.size() > 1) {
            notifyCurrentLayerChanged();
        }

        // Updates the UI
        layerAdapter.notifyDataSetChanged();

        // Scrolls to the new item in the list. This is similar to using the 'Transcript mode', but
        // it also can maintain the scroll position on config change
        layerDragSortListView.smoothScrollToPosition(index);

        return layer;
    }

    private Layer deleteLayer(int index) {
        Layer currentLayer = layers.get(currentLayerIndex);
        Layer layer = layers.get(index);

        // Updates the data structure and animates out the item
        layers.remove(index);

        // If the current layer has moved, points the currentLayerIndex at it's new position
        if (index == currentLayerIndex && index == layers.size()) {
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

        return layer;
    }

    /**
     * Moves a layer in the data set. This does not push/pop the undo stack.
     * @param from The source index from which the layer must move
     * @param to The destination index to which the layer must move
     */
    private void moveLayer(int from, int to) {
        Layer currentLayer = layers.get(currentLayerIndex);

        Layer layer = layers.remove(from);
        layers.add(to, layer);
        layerAdapter.notifyDataSetChanged();

        int newCurrentLayerIndex = layers.indexOf(currentLayer);
        if (currentLayerIndex != newCurrentLayerIndex) {
            currentLayerIndex = newCurrentLayerIndex;
            notifyCurrentLayerChanged();
        }
    }

    private void pushAddItem(int index, Layer layer) {
        LayerUndoData layerUndoData = new LayerUndoData(LayerUndoData.LayerOperation.ADD, index, layer);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 0, layerUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    private void pushDeleteItem(int index, Layer layer) {
        LayerUndoData layerUndoData = new LayerUndoData(LayerUndoData.LayerOperation.DELETE, index, layer);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 0, layerUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    /**
     * Pushes an undo item stating that a layer has moved places.
     * @param from The source index from which the layer has already moved
     * @param to The destination index to which the layer has already moved
     */
    private void pushMoveItem(int from, int to) {
        LayerUndoData layerUndoData = new LayerUndoData(from, to);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 3, layerUndoData);
        undoManager.pushUndoItem(undoItem);
    }

    public void undo(Object undoData) {
        if (undoData instanceof LayerUndoData) {
            switch (((LayerUndoData) undoData).getType()) {
                case ADD:
                    int layerIndex = ((LayerUndoData) undoData).getLayerIndex();
                    deleteLayer(layerIndex);
                    break;
                case DELETE:
                    layerIndex = ((LayerUndoData) undoData).getLayerIndex();
                    Layer layer = ((LayerUndoData) undoData).getLayer();
                    layers.add(layerIndex, layer);
                    layerAdapter.notifyDataSetChanged();
                    notifyCurrentLayerChanged();
                    break;
                case MOVE:
                    int fromIndex = ((LayerUndoData) undoData).getFromIndex();
                    int toIndex = ((LayerUndoData) undoData).getToIndex();
                    moveLayer(toIndex, fromIndex);
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
                    int layerIndex = ((LayerUndoData) redoData).getLayerIndex();
                    Layer layer = ((LayerUndoData) redoData).getLayer();
                    layers.add(layerIndex, layer);
                    break;
                case DELETE:
                    layerIndex = ((LayerUndoData) redoData).getLayerIndex();
                    deleteLayer(layerIndex);
                    break;
                case MOVE:
                    int fromIndex = ((LayerUndoData) redoData).getFromIndex();
                    int toIndex = ((LayerUndoData) redoData).getToIndex();
                    moveLayer(fromIndex, toIndex);
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
        switch (view.getId()) {
            case R.id.ib_layer_add:
                if (layers.size() < MAX_LAYER_COUNT) {
                    int index = currentLayerIndex;
                    boolean duplicate = false;
                    Layer layer = addLayer(index, duplicate);
                    pushAddItem(index, layer);
                }
                break;
            case R.id.ib_layer_duplicate:
                if (layers.size() < MAX_LAYER_COUNT) {
                    int index = currentLayerIndex;
                    boolean duplicate = true;
                    Layer layer = addLayer(index, duplicate);
                    pushAddItem(index, layer);
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
        moveLayer(from, to);
        pushMoveItem(from, to);
    }

    @Override
    public void remove(int which) {
        Layer layer = deleteLayer(which);
        pushDeleteItem(which, layer);
        notifyCurrentLayerChanged();
    }

    private void activate(int index) {
        layerDragSortListView.setSelection(index);
        layerAdapter.setCurrentLayerIndex(index);
    }

    public interface LayerListener {
        public void onLayersInitialised(LinkedList<Layer> layers);
        public void onCurrentLayerChange(int index);
        public void onMergeLayer(int index);
    }
}
