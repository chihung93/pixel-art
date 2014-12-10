package com.jaween.pixelart.io;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.ui.animation.Frame;
import com.jaween.pixelart.util.PreferenceManager;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by ween on 12/9/14.
 */
public class LoadFileDialog extends DialogFragment implements
        AdapterView.OnItemClickListener, FileAdapter.FileItemListener {

    private GridView fileGrid;
    private FileAdapter fileAdapter;

    private ArrayList<AnimationFile> files;
    private LoadFileDialogListener loadFileDialogListener;

    private String selectedFilename = null;
    private int selectedPosition = 0;

    private PreferenceManager preferenceManager;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        preferenceManager = new PreferenceManager(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // TODO: Load files only when opened (currently loads each config change)
        files = ImportExport.load(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.load_dialog, null);
        initialiseViews(view);

        builder.setTitle(getString(R.string.text_load_dialog_title));
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AnimationFile file = files.get(selectedPosition);
                String filename = file.getFilename();
                loadFileDialogListener.onDismiss(filename, file);
            }
        });

        return builder.create();
    }

    private void initialiseViews(View v) {
        fileGrid = (GridView) v.findViewById(R.id.gv_file_grid);

        // Transparency checkerboard background
        Resources resources = getResources();
        Bitmap checkerboardBitmap = BitmapFactory.decodeResource(resources, R.drawable.checkerboard);
        BitmapDrawable checkerboardTile = new BitmapDrawable(resources, checkerboardBitmap);
        checkerboardTile.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        fileAdapter = new FileAdapter(getActivity(), files, checkerboardTile);
        fileAdapter.setFileItemListener(this);
        fileGrid.setAdapter(fileAdapter);
        fileGrid.setOnItemClickListener(this);

        if (selectedFilename != null) {
            for (int i = 0; i < files.size(); i++) {
                if (files.get(i).getFilename().equals(selectedFilename)) {
                    activate(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            loadFileDialogListener = (LoadFileDialogListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().toString()
                    + " must implement LoadFileDialogListener");
        }
    }

    public void setCurrentFilename(String filename) {
        this.selectedFilename = filename;
    }

    private void delete(int deleteIndex) {
        boolean success = ImportExport.delete(getActivity(), files.get(deleteIndex).getFilename());
        if (success) {
            AnimationFile currentFile = files.get(selectedPosition);

            // Updates the data structure
            files.remove(deleteIndex);

            // If the current file has moved, points the selectedPosition to its new position
            if (selectedPosition == deleteIndex && selectedPosition == files.size()) {
                // File deleted was the current file and it was the final file
                // New current file is the one above it
                if (files.size() > 0) {
                    selectedPosition--;
                }
            } else {
                // Finds the position of the new frame
                int newSelectedPosition = files.indexOf(currentFile);
                if (newSelectedPosition != -1) {
                    if (selectedPosition != newSelectedPosition) {
                        selectedPosition = newSelectedPosition;
                    }
                }
            }
            activate(selectedPosition);
        } else {
            Log.e("LoadFileDialog", files.get(deleteIndex).getFilename() + " could not be deleted");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        // TODO: Commit File creations and deletions on dialog dismissal
        if (position == files.size()) {
            AnimationFile animationFile = createAnimationFile();
            Bitmap bitmap = animationFile.getFrames().get(0).getLayers().get(0).getImage();
            String filename = animationFile.getFilename();
            ImportExport.save(getActivity(), bitmap, filename);
            files.add(animationFile);
            fileAdapter.notifyDataSetChanged();
        } else {
            activate(position);
        }
    }

    private AnimationFile createAnimationFile() {
        Frame newFrame = loadFileDialogListener.requestFrame();
        LinkedList<Frame> frames = new LinkedList<Frame>();
        frames.add(newFrame);
        String filename = "Drawing " + preferenceManager.getFileCount();
        AnimationFile animationFile = new AnimationFile(filename, frames);
        return animationFile;
    }

    private void activate(int index) {
        fileAdapter.setSelectedIndex(index);
        fileAdapter.notifyDataSetChanged();
        selectedPosition = index;
    }

    @Override
    public void onDeleteFileFromList(int index) {
        delete(index);
    }

    public interface LoadFileDialogListener {
        public void onDismiss(String filename, AnimationFile file);
        public Frame requestFrame();
    }
}
