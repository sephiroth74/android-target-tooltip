package it.sephiroth.android.library.tooltip;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TooltipOverlay extends ImageView {

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

    public TooltipOverlay(Context context, AttributeSet attrs, int defStyleAttr, int defStyleResId) {
        super(context, attrs, defStyleAttr);
        init(context, defStyleResId);
    }

    private void init(final Context context, final int defStyleResId) {
        TooltipOverlayDrawable drawable = new TooltipOverlayDrawable(context, defStyleResId);
        setImageDrawable(drawable);
    }
}