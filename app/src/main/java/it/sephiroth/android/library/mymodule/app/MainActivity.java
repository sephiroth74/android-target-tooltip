package it.sephiroth.android.library.mymodule.app;

import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.tooltip.TooltipManager;


public class MainActivity extends ActionBarActivity implements AbsListView.OnScrollListener, AdapterView.OnItemClickListener {

	private static final String TAG = "MainActivity";
	TextView mTextView;
	final Handler handler = new Handler();
	TooltipManager tooltipManager;
	ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		List<String> array = new ArrayList<String>();
		for (int i = 0; i < 100; i++) {
			array.add(String.format("Item %d", i));
		}

		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, array));
		listView.setOnScrollListener(this);
		listView.setOnItemClickListener(this);

		tooltipManager = TooltipManager.getInstance(this);
	}

	@Override
	protected void onDestroy() {
		TooltipManager.removeInstance(this);
		super.onDestroy();
	}

	private int getActionBarSize() {
		if (Build.VERSION.SDK_INT >= 14) {
			int[] attrs = {android.R.attr.actionBarSize};
			TypedArray values = getTheme().obtainStyledAttributes(attrs);
			try {
				return values.getDimensionPixelSize(0, 0);
			} finally {
				values.recycle();
			}
		}

		int[] attrs = {R.attr.actionBarSize};
		TypedArray values = obtainStyledAttributes(attrs);
		try {
			return values.getDimensionPixelSize(0, 0);
		} finally {
			values.recycle();
		}
	}

	@Override
	public void onSupportContentChanged() {
		super.onSupportContentChanged();
		mTextView = (TextView) findViewById(android.R.id.text1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onScrollStateChanged(final AbsListView view, final int scrollState) {
		Log.i(TAG, "onScrollStateChanged: " + scrollState);

		if (scrollState == SCROLL_STATE_IDLE) {
			handler.removeCallbacks(showRunnable);
			handler.postDelayed(showRunnable, 100);
		}
		else if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
			handler.removeCallbacks(showRunnable);

			if(tooltipManager.active(100)){
				tooltipManager.remove(100);
			}
		}
	}


	Runnable showRunnable = new Runnable() {
		@Override
		public void run() {
			View child = listView.getChildAt(listView.getChildCount() / 2);
			tooltipManager
				.create(100)
				.maxWidth(450)
				.actionBarSize(getActionBarSize())
				.activateDelay(3000)
				.anchor(child, TooltipManager.Gravity.BOTTOM)
				.closePolicy(TooltipManager.ClosePolicy.TouchOutside, 0)
				.text("Test tooltip showing on a list, Test tooltip showing on a list, Test tooltip showing on a list...")
				.show();
		}
	};

	@Override
	public void onScroll(
		final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {

		if(null != tooltipManager) {
			tooltipManager.update(100);
		}
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		Log.d(TAG, "onItemClick: " + position);
	}
}
