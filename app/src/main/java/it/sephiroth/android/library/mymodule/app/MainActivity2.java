package it.sephiroth.android.library.mymodule.app;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.tooltip.TooltipManager;

public class MainActivity2 extends AppCompatActivity implements OnPageChangeListener {
    private static final String TAG = MainActivity2.class.getSimpleName();
    TooltipManager mTooltipManager;
    ViewPager mViewPager;
    private Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);

        mTooltipManager = new TooltipManager(this);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.addOnPageChangeListener(this);
        mAdapter = setupViewPager(mViewPager);
    }

    private Adapter setupViewPager(ViewPager viewPager) {

        if (viewPager.getAdapter() != null) {
            return (Adapter) viewPager.getAdapter();
        }

        Adapter adapter = new Adapter(getSupportFragmentManager());

        adapter.addFragment(new Fragment1(), "Tab 1");
        adapter.addFragment(new Fragment1(), "Tab 2");

        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        return adapter;
    }

    private void test() {
        TabLayout tablayout = (TabLayout) findViewById(R.id.tabs);
        final ViewGroup root = (ViewGroup) tablayout.getChildAt(0);
        final View tab = root.getChildAt(1);

        mTooltipManager.show(
            new TooltipManager.Builder(101)
                .anchor(tab, TooltipManager.Gravity.BOTTOM)
                .closePolicy(TooltipManager.ClosePolicy.TouchAnyWhere, 3000)
                .text("Tooltip on a TabLayout child...")
                .fadeDuration(200)
                .fitToScreen(false)
                .maxWidth(400)
                .showDelay(400)
                .toggleArrow(true)
                .build());
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
            startActivity(new Intent(this, MainActivity2.class));
        } else if (id == R.id.action_demo3) {
            startActivity(new Intent(this, MainActivity3.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mViewPager.removeOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(final int position) {

    }

    @Override
    public void onPageScrollStateChanged(final int state) {

    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();
        private Fragment mCurrentFragment;

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }

        public Fragment getCurrentFragment() {
            return mCurrentFragment;
        }

        @Override
        public void setPrimaryItem(final ViewGroup container, final int position, final Object object) {
            if (getCurrentFragment() != object) {
                mCurrentFragment = ((Fragment) object);
            }

            super.setPrimaryItem(container, position, object);
        }
    }

    static int tooltip_id = 0;

    public static class Fragment1 extends Fragment implements View.OnClickListener, TooltipManager.onTooltipClosingCallback {
        Button mButton1;
        Button mButton2;
        Button mButton3;
        Button mButton4;
        Button mButton5;
        Button mButton6;
        private final int ID_BUTTON1 = tooltip_id++;
        private final int ID_BUTTON2 = tooltip_id++;
        private final int ID_BUTTON3 = tooltip_id++;
        private final int ID_BUTTON4 = tooltip_id++;
        private final int ID_BUTTON5 = tooltip_id++;
        private final int ID_BUTTON6 = tooltip_id++;
        private TooltipManager mTooltipManager;

        @Nullable
        @Override
        public View onCreateView(
            final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
            return inflater.inflate(R.layout.activity2_fragment1, container, false);
        }

        @Override
        public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mButton1 = (Button) view.findViewById(R.id.button1);
            mButton2 = (Button) view.findViewById(R.id.button2);
            mButton3 = (Button) view.findViewById(R.id.button3);
            mButton4 = (Button) view.findViewById(R.id.button4);
            mButton5 = (Button) view.findViewById(R.id.button5);
            mButton6 = (Button) view.findViewById(R.id.button6);
            mButton1.setOnClickListener(this);
            mButton2.setOnClickListener(this);
            mButton3.setOnClickListener(this);
            mButton4.setOnClickListener(this);
            mButton5.setOnClickListener(this);
            mButton6.setOnClickListener(this);
        }

        @Override
        public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mTooltipManager = new TooltipManager(getActivity());
        }

        @Override
        public void onClick(final View v) {
            final int id = v.getId();

            Log.i(TAG, "onClick: " + id);

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            TooltipManager.Gravity gravity =
                TooltipManager.Gravity.values()[((int) (Math.random() * TooltipManager.Gravity.values().length))];

            if (id == mButton1.getId()) {
                mTooltipManager.show(
                    new TooltipManager.Builder(ID_BUTTON1)
                        .anchor(mButton1, TooltipManager.Gravity.BOTTOM)
                        .actionBarSize(Utils.getActionBarSize(getActivity()))
                        .closePolicy(TooltipManager.ClosePolicy.TouchOutside, 0)
                        .text(R.string.hello_world)
                        .toggleArrow(true)
                        .maxWidth(400)
                        .withStyleId(R.style.ToolTipLayoutDefaultStyle_TextColor1)
                        .build());

            } else if (id == mButton2.getId()) {

                mTooltipManager.show(
                    new TooltipManager.Builder(ID_BUTTON2)
                        .anchor(mButton2, TooltipManager.Gravity.BOTTOM)
                        .actionBarSize(Utils.getActionBarSize(getActivity()))
                        .closePolicy(TooltipManager.ClosePolicy.TouchInside, 0)
                        .text(R.string.hello_world)
                        .toggleArrow(true)
                        .maxWidth(400)
                        .withCallback(this)
                        .build());

                mButton2.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final ViewGroup.LayoutParams params = mButton2.getLayoutParams();
                        params.height += 100;
                        mButton2.requestLayout();

                    }
                }, 100);

            } else if (id == mButton3.getId()) {
                mTooltipManager.show(
                    new TooltipManager.Builder(ID_BUTTON3)
                        .anchor(mButton3, TooltipManager.Gravity.BOTTOM)
                        .actionBarSize(Utils.getActionBarSize(getActivity()))
                        .closePolicy(TooltipManager.ClosePolicy.TouchOutsideExclusive, 0)
                        .text("Touch outside exclusive")
                        .toggleArrow(true)
                        .maxWidth(400)
                        .withCallback(this)
                        .build());

            } else if (id == mButton4.getId()) {
                mTooltipManager.show(
                    new TooltipManager.Builder(ID_BUTTON4)
                        .anchor(mButton4, TooltipManager.Gravity.BOTTOM)
                        .actionBarSize(Utils.getActionBarSize(getActivity()))
                        .closePolicy(TooltipManager.ClosePolicy.TouchInsideExclusive, 0)
                        .withCustomView(R.layout.custom_textview, false)
                        .text("Custom view with touch inside exclusive")
                        .toggleArrow(true)
                        .maxWidth(300)
                        .withCallback(this)
                        .build());

            } else if (id == mButton5.getId()) {
                mTooltipManager.show(
                    new TooltipManager.Builder(ID_BUTTON5)
                        .anchor(new Point(metrics.widthPixels / 2, 250), TooltipManager.Gravity.BOTTOM)
                        .actionBarSize(Utils.getActionBarSize(getActivity()))
                        .closePolicy(TooltipManager.ClosePolicy.TouchOutsideExclusive, 0)
                        .withCustomView(R.layout.custom_textview, true)
                        .text("Custom view, custom background, activate delay, touch outside exclusive")
                        .toggleArrow(true)
                        .maxWidth(600)
                        .showDelay(300)
                        .activateDelay(2000)
                        .withCallback(this)
                        .build());

            } else if (id == mButton6.getId()) {
                mTooltipManager.show(
                    new TooltipManager.Builder(ID_BUTTON6)
                        .anchor(v, TooltipManager.Gravity.TOP)
                        .actionBarSize(Utils.getActionBarSize(getActivity()))
                        .closePolicy(TooltipManager.ClosePolicy.TouchAnyWhere, 0)
                        .text("Touch Anywhere to dismiss the tooltip")
                        .toggleArrow(true)
                        .maxWidth(400)
                        .showDelay(300)
                        .withCallback(this)
                        .build());
            }
        }

        @Override
        public void onClosing(final int id, final boolean fromUser, final boolean containsTouch) {
            Log.d(TAG, "onClosing: " + id + ", fromUser: " + fromUser + ", containsTouch: " + containsTouch);
        }
    }
}
