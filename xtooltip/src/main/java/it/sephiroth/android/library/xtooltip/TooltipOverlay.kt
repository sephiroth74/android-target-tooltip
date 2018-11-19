package it.sephiroth.android.library.xtooltip


import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

class TooltipOverlay : ImageView {
    var layoutMargins: Int = 0
        private set

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