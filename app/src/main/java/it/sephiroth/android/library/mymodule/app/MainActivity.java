package it.sephiroth.android.library.mymodule.app;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import it.sephiroth.android.library.tooltip.TooltipManager;


public class MainActivity extends ActionBarActivity {

	TextView mTextView;
	final Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final TooltipManager tooltipManager = TooltipManager.getInstance(this);

		//@formatter:off
		final TooltipManager.Builder builder = new TooltipManager.Builder(1)
			.text("<font color='#006699'>Hello</font> Damn Rotten World!")
			.anchor(mTextView, TooltipManager.Gravity.BOTTOM)
		    .strokeWidth(4)
		    .cornerRadius(14)
			.actionBarSize(getActionBarSize())
			.textResId(R.layout.custom_textview)
			.closePolicy(TooltipManager.ClosePolicy.None, 5000)
			.maxWidth(400);
		//@formatter:on

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				tooltipManager.show(builder);
			}
		}, 1000);

		findViewById(android.R.id.text1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				TooltipManager.getInstance(MainActivity.this).show(builder);
			}
		});

		findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				TooltipManager.Builder builder = new TooltipManager.Builder(2).actionBarSize(getActionBarSize()).text("Swipe " +
				                                                                                                      "left" +
				                                                                                                      " to see " +
				                                                                                                      "more " +
				                                                                                                      "content!")
				                                                              .anchor(v, TooltipManager.Gravity.BOTTOM)
				                                                              .closePolicy(TooltipManager.ClosePolicy.None, 3000);
				TooltipManager.getInstance(MainActivity.this).show(builder);
			}
		});

		findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				TooltipManager.getInstance(MainActivity.this).hide(2);
			}
		});

		findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				TooltipManager.getInstance(MainActivity.this).remove(1);
			}
		});

		findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {

				DisplayMetrics metrics = getResources().getDisplayMetrics();

				TooltipManager.Builder builder = new TooltipManager.Builder(3)
					.text("Swipe this way right<br />or you can die moron!!!")
					.anchor(new Point(metrics.widthPixels, getActionBarSize() + 100), TooltipManager.Gravity.LEFT)
					.actionBarSize(getActionBarSize())
					.maxWidth(400)
					.closePolicy(TooltipManager.ClosePolicy.TouchOutside, 5000);
				TooltipManager.getInstance(MainActivity.this).show(builder);
			}
		});
	}

	@Override
	protected void onDestroy() {
		TooltipManager.removeInstance(this);
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		Log.i("TAG", "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
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
}
