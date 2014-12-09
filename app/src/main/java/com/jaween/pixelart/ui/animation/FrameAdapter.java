package com.jaween.pixelart.ui.animation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.jaween.pixelart.R;

import java.util.LinkedList;

/**
 * Created by ween on 12/6/14.
 */
public class FrameAdapter extends BaseAdapter implements View.OnClickListener {

    private LinkedList<Frame> data;
    private LayoutInflater inflater;

    private BitmapDrawable checkerboardTile;

    private int currentFrameIndex = 0;

    private FrameListItemListener frameListItemListener = null;

    public FrameAdapter(Context context, LinkedList<Frame> data, BitmapDrawable checkerboardTile) {
        inflater = LayoutInflater.from(context);
        this.data = data;
        this.checkerboardTile = checkerboardTile;
    }

    public void setCurrentFrameIndex(int currentFrameIndex) {
        this.currentFrameIndex = currentFrameIndex;
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.frame_item, null);

            holder = new ViewHolder();
            holder.frameImage = (ImageView) convertView.findViewById(R.id.iv_frame_image);
            //holder.frameDragHandle = (ImageView) recycledView.findViewById(R.id.iv_frame_drag_handle);
            holder.frameDelete = (ImageView) convertView.findViewById(R.id.iv_frame_delete);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Initialises views
        holder.frameImage.setBackgroundDrawable(checkerboardTile);
        holder.frameImage.setImageBitmap(data.get(position).getCompositeBitmap());

        // Highlights the selected frame
        if (position == currentFrameIndex) {
            convertView.setBackgroundResource(R.color.highlight);
        } else {
            convertView.setBackgroundResource(0);
        }

        // Pre-Jellybean doesn't have setBackground()
        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            holder.frameImage.setBackgroundDrawable(checkerboardTile);
        } else {
            holder.frameImage.setBackground(checkerboardTile);
        }

        holder.frameDelete.setOnClickListener(this);

        // Stores this view's position in the list within the View tag (for use with onClick())
        holder.frameDelete.setTag(position);

        return convertView;
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

        switch(view.getId()) {
            case R.id.iv_frame_delete:
                if (data.size() > 1) {
                    // Deletion occurs in the Fragment to avoid accidentally deleting two items,
                    // once from this adapter and once from AnimationFragment.deleteFrame()
                    if (frameListItemListener != null) {
                        frameListItemListener.onDeleteFrameFromList(position);
                    }
                }
        }
    }

    private class ViewHolder {
        ImageView frameImage;
        //ImageView frameDragHandle;
        ImageView frameDelete;
    }


    public void setFrameListItemListener(FrameListItemListener frameListItemListener) {
        this.frameListItemListener = frameListItemListener;
    }

    public interface FrameListItemListener {
        public void onDeleteFrameFromList(int i);
    }
}
