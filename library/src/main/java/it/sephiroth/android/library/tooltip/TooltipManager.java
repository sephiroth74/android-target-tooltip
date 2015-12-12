package it.sephiroth.android.library.tooltip;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Point;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;

public class TooltipManager {
    public static boolean DBG = true;
    private static final String TAG = "TooltipManager";
    private final WeakReference<Context> mContextRef;
    private final List<OnTooltipAttachedStateChange> mTooltipAttachStatusListeners = new ArrayList<>();
    final WeakHashMap<Integer, WeakReference<TooltipView>> mTooltips = new WeakHashMap<>();
    private final Object mLock = new Object();

    public interface OnTooltipAttachedStateChange {
        void onTooltipAttached(int id);

        void onTooltipDetached(int id);
    }

    private TooltipView.OnToolTipListener mTooltipListener = new TooltipView.OnToolTipListener() {
        @Override
        public void onHideCompleted(final TooltipView layout) {
            log(TAG, INFO, "onHideCompleted: %d", layout.getTooltipId());
            remove(layout.getTooltipId());
        }

        @Override
        public void onShowCompleted(final TooltipView layout) {
            log(TAG, INFO, "onShowCompleted: %d", layout.getTooltipId());
        }

        @Override
        public void onShowFailed(final TooltipView layout) {
            log(TAG, INFO, "onShowFailed: %d", layout.getTooltipId());
            remove(layout.getTooltipId());
        }
    };

    public TooltipManager(final Context context) {
        mContextRef = new WeakReference<>(context);
    }

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

    @SuppressWarnings("unused")
    public Tooltip show(Builder builder) {
        log(TAG, INFO, "show");

        if (!builder.completed) {
            throw new IllegalArgumentException("Builder incomplete. Call 'build()' first");
        }

        TooltipView view;

        synchronized (mLock) {
            if (mTooltips.containsKey(builder.id)) {
                Log.w(TAG, "A Tooltip with the same id was walready specified");
                return null;
            }

            final Activity act = getActivity(mContextRef.get());
            if (null == act || act.getWindow() == null || act.getWindow().getDecorView() == null || act.isFinishing()) {
                return null;
            }

            view = new TooltipView(mContextRef.get(), this, builder);
            view.setOnToolTipListener(mTooltipListener);
            mTooltips.put(builder.id, new WeakReference<>(view));
            showInternal(act.getWindow().getDecorView(), view, true);
        }

        printStats();
        return view;
    }

    @SuppressWarnings("unused")
    public void hide(int id) {
        log(TAG, INFO, "hide(%d)", id);

        final WeakReference<TooltipView> layout;
        synchronized (mLock) {
            layout = mTooltips.remove(id);
        }
        if (null != layout) {
            TooltipView tooltipView = layout.get();
            tooltipView.hide(true);
        }
    }

    @Nullable
    public Tooltip get(int id) {
        synchronized (mLock) {
            WeakReference<TooltipView> weakReference = mTooltips.get(id);

            if (weakReference != null) {
                return weakReference.get();
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public boolean active(int id) {
        synchronized (mLock) {
            return mTooltips.containsKey(id);
        }
    }

    @SuppressWarnings("unused")
    public void remove(int id) {
        log(TAG, INFO, "remove(%d)", id);

        final WeakReference<TooltipView> layout;
        synchronized (mLock) {
            layout = mTooltips.remove(id);
        }
        if (null != layout) {
            TooltipView tooltipView = layout.get();
            tooltipView.setOnToolTipListener(null);
            tooltipView.removeFromParent();
            fireOnTooltipDetached(id);
        }
    }

    @SuppressWarnings("unused")
    public void setText(int id, final CharSequence text) {
        TooltipView layout;
        synchronized (mLock) {
            layout = (TooltipView) get(id);
        }
        if (null != layout) {
            layout.setText(text);
        }
    }

    private void printStats() {
        log(TAG, VERBOSE, "active tooltips: %d", mTooltips.size());
    }

    private void destroy() {
        log(TAG, INFO, "destroy");

        synchronized (mLock) {
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
                log(TAG, VERBOSE, "attach to mToolTipLayout parent");

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
        int id;
        CharSequence text;
        View view;
        Gravity gravity;
        int actionbarSize = 0;
        int textResId = R.layout.tooltip_textview;
        ClosePolicy closePolicy;
        long showDuration;
        Point point;
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
        boolean completed;

        public Builder(int id) {
            this.id = id;
        }

        private void throwIfCompleted() {
            if (completed) {
                throw new IllegalStateException("Builder cannot be modified");
            }
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
            throwIfCompleted();
            return withCustomView(resId, true);
        }

        public Builder withStyleId(int styleId) {
            throwIfCompleted();
            this.defStyleAttr = 0;
            this.defStyleRes = styleId;
            return this;
        }

        public Builder fitToScreen(boolean value) {
            throwIfCompleted();
            restrictToScreenEdges = value;
            return this;
        }

        public Builder fadeDuration(long ms) {
            throwIfCompleted();
            fadeDuration = ms;
            return this;
        }

        public Builder withCallback(onTooltipClosingCallback callback) {
            throwIfCompleted();
            this.closeCallback = callback;
            return this;
        }

        public Builder text(Resources res, @StringRes int resId) {
            return text(res.getString(resId));
        }

        public Builder text(CharSequence text) {
            throwIfCompleted();
            this.text = text;
            return this;
        }

        public Builder maxWidth(Resources res, @DimenRes int dimension) {
            return maxWidth(res.getDimensionPixelSize(dimension));
        }

        public Builder maxWidth(int maxWidth) {
            throwIfCompleted();
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder anchor(View view, Gravity gravity) {
            throwIfCompleted();
            this.point = null;
            this.view = view;
            this.gravity = gravity;
            return this;
        }

        public Builder anchor(final Point point, final Gravity gravity) {
            throwIfCompleted();
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
            throwIfCompleted();
            this.hideArrow = !show;
            return this;
        }

        public Builder actionBarSize(final int actionBarSize) {
            throwIfCompleted();
            this.actionbarSize = actionBarSize;
            return this;
        }

        public Builder actionBarSize(Resources resources, int resId) {
            return actionBarSize(resources.getDimensionPixelSize(resId));
        }

        public Builder closePolicy(ClosePolicy policy, long milliseconds) {
            throwIfCompleted();
            this.closePolicy = policy;
            this.showDuration = milliseconds;
            return this;
        }

        public Builder activateDelay(long ms) {
            throwIfCompleted();
            this.activateDelay = ms;
            return this;
        }

        public Builder showDelay(long ms) {
            throwIfCompleted();
            this.showDelay = ms;
            return this;
        }

        public Builder build() {
            throwIfCompleted();
            completed = true;
            return this;
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

    static void log(final String tag, final int level, final String format, Object... args) {
        if (DBG) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(tag, String.format(format, args));
                    break;
                case Log.ERROR:
                    Log.e(tag, String.format(format, args));
                    break;
                case INFO:
                    Log.i(tag, String.format(format, args));
                    break;
                case Log.WARN:
                    Log.w(tag, String.format(format, args));
                    break;
                default:
                case VERBOSE:
                    Log.v(tag, String.format(format, args));
                    break;
            }
        }
    }
}
