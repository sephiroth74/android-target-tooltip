package it.sephiroth.android.library.tooltip;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class TooltipManager {
	static final boolean DBG = false;

	private static ConcurrentHashMap<Activity, TooltipManager> instances = new ConcurrentHashMap<Activity, TooltipManager>();
	private static final String TAG = "TooltipManager";


	final Object lock = new Object();
	final HashMap<Integer, ToolTipLayout> mTooltips = new HashMap<Integer, ToolTipLayout>();
	final Activity mActivity;

	private ToolTipLayout.OnCloseListener mCloseListener = new ToolTipLayout.OnCloseListener() {
		@Override
		public void onClose(final ToolTipLayout layout) {
			if (DBG) Log.i(TAG, "onClose: " + layout.getTooltipId());
			hide(layout.getTooltipId());
		}
	};

	private ToolTipLayout.OnToolTipListener mTooltipListener = new ToolTipLayout.OnToolTipListener() {
		@Override
		public void onHideCompleted(final ToolTipLayout layout) {
			if (DBG) Log.i(TAG, "onHideCompleted: " + layout.getTooltipId());
			layout.removeFromParent();
			printStats();
		}

		@Override
		public void onShowCompleted(final ToolTipLayout layout) {
			if (DBG) Log.i(TAG, "onShowCompleted: " + layout.getTooltipId());
		}

		@Override
		public void onShowFailed(final ToolTipLayout layout) {
			if (DBG) Log.i(TAG, "onShowFailed: " + layout.getTooltipId());
			remove(layout.getTooltipId());
		}
	};

	public TooltipManager(final Activity activity) {
		if (DBG) Log.i(TAG, "TooltipManager: " + activity);
		mActivity = activity;
	}

	public Builder create(int id) {
		return new Builder(this, id);
	}

	private boolean show(Builder builder) {
		if (DBG) Log.i(TAG, "show");

		synchronized (lock) {
			if (mTooltips.containsKey(builder.id)) {
				Log.w(TAG, "A Tooltip with the same id was walready specified");
				return false;
			}

			ToolTipLayout layout = new ToolTipLayout(mActivity, builder);
			layout.setOnCloseListener(mCloseListener);
			layout.setOnToolTipListener(mTooltipListener);
			mTooltips.put(builder.id, layout);
			showInternal(layout);
		}
		printStats();
		return true;
	}

	public void hide(int id) {
		if (DBG) Log.i(TAG, "hide: " + id);

		final ToolTipLayout layout;
		synchronized (lock) {
			layout = mTooltips.remove(id);
		}
		if (null != layout) {
			layout.setOnCloseListener(null);
			layout.doHide();
			printStats();
		}
	}

	public void update(int id) {
		final ToolTipLayout layout;
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

		final ToolTipLayout layout;
		synchronized (lock) {
			layout = mTooltips.remove(id);
		}

		if (null != layout) {
			layout.setOnCloseListener(null);
			layout.setOnToolTipListener(null);
			layout.removeFromParent();
		}
		printStats();
	}

	public void setText(int id, final CharSequence text) {
		ToolTipLayout layout;
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
		printStats();
	}

	private void showInternal(ToolTipLayout layout) {
		if (null != mActivity) {
			ViewGroup decor = (ViewGroup) mActivity.getWindow().getDecorView();
			if (null == decor) return;
			if (layout.getParent() == null) {
				if (DBG) Log.v(TAG, "attach to mToolTipLayout parent");
				ViewGroup.LayoutParams params =
					new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
				decor.addView(layout, params);
			}
			layout.doShow();
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

		Builder(final TooltipManager manager, int id) {
			this.manager = new WeakReference<TooltipManager>(manager);
			this.id = id;
		}

		/**
		 * Use a custom View for the tooltip. Note that the custom view
		 * must include a TextView which id is `@android:id/text1`
		 *
		 * @param resId
		 * @return
		 */
		public Builder withCustomView(int resId) {
			this.textResId = resId;
			return this;
		}

		public Builder withStyleId(int styleId) {
			this.defStyleAttr = 0;
			this.defStyleRes = styleId;
			return this;
		}

		public Builder text(Context context, int resid) {
			return text(context.getString(resid));
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

		public Builder toggleArrow(boolean show) {
			this.hideArrow = ! show;
			return this;
		}

		public Builder actionBarSize(final int actionBarSize) {
			this.actionbarSize = actionBarSize;
			return this;
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
				return tmanager.show(this);
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
		if (DBG) Log.i(TAG, "removeInstance: " + activity);
		TooltipManager sInstance = instances.remove(activity);

		if (sInstance != null) {
			synchronized (TooltipManager.class) {
				sInstance.destroy();
			}
		}
	}

	public static TooltipManager getInstance(Activity activity) {
		if (DBG) Log.i(TAG, "getInstance: " + activity);
		TooltipManager sInstance = instances.get(activity);

		if (sInstance == null) {
			synchronized (TooltipManager.class) {
				sInstance = instances.get(activity);
				if (sInstance == null) {
					synchronized (TooltipManager.class) {
						sInstance = new TooltipManager(activity);
						instances.putIfAbsent(activity, sInstance);
					}
				}
			}
		}
		return sInstance;
	}
}
