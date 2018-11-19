package it.sephiroth.android.library.xtooltip

import android.content.Context
import android.graphics.Typeface
import timber.log.Timber
import java.util.*

object Typefaces {
    private const val TAG = "Typefaces"
    private val FONT_CACHE = Hashtable<String, Typeface>()

    operator fun get(c: Context, assetPath: String): Typeface? {
        synchronized(FONT_CACHE) {
            if (!FONT_CACHE.containsKey(assetPath)) {
                try {
                    val t = Typeface.createFromAsset(c.assets, assetPath)
                    FONT_CACHE[assetPath] = t
                } catch (e: Exception) {
                    Timber.e("Could not get typeface '$assetPath' because ${e.message}")
                    return null
                }

            }
            return FONT_CACHE[assetPath]
        }
    }
}