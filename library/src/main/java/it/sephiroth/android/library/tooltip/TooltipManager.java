package it.sephiroth.android.library.tooltip;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class TooltipManager {
	static final boolean DBG = false;
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
			if (DBG) Log.i(TAG, "onClose: " + layout.getTooltipId());
			hide(layout.getTooltipId());
		}
	};

	private TooltipView.OnToolTipListener mTooltipListener = new TooltipView.OnToolTipListener() {
		@Override
		public void onHideCompleted(final TooltipView layout) {
			if (DBG) Log.i(TAG, "onHideCompleted: " + layout.getTooltipId());
			int id = layout.getTooltipId();
			layout.removeFromParent();
			fireOnTooltipDetached(id);
		}

		@Override
		public void onShowCompleted(final TooltipView layout) {
			if (DBG) Log.i(TAG, "onShowCompleted: " + layout.getTooltipId());
		}

		@Override
		public void onShowFailed(final TooltipView layout) {
			if (DBG) Log.i(TAG, "onShowFailed: " + layout.getTooltipId());
			remove(layout.getTooltipId());
		}
	};

	public void addOnTooltipAttachedStateChange(OnTooltipAttachedStateChange listener) {
		if (! mTooltipAttachStatusListeners.contains(listener)) {
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

	public Builder create(int id) {
		return new Builder(this, id);
	}

	private boolean show(Builder builder, boolean immediate) {
		if (DBG) Log.i(TAG, "show");

		synchronized (lock) {
			if (mTooltips.containsKey(builder.id)) {
				Log.w(TAG, "A Tooltip with the same id was walready specified");
				return false;
			}

			TooltipView layout = new TooltipView(builder.view.getContext(), builder);
			layout.setOnCloseListener(mCloseListener);
			layout.setOnToolTipListener(mTooltipListener);
			mTooltips.put(builder.id, new WeakReference<>(layout));
			showInternal(builder.view.getRootView(), layout, immediate);
		}
		printStats();
		return true;
	}

	public void hide(int id) {
		if (DBG) Log.i(TAG, "hide: " + id);

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
	public TooltipView get(int id) {
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
			layout = get(id);
		}
		if (null != layout) {
			if (DBG) Log.i(TAG, "update: " + id);
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
		if (DBG) Log.i(TAG, "remove: " + id);

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
			layout = get(id);
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
		if (DBG) Log.i(TAG, "destroy");
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
				if (DBG) Log.v(TAG, "attach to mToolTipLayout parent");
				ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
				((ViewGroup) rootView).addView(layout, params);
			}

			if (immediate) {
				layout.show();
			}

			fireOnTooltipAttached(layout.getTooltipId());
		}
	}

	public static final class Builder {
		int id;
		String contentDescription;
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
		int maxWidth = - 1;
		int defStyleRes = R.style.ToolTipLayoutDefaultStyle;
		int defStyleAttr = R.attr.ttlm_defaultStyle;
		long activateDelay = 0;
		boolean isCustomView;
		boolean restrictToScreenEdges = true;
		long fadeDuration = 200;
		onTooltipClosingCallback closeCallback;

		@ColorInt int bgColor = Color.DKGRAY;
		@ColorInt int textColor = Color.WHITE;
		@ColorInt int strokeColor = Color.DKGRAY;

		@Size int textSize = 14; //sp
		int textGravity = android.view.Gravity.CENTER;
		int textAlignment;
		int textDirection;

		Builder(final TooltipManager manager, int id) {
			this.manager = new WeakReference<>(manager);
			this.id = id;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
				textAlignment = View.TEXT_ALIGNMENT_CENTER;
				textDirection = View.TEXT_DIRECTION_INHERIT;
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
			this.hideArrow = ! show;
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

		public Builder backgroundRes(@ColorRes int color) {
			bgColor = view.getContext().getResources().getColor(color);
			return this;
		}

		public Builder background(@ColorInt int color) {
			bgColor = color;
			return this;
		}

		public Builder background(String color) {
			bgColor = Color.parseColor(color);
			return this;
		}

		public Builder strokeColorRes(@ColorRes int color) {
			strokeColor = view.getContext().getResources().getColor(color);
			return this;
		}

		public Builder strokeColor(@ColorInt int color) {
			strokeColor = color;
			return this;
		}

		public Builder strokeColor(String color) {
			strokeColor = Color.parseColor(color);
			return this;
		}

		public Builder textResColor(@ColorRes int color) {
			textColor = view.getContext().getResources().getColor(color);
			return this;
		}

		public Builder textColor(@ColorInt int color) {
			textColor = color;
			return this;
		}

		public Builder textColor(String color) {
			textColor = Color.parseColor(color);
			return this;
		}

		public Builder textGravity(int gravity) {
			textGravity = gravity;
			return this;
		}

		public Builder textAlignment(int alignment) {
			textAlignment = alignment;
			return this;
		}

		public Builder textDirection(int direction) {
			textDirection = direction;
			return this;
		}

		public Builder setContentDescription(String description) {
			contentDescription = description;
			return this;
		}

		public boolean show() {
			// verification
			if (null == closePolicy) throw new IllegalStateException("ClosePolicy cannot be null");
			if (null == point && null == view) throw new IllegalStateException("Target point or target view must be specified");
			if (gravity == Gravity.CENTER) hideArrow = true;

			TooltipManager tmanager = this.manager.get();
			if (null != tmanager) {
				return tmanager.show(this, true);
			}
			return false;
		}

		public boolean build() {
			// verification
			if (null == closePolicy) throw new IllegalStateException("ClosePolicy cannot be null");
			if (null == point && null == view)
				throw new IllegalStateException("Target point or target view must be specified");
			if (gravity == Gravity.CENTER) hideArrow = true;

			TooltipManager tmanager = this.manager.get();
			return null != tmanager && tmanager.show(this, false);
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
		 * @param id
		 * @param fromUser true if the close operation started from a user click
         * @param containsTouch true if the original touch came from inside the tooltip
         */
		void onClosing(int id, boolean fromUser, final boolean containsTouch);
	}
}
