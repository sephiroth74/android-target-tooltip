package it.sephiroth.android.library.tooltip;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Log;
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

import static it.sephiroth.android.library.tooltip.TooltipManager.ClosePolicy;
import static it.sephiroth.android.library.tooltip.TooltipManager.DBG;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.BOTTOM;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.CENTER;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.LEFT;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.RIGHT;
import static it.sephiroth.android.library.tooltip.TooltipManager.Gravity.TOP;

class TooltipView extends ViewGroup implements Tooltip, ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "TooltipView";
    private static final List<Gravity> gravities = new ArrayList<>(Arrays.asList(LEFT, RIGHT, TOP, BOTTOM, CENTER));
    private final List<Gravity> viewGravities = new ArrayList<>(gravities);
    private final long showDelay;
    private final int textAppearance;
    private WeakReference<View> mViewAnchor;
    private boolean mAttached;
    private boolean mInitialized;
    private boolean mActivated;
    private final int toolTipId;
    private final Rect drawRect;
    private final Rect tempRect;
    private final long showDuration;
    private final ClosePolicy closePolicy;
    private final Point point;
    private final int textResId;
    private final int topRule;
    private final int maxWidth;
    private final boolean hideArrow;
    private int padding;
    private final long activateDelay;
    private final boolean restrict;
    private final long fadeDuration;
    private final TooltipManager.onTooltipClosingCallback closeCallback;
    private CharSequence text;
    private Rect viewRect;
    Gravity gravity;
    private View mView;
    private TextView mTextView;
    private final TooltipTextDrawable mDrawable;

    public TooltipView(Context context, TooltipManager.Builder builder) {
        super(context);

        TypedArray theme =
            context.getTheme().obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes);
        this.padding = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_padding, 30);
        this.textAppearance = theme.getResourceId(R.styleable.TooltipLayout_android_textAppearance, 0);
        theme.recycle();

        this.toolTipId = builder.id;
        this.text = builder.text;
        this.gravity = builder.gravity;
        this.textResId = builder.textResId;
        this.maxWidth = builder.maxWidth;
        this.topRule = builder.actionbarSize;
        this.closePolicy = builder.closePolicy;
        this.showDuration = builder.showDuration;
        this.showDelay = builder.showDelay;
        this.hideArrow = builder.hideArrow;
        this.activateDelay = builder.activateDelay;
        this.restrict = builder.restrictToScreenEdges;
        this.fadeDuration = builder.fadeDuration;
        this.closeCallback = builder.closeCallback;

        if (null != builder.point) {
            this.point = new Point(builder.point);
            this.point.y += topRule;
        } else {
            this.point = null;
        }

        this.drawRect = new Rect();
        this.tempRect = new Rect();

        if (null != builder.view) {
            viewRect = new Rect();
            builder.view.getGlobalVisibleRect(viewRect);
            mViewAnchor = new WeakReference<>(builder.view);
            builder.view.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }

        if (!builder.isCustomView) {
            this.mDrawable = new TooltipTextDrawable(context, builder);
        } else {
            this.mDrawable = null;
        }

        setVisibility(INVISIBLE);
    }

    int getTooltipId() {
        return toolTipId;
    }

    @Override
    public void show() {
        if (DBG) {
            Log.i(TAG, "show");
        }
        if (!isAttached()) {
            if (DBG) {
                Log.e(TAG, "not attached!");
            }
            return;
        }
        fadeIn();
    }

    @Override
    public void hide(boolean remove) {
        if (DBG) {
            Log.i(TAG, "hide");
        }
        if (!isAttached()) {
            return;
        }
        fadeOut(remove);
    }

    Animator mShowAnimation;
    boolean mShowing;

    protected void fadeIn() {
        if (mShowing) {
            return;
        }

        if (null != mShowAnimation) {
            mShowAnimation.cancel();
        }

        if (DBG) {
            Log.i(TAG, "fadeIn");
        }

        mShowing = true;

        if (fadeDuration > 0) {
            mShowAnimation = ObjectAnimator.ofFloat(this, "alpha", 0, 1);
            mShowAnimation.setDuration(fadeDuration);
            if (this.showDelay > 0) {
                mShowAnimation.setStartDelay(this.showDelay);
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
                        if (DBG) {
                            Log.i(TAG, "fadein::onAnimationEnd, cancelled: " + cancelled);
                        }
                        if (null != tooltipListener && !cancelled) {
                            tooltipListener.onShowCompleted(TooltipView.this);
                            postActivate(activateDelay);
                        }
                    }

                    @Override
                    public void onAnimationCancel(final Animator animation) {
                        if (DBG) {
                            Log.i(TAG, "fadein::onAnimationCancel");
                        }
                        cancelled = true;
                    }

                    @Override
                    public void onAnimationRepeat(final Animator animation) {

                    }
                }
                                      );
            mShowAnimation.start();
        } else {
            setVisibility(View.VISIBLE);
            tooltipListener.onShowCompleted(TooltipView.this);
            if (!mActivated) {
                postActivate(activateDelay);
            }
        }

        if (showDuration > 0) {
            getHandler().removeCallbacks(hideRunnable);
            getHandler().postDelayed(hideRunnable, showDuration);
        }
    }

    Runnable activateRunnable = new Runnable() {
        @Override
        public void run() {
            if (DBG) {
                Log.v(TAG, "activated..");
            }
            mActivated = true;
        }
    };
    Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            onClose(false, false);
        }
    };

    boolean isShowing() {
        return mShowing;
    }

    void postActivate(long ms) {
        if (DBG) {
            Log.i(TAG, "postActivate: " + ms);
        }
        if (ms > 0) {
            if (isAttached()) {
                postDelayed(activateRunnable, ms);
            }
        } else {
            mActivated = true;
        }
    }

    void removeFromParent() {
        if (DBG) {
            Log.i(TAG, "removeFromParent: " + toolTipId);
        }
        ViewParent parent = getParent();
        if (null != parent) {
            if (null != getHandler()) {
                getHandler().removeCallbacks(hideRunnable);
            }
            ((ViewGroup) parent).removeView(TooltipView.this);

            if (null != mShowAnimation && mShowAnimation.isStarted()) {
                mShowAnimation.cancel();
            }
        }
    }

    protected void fadeOut(final boolean remove) {
        if (!isAttached() || !mShowing) {
            return;
        }
        if (DBG) {
            Log.i(TAG, "fadeOut");
        }

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
                        if (DBG) {
                            Log.i(TAG, "fadeout::onAnimationEnd, cancelled: " + cancelled);
                        }
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
                        if (DBG) {
                            Log.i(TAG, "fadeout::onAnimationCancel");
                        }
                        cancelled = true;
                    }

                    @Override
                    public void onAnimationRepeat(final Animator animation) {

                    }
                }
                                      );
            mShowAnimation.start();
        } else {
            setVisibility(View.INVISIBLE);
            if (remove) {
                fireOnHideCompleted();
            }
        }
    }

    private void fireOnHideCompleted() {
        if (null != tooltipListener) {
            tooltipListener.onHideCompleted(TooltipView.this);
        }
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b) {
        if (DBG) {
            Log.i(TAG, "onLayout, changed: " + changed + ", " + l + ", " + t + ", " + r + ", " + b);
        }

        //  The layout has actually already been performed and the positions
        //  cached.  Apply the cached values to the children.
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.layout(child.getLeft(), child.getTop(), child.getMeasuredWidth(), child.getMeasuredHeight());
            }
        }

        if (changed) {
            viewGravities.clear();
            viewGravities.addAll(gravities);
            viewGravities.remove(gravity);
            viewGravities.add(0, gravity);
            calculatePositions(viewGravities);
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (DBG) {
            Log.i(TAG, "onMeasure");
        }
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

        if (DBG) {
            Log.v(TAG, "myWidth: " + myWidth);
            Log.v(TAG, "myHeight: " + myHeight);
        }

        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(myWidth, MeasureSpec.AT_MOST);
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(myHeight, MeasureSpec.AT_MOST);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }

        setMeasuredDimension(myWidth, myHeight);
    }

    @Override
    protected void onAttachedToWindow() {
        if (DBG) {
            Log.i(TAG, "onAttachedToWindow");
        }
        super.onAttachedToWindow();
        mAttached = true;
        initializeView();
        show();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (DBG) {
            Log.i(TAG, "onDetachedFromWindow");
        }
        super.onDetachedFromWindow();
        removeTreeObserver();
        mAttached = false;
        mViewAnchor = null;
    }

    private void removeTreeObserver() {
        if (DBG) {
            Log.i(TAG, "removeTreeObserver");
        }

        if (null != mViewAnchor) {
            View view = mViewAnchor.get();
            if (null != view) {
                if (Build.VERSION.SDK_INT >= 16) {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        }
    }

    private void initializeView() {
        if (!isAttached() || mInitialized) {
            return;
        }
        mInitialized = true;

        if (DBG) {
            Log.i(TAG, "initializeView");
        }

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mView = LayoutInflater.from(getContext()).inflate(textResId, this, false);
        mView.setLayoutParams(params);

        if (null != mDrawable) {
            mView.setBackgroundDrawable(mDrawable);
            if (hideArrow) {
                mView.setPadding(padding / 2, padding / 2, padding / 2, padding / 2);
            } else {
                mView.setPadding(padding, padding, padding, padding);
            }
        }

        mTextView = (TextView) mView.findViewById(android.R.id.text1);
        mTextView.setText(Html.fromHtml((String) this.text));
        if (maxWidth > -1) {
            mTextView.setMaxWidth(maxWidth);
        }

        if (0 != textAppearance) {
            mTextView.setTextAppearance(getContext(), textAppearance);
        }

        this.addView(mView);
    }

    private void calculatePositions(List<Gravity> gravities) {
        if (!isAttached()) {
            return;
        }

        // failed to display the tooltip due to
        // something wrong with its dimensions or
        // the target position..
        if (gravities.size() < 1) {
            if (null != tooltipListener) {
                tooltipListener.onShowFailed(this);
            }
            setVisibility(View.GONE);
            return;
        }

        Gravity gravity = gravities.remove(0);

        if (DBG) {
            Log.i(TAG, "calculatePositions: " + gravity + ", gravities: " + gravities.size());
        }

        Rect screenRect = new Rect();
        final Activity act = TooltipManager.getActivity(getContext());
        if (act != null) {
            Window window = act.getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(screenRect);
        } else {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            android.view.Display display = wm.getDefaultDisplay();
            display.getRectSize(screenRect);
        }

        int statusbarHeight = screenRect.top;

        if (viewRect == null) {
            viewRect = new Rect();
            viewRect.set(point.x, point.y + statusbarHeight, point.x, point.y + statusbarHeight);
        }

        screenRect.top += topRule;

        int width = mView.getWidth();
        int height = mView.getMeasuredHeight();

        if (DBG) {
            Log.v(TAG, "mView.size: " + width + "x" + height);
        }

        // get the destination point
        Point point = new Point();

        if (gravity == BOTTOM) {
            drawRect.set(
                viewRect.centerX() - width / 2,
                viewRect.bottom,
                viewRect.centerX() + width / 2,
                viewRect.bottom + height);

            point.x = viewRect.centerX();
            point.y = viewRect.bottom;

            if (restrict && !screenRect.contains(drawRect)) {
                if (drawRect.right > screenRect.right) {
                    drawRect.offset(screenRect.right - drawRect.right, 0);
                } else if (drawRect.left < screenRect.left) {
                    drawRect.offset(-drawRect.left, 0);
                }
                if (drawRect.bottom > screenRect.bottom) {
                    // this means there's no enough space!
                    calculatePositions(gravities);
                    return;
                } else if (drawRect.top < screenRect.top) {
                    drawRect.offset(0, screenRect.top - drawRect.top);
                }
            }
        } else if (gravity == TOP) {
            drawRect.set(
                viewRect.centerX() - width / 2,
                viewRect.top - height,
                viewRect.centerX() + width / 2,
                viewRect.top);

            point.x = viewRect.centerX();
            point.y = viewRect.top;

            if (restrict && !screenRect.contains(drawRect)) {
                if (drawRect.right > screenRect.right) {
                    drawRect.offset(screenRect.right - drawRect.right, 0);
                } else if (drawRect.left < screenRect.left) {
                    drawRect.offset(-drawRect.left, 0);
                }
                if (drawRect.top < screenRect.top) {
                    // this means there's no enough space!
                    calculatePositions(gravities);
                    return;
                } else if (drawRect.bottom > screenRect.bottom) {
                    drawRect.offset(0, screenRect.bottom - drawRect.bottom);
                }
            }
        } else if (gravity == RIGHT) {
            drawRect.set(
                viewRect.right,
                viewRect.centerY() - height / 2,
                viewRect.right + width,
                viewRect.centerY() + height / 2);

            point.x = viewRect.right;
            point.y = viewRect.centerY();

            if (restrict && !screenRect.contains(drawRect)) {
                if (drawRect.bottom > screenRect.bottom) {
                    drawRect.offset(0, screenRect.bottom - drawRect.bottom);
                } else if (drawRect.top < screenRect.top) {
                    drawRect.offset(0, screenRect.top - drawRect.top);
                }
                if (drawRect.right > screenRect.right) {
                    // this means there's no enough space!
                    calculatePositions(gravities);
                    return;
                } else if (drawRect.left < screenRect.left) {
                    drawRect.offset(screenRect.left - drawRect.left, 0);
                }
            }
        } else if (gravity == LEFT) {
            drawRect.set(
                viewRect.left - width,
                viewRect.centerY() - height / 2,
                viewRect.left,
                viewRect.centerY() + height / 2);

            point.x = viewRect.left;
            point.y = viewRect.centerY();

            if (restrict && !screenRect.contains(drawRect)) {
                if (drawRect.bottom > screenRect.bottom) {
                    drawRect.offset(0, screenRect.bottom - drawRect.bottom);
                } else if (drawRect.top < screenRect.top) {
                    drawRect.offset(0, screenRect.top - drawRect.top);
                }
                if (drawRect.left < screenRect.left) {
                    // this means there's no enough space!
                    this.gravity = RIGHT;
                    calculatePositions(gravities);
                    return;
                } else if (drawRect.right > screenRect.right) {
                    drawRect.offset(screenRect.right - drawRect.right, 0);
                }
            }
        } else if (this.gravity == CENTER) {
            drawRect.set(
                viewRect.centerX() - width / 2,
                viewRect.centerY() - height / 2,
                viewRect.centerX() - width / 2,
                viewRect.centerY() + height / 2);

            point.x = viewRect.centerX();
            point.y = viewRect.centerY();

            if (restrict && !screenRect.contains(drawRect)) {
                if (drawRect.bottom > screenRect.bottom) {
                    drawRect.offset(0, screenRect.bottom - drawRect.bottom);
                } else if (drawRect.top < screenRect.top) {
                    drawRect.offset(0, screenRect.top - drawRect.top);
                }
                if (drawRect.right > screenRect.right) {
                    drawRect.offset(screenRect.right - drawRect.right, 0);
                } else if (drawRect.left < screenRect.left) {
                    drawRect.offset(screenRect.left - drawRect.left, 0);
                }
            }
        }

        if (DBG) {
            Log.d(TAG, "screenRect: " + screenRect + ", topRule: " + topRule + ", statusBar: " + statusbarHeight);
            Log.d(TAG, "drawRect: " + drawRect);
        }

        // translate the textview

        mView.setTranslationX(drawRect.left);
        mView.setTranslationY(drawRect.top);

        if (null != mDrawable) {
            // get the global rect for the textview
            mView.getGlobalVisibleRect(tempRect);

            if (DBG) {
                Log.v(TAG, "mView visible rect: " + tempRect);
            }

            point.x -= tempRect.left;
            point.y -= tempRect.top;

            if (!hideArrow) {
                if (gravity == LEFT || gravity == RIGHT) {
                    point.y -= padding / 2;
                } else if (gravity == TOP || gravity == BOTTOM) {
                    point.x -= padding / 2;
                }
            }
            mDrawable.setAnchor(gravity, hideArrow ? 0 : padding / 2, hideArrow ? null : point);
        }
    }

    @Override
    public void setOffsetX(int x) {
        setTranslationX(x - viewRect.left);
    }

    @Override
    public void setOffsetY(int y) {
        setTranslationY(y - viewRect.top);
    }

    @Override
    public void offsetTo(final int x, final int y) {
        setTranslationX(x - viewRect.left);
        setTranslationY(y - viewRect.top);
    }

    @Override
    public boolean isAttached() {
        return mAttached;
    }

    void setText(final CharSequence text) {
        if (DBG) {
            Log.i(TAG, "setText: " + text);
        }
        this.text = text;
        if (null != mTextView) {
            mTextView.setText(Html.fromHtml((String) text));
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        if (!mAttached || !mShowing || !isShown()) {
            return false;
        }

        if (DBG) {
            Log.i(TAG, "onTouchEvent: " + event.getAction() + ", active: " + mActivated);
        }

        final int action = event.getActionMasked();

        if (closePolicy == ClosePolicy.TouchOutside
            || closePolicy == ClosePolicy.TouchInside
            || closePolicy == ClosePolicy.TouchInsideExclusive
            || closePolicy == ClosePolicy.TouchOutsideExclusive
            ) {

            if (!mActivated) {
                if (DBG) {
                    Log.w(TAG, "not yet activated..., " + action);
                }
                return true;
            }

            if (action == MotionEvent.ACTION_DOWN) {

                final boolean containsTouch = drawRect.contains((int) event.getX(), (int) event.getY());

                if (closePolicy == ClosePolicy.TouchInside || closePolicy == ClosePolicy.TouchInsideExclusive) {
                    if (containsTouch) {
                        onClose(true, true);
                        return true;
                    }
                    return closePolicy == ClosePolicy.TouchInsideExclusive;
                } else {
                    onClose(true, containsTouch);
                    return closePolicy == ClosePolicy.TouchOutsideExclusive || containsTouch;
                }
            }
        }

        return false;
    }

    private void onClose(boolean fromUser, boolean containsTouch) {
        if (DBG) {
            Log.i(TAG, "onClose. fromUser: " + fromUser + ", containsTouch: " + containsTouch);
        }

        if (null == getHandler()) {
            return;
        }
        if (!isAttached()) {
            return;
        }

        getHandler().removeCallbacks(hideRunnable);

        if (null != closeListener) {
            closeListener.onClose(this);
        }

        if (null != closeCallback) {
            closeCallback.onClosing(toolTipId, fromUser, containsTouch);
        }
    }

    private OnCloseListener closeListener;
    private OnToolTipListener tooltipListener;

    void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }

    void setOnToolTipListener(OnToolTipListener listener) {
        this.tooltipListener = listener;
    }

    /**
     * Callback method to be invoked when the global layout state or the visibility of views
     * within the view tree changes
     */
    @Override
    public void onGlobalLayout() {
        if (!mAttached) {
            removeTreeObserver();
            return;
        }

        if (DBG) {
            Log.i(TAG, "onGlobalLayout: " + mViewAnchor);
        }

        if (null != mViewAnchor) {
            View view = mViewAnchor.get();

            if (null != view) {
                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);

                if (DBG) {
                    Log.v(TAG, "oldRect: " + viewRect);
                    Log.v(TAG, "newRect: " + rect);
                    Log.v(TAG, "equals: " + viewRect.equals(rect));
                }

                if (!viewRect.equals(rect)) {
                    viewRect.set(rect);
                    viewGravities.clear();
                    viewGravities.addAll(gravities);
                    viewGravities.remove(gravity);
                    viewGravities.add(0, gravity);
                    calculatePositions(viewGravities);
                    requestLayout();
                }
            } else {
                if (DBG) {
                    Log.w(TAG, "view is null");
                }
            }
        }
    }

    interface OnCloseListener {
        void onClose(TooltipView layout);
    }

    interface OnToolTipListener {
        void onHideCompleted(TooltipView layout);

        void onShowCompleted(TooltipView layout);

        void onShowFailed(TooltipView layout);
    }
}
