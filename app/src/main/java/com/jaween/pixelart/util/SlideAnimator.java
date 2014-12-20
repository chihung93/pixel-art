package com.jaween.pixelart.util;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TableLayout;
import android.widget.TableRow;

/**
 * Created by ween on 12/13/14.
 */
public class SlideAnimator {

    // Animation constants
    private static final int ANIM_SLIDE_DURATION = 160;
    private static final int ANIM_ITEM_DURATION = 100;
    private static final int ANIM_ITEM_DELAY = 30;
    private static final int ANIM_BAR_FADE_DURATION = 160;
    private static final int ANIM_BAR_FADE_IN_DELAY = 240;

    // Views
    private static final int NORM_ROW_COUNT = 8;
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

        setupAnimations();
    }

    /** Returns true if either a panel is sliding or the content in fading in/out. **/
    public boolean animationStarted() {
        return slideAnimator.isRunning() ||
                contentAnimatorSetIn.isRunning() ||
                contentAnimatorSetOut.isRunning();
    }

    /** Allocates memory for Animators and AnimatorSets prior to running the animations. **/
    private void setupAnimations() {
        // Panel sliding animation
        slideAnimator = ObjectAnimator.ofFloat(container, "yFraction", 0, 1f);
        slideAnimator.setDuration(ANIM_SLIDE_DURATION);

        createContentInAnimation();
        createContentOutAnimation();
    }

    /**
     * Starts the 'forward' animation. This can be a slide followed by a fade animation, or simply
     * just a fade animation. If the from height is less than 100% of this panel's height, both the
     * slide followed by the fade in occur. However if the slide is equal to or greater than 100%
     * only the fade in occurs. This is because the panel is of a set height and sliding from
     * greater than its height would cause a tearing effect (the background would show through above
     * the panel). The other panel is taller and will slide *down* to this panels height in that
     * case.
     * @param fromHeightPixels The height that the animation visually should begin at.
     */
    public void start(int fromHeightPixels) {
        // Only starts animation if there are no other animations playing
        if (animationStarted()) {
            return;
        }

        // Makes all the items invisible to begin with (scale of 0)
        for (int j = 0; j < table.getChildCount(); j++) {
            TableRow tableRow = (TableRow) table.getChildAt(j);
            for (int i = 0; i < tableRow.getChildCount(); i++) {
                View view = tableRow.getChildAt(i);
                view.setScaleX(0);
                view.setScaleY(0);
            }
        }

        // Decides which type of panel transition to use
        float normalisedFromHeight = (float) fromHeightPixels / fragment.getView().getHeight();
        if (normalisedFromHeight > 1) {
            // Waits until the other panel fades its content out and also slides down to this panel
            int duration = NORM_ROW_COUNT * ANIM_ITEM_DELAY + ANIM_ITEM_DURATION + ANIM_SLIDE_DURATION;
            slideAnimator.setStartDelay(duration);
            slideAnimator.setFloatValues(1f, 1f);
        } else if (normalisedFromHeight > 0) {
            // Waits until the other panel fades its content out
            // (We should use NORM_ROW_COUNT without the minus one, however occasionally it seem to
            // wait too long and the panel goes invisible *before* the new panel is made visible,
            // showing the background for a fraction of a second)
            int duration = (NORM_ROW_COUNT - 1) * ANIM_ITEM_DELAY + ANIM_ITEM_DURATION;
            slideAnimator.setStartDelay(duration);
            slideAnimator.setFloatValues(normalisedFromHeight, 1f);
        } else {
            // There are no other panels visible, no delay
            slideAnimator.setStartDelay(0);
            slideAnimator.setFloatValues(normalisedFromHeight, 1f);
        }

        // We reuse this animator for both up and down slides, so we must remove all other listeners
        slideAnimator.removeAllListeners();
        slideAnimator.addListener(panelForwardListener);
        slideAnimator.setInterpolator(decelerate);

        // Finally we begin the chosen animation style
        slideAnimator.start();
    }

    /**
     * Starts the 'reverse' animation. Similar to the 'forward' animation, the 'reverse' animation
     * has multiple styles. The panels first fades out the content, then either goes immediately
     * invisible or slides up. The invisible style is used when the new panel is larger than this
     * panel and so this panel will be obscured. The slide animation is used when there is no new
     * panel to be shown or the new panel is smaller than this panel.
     * @param toHeightPixels The height that the animation visually should begin at.
     */
    public void reverse(int toHeightPixels) {
        // Only starts animation if there are no other animations playing
        if (animationStarted()) {
            return;
        }

        // Decides which type of panel transition to use
        float normalisedToHeight = (float) toHeightPixels / fragment.getView().getHeight();
        contentAnimatorSetOut.setStartDelay(0);
        contentAnimatorSetOut.removeAllListeners();
        if (normalisedToHeight > 1) {
            // Hides the panel immediately once the content is gone
            contentAnimatorSetOut.addListener(panelRemoveListener);
        } else {
            // Slides the panel and then hides it once the content is gone
            contentAnimatorSetOut.addListener(panelReverseListener);
        }

        // Plays the animation in reverse
        slideAnimator.setStartDelay(0);
        slideAnimator.setFloatValues(1f, normalisedToHeight);
        slideAnimator.setInterpolator(accelerate);
        contentAnimatorSetOut.start();
    }

    private void createContentInAnimation() {
        // Counts the number of child items in the table
        int totalChildren = 0;
        for (int i = 0; i < table.getChildCount(); i++) {
            totalChildren += ((TableRow) table.getChildAt(i)).getChildCount();
        }
        ObjectAnimator[] itemAnimators = new ObjectAnimator[totalChildren];

        // Sets delays on the children to stagger their animations from left to right and diagonally
        for (int row = 0; row < table.getChildCount(); row++) {
            TableRow tableRow = (TableRow) table.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                // Children expand into existence
                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 1f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 1f);

                // Even though the panels each have a different number of children, we scale the
                // animation duration of each child to make the durations identical
                float speedUpFactor = (float) (NORM_ROW_COUNT - 1) /
                        (((TableRow) table.getChildAt(row)).getChildCount() - 1);
                int delay = (int) (col * ANIM_ITEM_DELAY * speedUpFactor + row * ANIM_ITEM_DELAY);

                // Creates the child animations and adds them to a set
                ObjectAnimator itemAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                itemAnimator.setInterpolator(decelerate);
                itemAnimator.setStartDelay(delay);
                itemAnimator.setDuration(ANIM_ITEM_DURATION);
                itemAnimators[row * tableRow.getChildCount() + col] = itemAnimator;
            }
        }

        // Plays the table children creation animation with the bar fading animation
        AnimatorSet itemAnimatorSet = new AnimatorSet();
        itemAnimatorSet.playTogether(itemAnimators);
        ObjectAnimator barAnimator = ObjectAnimator.ofFloat(bar, "alpha", 0f, 1f);
        barAnimator.setDuration(ANIM_BAR_FADE_DURATION);
        barAnimator.setStartDelay(ANIM_BAR_FADE_IN_DELAY);
        contentAnimatorSetIn.playTogether(itemAnimatorSet, barAnimator);
        contentAnimatorSetIn.setInterpolator(decelerate);
    }

    private void createContentOutAnimation() {
        // Counts the number of child items in the table
        int totalChildren = 0;
        for (int i = 0; i < table.getChildCount(); i++) {
            totalChildren += ((TableRow) table.getChildAt(i)).getChildCount();
        }
        ObjectAnimator[] itemAnimators = new ObjectAnimator[totalChildren];

        // Sets delays on the children to stagger their animations from left to right and diagonally
        for (int row = 0; row < table.getChildCount(); row++) {
            TableRow tableRow = (TableRow) table.getChildAt(row);
            for (int col = 0; col < tableRow.getChildCount(); col++) {
                final View view = tableRow.getChildAt(col);

                // Children shrink into inanimateness
                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 0f);
                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 0f);

                // Even though the panels each have a different number of children, we scale the
                // animation duration of each child to make the durations identical
                float speedUpFactor = (float) (NORM_ROW_COUNT - 1) /
                        (((TableRow) table.getChildAt(row)).getChildCount() - 1);
                int delay = (int) (col * ANIM_ITEM_DELAY * speedUpFactor + row * ANIM_ITEM_DELAY);

                // Creates the child animations and adds them to a set
                ObjectAnimator itemAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhSX, pvhSY);
                itemAnimator.setInterpolator(accelerate);
                itemAnimator.setStartDelay(delay);
                itemAnimator.setDuration(ANIM_ITEM_DURATION);
                itemAnimators[row * tableRow.getChildCount() + col] = itemAnimator;
            }
        }

        // Plays the table children destruction animation with the bar fading animation
        AnimatorSet itemAnimatorSet = new AnimatorSet();
        itemAnimatorSet.playTogether(itemAnimators);
        ObjectAnimator barAnimator = ObjectAnimator.ofFloat(bar, "alpha", 1f, 0f);
        barAnimator.setDuration(ANIM_BAR_FADE_DURATION);
        contentAnimatorSetOut.playTogether(barAnimator, itemAnimatorSet);
        contentAnimatorSetOut.setInterpolator(accelerate);
    }

    /** Listener to be used for a panel's forward animation. **/
    private Animator.AnimatorListener panelForwardListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            // The panel's content is invisible while the panel slides down
            fragment.getView().setVisibility(View.VISIBLE);
            table.setVisibility(View.INVISIBLE);
            bar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            // Once the panel slides down the content animates in
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

    /** Listener to be used for a panel's reverse animation. **/
    private Animator.AnimatorListener panelReverseListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            // The panel slides up once its content has disappeared
            slideAnimator.removeAllListeners();
            slideAnimator.addListener(panelRemoveListener);
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

    /** Listener to be used to remove a panel once it has completed its reverse animation. **/
    private Animator.AnimatorListener panelRemoveListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animator) {
            // No implementation
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            // One the the panel is in its new position, it must become invisible if it's obscuring
            // a panel below
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
