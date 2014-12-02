package com.jaween.pixelart.ui.layer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jaween.pixelart.R;

import java.util.LinkedList;

/**
 * Created by ween on 11/27/14.
 */
public class LayerAdapter extends BaseAdapter implements View.OnClickListener {

    private LinkedList<Layer> data;
    private LayoutInflater inflater;

    // Layer item drawables
    private Drawable layerVisible;
    private Drawable layerInvisible;
    private Drawable layerUnlocked;
    private Drawable layerLocked;

    // Transparency checkerboard
    private BitmapDrawable checkerboardTile;

    // Layer deletion callback
    private LayerListItemListener layerListItemListener = null;

    public LayerAdapter(Context context, LinkedList<Layer> data) {
        inflater = LayoutInflater.from(context);

        // Loads drawable resources here to avoid keeping the Context around (memory leak)
        layerVisible = context.getResources().getDrawable(R.drawable.ic_action_layer_visible);
        layerInvisible = context.getResources().getDrawable(R.drawable.tool_oval);
        layerUnlocked = context.getResources().getDrawable(R.drawable.tool_pen);
        layerLocked = context.getResources().getDrawable(R.drawable.tool_oval);

        // Transparency checkerboard background
        Bitmap checkerboardBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.checkerboard);
        checkerboardTile = new BitmapDrawable(context.getResources(), checkerboardBitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        // Gets the recycled view (convertView) or creates a new one
        ViewHolder holder;
        if (convertView == null) {
            // Inflates convertView with item layout
            convertView = inflater.inflate(R.layout.layer_item, null);

            // Links to XML
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.tv_layer_title);
            holder.handle = (ImageView) convertView.findViewById(R.id.iv_layer_handle);
            holder.image = (ImageView) convertView.findViewById(R.id.iv_layer_image);
            holder.visibility = (ImageView) convertView.findViewById(R.id.iv_layer_visibility);
            holder.lock = (ImageView) convertView.findViewById(R.id.iv_layer_lock);
            holder.delete = (ImageView) convertView.findViewById(R.id.iv_layer_delete);

            holder.visibility.setOnClickListener(this);
            holder.lock.setOnClickListener(this);
            holder.delete.setOnClickListener(this);

            // Saves the holder and it's views for reuse
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        initialiseViews(holder, position);

        return convertView;
    }

    private void initialiseViews(ViewHolder holder, int position) {
        holder.title.setText(data.get(position).getTitile());
        holder.image.setBackgroundDrawable(checkerboardTile);
        holder.image.setImageBitmap(data.get(position).getImage());

        if (data.get(position).isVisible()) {
            holder.visibility.setImageDrawable(layerVisible);
        } else {
            holder.visibility.setImageDrawable(layerInvisible);
        }

        if (data.get(position).isLocked()) {
            holder.lock.setImageDrawable(layerLocked);
        } else {
            holder.lock.setImageDrawable(layerUnlocked);
        }

        // Disables layer deletion when there's only a single layer
        if (data.size() == 1) {
            holder.delete.setEnabled(false);
        } else {
            holder.delete.setEnabled(true);
        }

        // Stores this view's position in the list within the View tag (for use with onClick())
        holder.visibility.setTag(position);
        holder.lock.setTag(position);
        holder.delete.setTag(position);
    }

    @Override
    public void onClick(View view) {
        // Determines which row this view belongs to
        int position;
        if (view.getTag() != null) {
            position = (Integer) view.getTag();
        } else {
            return;
        }

        // TODO: Modifications to layer properties (such as visibility) aren't restored on undo/redo
        switch (view.getId()) {
            case R.id.iv_layer_visibility:
                data.get(position).setVisible(!data.get(position).isVisible());
                break;
            case R.id.iv_layer_lock:
                data.get(position).setLocked(!data.get(position).isLocked());
                break;
            case R.id.iv_layer_delete:
                if (data.size() > 1) {
                    // Deletion occurs in the fragment to avoid accidentally deleting two items,
                    // once from this adapter and once from LayerFragment.deleteLayer()
                    if (layerListItemListener != null) {
                        layerListItemListener.onDeleteLayerFromList(position);
                    }
                }
                break;
        }

        notifyDataSetChanged();
    }

    static class ViewHolder {
        TextView title;
        ImageView handle;
        ImageView image;
        ImageView visibility;
        ImageView lock;
        ImageView delete;
    }

    public void setLayerListItemListener(LayerListItemListener layerListItemListener) {
        this.layerListItemListener = layerListItemListener;
    }

    public interface LayerListItemListener {
        public void onDeleteLayerFromList(int i);
    }
}
