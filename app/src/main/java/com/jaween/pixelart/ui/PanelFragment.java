package com.jaween.pixelart.ui;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TableLayout;

import com.jaween.pixelart.util.SlideAnimator;
import com.jaween.pixelart.util.SlidingLinearLayout;

/**
 * Created by ween on 12/19/14.
 */
public class PanelFragment extends Fragment {

    // Animator
    private SlideAnimator slideAnimator;

    protected void setupAnimation(SlidingLinearLayout container, TableLayout table, View bar) {
        slideAnimator = new SlideAnimator(container, table, bar, this);
    }

    public boolean animationStarted() {
        return slideAnimator.animationStarted();
    }

    public void slide(boolean forward, int height) {
        if (forward) {
            slideAnimator.start(height);
        } else {
            slideAnimator.reverse(height);
        }
    }
}
