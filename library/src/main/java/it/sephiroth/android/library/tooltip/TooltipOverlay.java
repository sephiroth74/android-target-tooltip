package it.sephiroth.android.library.tooltip;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TooltipOverlay extends ImageView {
    private int mMargins;

    public TooltipOverlay(Context context) {
        this(context, null);
    }

    public TooltipOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.ToolTipOverlayDefaultStyle);
    }

    public TooltipOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, R.style.ToolTipLayoutDefaultStyle);
    }

    private void init(final Context context, final int defStyleResId) {
        TooltipOverlayDrawable drawable = new TooltipOverlayDrawable(context, defStyleResId);
        setImageDrawable(drawable);

        final TypedArray array =
            context.getTheme().obtainStyledAttributes(defStyleResId, R.styleable.TooltipOverlay);
        mMargins = array.getDimensionPixelSize(R.styleable.TooltipOverlay_android_layout_margin, 0);
        array.recycle();

    }

    public TooltipOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleResId) {
        super(context, attrs, defStyleAttr);
        init(context, defStyleResId);
    }

    public int getLayoutMargins() {
        return mMargins;
    }
}