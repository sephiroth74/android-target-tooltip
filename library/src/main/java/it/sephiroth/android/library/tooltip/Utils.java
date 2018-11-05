package it.sephiroth.android.library.tooltip;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static it.sephiroth.android.library.tooltip.Tooltip.dbg;

/**
 * Created by alessandro crugnola on 12/12/15.
 */
final class Utils {
    private Utils() {
    }

    @Nullable
    static Activity getActivity(@Nullable Context cont) {
        if (cont == null) {
            return null;
        } else if (cont instanceof Activity) {
            return (Activity) cont;
        } else if (cont instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) cont).getBaseContext());
        }
        return null;
    }

    static void log(final String tag, final int level, final String format, Object... args) {
        if (dbg) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(tag, String.format(format, args));
                    break;
                case Log.ERROR:
                    Log.e(tag, String.format(format, args));
                    break;
                case INFO:
                    Log.i(tag, String.format(format, args));
                    break;
                case Log.WARN:
                    Log.w(tag, String.format(format, args));
                    break;
                default:
                case VERBOSE:
                    Log.v(tag, String.format(format, args));
                    break;
            }
        }
    }

    static boolean equals(@Nullable Object a, @Nullable Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    static boolean rectContainsRectWithTolerance(@NonNull final Rect parentRect, @NonNull final Rect childRect, final int t) {
        return parentRect.contains(childRect.left + t, childRect.top + t, childRect.right - t, childRect.bottom - t);
    }

    static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        } else {
            result = (int) Math.ceil((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 24 : 25)
                    * context.getResources().getDisplayMetrics().density);
        }
        return result;
    }

    static int getSoftButtonsBarHeight(Context context) {
        // getRealMetrics is only available with API 17 and +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Activity activity = getActivity(context);
            if (activity == null) {
                return 0;
            }
            int orientation = activity.getResources().getConfiguration().orientation;
            DisplayMetrics metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableSize = orientation == Configuration.ORIENTATION_LANDSCAPE ? metrics.widthPixels : metrics.heightPixels;
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realSize = orientation == Configuration.ORIENTATION_LANDSCAPE ? metrics.widthPixels : metrics.heightPixels;
            if (realSize > usableSize) {
                return realSize - usableSize;
            } else {
                return 0;
            }
        }
        return 0;
    }

    static int getDeviceRotation(Context context) {
        Activity activity = getActivity(context);
        if (activity == null) {
            return -1;
        }
        return activity.getWindowManager().getDefaultDisplay().getRotation();
    }
}
