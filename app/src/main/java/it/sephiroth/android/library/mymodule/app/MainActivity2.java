package it.sephiroth.android.library.mymodule.app;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import it.sephiroth.android.library.tooltip.TooltipManager;

public class MainActivity2 extends ActionBarActivity implements View.OnClickListener, TooltipManager.onTooltipClosingCallback {
    private static final String TAG = MainActivity2.class.getSimpleName();
    Button mButton1;
    Button mButton2;
    Button mButton3;
    Button mButton4;
    Button mButton5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);

        mButton1 = (Button) findViewById(R.id.button1);
        mButton2 = (Button) findViewById(R.id.button2);
        mButton3 = (Button) findViewById(R.id.button3);
        mButton4 = (Button) findViewById(R.id.button4);
        mButton5 = (Button) findViewById(R.id.button5);
        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
        mButton3.setOnClickListener(this);
        mButton4.setOnClickListener(this);
        mButton5.setOnClickListener(this);
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
        } else if (id == R.id.action_demo2) {
            startActivity(new Intent(this, MainActivity2.class));
        } else if (id == R.id.action_demo3) {
            startActivity(new Intent(this, MainActivity3.class));
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

        TooltipManager manager = TooltipManager.getInstance();

        if (id == mButton1.getId()) {
            manager.create(this, 0)
                .anchor(mButton1, TooltipManager.Gravity.RIGHT)
                .actionBarSize(Utils.getActionBarSize(getBaseContext()))
                .closePolicy(TooltipManager.ClosePolicy.TouchOutside, 3000)
                .text(R.string.hello_world)
                .toggleArrow(true)
                .maxWidth(400)
                .showDelay(300)
                .withCallback(this)
                .show();
        } else if (id == mButton2.getId()) {
            manager.create(this, 1)
                .anchor(mButton2, TooltipManager.Gravity.LEFT)
                .actionBarSize(Utils.getActionBarSize(getBaseContext()))
                .closePolicy(TooltipManager.ClosePolicy.TouchInside, 0)
                .text(R.string.hello_world)
                .toggleArrow(true)
                .maxWidth(400)
                .withCallback(this)
                .show();
        } else if (id == mButton3.getId()) {
            manager.create(this, 2)
                .anchor(mButton3, TooltipManager.Gravity.BOTTOM)
                .actionBarSize(Utils.getActionBarSize(getBaseContext()))
                .closePolicy(TooltipManager.ClosePolicy.TouchOutsideExclusive, 0)
                .text("Touch outside exclusive")
                .toggleArrow(true)
                .maxWidth(400)
                .withCallback(this)
                .show();
        } else if (id == mButton4.getId()) {
            manager.create(this, 2)
                .anchor(mButton4, TooltipManager.Gravity.BOTTOM)
                .actionBarSize(Utils.getActionBarSize(getBaseContext()))
                .closePolicy(TooltipManager.ClosePolicy.TouchInsideExclusive, 0)
                .withCustomView(R.layout.custom_textview, false)
                .text("Custom view with touch inside exclusive")
                .toggleArrow(false)
                .maxWidth(300)
                .withCallback(this)
                .show();
        } else if (id == mButton5.getId()) {
            manager.create(this, 2)
                .anchor(new Point(metrics.widthPixels / 2, 250), TooltipManager.Gravity.BOTTOM)
                .actionBarSize(Utils.getActionBarSize(getBaseContext()))
                .closePolicy(TooltipManager.ClosePolicy.TouchOutsideExclusive, 0)
                .withCustomView(R.layout.custom_textview, true)
                .text("Custom view, custom background, activate delay, touch outside exclusive")
                .toggleArrow(true)
                .maxWidth(600)
                .showDelay(300)
                .activateDelay(2000)
                .withCallback(this)
                .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClosing(final int id, final boolean fromUser, final boolean containsTouch) {
        Log.d(TAG, "onClosing: " + id + ", fromUser: " + fromUser + ", containsTouch: " + containsTouch);
    }
}
