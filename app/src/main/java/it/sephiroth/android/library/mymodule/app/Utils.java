package it.sephiroth.android.library.mymodule.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;

/**
 * Created by alessandro on 04/09/14.
 */
public class Utils {

    public static int getActionBarSize(final Context context) {

        final int[] attrs;

        if (Build.VERSION.SDK_INT >= 14) {
            attrs = new int[]{android.R.attr.actionBarSize};
        } else {
            attrs = new int[]{R.attr.actionBarSize};
        }

        TypedArray values = context.getTheme().obtainStyledAttributes(attrs);
        try {
            return values.getDimensionPixelSize(0, 0);
        } finally {
            values.recycle();
        }
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
