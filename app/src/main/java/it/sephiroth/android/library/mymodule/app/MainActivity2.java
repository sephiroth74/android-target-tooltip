package it.sephiroth.android.library.mymodule.app;

import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import it.sephiroth.android.library.tooltip.TooltipManager;


public class MainActivity2 extends ActionBarActivity implements View.OnClickListener {

	private static final String TAG = MainActivity2.class.getSimpleName();

	Button mButton1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_activity2);

		mButton1 = (Button) findViewById(R.id.button1);
		mButton1.setOnClickListener(this);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity2, menu);
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
	public void onClick(final View v) {
		final int id = v.getId();

		Log.i(TAG, "onClick: " + id);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		TooltipManager.Gravity gravity =
			TooltipManager.Gravity.values()[((int) (Math.random() * TooltipManager.Gravity.values().length))];

		if (id == mButton1.getId()) {
			TooltipManager.getInstance(this)
			              .create(0)
			              .anchor(
				              new Point(
					              250, 250
				              ), TooltipManager.Gravity.BOTTOM
			              )
			              .actionBarSize(getActionBarSize())
			              .closePolicy(TooltipManager.ClosePolicy.TouchOutside, 0)
			              .text(R.string.hello_world)
			              .toggleArrow(false)
			              .maxWidth(400)
			              .showDelay(300)
			              .show();
		}
	}

	private int getTopRule() {
		return getActionBarSize() + getStatusBarHeight();
	}

	private int getActionBarSize() {
		final int[] attrs;
		if (Build.VERSION.SDK_INT >= 14) {
			attrs = new int[]{android.R.attr.actionBarSize};
		}
		else {
			attrs = new int[]{R.attr.actionBarSize};
		}
		TypedArray values = getTheme().obtainStyledAttributes(attrs);
		try {
			return values.getDimensionPixelSize(0, 0);
		} finally {
			values.recycle();
		}
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TooltipManager.removeInstance(this);
	}
}
