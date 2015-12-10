package it.sephiroth.android.library.tooltip;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static it.sephiroth.android.library.tooltip.TooltipManager.ClosePolicy;
import static it.sephiroth.android.library.tooltip.TooltipManager.DBG;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.BOTTOM;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.CENTER;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.LEFT;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.RIGHT;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.TOP;
import static it.sephiroth.android.library.tooltip.TooltipManager.log;

class TooltipView extends ViewGroup implements Tooltip {
    private static final String TAG = "TooltipView";
    private static final List<Gravity> gravities = new ArrayList<>(Arrays.asList(LEFT, RIGHT, TOP, BOTTOM, CENTER));
    private final List<Gravity> viewGravities = new ArrayList<>(gravities);
    private final long mShowDelay;
    private final int mTextAppearance;
    private final int mToolTipId;
    private final Rect mDrawRect;
    private final Rect mTempRect;
    private final long mShowDuration;
    private final ClosePolicy mClosePolicy;
    private final Point mPoint;
    private final int mTextResId;
    private final int mTopRule;
    private final int mMaxWidth;
    private final boolean mHideArrow;
    private final long mActivateDelay;
    private final boolean mRestrict;
    private final long mFadeDuration;
    private final TooltipManager.onTooltipClosingCallback mCloseCallback;
    private final ATooltipTextDrawable mDrawable;
    private final int[] mTempLocation = new int[2];
    private final Handler mHandler = new Handler();
    private final Rect mScreenRect = new Rect();
    private final Point mTmpPoint = new Point();
    private Gravity mGravity;
    private int mGlobalLayoutCount = 0;
    private Animator mShowAnimation;
    private boolean mShowing;
    private WeakReference<View> mViewAnchor;
    private boolean mAttached;
    private boolean mInitialized;
    private boolean mActivated;
    private int mPadding;
    private CharSequence mText;
    private Rect mViewRect;
    private View mView;
    private TextView mTextView;
    private OnToolTipListener mTooltipListener;
    Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            onClose(false, false, false);
        }
    };
    Runnable activateRunnable = new Runnable() {
        @Override
        public void run() {
            log(TAG, VERBOSE, "activated..");

            mActivated = true;
        }
    };
    private final ViewTreeObserver.OnPreDrawListener mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (!mAttached) {
                log(TAG, WARN, "onPreDraw. not attached");
                removePreDrawObserver(null);
                return true;
            }

            if (null != mViewAnchor) {
                View view = mViewAnchor.get();
                if (null != view) {
                    view.getLocationOnScreen(mTempLocation);

                    if (mTempLocation[0] != mViewRect.left) {
                        setOffsetX(mTempLocation[0]);
                    }

                    if (mTempLocation[0] != mViewRect.top) {
                        setOffsetY(mTempLocation[1]);
                    }
                }
            }
            return true;
        }
    };
    private final ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (!mAttached) {
                log(TAG, WARN, "onGlobalLayout. removeListeners");
                removeGlobalLayoutObserver(null);
                return;
            }

            log(TAG, INFO, "onGlobalLayout");

            if (null != mViewAnchor) {
                View view = mViewAnchor.get();

                if (null != view) {
                    Rect rect = new Rect();
                    view.getGlobalVisibleRect(rect);

                    if (DBG) {
                        log(TAG, VERBOSE, "mViewRect: %s, newRect: %s, equals: %b", mViewRect, rect, mViewRect.equals(rect));
                    }

                    if (!mViewRect.equals(rect)) {
                        mViewRect.set(rect);
                        viewGravities.clear();
                        viewGravities.addAll(gravities);
                        viewGravities.remove(mGravity);
                        viewGravities.add(0, mGravity);
                        calculatePositions(viewGravities, ++mGlobalLayoutCount <= 1 && mRestrict);
                        requestLayout();
                    }
                } else {
                    log(TAG, WARN, "view is null");
                }
            }
        }
    };
    private final View.OnAttachStateChangeListener mAttachedStateListener = new OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(final View v) {
            // setVisibility(VISIBLE);
        }

        @Override
        @TargetApi (17)
        public void onViewDetachedFromWindow(final View v) {
            log(TAG, INFO, "onViewDetachedFromWindow");
            removeViewListeners(v);

            if (!mAttached) {
                log(TAG, WARN, "not attached");
                return;
            }

            Activity activity = (Activity) getContext();
            if (null != activity) {
                if (activity.isFinishing()) {
                    log(TAG, WARN, "skipped because activity is finishing...");
                    return;
                }
                if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) {
                    return;
                }
                onClose(false, false, true);
            }
        }
    };

    public TooltipView(Context context, final TooltipManager manager, final TooltipManager.Builder builder) {
        super(context);

        TypedArray theme =
            context.getTheme().obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes);
        this.mPadding = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_padding, 30);
        this.mTextAppearance = theme.getResourceId(R.styleable.TooltipLayout_android_textAppearance, 0);
        theme.recycle();

        this.mToolTipId = builder.id;
        this.mText = builder.text;
        this.mGravity = builder.gravity;
        this.mTextResId = builder.textResId;
        this.mMaxWidth = builder.maxWidth;
        this.mTopRule = builder.actionbarSize;
        this.mClosePolicy = builder.closePolicy;
        this.mShowDuration = builder.showDuration;
        this.mShowDelay = builder.showDelay;
        this.mHideArrow = builder.hideArrow;
        this.mActivateDelay = builder.activateDelay;
        this.mRestrict = builder.restrictToScreenEdges;
        this.mFadeDuration = builder.fadeDuration;
        this.mCloseCallback = builder.closeCallback;

        if (null != builder.point) {
            this.mPoint = new Point(builder.point);
            this.mPoint.y += mTopRule;
        } else {
            this.mPoint = null;
        }

        this.mDrawRect = new Rect();
        this.mTempRect = new Rect();

        if (null != builder.view) {
            mViewRect = new Rect();
            builder.view.getGlobalVisibleRect(mViewRect);
            mViewAnchor = new WeakReference<>(builder.view);

            if (builder.view.getViewTreeObserver().isAlive()) {
                builder.view.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
                builder.view.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
                builder.view.addOnAttachStateChangeListener(mAttachedStateListener);
            }
        }

        if (!builder.isCustomView) {
            this.mDrawable = new ATooltipTextDrawable(context, builder);
        } else {
            this.mDrawable = null;
        }
        setVisibility(INVISIBLE);
    }

    @Override
    public int getTooltipId() {
        return mToolTipId;
    }

    @SuppressWarnings ("unused")
    public boolean isShowing() {
        return mShowing;
    }

    void removeFromParent() {
        log(TAG, INFO, "removeFromParent: %d", mToolTipId);
        ViewParent parent = getParent();
        removeCallbacks();

        if (null != parent) {
            ((ViewGroup) parent).removeView(TooltipView.this);

            if (null != mShowAnimation && mShowAnimation.isStarted()) {
                mShowAnimation.cancel();
            }
        }
    }

    private void removeCallbacks() {
        mHandler.removeCallbacks(hideRunnable);
        mHandler.removeCallbacks(activateRunnable);
    }

    @Override
    protected void onAttachedToWindow() {
        log(TAG, INFO, "onAttachedToWindow");
        super.onAttachedToWindow();
        mAttached = true;

        final Activity act = TooltipManager.getActivity(getContext());
        if (act != null) {
            Window window = act.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(mScreenRect);
        } else {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            android.view.Display display = wm.getDefaultDisplay();
            display.getRectSize(mScreenRect);
        }

        initializeView();
        show();
    }

    @Override
    protected void onDetachedFromWindow() {
        log(TAG, INFO, "onDetachedFromWindow");
        removeListeners();
        mAttached = false;
        mViewAnchor = null;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        log(TAG, INFO, "onLayout(%b, %d, %d, %d, %d)", changed, l, t, r, b);

        //  The layout has actually already been performed and the positions
        //  cached.  Apply the cached values to the children.
        if (null != mView) {
            mView.layout(mView.getLeft(), mView.getTop(), mView.getMeasuredWidth(), mView.getMeasuredHeight());
        }

        if (changed) {
            viewGravities.clear();
            viewGravities.addAll(gravities);
            viewGravities.remove(mGravity);
            viewGravities.add(0, mGravity);
            calculatePositions(viewGravities, mRestrict);
        }
    }

    private void removeListeners() {
        mTooltipListener = null;

        if (null != mViewAnchor) {
            View view = mViewAnchor.get();
            removeViewListeners(view);
        }
    }

    private void removeViewListeners(final View view) {
        log(TAG, INFO, "removeListeners");
        removeGlobalLayoutObserver(view);
        removePreDrawObserver(view);
        removeOnAttachStateObserver(view);
    }

    private void removeGlobalLayoutObserver(@Nullable View view) {
        if (null == view && null != mViewAnchor) {
            view = mViewAnchor.get();
        }
        if (null != view && view.getViewTreeObserver().isAlive()) {
            if (Build.VERSION.SDK_INT >= 16) {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(mGlobalLayoutListener);
            } else {
                view.getViewTreeObserver().removeGlobalOnLayoutListener(mGlobalLayoutListener);
            }
        } else {
            log(TAG, ERROR, "removeGlobalLayoutObserver failed");
        }
    }

    private void removePreDrawObserver(@Nullable View view) {
        if (null == view && null != mViewAnchor) {
            view = mViewAnchor.get();
        }
        if (null != view && view.getViewTreeObserver().isAlive()) {
            view.getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
        } else {
            log(TAG, ERROR, "removePreDrawObserver failed");
        }
    }

    private void removeOnAttachStateObserver(@Nullable View view) {
        if (null == view && null != mViewAnchor) {
            view = mViewAnchor.get();
        }
        if (null != view) {
            view.removeOnAttachStateChangeListener(mAttachedStateListener);
        } else {
            log(TAG, ERROR, "removeOnAttachStateObserver failed");
        }
    }

    private void initializeView() {
        if (!isAttached() || mInitialized) {
            return;
        }
        mInitialized = true;

        log(TAG, VERBOSE, "initializeView");

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mView = LayoutInflater.from(getContext()).inflate(mTextResId, this, false);
        mView.setLayoutParams(params);

        if (null != mDrawable) {
            mView.setBackgroundDrawable(mDrawable);
            if (mHideArrow) {
                mView.setPadding(mPadding / 2, mPadding / 2, mPadding / 2, mPadding / 2);
            } else {
                mView.setPadding(mPadding, mPadding, mPadding, mPadding);
            }
        }

        mTextView = (TextView) mView.findViewById(android.R.id.text1);
        mTextView.setText(Html.fromHtml((String) this.mText));
        if (mMaxWidth > -1) {
            mTextView.setMaxWidth(mMaxWidth);
        }

        if (0 != mTextAppearance) {
            mTextView.setTextAppearance(getContext(), mTextAppearance);
        }

        this.addView(mView);
    }

    @Override
    public void show() {
        log(TAG, INFO, "show");
        if (!isAttached()) {
            log(TAG, ERROR, "not attached!");
            return;
        }
        fadeIn(mFadeDuration);
    }

    @Override
    public void hide(boolean remove) {
        hide(remove, mFadeDuration);
    }

    private void hide(boolean remove, long fadeDuration) {
        log(TAG, INFO, "hide(%b, %d)", remove, fadeDuration);

        if (!isAttached()) {
            return;
        }
        fadeOut(remove, fadeDuration);
    }

    protected void fadeOut(final boolean remove, long fadeDuration) {
        if (!isAttached() || !mShowing) {
            return;
        }

        log(TAG, INFO, "fadeOut(%b, %d)", remove, fadeDuration);

        if (null != mShowAnimation) {
            mShowAnimation.cancel();
        }

        mShowing = false;

        if (fadeDuration > 0) {
            float alpha = getAlpha();
            mShowAnimation = ObjectAnimator.ofFloat(this, "alpha", alpha, 0);
            mShowAnimation.setDuration(fadeDuration);
            mShowAnimation.addListener(
                new Animator.AnimatorListener() {
                    boolean cancelled;

                    @Override
                    public void onAnimationStart(final Animator animation) {
                        cancelled = false;
                    }

                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        log(TAG, VERBOSE, "fadeout::onAnimationEnd, cancelled: %b", cancelled);
                        if (cancelled) {
                            return;
                        }

                        if (remove) {
                            fireOnHideCompleted();
                        }
                        mShowAnimation = null;
                    }

                    @Override
                    public void onAnimationCancel(final Animator animation) {
                        log(TAG, VERBOSE, "fadeout::onAnimationCancel");
                        cancelled = true;
                    }

                    @Override
                    public void onAnimationRepeat(final Animator animation) {

                    }
                });
            mShowAnimation.start();
        } else {
            setVisibility(View.INVISIBLE);
            if (remove) {
                fireOnHideCompleted();
            }
        }
    }

    private void fireOnHideCompleted() {
        if (null != mTooltipListener) {
            mTooltipListener.onHideCompleted(TooltipView.this);
        }
    }

    @Override
    public void setOffsetX(int x) {
        mView.setTranslationY(x - mViewRect.left + mDrawRect.left);
    }

    @Override
    public void setOffsetY(int y) {
        mView.setTranslationY(y - mViewRect.top + mDrawRect.top);
    }

    @Override
    public void offsetTo(final int x, final int y) {
        mView.setTranslationX(x - mViewRect.left + mDrawRect.left);
        mView.setTranslationY(y - mViewRect.top + mDrawRect.top);
    }

    @Override
    public boolean isAttached() {
        return mAttached;
    }

    protected void fadeIn(final long fadeDuration) {
        if (mShowing) {
            return;
        }

        if (null != mShowAnimation) {
            mShowAnimation.cancel();
        }

        log(TAG, INFO, "fadeIn");

        mShowing = true;

        if (fadeDuration > 0) {
            mShowAnimation = ObjectAnimator.ofFloat(this, "alpha", 0, 1);
            mShowAnimation.setDuration(fadeDuration);
            if (this.mShowDelay > 0) {
                mShowAnimation.setStartDelay(this.mShowDelay);
            }
            mShowAnimation.addListener(
                new Animator.AnimatorListener() {
                    boolean cancelled;

                    @Override
                    public void onAnimationStart(final Animator animation) {
                        setVisibility(View.VISIBLE);
                        cancelled = false;
                    }

                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        log(TAG, VERBOSE, "fadein::onAnimationEnd, cancelled: %b", cancelled);

                        if (null != mTooltipListener && !cancelled) {
                            mTooltipListener.onShowCompleted(TooltipView.this);
                            postActivate(mActivateDelay);
                        }
                    }

                    @Override
                    public void onAnimationCancel(final Animator animation) {
                        log(TAG, VERBOSE, "fadein::onAnimationCancel");
                        cancelled = true;
                    }

                    @Override
                    public void onAnimationRepeat(final Animator animation) {

                    }
                });
            mShowAnimation.start();
        } else {
            setVisibility(View.VISIBLE);
            mTooltipListener.onShowCompleted(TooltipView.this);
            if (!mActivated) {
                postActivate(mActivateDelay);
            }
        }

        if (mShowDuration > 0) {
            mHandler.removeCallbacks(hideRunnable);
            mHandler.postDelayed(hideRunnable, mShowDuration);
        }
    }

    void postActivate(long ms) {
        log(TAG, VERBOSE, "postActivate: %d", ms);
        if (ms > 0) {
            if (isAttached()) {
                mHandler.postDelayed(activateRunnable, ms);
            }
        } else {
            mActivated = true;
        }
    }

    private void calculatePositions(List<Gravity> gravities, final boolean checkEdges) {
        final long t1 = System.currentTimeMillis();
        if (!isAttached()) {
            return;
        }

        // failed to display the tooltip due to
        // something wrong with its dimensions or
        // the target position..
        if (gravities.size() < 1) {
            if (null != mTooltipListener) {
                mTooltipListener.onShowFailed(this);
            }
            setVisibility(View.GONE);
            return;
        }

        Gravity gravity = gravities.remove(0);

        log(TAG, INFO, "calculatePositions. mGravity: %s, gravities: %d, mRestrict: %b", gravity, gravities.size(), checkEdges);

        int statusbarHeight = mScreenRect.top;

        if (mViewRect == null) {
            mViewRect = new Rect();
            mViewRect.set(mPoint.x, mPoint.y + statusbarHeight, mPoint.x, mPoint.y + statusbarHeight);
        }

        mScreenRect.top += mTopRule;

        int width = mView.getWidth();
        int height = mView.getHeight();

        log(TAG, VERBOSE, "mView.size: %dx%d", width, height);

        // get the destination mPoint

        if (gravity == BOTTOM) {
            mDrawRect.set(
                mViewRect.centerX() - width / 2,
                mViewRect.bottom,
                mViewRect.centerX() + width / 2,
                mViewRect.bottom + height);

            mTmpPoint.x = mViewRect.centerX();
            mTmpPoint.y = mViewRect.bottom;

            if (mRestrict && !mScreenRect.contains(mDrawRect)) {
                if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0);
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(-mDrawRect.left, 0);
                }
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    // this means there's no enough space!
                    calculatePositions(gravities, checkEdges);
                    return;
                } else if (mDrawRect.top < mScreenRect.top) {
                    mDrawRect.offset(0, mScreenRect.top - mDrawRect.top);
                }
            }
        } else if (gravity == TOP) {
            mDrawRect.set(
                mViewRect.centerX() - width / 2,
                mViewRect.top - height,
                mViewRect.centerX() + width / 2,
                mViewRect.top);

            mTmpPoint.x = mViewRect.centerX();
            mTmpPoint.y = mViewRect.top;

            if (mRestrict && !mScreenRect.contains(mDrawRect)) {
                if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0);
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(-mDrawRect.left, 0);
                }
                if (mDrawRect.top < mScreenRect.top) {
                    // this means there's no enough space!
                    calculatePositions(gravities, checkEdges);
                    return;
                } else if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom);
                }
            }
        } else if (gravity == RIGHT) {
            mDrawRect.set(
                mViewRect.right,
                mViewRect.centerY() - height / 2,
                mViewRect.right + width,
                mViewRect.centerY() + height / 2);

            mTmpPoint.x = mViewRect.right;
            mTmpPoint.y = mViewRect.centerY();

            if (mRestrict && !mScreenRect.contains(mDrawRect)) {
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom);
                } else if (mDrawRect.top < mScreenRect.top) {
                    mDrawRect.offset(0, mScreenRect.top - mDrawRect.top);
                }
                if (mDrawRect.right > mScreenRect.right) {
                    // this means there's no enough space!
                    calculatePositions(gravities, checkEdges);
                    return;
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(mScreenRect.left - mDrawRect.left, 0);
                }
            }
        } else if (gravity == LEFT) {
            mDrawRect.set(
                mViewRect.left - width,
                mViewRect.centerY() - height / 2,
                mViewRect.left,
                mViewRect.centerY() + height / 2);

            mTmpPoint.x = mViewRect.left;
            mTmpPoint.y = mViewRect.centerY();

            if (mRestrict && !mScreenRect.contains(mDrawRect)) {
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom);
                } else if (mDrawRect.top < mScreenRect.top) {
                    mDrawRect.offset(0, mScreenRect.top - mDrawRect.top);
                }
                if (mDrawRect.left < mScreenRect.left) {
                    // this means there's no enough space!
                    this.mGravity = RIGHT;
                    calculatePositions(gravities, checkEdges);
                    return;
                } else if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0);
                }
            }
        } else if (this.mGravity == CENTER) {
            mDrawRect.set(
                mViewRect.centerX() - width / 2,
                mViewRect.centerY() - height / 2,
                mViewRect.centerX() + width / 2,
                mViewRect.centerY() + height / 2);

            mTmpPoint.x = mViewRect.centerX();
            mTmpPoint.y = mViewRect.centerY();

            if (mRestrict && !mScreenRect.contains(mDrawRect)) {
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom);
                } else if (mDrawRect.top < mScreenRect.top) {
                    mDrawRect.offset(0, mScreenRect.top - mDrawRect.top);
                }
                if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0);
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(mScreenRect.left - mDrawRect.left, 0);
                }
            }
        }

        if (DBG) {
            log(TAG, VERBOSE, "mScreenRect: %s, mTopRule: %d, statusBar: %d", mScreenRect, mTopRule, statusbarHeight);
            log(TAG, VERBOSE, "mDrawRect: %s", mDrawRect);
            log(TAG, VERBOSE, "mViewRect: %s", mViewRect);
        }

        // translate the textview

        mView.setTranslationX(mDrawRect.left);
        mView.setTranslationY(mDrawRect.top);

        log(TAG, VERBOSE, "setTranslationY: %g", mView.getTranslationY());

        if (null != mDrawable) {
            // get the global rect for the textview
            mView.getGlobalVisibleRect(mTempRect);

            log(TAG, VERBOSE, "mView visible rect: %s", mTempRect);

            mTmpPoint.x -= mTempRect.left;
            mTmpPoint.y -= mTempRect.top;

            if (!mHideArrow) {
                if (gravity == LEFT || gravity == RIGHT) {
                    mTmpPoint.y -= mPadding / 2;
                } else if (gravity == TOP || gravity == BOTTOM) {
                    mTmpPoint.x -= mPadding / 2;
                }
            }
            mDrawable.setAnchor(gravity, mHideArrow ? 0 : mPadding / 2, mHideArrow ? null : mTmpPoint);
        }

        if (DBG) {
            final long t2 = System.currentTimeMillis();
            log(TAG, WARN, "calculate time: %d", (t2 - t1));
        }
    }

    void setText(final CharSequence text) {
        this.mText = text;
        if (null != mTextView) {
            mTextView.setText(Html.fromHtml((String) text));
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        if (!mAttached || !mShowing || !isShown()) {
            return false;
        }

        final int action = event.getActionMasked();

        log(TAG, INFO, "onTouchEvent: %d, active: %b", action, mActivated);

        if (mClosePolicy != ClosePolicy.None) {

            if (!mActivated) {
                log(TAG, WARN, "not yet activated...");
                return true;
            }

            if (action == MotionEvent.ACTION_DOWN) {

                Rect outRect = new Rect();
                mView.getGlobalVisibleRect(outRect);
                final boolean containsTouch = outRect.contains((int) event.getX(), (int) event.getY());

                if (DBG) {
                    log(TAG, VERBOSE, "containsTouch: %b", containsTouch);
                    log(TAG, VERBOSE, "mDrawRect: %s, point: %g, %g", mDrawRect, event.getX(), event.getY());
                    log(
                        TAG,
                        VERBOSE, "real drawing rect: %s, contains: %b", outRect,
                        outRect.contains((int) event.getX(), (int) event.getY()));
                }

                switch (mClosePolicy) {
                    case TouchInside:
                    case TouchInsideExclusive:
                        if (containsTouch) {
                            onClose(true, true, false);
                            return true;
                        }
                        return mClosePolicy == ClosePolicy.TouchInsideExclusive;
                    case TouchOutside:
                    case TouchOutsideExclusive:
                        onClose(true, containsTouch, false);
                        return mClosePolicy == ClosePolicy.TouchOutsideExclusive || containsTouch;
                    case TouchAnyWhere:
                        onClose(true, containsTouch, false);
                        return false;
                    case None:
                        break;
                }
            }
        }

        return false;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (!mAttached) {
            return;
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int myWidth = 0;
        int myHeight = 0;

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // Record our dimensions if they are known;
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            myWidth = widthSize;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED) {
            myHeight = heightSize;
        }

        log(TAG, VERBOSE, "myWidth: %d, myHeight: %d", myWidth, myHeight);

        if (null != mView) {
            if (mView.getVisibility() != GONE) {
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(myWidth, MeasureSpec.AT_MOST);
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(myHeight, MeasureSpec.AT_MOST);
                mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                //                myWidth = mView.getMeasuredWidth();
                //                myHeight = mView.getMeasuredHeight();
            } else {
                myWidth = 0;
                myHeight = 0;
            }
        }

        setMeasuredDimension(myWidth, myHeight);
    }

    private void onClose(boolean fromUser, boolean containsTouch, boolean immediate) {
        log(TAG, INFO, "onClose. fromUser: %b, containsTouch: %b, immediate: %b", fromUser, containsTouch, immediate);

        if (!isAttached()) {
            return;
        }

        if (null != mCloseCallback) {
            mCloseCallback.onClosing(mToolTipId, fromUser, containsTouch);
        }

        hide(true, immediate ? 0 : mFadeDuration);
    }

    void setOnToolTipListener(OnToolTipListener listener) {
        this.mTooltipListener = listener;
    }

    interface OnToolTipListener {
        void onHideCompleted(TooltipView layout);

        void onShowCompleted(TooltipView layout);

        void onShowFailed(TooltipView layout);
    }
}
