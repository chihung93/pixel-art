package com.jaween.pixelart.ui;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.jaween.pixelart.R;
import com.jaween.pixelart.tools.Dropper;
import com.jaween.pixelart.tools.Eraser;
import com.jaween.pixelart.tools.FloodFill;
import com.jaween.pixelart.tools.FreeSelect;
import com.jaween.pixelart.tools.MagicWand;
import com.jaween.pixelart.tools.Oval;
import com.jaween.pixelart.tools.Pen;
import com.jaween.pixelart.tools.Rect;
import com.jaween.pixelart.tools.RectSelect;
import com.jaween.pixelart.tools.Tool;
import com.jaween.pixelart.tools.options.EraserOptionsView;
import com.jaween.pixelart.tools.options.MagicWandOptionsView;
import com.jaween.pixelart.tools.options.OvalOptionsView;
import com.jaween.pixelart.tools.options.PenOptionsView;
import com.jaween.pixelart.tools.options.RectOptionsView;
import com.jaween.pixelart.tools.options.ToolOptionsView;
import com.jaween.pixelart.util.SlidingLinearLayout;

import java.util.ArrayList;

/**
 * Created by ween on 11/1/14.
 */
public class ToolboxFragment extends PanelFragment implements
        View.OnClickListener,
        View.OnTouchListener {

    private static final String KEY_TOOL = "key_tool";
    private static final int NULL_TOOL_ID = -1;
    private static final int ANIM_OPTIONS_FADE_DURATION = 150;

    private ToolButton selectedToolButton;
    private ToolButton penButton;
    private ToolButton eraserButton;
    private ToolButton rectButton;
    private ToolButton ovalButton;
    private ToolButton rectSelectButton;
    private ToolButton floodFillButton;
    private ToolButton dropperButton;
    private TextView textButton;
    private ToolButton magicWandButton;
    private ToolButton freeSelectButton;

    private OnToolSelectListener onToolSelectListener;

    private Tool selectedTool;
    private Pen penTool;
    private Eraser eraserTool;
    private Rect rectTool;
    private Oval ovalTool;
    private RectSelect rectSelectTool;
    private FloodFill floodFillTool;
    private Dropper dropperTool;
    private MagicWand magicWandTool;
    private FreeSelect freeSelectTool;

    private ArrayList<Tool> tools = new ArrayList<Tool>();

    private ToolOptionsView currentToolOptions;
    private PenOptionsView penOptions;
    private EraserOptionsView eraserOptions;
    private RectOptionsView rectOptions;
    private OvalOptionsView ovalOptions;
    private MagicWandOptionsView magicWandOptions;

    // Views
    private SlidingLinearLayout toolboxLayout;
    private FrameLayout optionsFrameLayout;
    private TableLayout toolTable;

    private int restoredColour;

    private Tool newTool;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        penTool = new Pen(
                getString(R.string.tool_pen),
                getResources().getDrawable(R.drawable.tool_pen));
        eraserTool = new Eraser(
                getString(R.string.tool_eraser),
                getResources().getDrawable(R.drawable.tool_eraser));
        rectTool = new Rect(
                getString(R.string.tool_rect),
                getResources().getDrawable(R.drawable.tool_rect));
        floodFillTool = new FloodFill(
                getString(R.string.tool_flood_fill),
                getResources().getDrawable(R.drawable.tool_fill));
        rectSelectTool = new RectSelect(
                getString(R.string.tool_rect_select),
                getResources().getDrawable(R.drawable.tool_rect_select));
        ovalTool = new Oval(
                getString(R.string.tool_oval),
                getResources().getDrawable(R.drawable.tool_oval));
        dropperTool = new Dropper(
                getString(R.string.tool_dropper),
                getResources().getDrawable(R.drawable.tool_dropper));
        magicWandTool = new MagicWand(
                getString(R.string.tool_magic_wand),
                getResources().getDrawable(R.drawable.tool_magic_wand));
        freeSelectTool = new FreeSelect(
                getString(R.string.tool_free_select),
                getResources().getDrawable(R.drawable.tool_free_select));

        tools.add(penTool);
        tools.add(eraserTool);
        tools.add(rectTool);
        tools.add(ovalTool);
        tools.add(rectSelectTool);
        tools.add(floodFillTool);
        tools.add(dropperTool);
        tools.add(magicWandTool);
        tools.add(freeSelectTool);

        createToolOptions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbox_fragment, null);
        initialiseViews(view);

        onRestoreInstanceState(savedInstanceState);

        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, false);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_TOOL, selectedTool.getToolId());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restores the selected tool state
        if (savedInstanceState != null) {
            int toolId = savedInstanceState.getInt(KEY_TOOL, NULL_TOOL_ID);
            if (toolId == NULL_TOOL_ID) {
                toolId = penTool.getToolId();
            }

            // Searches for the tool name
            int i = 0;
            while (i < tools.size()) {
                if (tools.get(i).getToolId() == toolId) {
                    selectedTool = tools.get(i);
                    break;
                }
                i++;
            }

            // Tool not found
            if (i == tools.size()) {
                Log.e("ToolboxFragment", "Error: Restoring tool, toolId " + toolId + " not found");

                // Default tool
                selectedTool = penTool;
            }

            // DrawingFragment would have given us a restored colour
            selectedTool.getToolAttributes().getPaint().setColor(restoredColour);
        } else {
            // Default tool
            selectedTool = penTool;
        }

        // Updates the tool options UI
        setToolOptions(selectedTool);

        // Highlight the selected tool's button
        changeSelectedTool(selectedTool);
    }

    private void initialiseViews(View v) {
        // Links Java objects to XML
        penButton = (ToolButton) v.findViewById(R.id.ib_tool_pen);
        eraserButton = (ToolButton) v.findViewById(R.id.ib_tool_eraser);
        rectButton = (ToolButton) v.findViewById(R.id.ib_tool_rect);
        ovalButton = (ToolButton) v.findViewById(R.id.ib_tool_oval);
        rectSelectButton = (ToolButton) v.findViewById(R.id.ib_tool_rect_select);
        floodFillButton = (ToolButton) v.findViewById(R.id.ib_tool_flood_fill);
        dropperButton = (ToolButton) v.findViewById(R.id.ib_tool_dropper);
        textButton = (TextView) v.findViewById(R.id.ib_tool_text);
        magicWandButton = (ToolButton) v.findViewById(R.id.ib_tool_magic_wand);
        freeSelectButton = (ToolButton) v.findViewById(R.id.ib_tool_free_select);

        toolboxLayout = (SlidingLinearLayout) v.findViewById(R.id.sll_toolbox_content);
        toolTable = (TableLayout) v.findViewById(R.id.tl_toolbox_table);
        optionsFrameLayout = (FrameLayout) v.findViewById(R.id.fl_container_tool_options);

        // Sets up sliding animations
        setupAnimation(toolboxLayout, toolTable, optionsFrameLayout);

        // Sets up option transitions animations
        LayoutTransition layoutTransition = optionsFrameLayout.getLayoutTransition();
        layoutTransition.setDuration(ANIM_OPTIONS_FADE_DURATION);

        // OnClickListeners
        penButton.setOnClickListener(this);
        eraserButton.setOnClickListener(this);
        rectButton.setOnClickListener(this);
        ovalButton.setOnClickListener(this);
        rectSelectButton.setOnClickListener(this);
        floodFillButton.setOnClickListener(this);
        dropperButton.setOnClickListener(this);
        textButton.setOnClickListener(this);
        magicWandButton.setOnClickListener(this);
        freeSelectButton.setOnClickListener(this);

        // Tool selection
        penOptions.setToolAttributes(penTool.getToolAttributes());
        eraserOptions.setToolAttributes(eraserTool.getToolAttributes());
        rectOptions.setToolAttributes(rectTool.getToolAttributes());
        ovalOptions.setToolAttributes(ovalTool.getToolAttributes());
        magicWandOptions.setToolAttributes(magicWandTool.getToolAttributes());

        // Tool options
        currentToolOptions = penOptions;
        optionsFrameLayout.addView(currentToolOptions);

        // Consumes touches
        v.setOnTouchListener(this);
    }

    private void createToolOptions() {
        penOptions = new PenOptionsView(getActivity());
        eraserOptions = new EraserOptionsView(getActivity());
        rectOptions = new RectOptionsView(getActivity());
        ovalOptions = new OvalOptionsView(getActivity());
        magicWandOptions = new MagicWandOptionsView(getActivity());

        setupLayoutTransitions();
    }

    private void setupLayoutTransitions() {
        LayoutTransition layoutTransition;

        layoutTransition = penOptions.getChildLayoutTransition();
        layoutTransition.setDuration(ANIM_OPTIONS_FADE_DURATION);

        layoutTransition = rectOptions.getChildLayoutTransition();
        layoutTransition.setDuration(ANIM_OPTIONS_FADE_DURATION);
    }

    @Override
    public void onClick(View v) {
        // Can't change tool while animating
        if (animationStarted()) {
            return;
        }

        switch (v.getId()) {
            case R.id.ib_tool_pen:
                newTool = penTool;
                break;
            case R.id.ib_tool_eraser:
                newTool = eraserTool;
                break;
            case R.id.ib_tool_rect:
                newTool = rectTool;
                break;
            case R.id.ib_tool_oval:
                newTool = ovalTool;
                break;
            case R.id.ib_tool_rect_select:
                newTool = rectSelectTool;
                break;
            case R.id.ib_tool_flood_fill:
                newTool = floodFillTool;
                break;
            case R.id.ib_tool_dropper:
                newTool = dropperTool;
                break;
            case R.id.ib_tool_text:
                // No implementation
                return;
            case R.id.ib_tool_magic_wand:
                newTool = magicWandTool;
                break;
            case R.id.ib_tool_free_select:
                newTool = freeSelectTool;
                break;
            default:
                return;
        }

        // Sets the item button background
        if (v instanceof ToolButton) {
            changeSelectedToolButton((ToolButton) v);
        }

        //if (currentToolOptions != null) {
            //toolSelectionAnimation.setTarget(currentToolOptions);
            //toolSelectionAnimation.start();
        //} else {
            selectTool(newTool);
        //}
    }

    private void selectTool(Tool tool) {
        // User tapped the selected tool again, hides the panel if possible
        boolean dismissPanel = false;
        if (tool == selectedTool) {
            dismissPanel = true;
        }
        setToolOptions(tool);

        // Notifies of the DrawingFragment of a change in tool
        selectedTool = newTool;
        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, dismissPanel);
        }
    }

    private void changeSelectedTool(Tool tool) {
        ToolButton newToolButton = null;
        if (tool instanceof Pen) {
            newToolButton = penButton;
        } else if (tool instanceof Eraser) {
            newToolButton = eraserButton;
        } else if (tool instanceof Rect) {
            newToolButton = rectButton;
        } else if (tool instanceof Oval) {
            newToolButton = ovalButton;
        } else if (tool instanceof RectSelect) {
            newToolButton = rectSelectButton;
        } else if (tool instanceof FloodFill) {
            newToolButton = floodFillButton;
        } else if (tool instanceof Dropper) {
            newToolButton = dropperButton;
        //} else if (tool instanceof Text) {
        //    newToolButton = textButton;
        } else if (tool instanceof MagicWand) {
            newToolButton = magicWandButton;
        } else if (tool instanceof FreeSelect) {
            newToolButton = freeSelectButton;
        } else {
            Log.e("ToolboxFragment", "Missing ToolButton for tool " + tool.getName());
            return;
        }
        changeSelectedToolButton(newToolButton);
    }

    private void changeSelectedToolButton(ToolButton newToolButton) {
        // On the initial selection selectedToolButton will be null
        if (selectedToolButton != null) {
            selectedToolButton.setSelected(false);
        }

        selectedToolButton = newToolButton;
        selectedToolButton.setSelected(true);
    }

    /**
     * Removes the current ToolOptionsView, sets a new currentToolOptions and adds a new,
     * ToolOptionsView (if any).
     * @param tool The tool whose ToolOptionsView is to be added to the UI
     */
    private void setToolOptions(Tool tool) {
        if (tool instanceof Pen) {
            currentToolOptions = penOptions;
        } else if (tool instanceof  Eraser) {
            currentToolOptions = eraserOptions;
        } else if (tool instanceof  Rect) {
            currentToolOptions = rectOptions;
        } else if (tool instanceof  Oval) {
            currentToolOptions = ovalOptions;
        } else if (tool instanceof  RectSelect) {
            currentToolOptions = null;
        } else if (tool instanceof  FloodFill) {
            currentToolOptions = null;
        } else if (tool instanceof  Dropper) {
            currentToolOptions = null;
        /*} else if (tool instanceof  Text) {
            currentToolOptions = textOptions;*/
        } else if (tool instanceof  MagicWand) {
            currentToolOptions = magicWandOptions;
        } else if (tool instanceof  FreeSelect) {
            currentToolOptions = null;
        }

        // Updates the UI
        optionsFrameLayout.removeAllViews();
        if (currentToolOptions != null) {
            optionsFrameLayout.addView(currentToolOptions);
        }
    }

    public Tool getTool() {
        return selectedTool;
    }

    public void setDimensions(int width, int height) {
       floodFillTool.setBitmapConfiguration(width, height);
       magicWandTool.setBitmapConfiguration(width, height);
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
        if (selectedTool == null) {
            restoredColour = colour;
        } else {
            selectedTool.getToolAttributes().getPaint().setColor(colour);
        }
    }
}