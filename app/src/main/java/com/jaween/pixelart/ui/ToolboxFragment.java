package com.jaween.pixelart.ui;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.Dropper;
import com.jaween.pixelart.tools.FloodFill;
import com.jaween.pixelart.tools.FreeSelect;
import com.jaween.pixelart.tools.Oval;
import com.jaween.pixelart.tools.Pen;
import com.jaween.pixelart.tools.RectSelect;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.tools.options.OvalOptionsView;
import com.jaween.pixelart.tools.options.PenOptionsView;
import com.jaween.pixelart.tools.options.ToolOptionsView;

import java.util.ArrayList;

/**
 * Created by ween on 11/1/14.
 */
public class ToolboxFragment extends Fragment implements  View.OnClickListener, View.OnTouchListener {

    private ToolButton selectedToolButton;
    private ToolButton penButton;
    private ToolButton eraserButton;
    private ToolButton rectButton;
    private ToolButton ovalButton;
    private ToolButton rectSelectButton;
    private ToolButton floodFillButton;
    private ToolButton dropperButton;
    private ToolButton misingnoButton;
    private ToolButton magicWandButton;
    private ToolButton freeSelectButton;

    private OnToolSelectListener onToolSelectListener;

    private Tool selectedTool;
    private Pen penTool;
    private FloodFill floodFillTool;
    private Oval ovalTool;
    private RectSelect rectSelectTool;
    private Dropper dropperTool;
    private FreeSelect freeSelect;

    private ArrayList<Tool> tools = new ArrayList<Tool>();

    private ToolOptionsView currentToolOptions;
    private PenOptionsView penOptions;
    private OvalOptionsView ovalOptions;

    private FrameLayout optionsFrameLayout;

    private ViewGroup parentViewGroup;

    private static final String KEY_TOOL = "key_tool";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        penTool = new Pen(
                getString(R.string.tool_pen),
                getResources().getDrawable(R.drawable.tool_pen));
        floodFillTool = new FloodFill(
                getString(R.string.tool_flood_fill),
                getResources().getDrawable(R.drawable.tool_fill));
        ovalTool = new Oval(
                getString(R.string.tool_oval),
                getResources().getDrawable(R.drawable.tool_oval));
        rectSelectTool = new RectSelect(
                getString(R.string.tool_rect_select),
                getResources().getDrawable(R.drawable.tool_rect_select));
        dropperTool = new Dropper(
                getString(R.string.tool_dropper),
                getResources().getDrawable(R.drawable.tool_dropper));

        freeSelect = new FreeSelect(
                getString(R.string.tool_free_select),
                getResources().getDrawable(R.drawable.tool_free_select));

        tools.add(penTool);
        tools.add(floodFillTool);
        tools.add(ovalTool);
        tools.add(rectSelectTool);
        tools.add(dropperTool);
        tools.add(freeSelect);

        selectedTool = penTool;

        createToolOptions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        parentViewGroup = (ViewGroup) inflater.inflate(R.layout.toolbox_fragment, null);
        initialiseViews(parentViewGroup);

        onRestoreInstanceState(savedInstanceState);

        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, false);
        }

        changeSelectedTool(selectedTool);

        return parentViewGroup;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_TOOL, selectedTool.getName());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // TODO: Should restore state only if there is a state to be restored! A non-null instanceState doesn't ensure this!
        // Restores the tool state
        if (savedInstanceState != null) {

            String toolName = savedInstanceState.getString(KEY_TOOL);
            if (toolName == null) {
                toolName = penTool.getName();
            }

            // Searches for the tool name
            int i = 0;
            while (i < tools.size()) {
                if (tools.get(i).getName() == toolName) {
                    selectedTool = tools.get(i);
                    break;
                }
                i++;
            }

            // Tool not found
            if (i == tools.size()) {
                Log.e("ToolboxFragment", "Error: Restoring tool, tool name " + toolName + " not found");

                // Default tool
                selectedTool = penTool;
            }
        } else {
            // Default tool
            selectedTool = penTool;
        }
    }

    private void initialiseViews(ViewGroup v) {
        // Links Java objects to XML
        penButton = (ToolButton) v.findViewById(R.id.ib_tool_pen);
        eraserButton = (ToolButton) v.findViewById(R.id.ib_tool_eraser);
        rectButton = (ToolButton) v.findViewById(R.id.ib_tool_rect);
        ovalButton = (ToolButton) v.findViewById(R.id.ib_tool_oval);
        rectSelectButton = (ToolButton) v.findViewById(R.id.ib_tool_rect_select);
        floodFillButton = (ToolButton) v.findViewById(R.id.ib_tool_flood_fill);
        dropperButton = (ToolButton) v.findViewById(R.id.ib_tool_dropper);
        misingnoButton = (ToolButton) v.findViewById(R.id.ib_tool_misingno);
        magicWandButton = (ToolButton) v.findViewById(R.id.ib_tool_magic_wand);
        freeSelectButton = (ToolButton) v.findViewById(R.id.ib_tool_free_select);

        optionsFrameLayout = (FrameLayout) v.findViewById(R.id.fl_container_tool_options);

        // OnClickListeners
        penButton.setOnClickListener(this);
        eraserButton.setOnClickListener(this);
        rectButton.setOnClickListener(this);
        ovalButton.setOnClickListener(this);
        rectSelectButton.setOnClickListener(this);
        floodFillButton.setOnClickListener(this);
        dropperButton.setOnClickListener(this);
        misingnoButton.setOnClickListener(this);
        magicWandButton.setOnClickListener(this);
        freeSelectButton.setOnClickListener(this);

        // Tool selection
        penOptions.setToolAttributes(penTool.getToolAttributes());
        ovalOptions.setToolAttributes(ovalTool.getToolAttributes());
        selectedToolButton = penButton;
        selectedToolButton.setSelected(true);

        // Tool options
        currentToolOptions = penOptions;
        optionsFrameLayout.addView(currentToolOptions);

        // Consumes touches
        v.setOnTouchListener(this);
    }

    private void createToolOptions() {
        penOptions = new PenOptionsView(getActivity());
        ovalOptions = new OvalOptionsView(getActivity());
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ToolButton) {
            optionsFrameLayout.removeAllViews();
        }

        Tool clickedTool = null;
        switch (v.getId()) {
            case R.id.ib_tool_pen:
                clickedTool = penTool;
                currentToolOptions = penOptions;
                break;
            case R.id.ib_tool_eraser:
                // No implementation
                return;
            case R.id.ib_tool_rect:
                // No implementation
                return;
            case R.id.ib_tool_oval:
                clickedTool = ovalTool;
                currentToolOptions = ovalOptions;
                break;
            case R.id.ib_tool_rect_select:
                clickedTool = rectSelectTool;
                currentToolOptions = null;
                break;
            case R.id.ib_tool_flood_fill:
                clickedTool = floodFillTool;
                currentToolOptions = null;
                break;
            case R.id.ib_tool_dropper:
                clickedTool = dropperTool;
                currentToolOptions = null;
                break;
            case R.id.ib_tool_misingno:
                // No implementation
                return;
            case R.id.ib_tool_magic_wand:
                // No implementation
                return;
            case R.id.ib_tool_free_select:
                clickedTool = freeSelect;
                currentToolOptions = null;
                break;
            default:
                return;
        }

        // User tapped the selected tool again, hides the panel if possible
        boolean dismissPanel = false;
        if (clickedTool == selectedTool) {
            dismissPanel = true;
        }
        selectedTool = clickedTool;

        // Sets the item button background
        if (v instanceof ToolButton) {
            changeSelectedToolButton((ToolButton) v);
        }

        // On screen options
        if (currentToolOptions != null) {
            optionsFrameLayout.addView(currentToolOptions);
        }

        // Notifies of the DrawingFragment of a change in tool
        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, dismissPanel);
        }
    }

    private void changeSelectedTool(Tool tool) {
        ToolButton newToolButton = null;
        if (tool instanceof Pen) {
            newToolButton = penButton;
        //} else if (tool instanceof Eraser) {
        //    newToolButton = eraserButton;
        //} else if (tool instanceof Rect) {
        //    newToolButton = rectButton;
        } else if (tool instanceof Oval) {
            newToolButton = ovalButton;
        } else if (tool instanceof RectSelect) {
            newToolButton = rectSelectButton;
        } else if (tool instanceof FloodFill) {
            newToolButton = floodFillButton;
        } else if (tool instanceof Dropper) {
            newToolButton = dropperButton;
        //} else if (tool instanceof MissingNo) {
        //    newToolButton = missingNoButton;
        //} else if (tool instanceof MagicWand) {
        //    newToolButton = magicWandButton;
        } else if (tool instanceof FreeSelect) {
            newToolButton = freeSelectButton;
        } else {
            Log.e("ToolboxFragment", "Missing ToolButton for tool " + tool.getName());
            return;
        }
        changeSelectedToolButton(newToolButton);
    }

    private void changeSelectedToolButton(ToolButton newToolButton) {
        selectedToolButton.setSelected(false);
        selectedToolButton = newToolButton;
        selectedToolButton.setSelected(true);
    }

    public void setDimensions(int width, int height) {
        if (floodFillTool != null) {
            floodFillTool.setBitmapConfiguration(width, height, Bitmap.Config.ARGB_8888);
        }
    }

    public void setOnToolSelectListener(OnToolSelectListener onToolSelectListener) {
        this.onToolSelectListener = onToolSelectListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Consumes all touch events on the background so they don't pass through onto
        // a possible canvas below
        return true;
    }

    public interface OnToolSelectListener {
        public void onToolSelected(Tool tool, boolean done);
    }

    public void setColour(int colour) {
        selectedTool.getToolAttributes().getPaint().setColor(colour);
    }
}