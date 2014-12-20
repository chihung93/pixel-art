package com.jaween.pixelart.tools.options;

import android.animation.LayoutTransition;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jaween.pixelart.tools.attributes.ToolAttributes;

/**
 * Created by ween on 11/2/14.
 */
public class ToolOptionsView extends FrameLayout {

    protected ToolAttributes toolAttributes;
    protected ViewGroup viewGroup;

    protected ToolOptionsView(Context context) {
        super(context);
    }

    protected ToolOptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected ToolOptionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setToolAttributes(ToolAttributes toolToolAttributes) {
        this.toolAttributes = toolToolAttributes;
    }

    public LayoutTransition getChildLayoutTransition() {
        return viewGroup.getLayoutTransition();
    }
}