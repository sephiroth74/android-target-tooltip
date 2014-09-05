package it.sephiroth.android.library.tooltip;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TooltipManager {
	static final boolean DBG = false;
	private static final String TAG = "TooltipManager";

	public static interface OnTooltipAttachedStateChange {
		void onTooltipAttached(int id);

		void onTooltipDetached(int id);
	}

	private static ConcurrentHashMap<Integer, TooltipManager> instances = new ConcurrentHashMap<Integer, TooltipManager>();

	private final List<OnTooltipAttachedStateChange> mTooltipAttachStatusListeners = new ArrayList<OnTooltipAttachedStateChange>();

	final HashMap<Integer, TooltipView> mTooltips = new HashMap<Integer, TooltipView>();
	final Object lock = new Object();
	final Activity mActivity;

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
			printStats();
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

	public TooltipManager(final Activity activity) {
		if (DBG) Log.i(TAG, "TooltipManager: " + activity);
		mActivity = activity;
	}

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

			TooltipView layout = new TooltipView(mActivity, builder);
			layout.setOnCloseListener(mCloseListener);
			layout.setOnToolTipListener(mTooltipListener);
			mTooltips.put(builder.id, layout);
			showInternal(layout, immediate);
		}
		printStats();
		return true;
	}

	public void hide(int id) {
		if (DBG) Log.i(TAG, "hide: " + id);

		final TooltipView layout;
		synchronized (lock) {
			layout = mTooltips.remove(id);
		}
		if (null != layout) {
			layout.setOnCloseListener(null);
			layout.hide(true);
			printStats();
		}
	}

	public TooltipView get(int id) {
		synchronized (lock) {
			return mTooltips.get(id);
		}
	}

	public void update(int id) {
		final TooltipView layout;
		synchronized (lock) {
			layout = mTooltips.get(id);
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

	public void remove(int id) {
		if (DBG) Log.i(TAG, "remove: " + id);

		final TooltipView layout;
		synchronized (lock) {
			layout = mTooltips.remove(id);
		}

		if (null != layout) {
			layout.setOnCloseListener(null);
			layout.setOnToolTipListener(null);
			layout.removeFromParent();
			fireOnTooltipDetached(id);
		}
		printStats();
	}

	public void setText(int id, final CharSequence text) {
		TooltipView layout;
		synchronized (lock) {
			layout = mTooltips.get(id);
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

	private void showInternal(TooltipView layout, boolean immediate) {
		if (null != mActivity) {
			ViewGroup decor = (ViewGroup) mActivity.getWindow().getDecorView();
			if (null == decor) return;
			if (layout.getParent() == null) {
				if (DBG) Log.v(TAG, "attach to mToolTipLayout parent");
				ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
				decor.addView(layout, params);
			}

			if (immediate) {
				layout.show();
			}

			fireOnTooltipAttached(layout.getTooltipId());
		}
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

		Builder(final TooltipManager manager, int id) {
			this.manager = new WeakReference<TooltipManager>(manager);
			this.id = id;
		}

		/**
		 * Use a custom View for the tooltip. Note that the custom view
		 * must include a TextView which id is `@android:id/text1`.<br />
		 * Moreover, when using a custom view, the anchor arrow will not be shown
		 *
		 * @param resId              the custom layout view.
		 * @param replace_background if true the custom view's background won't be replaced
		 * @return
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

		public Builder text(Resources res, int resid) {
			return text(res.getString(resid));
		}

		public Builder text(int resid) {
			TooltipManager tipManager = manager.get();
			if (null != tipManager) {
				if (null != tipManager.mActivity) {
					return text(tipManager.mActivity.getResources().getString(resid));
				}
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
		 * @return
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
			if (null == point && null == view) throw new IllegalStateException("Target point or target view must be specified");
			if (gravity == Gravity.CENTER) hideArrow = true;

			TooltipManager tmanager = this.manager.get();
			if (null != tmanager) {
				return tmanager.show(this, false);
			}
			return false;
		}
	}

	public static enum ClosePolicy {
		/**
		 * tooltip will hide when touching it, or after the specified delay.
		 * If delay is '0' the tooltip will never hide until clicked
		 */
		TouchInside,
		/**
		 * tooltip will hide when user touches the screen, or after the specified delay.
		 * If delay is '0' the tooltip will never hide until clicked
		 */
		TouchOutside,
		/**
		 * tooltip is hidden only after the specified delay
		 */
		None
	}

	public static enum Gravity {
		LEFT, RIGHT, TOP, BOTTOM, CENTER
	}


	public static void removeInstance(Activity activity) {
		if (DBG) {
			Log.i(TAG, "removeInstance: " + activity + ", hashCode: " + activity.hashCode());
			Log.v(TAG, "instances: " + instances.size());
		}

		TooltipManager sInstance = instances.remove(activity.hashCode());

		if (sInstance != null) {
			synchronized (TooltipManager.class) {
				if (DBG) Log.d(TAG, "destroying instance: " + sInstance);
				sInstance.destroy();
			}
		}
	}

	public static TooltipManager getInstance(Activity activity) {
		if (DBG) Log.i(TAG, "getInstance: " + activity + ", hashCode: " + activity.hashCode());

		TooltipManager sInstance = instances.get(activity.hashCode());

		if (DBG) {
			Log.v(TAG, "instances: " + instances.size());
			Log.v(TAG, "sInstance: " + sInstance);
		}

		if (sInstance == null) {
			synchronized (TooltipManager.class) {
				sInstance = instances.get(activity.hashCode());
				if (sInstance == null) {
					synchronized (TooltipManager.class) {
						sInstance = new TooltipManager(activity);
						instances.putIfAbsent(activity.hashCode(), sInstance);
					}
				}
			}
		}
		return sInstance;
	}
}
