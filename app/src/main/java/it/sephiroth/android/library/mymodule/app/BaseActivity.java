package it.sephiroth.android.library.mymodule.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.tooltip.Tooltip;
import it.sephiroth.android.library.tooltip.Tooltip.AnimationBuilder;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_demo2) {
            startActivity(new Intent(this, BaseActivity.class));
        } else if (id == R.id.action_demo3) {
            startActivity(new Intent(this, MainActivity3.class));
        } else if (id == R.id.action_demo4) {
            startActivity(new Intent(this, MainActivity4.class));
        }
        return super.onOptionsItemSelected(item);
    }

}
