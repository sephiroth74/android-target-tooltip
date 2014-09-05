package it.sephiroth.android.library.mymodule.app;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Point;
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
	public void onScrollStateChanged(final AbsListView view, final int scrollState) {
		Log.i(TAG, "onScrollStateChanged: " + scrollState);

		if (scrollState == SCROLL_STATE_IDLE) {
			handler.removeCallbacks(showRunnable);
			handler.postDelayed(showRunnable, 100);
		}
		else if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
			handler.removeCallbacks(showRunnable);

			if (tooltipManager.active(100)) {
				tooltipManager.remove(100);
			}
		}
	}


	Runnable showRunnable = new Runnable() {
		@Override
		public void run() {
			View child = listView.getChildAt(0);
			tooltipManager.create(100)
			              .maxWidth(450)
			              .actionBarSize(Utils.getActionBarSize(getBaseContext()))
			              .activateDelay(100)
			              .anchor(new Point(512, 100), TooltipManager.Gravity.BOTTOM)
			              .closePolicy(TooltipManager.ClosePolicy.None, 1500)
			              .withCustomView(R.layout.custom_textview)
			              .withStyleId(R.style.ToolTipLayoutCustomStyle)
			              .text("Test tooltip showing on a list, Test tooltip showing on a list, Test tooltip showing on a list...")
			              .show();
		}
	};

	@Override
	public void onScroll(
		final AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {

		if (null != tooltipManager) {
			tooltipManager.update(100);
		}
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		Log.d(TAG, "onItemClick: " + position);
	}
}
