package com.jaween.pixelart.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.jaween.pixelart.PanelManagerFragment;

import java.util.ArrayList;

/**
 * Created by ween on 12/13/14.
 */
public class SlideAnimator {

    // Animation constants
    private static final int ANIM_SLIDE_DURATION = 200;
    private static final int ANIM_ITEM_DURATION = 80;
    private static final int ANIM_ITEM_DELAY = 40;
    private static final int ANIM_BAR_FADE_DURATION = 160;
    private static final int ANIM_BAR_FADE_IN_DELAY = 160;

    // Views
    private SlidingLinearLayout container;
    private TableLayout table;
    private View bar;
    private Fragment fragment;

    // Animators
    private ValueAnimator slideAnimator;
    private AnimatorSet contentAnimatorSetIn = new AnimatorSet();
    private AnimatorSet contentAnimatorSetOut = new AnimatorSet();
    private Interpolator decelerate = new DecelerateInterpolator();
    private Interpolator accelerate = new AccelerateInterpolator();

    public SlideAnimator(final SlidingLinearLayout container, TableLayout table, View bar, Fragment fragment) {
        this.container = container;
        this.table = table;
        this.bar = bar;
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

        float from = (float) fromHeight / (float) fragment.getView().getHeight();
        int duration = 0;
        if (from > 1) {
            duration = 7 * ANIM_ITEM_DELAY + ANIM_SLIDE_DURATION;
            slideAnimator.setStartDelay(duration);
            slideAnimator.setFloatValues(1f, 1f);
        } else if (from > 0) {
            duration = 7 * ANIM_ITEM_DELAY;
            slideAnimator.setStartDelay(duration);
            slideAnimator.setFloatValues(from, 1f);
        } else {
            slideAnimator.setStartDelay(0);
            slideAnimator.setFloatValues(from, 1f);
        }
        /*long total = slideAnimator.getDuration() + slideAnimator.getStartDelay();
        long contentIn = 320;
        total += contentIn;
        Log.d("SlideAnimator", "IN " + fragment.getClass().getSimpleName() + ". ContentAnimatorSetIn = " + contentIn + ", slideAnimator = " + slideAnimator.getDuration() + ", slideDelay = " + slideAnimator.getStartDelay() + ", total is " + total + ", duration is " + duration);
        */

        slideAnimator.removeAllListeners();
        slideAnimator.addListener(slideDownListener);
        slideAnimator.start();
    }

    public void reverse(int toHeight) {
        // Only starts animation if there are no other animations playing
        if (animationStarted()) {
            return;
        }

        float to = (float) toHeight / (float) fragment.getView().getHeight();
        contentAnimatorSetOut.setStartDelay(0);
        contentAnimatorSetOut.removeAllListeners();
        if (to > 1) {
            contentAnimatorSetOut.addListener(animOutNoSlideListener);
        } else {
            contentAnimatorSetOut.addListener(animOutListener);
        }

        /*long total = slideAnimator.getDuration() + slideAnimator.getStartDelay();
        long contentOut = 320;
        total += contentOut;
        Log.d("SlideAnimator", "OUT " + fragment.getClass().getSimpleName() + ". ContentAnimatorSetOut = " + contentOut + ", slideAnimator = " + slideAnimator.getDuration() + ", slideDelay = " + slideAnimator.getStartDelay() + ", total is " + total);
        */

        // Plays the animation in reverse
        slideAnimator.setStartDelay(0);
        slideAnimator.setFloatValues(1f, to);
        contentAnimatorSetOut.start();
    }

    private void createItemAnimationIn() {
        // Items appearing two by two
        int totalChildren = 0;
        for (int i = 0; i < table.getChildCount(); i++) {
            totalChildren += ((TableRow) table.getChildAt(i)).getChildCount();
        }
        ObjectAnimator[] itemAnimators = new ObjectAnimator[totalChildren];

        int delay = 0;
        for (int row = 0; row < table.getChildCount(); row++) {
            TableRow tableRow = (TableRow) table.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 1f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 1f);

                // Makes items appear diagonally
                float speedUpFactor = 7f / (float) (((TableRow) table.getChildAt(row)).getChildCount() - 1);
                delay = (int) (col * ANIM_ITEM_DELAY * speedUpFactor + row * ANIM_ITEM_DELAY);
                ObjectAnimator itemAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                itemAnimator.setInterpolator(decelerate);
                itemAnimator.setStartDelay(delay);
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
            fragment.getView().setVisibility(View.VISIBLE);
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

    private Animator.AnimatorListener slideUpListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            slideAnimator.setInterpolator(accelerate);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            fragment.getView().setVisibility(View.INVISIBLE);
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

    private Animator.AnimatorListener animOutNoSlideListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            fragment.getView().setVisibility(View.INVISIBLE);
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
