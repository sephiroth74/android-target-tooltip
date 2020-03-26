package it.sephiroth.android.library.tooltip;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.Hashtable;

public final class Typefaces {
    private static final String TAG = "Typefaces";
    private static final Hashtable<String, Typeface> FONT_CACHE = new Hashtable<>();

    private Typefaces() { }

    public static Typeface get(Context c, String assetPath) {
        synchronized (FONT_CACHE) {
            if (!FONT_CACHE.containsKey(assetPath)) {
                try {
                    Typeface t = Typeface.createFromAsset(c.getAssets(), assetPath);
                    FONT_CACHE.put(assetPath, t);
                } catch (Exception e) {
                    Log.e(TAG, "Could not get typeface '" + assetPath + "' because " + e.getMessage());
                    return null;
                }
            }
            return FONT_CACHE.get(assetPath);
        }
    }
}