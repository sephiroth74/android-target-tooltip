package it.sephiroth.android.library.mymodule.app;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by crugnola on 8/28/15.
 * android-target-tooltip
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
