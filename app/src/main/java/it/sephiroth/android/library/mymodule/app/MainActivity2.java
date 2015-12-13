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

import it.sephiroth.android.library.tooltip.Tooltip;
import it.sephiroth.android.library.tooltip.Tooltip.AnimationBuilder;

public class MainActivity2 extends AppCompatActivity implements OnPageChangeListener {
    private static final String TAG = MainActivity2.class.getSimpleName();
    ViewPager mViewPager;
    private Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);
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

        Tooltip.make(this,
                new Tooltip.Builder(101)
                        .anchor(tab, Tooltip.Gravity.BOTTOM)
                        .closePolicy(Tooltip.ClosePolicy.TouchAnyWhere, 3000)
                        .text("Tooltip on a TabLayout child...")
                        .fadeDuration(200)
                        .fitToScreen(false)
                        .maxWidth(400)
                        .showDelay(400)
                        .toggleArrow(true)
                        .build()).show();
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

    public static class Fragment1 extends Fragment implements View.OnClickListener, Tooltip.Callback {
        Button mButton1;
        Button mButton2;
        Button mButton3;
        Button mButton4;
        Button mButton5;
        private final int ID_BUTTON1 = tooltip_id++;
        private final int ID_BUTTON2 = tooltip_id++;
        private final int ID_BUTTON3 = tooltip_id++;
        private final int ID_BUTTON4 = tooltip_id++;
        private final int ID_BUTTON5 = tooltip_id++;
        private Tooltip.TooltipView tooltip;

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
            mButton1.setOnClickListener(this);
            mButton2.setOnClickListener(this);
            mButton3.setOnClickListener(this);
            mButton4.setOnClickListener(this);
            mButton5.setOnClickListener(this);
        }

        @Override
        public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public void onClick(final View v) {
            final int id = v.getId();

            Log.i(TAG, "onClick: " + id);

            DisplayMetrics metrics = getResources().getDisplayMetrics();

            if (id == mButton1.getId()) {

                Tooltip.make(getContext(),
                        new Tooltip.Builder(ID_BUTTON1)
                                .anchor(v, Tooltip.Gravity.RIGHT)
                                .closePolicy(Tooltip.ClosePolicy.TouchAnyWhere, 0)
                                .text("RIGHT. Touch outside to close this tooltip")
                                .withArrow(true)
                                .fitToScreen(true)
                                .maxWidth((int) (metrics.widthPixels / 2))
                                .withOverlay(true)
                                .withCallback(this)
                                .floatingAnimation(new AnimationBuilder().setDirection(0).setDuration(400).setRadius(8).build())
                                .build()).show();

            } else if (id == mButton2.getId()) {

                Tooltip.make(getContext(),
                        new Tooltip.Builder(ID_BUTTON2)
                                .anchor(mButton2, Tooltip.Gravity.BOTTOM)
                                .fitToScreen(true)
                                .closePolicy(Tooltip.ClosePolicy.TouchAnyWhereExclusive, 0)
                                .text("BOTTOM. Touch outside to dismiss the tooltip")
                                .withArrow(true)
                                .maxWidth(metrics.widthPixels / 2)
                                .withCallback(this)
                                .build()).show();

            } else if (id == mButton3.getId()) {
                Tooltip.make(getContext(),
                        new Tooltip.Builder(ID_BUTTON3)
                                .anchor(mButton3, Tooltip.Gravity.TOP)
                                .closePolicy(Tooltip.ClosePolicy.TouchInside, 0)
                                .text("TOP. Touch Inside the tooltip to dismiss..")
                                .withArrow(true)
                                .maxWidth((int) (metrics.widthPixels / 2.5))
                                .withCallback(this)
                                .floatingAnimation(new AnimationBuilder().setDirection(2).setDuration(1000).setRadius(20).build())
                                .build()).show();

            } else if (id == mButton4.getId()) {
                Tooltip.make(getContext(),
                        new Tooltip.Builder(ID_BUTTON4)
                                .anchor(v, Tooltip.Gravity.TOP)
                                .closePolicy(Tooltip.ClosePolicy.TouchInsideExclusive, 0)
                                .text("TOP. Touch Inside exclusive.")
                                .withArrow(true)
                                .withOverlay(false)
                                .maxWidth(metrics.widthPixels / 3)
                                .withCallback(this)
                                .build()).show();

            } else if (id == mButton5.getId()) {

                if (null == tooltip) {

                    tooltip = Tooltip.make(getActivity(),
                            new Tooltip.Builder(ID_BUTTON5)
                                    .anchor(v, Tooltip.Gravity.LEFT)
                                    .closePolicy(Tooltip.ClosePolicy.None, 3000)
                                    .text("LEFT. Touch None, so the tooltip won't disappear with a touch, but with a delay")
                                    .withArrow(false)
                                    .withOverlay(false)
                                    .maxWidth(metrics.widthPixels / 3)
                                    .showDelay(300)
                                    .withCallback(this)
                                    .build());
                    tooltip.show();
                } else {
                    tooltip.hide();
                    tooltip = null;
                }

            }
        }

        @Override
        public void onTooltipClose(final Tooltip.TooltipView view, final boolean fromUser, final boolean containsTouch) {
            Log.d(TAG, "onTooltipClose: " + view + ", fromUser: " + fromUser + ", containsTouch: " + containsTouch);
            if (null != tooltip && tooltip.getTooltipId() == view.getTooltipId()) {
                tooltip = null;
            }
        }

        @Override
        public void onTooltipFailed(Tooltip.TooltipView view) {
            Log.d(TAG, "onTooltipFailed: " + view.getTooltipId());
        }

        @Override
        public void onTooltipShown(Tooltip.TooltipView view) {
            Log.d(TAG, "onTooltipShown: " + view.getTooltipId());
        }

        @Override
        public void onTooltipHidden(Tooltip.TooltipView view) {
            Log.d(TAG, "onTooltipHidden: " + view.getTooltipId());
        }
    }
}
