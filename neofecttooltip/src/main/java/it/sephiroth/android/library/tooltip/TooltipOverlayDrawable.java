package it.sephiroth.android.library.tooltip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by alessandro on 12/12/15.
 */
public class TooltipOverlayDrawable extends Drawable {
    @SuppressWarnings ("unused")
    public static final String TAG = TooltipOverlay.class.getSimpleName();
    public static final float ALPHA_MAX = 255f;
    public static final double FADEOUT_START_DELAY = 0.55;
    public static final double FADEIN_DURATION = 0.3;
    public static final double SECOND_ANIM_START_DELAY = 0.25;
    private Paint mOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mOuterRadius;
    private float mInnerRadius = 0;
    private AnimatorSet mFirstAnimatorSet;
    private AnimatorSet mSecondAnimatorSet;
    private ValueAnimator mFirstAnimator;
    private ValueAnimator mSecondAnimator;
    private int mRepeatIndex;
    private boolean mStarted;
    private int mOuterAlpha;
    private int mInnerAlpha;
    private int mRepeatCount = 1;
    private long mDuration = 400;

    public TooltipOverlayDrawable(Context context, int defStyleResId) {
        mOuterPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setStyle(Paint.Style.FILL);

        final TypedArray array =
            context.getTheme().obtainStyledAttributes(defStyleResId, R.styleable.TooltipOverlay);

        for (int i = 0; i < array.getIndexCount(); i++) {
            int index = array.getIndex(i);

            if (index == R.styleable.TooltipOverlay_android_color) {
                int color = array.getColor(index, 0);
                mOuterPaint.setColor(color);
                mInnerPaint.setColor(color);

            } else if (index == R.styleable.TooltipOverlay_ttlm_repeatCount) {
                mRepeatCount = array.getInt(index, 1);

            } else if (index == R.styleable.TooltipOverlay_android_alpha) {
                int alpha = (int) (array.getFloat(index, mInnerPaint.getAlpha() / ALPHA_MAX) * 255);
                mInnerPaint.setAlpha(alpha);
                mOuterPaint.setAlpha(alpha);

            } else if (index == R.styleable.TooltipOverlay_ttlm_duration) {
                mDuration = array.getInt(index, 400);
            }
        }

        array.recycle();

        mOuterAlpha = getOuterAlpha();
        mInnerAlpha = getInnerAlpha();

        // first
        Animator fadeIn = ObjectAnimator.ofInt(this, "outerAlpha", 0, mOuterAlpha);
        fadeIn.setDuration((long) (mDuration * FADEIN_DURATION));

        Animator fadeOut = ObjectAnimator.ofInt(this, "outerAlpha", mOuterAlpha, 0, 0);
        fadeOut.setStartDelay((long) (mDuration * FADEOUT_START_DELAY));
        fadeOut.setDuration((long) (mDuration * (1.0 - FADEOUT_START_DELAY)));

        mFirstAnimator = ObjectAnimator.ofFloat(this, "outerRadius", 0, 1);
        mFirstAnimator.setDuration(mDuration);

        mFirstAnimatorSet = new AnimatorSet();
        mFirstAnimatorSet.playTogether(fadeIn, mFirstAnimator, fadeOut);
        mFirstAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mFirstAnimatorSet.setDuration(mDuration);

        // second
        fadeIn = ObjectAnimator.ofInt(this, "innerAlpha", 0, mInnerAlpha);
        fadeIn.setDuration((long) (mDuration * FADEIN_DURATION));

        fadeOut = ObjectAnimator.ofInt(this, "innerAlpha", mInnerAlpha, 0, 0);
        fadeOut.setStartDelay((long) (mDuration * FADEOUT_START_DELAY));
        fadeOut.setDuration((long) (mDuration * (1.0 - FADEOUT_START_DELAY)));

        mSecondAnimator = ObjectAnimator.ofFloat(this, "innerRadius", 0, 1);
        mSecondAnimator.setDuration(mDuration);

        mSecondAnimatorSet = new AnimatorSet();
        mSecondAnimatorSet.playTogether(fadeIn, mSecondAnimator, fadeOut);
        mSecondAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mSecondAnimatorSet.setStartDelay((long) (mDuration * SECOND_ANIM_START_DELAY));
        mSecondAnimatorSet.setDuration(mDuration);

        mFirstAnimatorSet.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled && isVisible() && ++mRepeatIndex < mRepeatCount) {
                    mFirstAnimatorSet.start();
                }
            }
        });

        mSecondAnimatorSet.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled && isVisible() && mRepeatIndex < mRepeatCount) {
                    mSecondAnimatorSet.setStartDelay(0);
                    mSecondAnimatorSet.start();
                }
            }
        });

    }

    public int getOuterAlpha() {
        return mOuterPaint.getAlpha();
    }

    @SuppressWarnings ("unused")
    public void setOuterAlpha(final int value) {
        mOuterPaint.setAlpha(value);
        invalidateSelf();
    }

    public int getInnerAlpha() {
        return mInnerPaint.getAlpha();
    }

    @SuppressWarnings ("unused")
    public void setInnerAlpha(final int value) {
        mInnerPaint.setAlpha(value);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int centerX = bounds.width() / 2;
        int centerY = bounds.height() / 2;
        canvas.drawCircle(centerX, centerY, mOuterRadius, mOuterPaint);
        canvas.drawCircle(centerX, centerY, mInnerRadius, mInnerPaint);

    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = isVisible() != visible;

        if (visible) {
            if (restart || !mStarted) {
                replay();
            }
        } else {
            stop();
        }

        return changed;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mOuterRadius = Math.min(bounds.width(), bounds.height()) / 2;
        mFirstAnimator.setFloatValues(0, mOuterRadius);
        mSecondAnimator.setFloatValues(0, mOuterRadius);
    }

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
    }

    public void play() {
        mRepeatIndex = 0;
        mStarted = true;
        mFirstAnimatorSet.start();
        mSecondAnimatorSet.setStartDelay((long) (mDuration * SECOND_ANIM_START_DELAY));
        mSecondAnimatorSet.start();
    }

    public void replay() {
        stop();
        play();
    }

    public void stop() {
        mFirstAnimatorSet.cancel();
        mSecondAnimatorSet.cancel();

        mRepeatIndex = 0;
        mStarted = false;

        setInnerRadius(0);
        setOuterRadius(0);
    }

    @SuppressWarnings ("unused")
    public float getInnerRadius() {
        return mInnerRadius;
    }

    @SuppressWarnings ("unused")
    public void setInnerRadius(final float rippleRadius) {
        mInnerRadius = rippleRadius;
        invalidateSelf();
    }

    @SuppressWarnings ("unused")
    public float getOuterRadius() {
        return mOuterRadius;
    }

    @SuppressWarnings ("unused")
    public void setOuterRadius(final float value) {
        mOuterRadius = value;
        invalidateSelf();
    }
}
