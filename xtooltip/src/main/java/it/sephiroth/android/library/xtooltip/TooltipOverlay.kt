package it.sephiroth.android.library.xtooltip


import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

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
class TooltipOverlay : AppCompatImageView {
    private var layoutMargins: Int = 0

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = R.style.ToolTipOverlayDefaultStyle
               ) : super(context, attrs, defStyleAttr) {
        init(context, R.style.ToolTipLayoutDefaultStyle)
    }

    private fun init(context: Context, defStyleResId: Int) {
        val drawable = TooltipOverlayDrawable(context, defStyleResId)
        setImageDrawable(drawable)

        val array = context.theme.obtainStyledAttributes(defStyleResId, R.styleable.TooltipOverlay)
        layoutMargins = array.getDimensionPixelSize(R.styleable.TooltipOverlay_android_layout_margin, 0)
        array.recycle()

    }

    constructor(context: Context, defStyleAttr: Int, defStyleResId: Int) : super(context, null, defStyleAttr) {
        init(context, defStyleResId)
    }
}