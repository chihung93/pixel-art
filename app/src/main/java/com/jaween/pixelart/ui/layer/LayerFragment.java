package com.jaween.pixelart.ui.layer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.jaween.pixelart.R;

import java.util.LinkedList;

/**
 * Created by ween on 11/27/14.
 */
public class LayerFragment extends Fragment implements
        View.OnClickListener,
        ListView.OnItemClickListener,
        LayerAdapter.LayerListener {

    // ListView data
    private LinkedList<Layer> layers = new LinkedList<Layer>();
    private LayerAdapter layerAdapter;

    // Views
    private ListView layerListView;
    private ImageButton layerAdd;
    private ImageButton layerDuplicate;

    // Layer properties
    private static final int MAX_LAYER_COUNT = 20;
    private int currentLayer = 0;

    // Layer callbacks
    private LayerListener layerListener;

    // Fragment save state
    private static final String KEY_LAYER_COUNT = "key_layer_count";
    private static final String KEY_SCROLL_INDEX = "key_scroll_index";
    private static final String KEY_SCROLL_OFFSET = "key_scroll_offset";
    private boolean stateRestored = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layerAdapter = new LayerAdapter(getActivity(), layers);
        layerAdapter.setLayerListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LAYER_COUNT, layers.size());
        outState.putInt(KEY_SCROLL_INDEX, layerListView.getFirstVisiblePosition());

        // Offset of topmost visible child
        View item = layerListView.getChildAt(0);
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
            int layerCount = savedInstanceState.getInt(KEY_LAYER_COUNT, 0);
            if (layerCount > 0) {
                stateRestored = true;
            }

            // TODO: Restore the state of layers (title, visibility, etc),
            // Layer bitmaps are restored separately in setInitialLayers() as they are large objects
            for (int i = 0; i < layerCount; i++) {
                String title = "FAKE RESTORED " + getString(R.string.layer) + " " + (i + 1);
                Layer layer = new Layer(null, title);
                layers.addFirst(layer);
            }
            layerAdapter.notifyDataSetChanged();

            // List scroll
            int index = savedInstanceState.getInt(KEY_SCROLL_INDEX, 0);
            int offset = savedInstanceState.getInt(KEY_SCROLL_OFFSET, 0);
            layerListView.setSelectionFromTop(index, offset);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layer_fragment, null);

        layerListView = (ListView) view.findViewById(R.id.lv_layer_list);
        layerAdd = (ImageButton) view.findViewById(R.id.ib_layer_add);
        layerDuplicate = (ImageButton) view.findViewById(R.id.ib_layer_duplicate);

        layerListView.setAdapter(layerAdapter);
        layerListView.setOnItemClickListener(this);
        layerAdd.setOnClickListener(this);
        layerDuplicate.setOnClickListener(this);

        onRestoreInstanceState(savedInstanceState);

        return view;
    }

    public void setInitialLayers(LinkedList<Bitmap> layers) {
        // Sets the bitmaps of the restored Layer objects
        for (int i = 0; i < layers.size(); i++) {
            if (stateRestored) {
                this.layers.get(i).setImage(layers.get(i));
            } else {
                // Initial startup, must create the layer titles
                String title = "FAKE " + getString(R.string.layer) + " " + (i + 1);
                Layer layer = new Layer(layers.get(i), title);
                this.layers.add(layer);
            }
        }

        layerAdapter.notifyDataSetChanged();

        // Notifies the DrawingSurface of the visible and locked layers
        onLayerStateChange();
    }

    private void addLayer(Bitmap image) {
        // Updates the UI
        String title = getString(R.string.layer) + " " + (layers.size() + 1);
        Layer layer = new Layer(image, title);
        layers.addFirst(layer);
        layerAdapter.notifyDataSetChanged();

        // This scrolls to the of the list on item add similar to using the Transcript mode, but it
        // also can maintain the scroll position on config change
        layerListView.smoothScrollToPosition(0);
    }

    public void setCurrentLayer(int currentLayer) {
        this.currentLayer = currentLayer;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ib_layer_add:
                if (layerListener != null) {
                    if (layers.size() < MAX_LAYER_COUNT) {
                        Bitmap newLayer = layerListener.onAddLayer(false);
                        addLayer(newLayer);
                    }
                }
                break;
            case R.id.ib_layer_duplicate:
                if (layerListener != null) {
                    if (layers.size() < MAX_LAYER_COUNT) {
                        Bitmap newLayer = layerListener.onAddLayer(true);
                        addLayer(newLayer);
                    }
                }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        switch (view.getId()) {
            case R.id.rl_layer_item:
                // TODO: Proper layer selection (currently uses a selector drawable and deselects on layer add)
                layerListView.setItemChecked(currentLayer, false);
                layerListView.setItemChecked(i, true);
                currentLayer = i;
                layerListener.onCurrentLayerChange(currentLayer);
        }
        layerAdapter.notifyDataSetChanged();
    }

    public void setLayerListener(LayerListener layerListener) {
        this.layerListener = layerListener;
    }

    @Override
    public void onLayerStateChange() {
        if (layerListener != null) {
            layerListener.onLayerStateChange(layers);
        }
    }

    @Override
    public void onDeleteLayer(int i) {
        if (layerListener != null) {
            layerListener.onDeleteLayer(i);
        }
    }

    public interface LayerListener {
        public void onCurrentLayerChange(int i);
        public void onLayerStateChange(LinkedList<Layer> layerItems);
        public Bitmap onAddLayer(boolean duplicate);
        public void onDeleteLayer(int i);
        public void onMergeLayer(int i);
    }
}
