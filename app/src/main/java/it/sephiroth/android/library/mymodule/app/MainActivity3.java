package it.sephiroth.android.library.mymodule.app;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.tooltip.Tooltip;
import it.sephiroth.android.library.tooltip.TooltipManager;


public class MainActivity3 extends ActionBarActivity
	implements AdapterView.OnItemClickListener, MyTextView.OnAttachStatusListener, TooltipManager.OnTooltipAttachedStateChange,
	           ViewTreeObserver.OnPreDrawListener {

	private static final String TAG = "MainActivity3";
	ListView listView;

	TooltipManager tooltipManager;

	static final int TOOLTIP_ID = 101;

	static final int LIST_POSITION = 15;

	private Tooltip tooltipView;

	private View mView;

	private int[] mTempLocation = {0, 0};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		List<String> array = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			array.add(String.format("Item %d", i));
		}

		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new MyAdapter(this, R.layout.custom_list_textview, android.R.id.text1, array));
		listView.setOnItemClickListener(this);

		tooltipManager = TooltipManager.getInstance(this);
		tooltipManager.addOnTooltipAttachedStateChange(this);

		tooltipManager.create(TOOLTIP_ID)
		              .maxWidth(200)
		              .anchor(new Point(80, 0), TooltipManager.Gravity.RIGHT)
		              .closePolicy(TooltipManager.ClosePolicy.TouchInside, 0)
		              .text("Brigthness, Saturation, Contrast and Warmth are now here!")
		              .actionBarSize(Utils.getActionBarSize(this))
		              .fitToScreen(false)
		              .fadeDuration(100)
		              .build();
	}

	@Override
	protected void onDestroy() {
		TooltipManager.getInstance(this).removeOnTooltipAttachedStateChange(this);
		TooltipManager.removeInstance(this);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_demo1) {
			startActivity(new Intent(this, MainActivity.class));
		}
		else if (id == R.id.action_demo2) {
			startActivity(new Intent(this, MainActivity2.class));
		}
		else if (id == R.id.action_demo3) {
			startActivity(new Intent(this, MainActivity3.class));
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		Log.d(TAG, "onItemClick: " + position);

		if (position == LIST_POSITION) {
			tooltipManager.remove(TOOLTIP_ID);
		}
	}

	@Override
	public void onAttachedtoWindow(final View view) {
		Log.i(TAG, "onAttachedtoWindow: " + view);

		if (null != tooltipView) {
			tooltipView.show();
			view.getViewTreeObserver().addOnPreDrawListener(this);
		}
	}

	@Override
	public void onDetachedFromWindow(final View view) {
		onFinishTemporaryDetach(view);
	}

	@Override
	public void onFinishTemporaryDetach(final View view) {
		Log.i(TAG, "onFinishTemporaryDetach: " + view);

		if (null != tooltipView && tooltipView.isShown()) {
			tooltipView.hide(false);
		}
		removeListeners();
	}

	@Override
	public void onTooltipAttached(final int id) {
		Log.i(TAG, "onTooltipAttached: " + id);

		if (id == TOOLTIP_ID) {
			tooltipView = tooltipManager.get(id);
		}
	}

	@Override
	public void onTooltipDetached(final int id) {
		Log.i(TAG, "onTooltipDetached: " + id);
		removeListeners();

		if (id == TOOLTIP_ID) {
			tooltipView = null;
		}
	}

	private void removeListeners() {
		Log.i(TAG, "removeListeners");

		if (null != mView) {
			mView.getViewTreeObserver().removeOnPreDrawListener(this);
			((MyTextView) mView).setOnAttachStatusListener(null);
			mView = null;
		}
	}

	@Override
	public boolean onPreDraw() {
		if (null == mView || ! mView.isShown() || null == tooltipView) {
			Log.e(TAG, "!isShown");
			return true;
		}

		mView.getLocationOnScreen(mTempLocation);
		tooltipView.setOffsetY(mTempLocation[1]);

//		Log.v(TAG, "onPreDraw::location: " + mTempLocation[0] + "x" + mTempLocation[1]);

		return true;
	}


	class MyAdapter extends ArrayAdapter<String> {

		public MyAdapter(final Context context, final int resource, final int textViewResourceId, final List<String> objects) {
			super(context, resource, textViewResourceId, objects);
		}

		@TargetApi (Build.VERSION_CODES.HONEYCOMB_MR1)
		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {

			final View result = super.getView(position, convertView, parent);

			if (position == LIST_POSITION && null != tooltipView) {

				final Handler handler = listView.getHandler();
				if (null != handler) {
					handler.post(
						new Runnable() {
							@Override
							public void run() {
								((MyTextView) result).setOnAttachStatusListener((MainActivity3) getContext());
								MainActivity3.this.onAttachedtoWindow(result);
								mView = result;
							}
						}
					);
				}
				else {
					Log.e(TAG, "handler is null!");
				}
			}

			return result;
		}
	}
}
