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

import java.util.LinkedList;

/**
 * Created by ween on 11/27/14.
 */
public class LayerFragment extends Fragment implements
        View.OnClickListener,
        ListView.OnItemClickListener,
        LayerAdapter.LayerListItemListener {

    // ListView data
    private LinkedList<Layer> layers = null;
    private LayerAdapter layerAdapter;

    // Views
    private ListView layerListView;
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
    private static final String KEY_LAYER_COUNT = "key_layer_count";
    private static final String KEY_SCROLL_INDEX = "key_scroll_index";
    private static final String KEY_SCROLL_OFFSET = "key_scroll_offset";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LAYER_COUNT, layers.size());
        outState.putInt(KEY_SCROLL_INDEX, layerListView.getFirstVisiblePosition());

        // Offset of top-most visible child
        View item = layerListView.getChildAt(0);
        int offset;
        if (item != null) {
            offset = item.getTop();
        } else {
            offset = 0;
        }
        outState.putInt(KEY_SCROLL_OFFSET, offset);

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

        if (layerListener != null) {
            layerListener.onLayersInitialised(layers);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layer_fragment, null);

        layerListView = (ListView) view.findViewById(R.id.lv_layer_list);
        layerAdd = (ImageButton) view.findViewById(R.id.ib_layer_add);
        layerDuplicate = (ImageButton) view.findViewById(R.id.ib_layer_duplicate);

        onRestoreInstanceState(savedInstanceState);

        layerAdapter.setLayerListItemListener(this);
        layerListView.setAdapter(layerAdapter);
        layerListView.setOnItemClickListener(this);
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

        // Pushes the item onto the undo stack if it's not the initial layer (must always have at least one layer)
        if (layers.size() > 0) {
            LayerUndoData layerUndoData = new LayerUndoData(LayerUndoData.LayerOperation.ADD, index, layer);
            UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 0, layerUndoData);
            undoManager.pushUndoItem(undoItem);
        }

        // Updates the data structure and the UI
        layers.add(index, layer);
        layerAdapter.notifyDataSetChanged();

        // Scrolls to the top of the layer list. This is similar to using the 'Transcript mode', but
        // it also can maintain the scroll position on config change
        layerListView.smoothScrollToPosition(0);

        return layer;
    }

    private void deleteLayer(int index) {
        Layer layer = layers.get(index);

        // Adds the item to the undo stack
        LayerUndoData layerUndoData = new LayerUndoData(LayerUndoData.LayerOperation.DELETE, index, layer);
        UndoItem undoItem = new UndoItem(UndoItem.Type.LAYER, 0, layerUndoData);
        undoManager.pushUndoItem(undoItem);

        // Updates the data structure and the UI
        layers.remove(layer);
        layerAdapter.notifyDataSetChanged();
    }

    public LinkedList<Layer> getLayers() {
        return layers;
    }

    public void undo(Object undoData) {
        if (undoData instanceof LayerUndoData) {
            switch (((LayerUndoData) undoData).getType()) {
                case ADD:
                    // TODO Compress bitmap here
                    int layerIndex = ((LayerUndoData) undoData).getLayerIndex();
                    layers.remove(layerIndex);
                    layerAdapter.notifyDataSetChanged();
                    break;
                case DELETE:
                    // TODO: Decompress bitmap here
                    layerIndex = ((LayerUndoData) undoData).getLayerIndex();
                    Layer layer = ((LayerUndoData) undoData).getLayer();
                    layers.add(layerIndex, layer);
                    layerAdapter.notifyDataSetChanged();
                    break;
                case MOVE:
                    //TODO: Undo move and merge layers
                    break;
                case MERGE:
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
                    layerAdapter.notifyDataSetChanged();
                    break;
                case DELETE:
                    layerIndex = ((LayerUndoData) redoData).getLayerIndex();
                    layers.remove(layerIndex);
                    layerAdapter.notifyDataSetChanged();
                    break;
                case MOVE:
                    //TODO: Redo move and merge layers
                    break;
                case MERGE:
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ib_layer_add:
                if (layers.size() < MAX_LAYER_COUNT) {
                    int index = 0;
                    boolean duplicate = false;
                    addLayer(index, duplicate);
                }
                break;
            case R.id.ib_layer_duplicate:
                if (layers.size() < MAX_LAYER_COUNT) {
                    int index = currentLayerIndex;
                    boolean duplicate = true;
                    addLayer(index, duplicate);
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        switch (view.getId()) {
            case R.id.rl_layer_item:
                // TODO: Proper layer selection (currently uses a selector drawable and deselects on layer add)
                layerListView.setItemChecked(currentLayerIndex, false);
                layerListView.setItemChecked(i, true);
                currentLayerIndex = i;

                // Notifies the SurfaceView to perform future draw operations on the new current layer
                layerListener.onCurrentLayerChange(currentLayerIndex);
        }
        layerAdapter.notifyDataSetChanged();
    }

    public void setLayerListener(LayerListener layerListener) {
        this.layerListener = layerListener;
    }

    @Override
    public void onDeleteLayerFromList(int index) {
        deleteLayer(index);
    }

    public interface LayerListener {
        public void onLayersInitialised(LinkedList<Layer> layers);
        public void onCurrentLayerChange(int index);
        public void onMergeLayer(int index);
    }
}
