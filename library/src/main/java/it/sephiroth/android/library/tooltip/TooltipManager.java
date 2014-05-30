package it.sephiroth.android.library.tooltip;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

public class TooltipManager implements ToolTipLayout.OnCloseListener, ToolTipLayout.OnToolTipListener {
	static final boolean DBG = false;
	private static ConcurrentHashMap<Activity, TooltipManager> instances = new ConcurrentHashMap<Activity, TooltipManager>();
	private static final String TAG = "TooltipManager";


	final ConcurrentHashMap<Integer, ToolTipLayout> mTooltips = new ConcurrentHashMap<Integer, ToolTipLayout>();

	final Activity mActivity;

	public TooltipManager(final Activity activity) {
		if(DBG) Log.i(TAG, "TooltipManager: " + activity);
		mActivity = activity;
	}

	public Builder create(int id) {
		return new Builder(this, id);
	}

	private boolean show(Builder builder) {
		if(DBG) Log.i(TAG, "show");

		if (mTooltips.containsKey(builder.id)) {
			Log.w(TAG, "A Tooltip with the same id was walready specified");
			return false;
		}

		ToolTipLayout layout = new ToolTipLayout(mActivity, builder);
		layout.setOnCloseListener(this);
		layout.setOnToolTipListener(this);
		mTooltips.put(builder.id, layout);
		showInternal(layout);
		printStats();
		return true;
	}

	public void hide(int id) {
		if(DBG) Log.i(TAG, "hide: " + id);

		ToolTipLayout layout = mTooltips.remove(id);
		if (null != layout) {
			layout.setOnCloseListener(null);
			layout.doHide();
		}
		printStats();
	}

	public void remove(int id) {
		if(DBG) Log.i(TAG, "remove: " + id);

		ToolTipLayout layout = mTooltips.remove(id);
		if (null != layout) {
			layout.setOnCloseListener(null);
			layout.setOnToolTipListener(null);
			layout.removeFromParent();
		}
		printStats();
	}

	public void setText(int id, final CharSequence text) {
		ToolTipLayout layout = mTooltips.get(id);
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
		if(DBG) Log.i(TAG, "destroy");
		for (int id : mTooltips.keySet()) {
			remove(id);
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

	@Override
	public void onClose(final ToolTipLayout layout) {
		if(DBG) Log.i(TAG, "onClose: " + layout.getTooltipId());
		hide(layout.getTooltipId());
	}

	@Override
	public void onHideCompleted(final ToolTipLayout layout) {
		if(DBG) Log.i(TAG, "onHideCompleted: " + layout.getTooltipId());
		layout.removeFromParent();
		printStats();
	}

	@Override
	public void onShowCompleted(final ToolTipLayout layout) {
		if(DBG) Log.i(TAG, "onShowCompleted: " + layout.getTooltipId());
	}

	public static final class Builder {
		int id;
		CharSequence text;
		View view;
		Gravity gravitiy;
		int maxWidth = - 1;
		int ellipseSize = 12;
		int strokeWidth = 4;
		int backgroundColor = 0xEE131518;
		int strokeColor = 0xFFe9ecef;
		int actionbarSize = 0;
		int textResId = R.layout.tooltip_textview;
		ClosePolicy closePolicy;
		long showDuration;
		Point point;
		WeakReference<TooltipManager> manager;
		long showDelay = 0;

		Builder(final TooltipManager manager, int id) {
			this.manager = new WeakReference<TooltipManager>(manager);
			this.id = id;
		}

		public Builder textResId(int resId) {
			this.textResId = resId;
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
			this.gravitiy = gravity;
			return this;
		}

		public Builder anchor(final Point point, final Gravity gravity) {
			this.view = null;
			this.point = new Point(point);
			this.gravitiy = gravity;
			return this;
		}

		public Builder strokeWidth(int size) {
			this.strokeWidth = size;
			return this;
		}

		public Builder cornerRadius(int size) {
			this.ellipseSize = size;
			return this;
		}

		public Builder strokeColor(int color) {
			this.strokeColor = color;
			return this;
		}

		public Builder backgroundColor(int color) {
			this.backgroundColor = color;
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

		public Builder showDelay(long ms) {
			this.showDelay = ms;
			return this;
		}

		public boolean show() {
			TooltipManager tmanager = this.manager.get();
			if (null != tmanager) {
				return tmanager.show(this);
			}
			return false;
		}
	}

	public static enum ClosePolicy {
		None, TouchOutside, TouchInside
	}

	public static enum Gravity {
		LEFT, RIGHT, TOP, BOTTOM
	}


	public static void removeInstance(Activity activity) {
		if(DBG) Log.i(TAG, "removeInstance: " + activity);
		TooltipManager sInstance = instances.remove(activity);

		if (sInstance != null) {
			synchronized (TooltipManager.class) {
				sInstance.destroy();
			}
		}
	}

	public static TooltipManager getInstance(Activity activity) {
		if(DBG) Log.i(TAG, "getInstance: " + activity);
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
