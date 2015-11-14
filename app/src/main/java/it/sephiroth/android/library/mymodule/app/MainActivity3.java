package it.sephiroth.android.library.mymodule.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.tooltip.Tooltip;
import it.sephiroth.android.library.tooltip.TooltipManager;

public class MainActivity3 extends AppCompatActivity
    implements AdapterView.OnItemClickListener, TooltipManager.OnTooltipAttachedStateChange {
    private static final String TAG = "MainActivity3";
    RecyclerView mRecyclerView;
    TooltipManager mTooltipManager;
    static final int TOOLTIP_ID = 101;
    static final int LIST_POSITION = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_activity3);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        List<String> array = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            array.add(String.format("Item %d", i));
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new MyAdapter(this, R.layout.custom_list_textview, array));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnScrollListener(
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(
                    final RecyclerView recyclerView, final int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {

                    }
                }
            });

        TooltipManager.DBG = true;
        mTooltipManager = new TooltipManager(this);
        mTooltipManager.addOnTooltipAttachedStateChange(this);
    }

    @Override
    protected void onDestroy() {
        mTooltipManager.removeOnTooltipAttachedStateChange(this);
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

        if (id == R.id.action_demo2) {
            startActivity(new Intent(this, MainActivity2.class));
        } else if (id == R.id.action_demo3) {
            startActivity(new Intent(this, MainActivity3.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        Log.d(TAG, "onItemClick: " + position);

        if (position == LIST_POSITION) {
            mTooltipManager.remove(TOOLTIP_ID);
        }
    }

    @Override
    public void onTooltipAttached(final int id) {
        Log.i(TAG, "onTooltipAttached: " + id);
    }

    @Override
    public void onTooltipDetached(final int id) {
        Log.i(TAG, "onTooltipDetached: " + id);
    }

    class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int mResId;
        private final List<String> mData;

        public MyAdapter(final Context context, final int resource, final List<String> objects) {
            super();
            setHasStableIds(true);
            mResId = resource;
            mData = objects;
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            View view = LayoutInflater.from(MainActivity3.this).inflate(mResId, parent, false);
            final RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(view) { };
            view.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        // mTooltipManager.remove(TOOLTIP_ID);
                        final Tooltip tooltip = mTooltipManager.get(TOOLTIP_ID);
                        if (null != tooltip) {
                            tooltip.hide(true);
                        } else {
                            showTooltip(holder);
                        }
                    }
                });
            return holder;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            ((TextView) holder.itemView.findViewById(android.R.id.text1)).setText(mData.get(position));

            if (position == LIST_POSITION) {
                showTooltip(holder);
            }
        }

        private void showTooltip(final RecyclerView.ViewHolder holder) {
            mTooltipManager.remove(TOOLTIP_ID);

            mTooltipManager.show(
                new TooltipManager.Builder(TOOLTIP_ID)
                    .maxWidth(450)
                    .anchor(holder.itemView.findViewById(android.R.id.text1), TooltipManager.Gravity.RIGHT)
                    .closePolicy(TooltipManager.ClosePolicy.TouchInside, 0)
                    .text("Brigthness, Saturation, Contrast and Warmth are now here!")
                    .fitToScreen(false)
                    .fadeDuration(100)
                    .showDelay(200)
                    .withCallback(
                        new TooltipManager.onTooltipClosingCallback() {
                            @Override
                            public void onClosing(final int id, final boolean fromUser, final boolean containsTouch) {
                                Log.w(
                                    TAG, "onClosing: " + id + ", fromUser: " + fromUser + ", containsTouch: " + containsTouch);
                            }
                        })
                    .build());

        }
    }
}
