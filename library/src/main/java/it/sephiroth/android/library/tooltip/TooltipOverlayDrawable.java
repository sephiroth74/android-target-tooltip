package it.sephiroth.android.library.tooltip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
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
    @SuppressWarnings("unused")
    public static final String TAG = TooltipOverlay.class.getSimpleName();
    private Paint mOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mOuterRadius;
    private float mInnerRadius = 0;
    private AnimatorSet mFirstAnimatorSet;
    private AnimatorSet mSecondAnimatorSet;
    private ValueAnimator mFirstAnimator;
    private ValueAnimator mSecondAnimator;
    private int mRepeatCount;
    private int mRepeatIndex;
    private boolean mStarted;
    private int mOuterAlpha;
    private int mInnerAlpha;
    private int mColorAlpha;
    private long mDuration;

    public TooltipOverlayDrawable(Context context, int defStyleResId) {

        final TypedArray array =
                context.obtainStyledAttributes(defStyleResId, R.styleable.TooltipOverlay);

        mOuterPaint.setStyle(Paint.Style.FILL);
        mOuterPaint.setColor(array.getColor(R.styleable.TooltipOverlay_android_color, Color.BLUE));

        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setColor(array.getColor(R.styleable.TooltipOverlay_android_color, Color.BLUE));

        mRepeatCount = array.getInt(R.styleable.TooltipOverlay_ttlm_repeatCount, 1);

        mColorAlpha = (int) (array.getFloat(R.styleable.TooltipOverlay_android_alpha, mInnerPaint.getAlpha() / 255f) * 255);
        mInnerPaint.setAlpha(mColorAlpha);
        mOuterPaint.setAlpha(mColorAlpha);

        mDuration = array.getInt(R.styleable.TooltipOverlay_ttlm_duration, 1000);
        array.recycle();

        mOuterAlpha = getOuterAlpha();
        mInnerAlpha = getInnerAlpha();

        // first
        Animator fadeIn = ObjectAnimator.ofInt(this, "outerAlpha", 0, mOuterAlpha);
        fadeIn.setDuration((long) (mDuration * 0.2));

        Animator fadeOut = ObjectAnimator.ofInt(this, "outerAlpha", mOuterAlpha, 0, 0);
        fadeOut.setStartDelay((long) (mDuration * 0.7));
        fadeOut.setDuration((long) (mDuration * 0.3));

        mFirstAnimator = ObjectAnimator.ofFloat(this, "outerRadius", 0, 1);
        mFirstAnimator.setDuration(mDuration);

        mFirstAnimatorSet = new AnimatorSet();
        mFirstAnimatorSet.playTogether(fadeIn, mFirstAnimator, fadeOut);
        mFirstAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mFirstAnimatorSet.setDuration(mDuration);

        // second
        fadeIn = ObjectAnimator.ofInt(this, "innerAlpha", 0, mInnerAlpha);
        fadeIn.setDuration((long) (mDuration * 0.2));

        fadeOut = ObjectAnimator.ofInt(this, "innerAlpha", mInnerAlpha, 0, 0);
        fadeOut.setStartDelay((long) (mDuration * 0.7));
        fadeOut.setDuration((long) (mDuration * 0.3));

        mSecondAnimator = ObjectAnimator.ofFloat(this, "innerRadius", 0, 1);
        mSecondAnimator.setDuration(mDuration);

        mSecondAnimatorSet = new AnimatorSet();
        mSecondAnimatorSet.playTogether(fadeIn, mSecondAnimator, fadeOut);
        mSecondAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        mSecondAnimatorSet.setStartDelay((long) (mDuration * 0.35));
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
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void play() {
        mRepeatIndex = 0;
        mStarted = true;
        mFirstAnimatorSet.start();
        mSecondAnimatorSet.setStartDelay((long) (mDuration * 0.35));
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

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mOuterRadius = Math.min(bounds.width(), bounds.height()) / 2;
        mFirstAnimator.setFloatValues(0, mOuterRadius);
        mSecondAnimator.setFloatValues(0, mOuterRadius);
    }

    @SuppressWarnings("unused")
    public float getInnerRadius() {
        return mInnerRadius;
    }

    @SuppressWarnings("unused")
    public void setInnerRadius(final float rippleRadius) {
        mInnerRadius = rippleRadius;
        invalidateSelf();
    }


    @SuppressWarnings("unused")
    public void setOuterRadius(final float value) {
        mOuterRadius = value;
        invalidateSelf();
    }

    @SuppressWarnings("unused")
    public float getOuterRadius() {
        return mOuterRadius;
    }

    @SuppressWarnings("unused")
    public void setOuterAlpha(final int value) {
        mOuterPaint.setAlpha(value);
        invalidateSelf();
    }

    public int getOuterAlpha() {
        return mOuterPaint.getAlpha();
    }

    @SuppressWarnings("unused")
    public void setInnerAlpha(final int value) {
        mInnerPaint.setAlpha(value);
        invalidateSelf();
    }

    public int getInnerAlpha() {
        return mInnerPaint.getAlpha();
    }

    @Override
    public int getIntrinsicWidth() {
        return 96;
    }

    @Override
    public int getIntrinsicHeight() {
        return 96;
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
}
