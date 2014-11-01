package com.jaween.pixelart.ui;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.FloodFill;
import com.jaween.pixelart.tools.Oval;
import com.jaween.pixelart.tools.Pen;
import com.jaween.pixelart.tools.Tool;

/**
 * Created by ween on 11/1/14.
 */
public class ToolboxFragment extends Fragment implements View.OnClickListener {

    private ToolButton selectedToolButton;
    private ToolButton penButton;
    private ToolButton eraserButton;
    private ToolButton rectButton;
    private ToolButton ovalButton;
    private ToolButton rectSelectButton;
    private ToolButton floodFillButton;
    private ToolButton colourPickerButton;
    private ToolButton misingnoButton;
    private ToolButton magicWandButton;
    private ToolButton drawSelectButton;

    private OnToolSelectListener onToolSelectListener;

    private Tool selectedTool;
    private Pen penTool;
    private FloodFill floodFillTool;
    private Oval ovalTool;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        penTool = new Pen(getString(R.string.tool_pen), getResources().getDrawable(R.drawable.tool_pen));
        floodFillTool = new FloodFill(getString(R.string.tool_flood_fill), getResources().getDrawable(R.drawable.tool_fill));
        ovalTool = new Oval(getString(R.string.tool_oval), getResources().getDrawable(R.drawable.tool_oval));
        selectedTool = penTool;

        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbox_fragment, null);
        initialiseViews(view);

        return view;
    }
    
    private void initialiseViews(View v) {
        // Links Java objects to XML
        penButton = (ToolButton) v.findViewById(R.id.ib_tool_pen);
        eraserButton = (ToolButton) v.findViewById(R.id.ib_tool_eraser);
        rectButton = (ToolButton) v.findViewById(R.id.ib_tool_rect);
        ovalButton = (ToolButton) v.findViewById(R.id.ib_tool_oval);
        rectSelectButton = (ToolButton) v.findViewById(R.id.ib_tool_rect_select);
        floodFillButton = (ToolButton) v.findViewById(R.id.ib_tool_flood_fill);
        colourPickerButton = (ToolButton) v.findViewById(R.id.ib_tool_colour_picker);
        misingnoButton = (ToolButton) v.findViewById(R.id.ib_tool_misingno);
        magicWandButton = (ToolButton) v.findViewById(R.id.ib_tool_magic_wand);
        drawSelectButton = (ToolButton) v.findViewById(R.id.ib_tool_draw_select);

        // OnClickListeners
        penButton.setOnClickListener(this);
        eraserButton.setOnClickListener(this);
        rectButton.setOnClickListener(this);
        ovalButton.setOnClickListener(this);
        rectSelectButton.setOnClickListener(this);
        floodFillButton.setOnClickListener(this);
        colourPickerButton.setOnClickListener(this);
        misingnoButton.setOnClickListener(this);
        magicWandButton.setOnClickListener(this);
        drawSelectButton.setOnClickListener(this);

        selectedToolButton = penButton;
        selectedToolButton.setSelected(true);
    }

    @Override
    public void onClick(View v) {
        Tool clickedTool;
        switch (v.getId()) {
            case R.id.ib_tool_pen:
                clickedTool = penTool;
                break;
            case R.id.ib_tool_eraser:
                // No implementation
                return;
            case R.id.ib_tool_rect:
                // No implementation
                return;
            case R.id.ib_tool_oval:
                clickedTool = ovalTool;
                break;
            case R.id.ib_tool_rect_select:
                // No implementation
                return;
            case R.id.ib_tool_flood_fill:
                clickedTool = floodFillTool;
                break;
            case R.id.ib_tool_colour_picker:
                // No implementation
                return;
            case R.id.ib_tool_misingno:
                // No implementation
                return;
            case R.id.ib_tool_magic_wand:
                // No implementation
                return;
            case R.id.ib_tool_draw_select:
                // No implementation
                return;
            default:
                return;
        }

        // User tapped the selected tool again, hides the panel if on phone
        boolean done = false;
        if (clickedTool == selectedTool) {
            done = true;
        }

        if (v instanceof ToolButton) {
            selectedToolButton.setSelected(false);
            selectedToolButton = (ToolButton) v;
            selectedToolButton.setSelected(true);
        }
        
        selectedTool = clickedTool;

        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, done);
        }
    }

    public void setDimensions(int width, int height) {
        if (floodFillTool != null) {
            floodFillTool.setBitmapConfiguration(width, height, Bitmap.Config.ARGB_8888);
        }
    }

    public void setOnToolSelectListener(OnToolSelectListener onToolSelectListener) {
        this.onToolSelectListener = onToolSelectListener;
    }

    public interface OnToolSelectListener {
        public void onToolSelected(Tool tool, boolean done);
    }
}