package com.jaween.pixelart.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.jaween.pixelart.PanelManagerFragment;
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

    private ViewGroup parentViewGroup;
    private FrameLayout optionsFrameLayout;
    private TableLayout toolTable;

    // Animations
    private static final int ANIM_SLIDE_DURATION = 200;
    private static final int ANIM_TOOL_DURATION = 100;
    private static final int ANIM_TOOL_DELAY = 40;
    private static final int ANIM_FADE_DURATION = 250;

    private LinearLayout toolboxBase;
    private ValueAnimator slideAnimator;

    private int restoredColour;

    private static final String KEY_TOOL = "key_tool";
    private static final int NULL_TOOL_ID = -1;

    private AnimatorSet contentAnimatorSetIn = new AnimatorSet();
    private AnimatorSet contentAnimatorSetOut = new AnimatorSet();
    private Interpolator decelerate = new DecelerateInterpolator();
    private Interpolator accelerate = new AccelerateInterpolator();
    private int measuredHeight;


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
        parentViewGroup = (ViewGroup) inflater.inflate(R.layout.toolbox_fragment, null);
        initialiseViews(parentViewGroup);

        onRestoreInstanceState(savedInstanceState);

        if (onToolSelectListener != null) {
            onToolSelectListener.onToolSelected(selectedTool, false);
        }

        setupAnimations();

        return parentViewGroup;
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

    private void initialiseViews(ViewGroup v) {
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

        toolboxBase = (LinearLayout) v.findViewById(R.id.ll_toolbox_base);
        toolTable = (TableLayout) v.findViewById(R.id.tl_toolbox_table);
        optionsFrameLayout = (FrameLayout) v.findViewById(R.id.fl_container_tool_options);

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
    }

    @Override
    public void onClick(View v) {
        // Can't change tool while animating
        if (animationStarted()) {
            return;
        }

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
                clickedTool = eraserTool;
                currentToolOptions = eraserOptions;
                break;
            case R.id.ib_tool_rect:
                clickedTool = rectTool;
                currentToolOptions = rectOptions;
                break;
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
            case R.id.ib_tool_text:
                // No implementation
                return;
            case R.id.ib_tool_magic_wand:
                clickedTool = magicWandTool;
                currentToolOptions = magicWandOptions;
                break;
            case R.id.ib_tool_free_select:
                clickedTool = freeSelectTool;
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
     * Updates the Tool options UI given a Tool
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

    private void hideFragment() {
        ((PanelManagerFragment) getParentFragment()).hideFragmentTemp(this);
    }

    private boolean animationStarted() {
        return slideAnimator.isStarted() ||
                contentAnimatorSetIn.isStarted() ||
                contentAnimatorSetOut.isStarted();
    }

    private void setupAnimations() {
        // Panel sliding animation
        slideAnimator = ValueAnimator.ofInt(0, 0);
        slideAnimator.setDuration(ANIM_SLIDE_DURATION);
        slideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //Update Height
                int value = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = toolboxBase.getLayoutParams();
                layoutParams.height = value;
                toolboxBase.setLayoutParams(layoutParams);
            }
        });

        createToolAnimationIn();
        createToolAnimationOut();
    }

    private void getViewHeight() {
        ViewTreeObserver viewTreeObserver = toolboxBase.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                measuredHeight = toolboxBase.getMeasuredHeight();
                if (measuredHeight > 0) {
                    toolboxBase.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    public void startAnimation(final boolean forward) {
        // Only starts animation if there are no other animations playing
        if (animationStarted()) {
            return;
        }

        getViewHeight();

        if (forward) {
            // Makes all the ToolButtons invisible to begin with
            for (int j = 0; j < toolTable.getChildCount(); j++) {
                TableRow tableRow = (TableRow) toolTable.getChildAt(j);
                for (int i = 0; i < tableRow.getChildCount(); i++) {
                    // Initially they are gone
                    View view = tableRow.getChildAt(i);
                    view.setScaleX(0);
                    view.setScaleY(0);
                }
            }
            toolTable.setVisibility(View.INVISIBLE);
            optionsFrameLayout.setVisibility(View.INVISIBLE);

            slideAnimator.setIntValues(0, measuredHeight);
            slideAnimator.removeAllListeners();
            slideAnimator.addListener(slideDownListener);
            slideAnimator.start();
        } else {
            // Plays the animation in reverse
            slideAnimator.setIntValues(toolboxBase.getHeight(), 0);
            contentAnimatorSetOut.removeAllListeners();
            contentAnimatorSetOut.addListener(slideUpListener);
            contentAnimatorSetOut.start();
        }
    }


    private void createToolAnimationIn() {
        // Tools appearing two by two
        ObjectAnimator[] toolAnimators = new ObjectAnimator[10];
        for (int row = 0; row < toolTable.getChildCount(); row++) {
            TableRow tableRow = (TableRow) toolTable.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f);

                // Makes items appear diagonally
                ObjectAnimator toolAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                toolAnimator.setInterpolator(decelerate);
                toolAnimator.setStartDelay(col * ANIM_TOOL_DELAY + row * ANIM_TOOL_DELAY);
                toolAnimator.setDuration(ANIM_TOOL_DURATION);
                toolAnimators[row * tableRow.getChildCount() + col] = toolAnimator;
            }
        }
        AnimatorSet toolAnimatorSet = new AnimatorSet();
        toolAnimatorSet.playTogether(toolAnimators);
        ObjectAnimator optionsAnimator = ObjectAnimator.ofFloat(optionsFrameLayout, View.ALPHA, 0f, 1f);
        optionsAnimator.setDuration(ANIM_FADE_DURATION);
        contentAnimatorSetIn.playSequentially(toolAnimatorSet, optionsAnimator);
        contentAnimatorSetIn.setInterpolator(decelerate);
    }

    private void createToolAnimationOut() {
        // Tools disappearing two by two
        ObjectAnimator[] toolAnimators = new ObjectAnimator[10];
        for (int row = 0; row < toolTable.getChildCount(); row++) {
            TableRow tableRow = (TableRow) toolTable.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f);

                // Makes items disappear diagonally
                ObjectAnimator toolAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                toolAnimator.setInterpolator(accelerate);
                toolAnimator.setStartDelay(col * ANIM_TOOL_DELAY + row * ANIM_TOOL_DELAY);
                toolAnimator.setDuration(ANIM_TOOL_DURATION);
                toolAnimators[row * tableRow.getChildCount() + col] = toolAnimator;
            }
        }
        AnimatorSet toolAnimatorSet = new AnimatorSet();
        toolAnimatorSet.playTogether(toolAnimators);
        ObjectAnimator optionsAnimator = ObjectAnimator.ofFloat(optionsFrameLayout, View.ALPHA, 1f, 0f);
        optionsAnimator.setDuration(ANIM_FADE_DURATION);
        contentAnimatorSetOut.playSequentially(optionsAnimator, toolAnimatorSet);
        contentAnimatorSetOut.setInterpolator(accelerate);
    }


    private Animator.AnimatorListener slideDownListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            slideAnimator.setInterpolator(decelerate);
            toolTable.setVisibility(View.INVISIBLE);
            optionsFrameLayout.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            // Modified the LayoutParams to get the expand/collapse effect, so restores the original behaviour
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) toolboxBase.getLayoutParams();
            layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            toolboxBase.setLayoutParams(layoutParams);

            toolTable.setVisibility(View.VISIBLE);
            optionsFrameLayout.setVisibility(View.VISIBLE);
            contentAnimatorSetIn.start();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // No implementation
        }
    };

    private Animator.AnimatorListener slideUpListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            slideAnimator.removeAllListeners();
            slideAnimator.addListener(slideUpFragmentListener);
            slideAnimator.start();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // No implementation
        }
    };

    private Animator.AnimatorListener slideUpFragmentListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            slideAnimator.setInterpolator(accelerate);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            hideFragment();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            // No implementation
        }
    };
}