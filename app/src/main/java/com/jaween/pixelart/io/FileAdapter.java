package com.jaween.pixelart.io;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jaween.pixelart.R;

import java.util.ArrayList;

/**
 * Created by ween on 12/9/14.
 */
public class FileAdapter extends BaseAdapter implements View.OnClickListener {

    private ArrayList<AnimationFile> data;
    private LayoutInflater inflater;
    private BitmapDrawable checkerboardTile;
    private int selectedIndex = 0;
    private FileItemListener fileItemListener;

    public FileAdapter(Context context, ArrayList<AnimationFile> data, BitmapDrawable checkerboardTile) {
        this.data = data;
        this.checkerboardTile = checkerboardTile;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        // Adds one to size for the 'fileAdd file' item
        return data.size() + 1;
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.file_item, null);

            holder = new ViewHolder();
            holder.drawing = (ImageView) convertView.findViewById(R.id.iv_drawing);
            holder.delete = (ImageView) convertView.findViewById(R.id.iv_delete);
            holder.filename = (TextView) convertView.findViewById(R.id.tv_filename);
            holder.fileContent = (LinearLayout) convertView.findViewById(R.id.ll_file_content);
            holder.fileAdd = (LinearLayout) convertView.findViewById(R.id.ll_file_add);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position == data.size()) {
            // Last item in the list is the add button
            holder.fileContent.setVisibility(View.GONE);
            holder.fileAdd.setVisibility(View.VISIBLE);
            convertView.setBackgroundResource(0);
            return convertView;
        } else {
            // Other items in the list are regular items
            holder.fileContent.setVisibility(View.VISIBLE);
            holder.fileAdd.setVisibility(View.GONE);

            // Initialises views
            holder.drawing.setBackgroundDrawable(checkerboardTile);
            holder.drawing.setImageBitmap(data.get(position).getFrames().get(0).getLayers().get(0).getImage());
            holder.filename.setText(data.get(position).getFilename());

            holder.delete.setOnClickListener(this);

            // Highlights the selected item
            if (position == selectedIndex) {
                convertView.setBackgroundResource(R.color.list_item_selected_colour);
            } else {
                convertView.setBackgroundResource(0);
            }

            // Pre-Jellybean doesn't have setBackground()
            int sdk = Build.VERSION.SDK_INT;
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                holder.drawing.setBackgroundDrawable(checkerboardTile);
            } else {
                holder.drawing.setBackground(checkerboardTile);
            }

            // Stores this view's position in the list within the View tag (for use with onClick())
            holder.delete.setTag(position);
        }

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

        switch (view.getId()) {
            case R.id.iv_delete:
                if (data.size() > 1) {
                    if (fileItemListener != null) {
                        fileItemListener.onDeleteFileFromList(position);
                    }
                }
                break;
        }
    }

    private class ViewHolder {
        ImageView drawing;
        ImageView delete;
        TextView filename;
        LinearLayout fileContent;
        LinearLayout fileAdd;
    }

    public void setFileItemListener(FileItemListener fileItemListener) {
        this.fileItemListener = fileItemListener;
    }

    public interface FileItemListener {
        public void onDeleteFileFromList(int index);
    }
}
