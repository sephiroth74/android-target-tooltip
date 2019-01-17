package it.sephiroth.android.library.xtooltip

import android.content.Context
import android.graphics.Typeface
import android.util.LruCache
import timber.log.Timber

/**
 * Created by alessandro crugnola on 12/12/15.
 * alessandro.crugnola@gmail.com
 *
 *
 * LICENSE
 * Copyright 2015 Alessandro Crugnola
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
object Typefaces {
    private val FONT_CACHE = LruCache<String, Typeface>(4)

    operator fun get(c: Context, assetPath: String): Typeface? {
        synchronized(FONT_CACHE) {
            var typeface = FONT_CACHE.get(assetPath)
            if (typeface == null) {
                try {
                    typeface = Typeface.createFromAsset(c.assets, assetPath)
                    FONT_CACHE.put(assetPath, typeface)
                } catch (e: Exception) {
                    Timber.e("Could not get typeface '$assetPath' because ${e.message}")
                    return null
                }
            }
            return typeface
        }
    }
}