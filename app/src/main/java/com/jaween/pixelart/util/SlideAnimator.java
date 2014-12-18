package com.jaween.pixelart.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.jaween.pixelart.PanelManagerFragment;

/**
 * Created by ween on 12/13/14.
 */
public class SlideAnimator {

    // Animation constants
    private static final int ANIM_SLIDE_DURATION = 200;
    private static final int ANIM_ITEM_DURATION = 100;
    private static final int ANIM_ITEM_DELAY = 40;
    private static final int ANIM_BAR_FADE_DURATION = 250;
    private static final int ANIM_BAR_FADE_IN_DELAY = 200;

    // Animators
    private ValueAnimator slideAnimator;
    private AnimatorSet contentAnimatorSetIn = new AnimatorSet();
    private AnimatorSet contentAnimatorSetOut = new AnimatorSet();
    private Interpolator decelerate = new DecelerateInterpolator();
    private Interpolator accelerate = new AccelerateInterpolator();

    // Views
    private SlidingLinearLayout container;
    private TableLayout table;
    private View bar;

    private PanelManagerFragment panelManagerFragment;
    private Fragment fragment;

    public SlideAnimator(final SlidingLinearLayout container, TableLayout table, View bar, PanelManagerFragment panelManagerFragment, Fragment fragment) {
        this.container = container;
        this.table = table;
        this.bar = bar;
        this.panelManagerFragment = panelManagerFragment;
        this.fragment = fragment;

        // Stops the first frame of the first play through from popping in then back out
        ViewTreeObserver viewTreeObserver = container.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (container.getHeight() > 0) {
                    container.getViewTreeObserver().removeOnPreDrawListener(this);
                    container.setYFraction(0);
                }
                return true;
            }
        });

        setupAnimations();
    }

    public boolean animationStarted() {
        return slideAnimator.isRunning() ||
                contentAnimatorSetIn.isRunning() ||
                contentAnimatorSetOut.isRunning();
    }

    private void hideFragment() {
        panelManagerFragment.hideFragmentTemp(fragment);
    }

    private void setupAnimations() {
        // Panel sliding animation
        slideAnimator = ObjectAnimator.ofFloat(container, "yFraction", 0, 1f);
        slideAnimator.setDuration(ANIM_SLIDE_DURATION);

        createItemAnimationIn();
        createItemAnimationOut();
    }

    public void start(int fromHeight) {
        // Only starts animation if there are no other animations playing
        if (animationStarted()) {
            return;
        }

        // Makes all the items invisible to begin with
        for (int j = 0; j < table.getChildCount(); j++) {
            TableRow tableRow = (TableRow) table.getChildAt(j);
            for (int i = 0; i < tableRow.getChildCount(); i++) {
                // Initially they are gone
                View view = tableRow.getChildAt(i);
                view.setScaleX(0);
                view.setScaleY(0);
            }
        }

        slideAnimator.setFloatValues(0f, 1f);
        slideAnimator.removeAllListeners();
        slideAnimator.addListener(slideDownListener);
        slideAnimator.start();
    }

    public void reverse(int toHeight) {
        // Only starts animation if there are no other animations playing
        if (animationStarted()) {
            return;
        }

        // Plays the animation in reverse
        slideAnimator.setFloatValues(1f, 0f);
        contentAnimatorSetOut.removeAllListeners();
        contentAnimatorSetOut.addListener(animOutListener);
        contentAnimatorSetOut.start();
    }

    private void createItemAnimationIn() {
        // Items appearing two by two
        int totalChildren = 0;
        for (int i = 0; i < table.getChildCount(); i++) {
            totalChildren += ((TableRow) table.getChildAt(i)).getChildCount();
        }
        ObjectAnimator[] itemAnimators = new ObjectAnimator[totalChildren];

        for (int row = 0; row < table.getChildCount(); row++) {
            TableRow tableRow = (TableRow) table.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 1f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 1f);

                // Makes items appear diagonally
                ObjectAnimator itemAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                itemAnimator.setInterpolator(decelerate);
                itemAnimator.setStartDelay(col * ANIM_ITEM_DELAY + row * ANIM_ITEM_DELAY);
                itemAnimator.setDuration(ANIM_ITEM_DURATION);
                itemAnimators[row * tableRow.getChildCount() + col] = itemAnimator;
            }
        }
        AnimatorSet itemAnimatorSet = new AnimatorSet();
        itemAnimatorSet.playTogether(itemAnimators);
        ObjectAnimator barAnimator = ObjectAnimator.ofFloat(bar, "alpha", 0f, 1f);
        barAnimator.setDuration(ANIM_BAR_FADE_DURATION);
        barAnimator.setStartDelay(ANIM_BAR_FADE_IN_DELAY);
        contentAnimatorSetIn.playTogether(itemAnimatorSet, barAnimator);
        contentAnimatorSetIn.setInterpolator(decelerate);
    }

    private void createItemAnimationOut() {
        // Items appearing two by two
        int totalChildren = 0;
        for (int i = 0; i < table.getChildCount(); i++) {
            totalChildren += ((TableRow) table.getChildAt(i)).getChildCount();
        }
        ObjectAnimator[] itemAnimators = new ObjectAnimator[totalChildren];

        for (int row = 0; row < table.getChildCount(); row++) {
            TableRow tableRow = (TableRow) table.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 0f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 0f);

                // Makes items disappear diagonally
                ObjectAnimator itemAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                itemAnimator.setInterpolator(accelerate);
                itemAnimator.setStartDelay(col * ANIM_ITEM_DELAY + row * ANIM_ITEM_DELAY);
                itemAnimator.setDuration(ANIM_ITEM_DURATION);
                itemAnimators[row * tableRow.getChildCount() + col] = itemAnimator;
            }
        }
        AnimatorSet itemAnimatorSet = new AnimatorSet();
        itemAnimatorSet.playTogether(itemAnimators);
        ObjectAnimator barAnimator = ObjectAnimator.ofFloat(bar, "alpha", 1f, 0f);
        barAnimator.setDuration(ANIM_BAR_FADE_DURATION);
        contentAnimatorSetOut.playTogether(barAnimator, itemAnimatorSet);
        contentAnimatorSetOut.setInterpolator(accelerate);
    }

    private Animator.AnimatorListener slideDownListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            slideAnimator.setInterpolator(decelerate);
            table.setVisibility(View.INVISIBLE);
            bar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            table.setVisibility(View.VISIBLE);
            bar.setVisibility(View.VISIBLE);
            bar.setAlpha(0);
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

    private Animator.AnimatorListener animOutListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            slideAnimator.removeAllListeners();
            slideAnimator.addListener(slideUpListener);
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

    private Animator.AnimatorListener slideUpListener = new Animator.AnimatorListener() {
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
