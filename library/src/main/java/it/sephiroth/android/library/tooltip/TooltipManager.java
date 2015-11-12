package it.sephiroth.android.library.tooltip;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class TooltipManager {
    public static boolean DBG = false;
    private static final String TAG = "TooltipManager";
    private volatile static TooltipManager INSTANCE;

    private TooltipManager() {}

    public static synchronized TooltipManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TooltipManager();
        }

        return INSTANCE;
    }

    public interface OnTooltipAttachedStateChange {
        void onTooltipAttached(int id);

        void onTooltipDetached(int id);
    }

    private final List<OnTooltipAttachedStateChange> mTooltipAttachStatusListeners = new ArrayList<>();
    final WeakHashMap<Integer, WeakReference<TooltipView>> mTooltips = new WeakHashMap<>();
    final Object lock = new Object();
    private TooltipView.OnCloseListener mCloseListener = new TooltipView.OnCloseListener() {
        @Override
        public void onClose(final TooltipView layout) {
            if (DBG) {
                Log.i(TAG, "onClose: " + layout.getTooltipId());
            }
            hide(layout.getTooltipId());
        }
    };
    private TooltipView.OnToolTipListener mTooltipListener = new TooltipView.OnToolTipListener() {
        @Override
        public void onHideCompleted(final TooltipView layout) {
            if (DBG) {
                Log.i(TAG, "onHideCompleted: " + layout.getTooltipId());
            }
            int id = layout.getTooltipId();
            layout.removeFromParent();
            fireOnTooltipDetached(id);
        }

        @Override
        public void onShowCompleted(final TooltipView layout) {
            if (DBG) {
                Log.i(TAG, "onShowCompleted: " + layout.getTooltipId());
            }
        }

        @Override
        public void onShowFailed(final TooltipView layout) {
            if (DBG) {
                Log.i(TAG, "onShowFailed: " + layout.getTooltipId());
            }
            remove(layout.getTooltipId());
        }
    };

    public void addOnTooltipAttachedStateChange(OnTooltipAttachedStateChange listener) {
        if (!mTooltipAttachStatusListeners.contains(listener)) {
            mTooltipAttachStatusListeners.add(listener);
        }
    }

    public void removeOnTooltipAttachedStateChange(OnTooltipAttachedStateChange listener) {
        mTooltipAttachStatusListeners.remove(listener);
    }

    private void fireOnTooltipDetached(int id) {
        if (mTooltipAttachStatusListeners.size() > 0) {
            for (OnTooltipAttachedStateChange listener : mTooltipAttachStatusListeners) {
                listener.onTooltipDetached(id);
            }
        }
    }

    private void fireOnTooltipAttached(int id) {
        if (mTooltipAttachStatusListeners.size() > 0) {
            for (OnTooltipAttachedStateChange listener : mTooltipAttachStatusListeners) {
                listener.onTooltipAttached(id);
            }
        }
    }

    public Builder create(final Context context, int id) {
        return new Builder(this, context, id);
    }

    private boolean show(Builder builder, boolean immediate) {
        if (DBG) {
            Log.i(TAG, "show");
        }

        synchronized (lock) {
            if (mTooltips.containsKey(builder.id)) {
                Log.w(TAG, "A Tooltip with the same id was walready specified");
                return false;
            }

            TooltipView layout = new TooltipView(builder.context, builder);
            layout.setOnCloseListener(mCloseListener);
            layout.setOnToolTipListener(mTooltipListener);
            mTooltips.put(builder.id, new WeakReference<>(layout));

            final Activity act = getActivity(builder.context);

            if (null == act || act.getWindow() == null || act.getWindow().getDecorView() == null) {
                return false;
            }
            showInternal(act.getWindow().getDecorView(), layout, immediate);
        }
        printStats();
        return true;
    }

    public void hide(int id) {
        if (DBG) {
            Log.i(TAG, "hide: " + id);
        }

        final WeakReference<TooltipView> layout;
        synchronized (lock) {
            layout = mTooltips.remove(id);
        }
        if (null != layout) {
            TooltipView tooltipView = layout.get();
            tooltipView.setOnCloseListener(null);
            tooltipView.hide(true);
        }
    }

    @Nullable
    public Tooltip get(int id) {
        synchronized (lock) {
            WeakReference<TooltipView> weakReference = mTooltips.get(id);

            if (weakReference != null) {
                return weakReference.get();
            }
        }
        return null;
    }

    public void update(int id) {
        final TooltipView layout;
        synchronized (lock) {
            layout = (TooltipView) get(id);
        }
        if (null != layout) {
            if (DBG) {
                Log.i(TAG, "update: " + id);
            }
            layout.layout(layout.getLeft(), layout.getTop(), layout.getRight(), layout.getBottom());
            layout.requestLayout();
        }
    }

    public boolean active(int id) {
        synchronized (lock) {
            return mTooltips.containsKey(id);
        }
    }

    public synchronized void remove(int id) {
        if (DBG) {
            Log.i(TAG, "remove: " + id);
        }

        final WeakReference<TooltipView> layout;
        synchronized (lock) {
            layout = mTooltips.remove(id);
        }
        if (null != layout) {
            TooltipView tooltipView = layout.get();
            tooltipView.setOnCloseListener(null);
            tooltipView.setOnToolTipListener(null);
            tooltipView.removeFromParent();
            fireOnTooltipDetached(id);
        }
    }

    public void setText(int id, final CharSequence text) {
        TooltipView layout;
        synchronized (lock) {
            layout = (TooltipView) get(id);
        }
        if (null != layout) {
            layout.setText(text);
        }
    }

    private void printStats() {
        if (DBG) {
            Log.d(TAG, "active tooltips: " + mTooltips.size());
        }
    }

    private void destroy() {
        if (DBG) {
            Log.i(TAG, "destroy");
        }
        synchronized (lock) {
            for (int id : mTooltips.keySet()) {
                remove(id);
            }
        }
        mTooltipAttachStatusListeners.clear();
        printStats();
    }

    private void showInternal(View rootView, TooltipView layout, boolean immediate) {
        if (null != rootView && rootView instanceof ViewGroup) {
            if (layout.getParent() == null) {
                if (DBG) {
                    Log.v(TAG, "attach to mToolTipLayout parent");
                }
                ViewGroup.LayoutParams params =
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                ((ViewGroup) rootView).addView(layout, params);
            }

            if (immediate) {
                layout.show();
            }

            fireOnTooltipAttached(layout.getTooltipId());
        }
    }

    @Nullable
    static Activity getActivity(@Nullable Context cont) {
        if (cont == null) {
            return null;
        } else if (cont instanceof Activity) {
            return (Activity) cont;
        } else if (cont instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) cont).getBaseContext());
        }
        return null;
    }

    public static final class Builder {
        final Context context;
        int id;
        CharSequence text;
        View view;
        Gravity gravity;
        int actionbarSize = 0;
        int textResId = R.layout.tooltip_textview;
        ClosePolicy closePolicy;
        long showDuration;
        Point point;
        WeakReference<TooltipManager> manager;
        long showDelay = 0;
        boolean hideArrow;
        int maxWidth = -1;
        int defStyleRes = R.style.ToolTipLayoutDefaultStyle;
        int defStyleAttr = R.attr.ttlm_defaultStyle;
        long activateDelay = 0;
        boolean isCustomView;
        boolean restrictToScreenEdges = true;
        long fadeDuration = 200;
        onTooltipClosingCallback closeCallback;

        Builder(final TooltipManager manager, final Context context, int id) {
            this.manager = new WeakReference<>(manager);
            this.id = id;
            this.context = context;
        }

        /**
         * Use a custom View for the tooltip. Note that the custom view
         * must include a TextView which id is `@android:id/text1`.<br />
         * Moreover, when using a custom view, the anchor arrow will not be shown
         *
         * @param resId              the custom layout view.
         * @param replace_background if true the custom view's background won't be replaced
         * @return the builder for chaining.
         */
        public Builder withCustomView(int resId, boolean replace_background) {
            this.textResId = resId;
            this.isCustomView = replace_background;
            return this;
        }

        public Builder withCustomView(int resId) {
            return withCustomView(resId, true);
        }

        public Builder withStyleId(int styleId) {
            this.defStyleAttr = 0;
            this.defStyleRes = styleId;
            return this;
        }

        public Builder fitToScreen(boolean value) {
            restrictToScreenEdges = value;
            return this;
        }

        public Builder fadeDuration(long ms) {
            fadeDuration = ms;
            return this;
        }

        public Builder withCallback(onTooltipClosingCallback callback) {
            this.closeCallback = callback;
            return this;
        }

        public Builder text(Resources res, int resid) {
            return text(res.getString(resid));
        }

        public Builder text(int resid) {
            if (null != view) {
                return text(view.getResources().getString(resid));
            }
            return this;
        }

        public Builder text(CharSequence text) {
            this.text = text;
            return this;
        }

        public Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder anchor(View view, Gravity gravity) {
            this.point = null;
            this.view = view;
            this.gravity = gravity;
            return this;
        }

        public Builder anchor(final Point point, final Gravity gravity) {
            this.view = null;
            this.point = new Point(point);
            this.gravity = gravity;
            return this;
        }

        /**
         * @param show true to show the arrow, false to hide it
         * @return the builder for chaining.
         */
        public Builder toggleArrow(boolean show) {
            this.hideArrow = !show;
            return this;
        }

        public Builder actionBarSize(final int actionBarSize) {
            this.actionbarSize = actionBarSize;
            return this;
        }

        public Builder actionBarSize(Resources resources, int resId) {
            return actionBarSize(resources.getDimensionPixelSize(resId));
        }

        public Builder closePolicy(ClosePolicy policy, long milliseconds) {
            this.closePolicy = policy;
            this.showDuration = milliseconds;
            return this;
        }

        public Builder activateDelay(long ms) {
            this.activateDelay = ms;
            return this;
        }

        public Builder showDelay(long ms) {
            this.showDelay = ms;
            return this;
        }

        public boolean show() {
            // verification
            if (null == closePolicy) {
                throw new IllegalStateException("ClosePolicy cannot be null");
            }
            if (null == point && null == view) {
                throw new IllegalStateException("Target point or target view must be specified");
            }
            if (gravity == Gravity.CENTER) {
                hideArrow = true;
            }

            TooltipManager tmanager = this.manager.get();
            if (null != tmanager) {
                return tmanager.show(this, true);
            }
            return false;
        }
    }

    public enum ClosePolicy {
        /**
         * tooltip will hide when touching it, or after the specified delay.
         * If delay is '0' the tooltip will never hide until clicked
         */
        TouchInside,
        /**
         * tooltip will hide when touching it, or after the specified delay.
         * If delay is '0' the tooltip will never hide until clicked.
         * In exclusive mode all touches will be consumed by the tooltip itself
         */
        TouchInsideExclusive,
        /**
         * tooltip will hide when user touches the screen, or after the specified delay.
         * If delay is '0' the tooltip will never hide until clicked
         */
        TouchOutside,
        /**
         * tooltip will hide when user touches the screen, or after the specified delay.
         * If delay is '0' the tooltip will never hide until clicked.
         * Touch will be consumed in any case.
         */
        TouchOutsideExclusive,
        /**
         * Any touch will hide the tooltip.
         * Touch is never consumed
         */
        TouchAnyWhere,
        /**
         * tooltip is hidden only after the specified delay
         */
        None
    }

    public enum Gravity {
        LEFT, RIGHT, TOP, BOTTOM, CENTER
    }

    public interface onTooltipClosingCallback {
        /**
         * tooltip is being closed
         *
         * @param id
         * @param fromUser      true if the close operation started from a user click
         * @param containsTouch true if the original touch came from inside the tooltip
         */
        void onClosing(int id, boolean fromUser, final boolean containsTouch);
    }
}
