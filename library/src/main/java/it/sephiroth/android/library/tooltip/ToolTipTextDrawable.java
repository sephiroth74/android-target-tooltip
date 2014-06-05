package it.sephiroth.android.library.tooltip;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

class ToolTipTextDrawable extends Drawable {
	static final String TAG = "ToolTipTextDrawable";
	static final boolean DBG = TooltipManager.DBG;

	private final RectF rectF;
	private final Path path;
	private Point point;

	private final Paint bgPaint;
	private final Paint stPaint;

	private final float ellipseSize;
	private final int strokeWidth;
	private final int strokeColor;
	private final int backgroundColor;

	private int padding = 0;

	private TooltipManager.Gravity gravity;

	public ToolTipTextDrawable(final Context context, final TooltipManager.Builder builder) {

		TypedArray theme =
			context.getTheme().obtainStyledAttributes(null, R.styleable.ToolTipLayout, builder.defStyleAttr, builder.defStyleRes);
		this.ellipseSize = theme.getDimensionPixelSize(R.styleable.ToolTipLayout_ttlm_cornerRadius, 4);
		this.strokeWidth = theme.getDimensionPixelSize(R.styleable.ToolTipLayout_ttlm_strokeWeight, 30);
		this.backgroundColor = theme.getColor(R.styleable.ToolTipLayout_ttlm_backgroundColor, 0);
		this.strokeColor = theme.getColor(R.styleable.ToolTipLayout_ttlm_strokeColor, 0);
		theme.recycle();

		this.rectF = new RectF();

		bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bgPaint.setColor(this.backgroundColor);
		bgPaint.setStyle(Paint.Style.FILL);

		stPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		stPaint.setColor(strokeColor);
		stPaint.setStyle(Paint.Style.STROKE);
		stPaint.setStrokeWidth(strokeWidth);

		path = new Path();
	}

	private void calculatePath(Rect outBounds) {
		if (DBG) Log.i(TAG, "calculateBounds, padding: " + padding);

		int left = outBounds.left + padding;
		int top = outBounds.top + padding;
		int right = outBounds.right - padding;
		int bottom = outBounds.bottom - padding;
		int arrowWeight = (int) ((float)padding / 1.2f);

		if (null != point && null != gravity) {
			path.reset();

			// clamp the point..
			if (point.y < top) point.y = (int) (top);
			else if (point.y > bottom) point.y = (int) (bottom);
			if (point.x < left) point.x = (int) (left);
			if (point.x > right) point.x = (int) (right);

			// top/left
			path.moveTo(left + ellipseSize, top);

			if (gravity == TooltipManager.Gravity.BOTTOM) {
				path.lineTo(left + point.x - arrowWeight, top);
				path.lineTo(left + point.x, outBounds.top);
				path.lineTo(left + point.x + arrowWeight, top);
			}

			// top/right
			path.lineTo(right - ellipseSize, top);
			path.quadTo(right, top, right, top + ellipseSize);

			if (gravity == TooltipManager.Gravity.LEFT) {
				path.lineTo(right, top + point.y - arrowWeight);
				path.lineTo(outBounds.right, top + point.y);
				path.lineTo(right, top + point.y + arrowWeight);
			}

			// bottom/right
			path.lineTo(right, bottom - ellipseSize);
			path.quadTo(right, bottom, right - ellipseSize, bottom);

			if (gravity == TooltipManager.Gravity.TOP) {
				path.lineTo(left + point.x + arrowWeight, bottom);
				path.lineTo(left + point.x, outBounds.bottom);
				path.lineTo(left + point.x - arrowWeight, bottom);
			}

			// bottom/left
			path.lineTo(left + ellipseSize, bottom);
			path.quadTo(left, bottom, left, bottom - ellipseSize);

			if (gravity == TooltipManager.Gravity.RIGHT && point.y > top) {
				path.lineTo(left, top + point.y + arrowWeight);
				path.lineTo(outBounds.left, top + point.y);
				path.lineTo(left, top + point.y - arrowWeight);
			}

			// top/left
			path.lineTo(left, top + ellipseSize);
			path.quadTo(left, top, left + ellipseSize, top);
		}
		else {
			rectF.set(left, top, right, bottom);
			path.addRoundRect(rectF, ellipseSize, ellipseSize, Path.Direction.CW);
		}
	}

	@Override
	public void draw(final Canvas canvas) {
		if (DBG) Log.i(TAG, "draw");
		canvas.drawPath(path, bgPaint);
		canvas.drawPath(path, stPaint);
	}

	@Override
	protected void onBoundsChange(final Rect bounds) {
		if (DBG) Log.i(TAG, "onBoundsChange");
		super.onBoundsChange(bounds);
		calculatePath(bounds);
	}

	@Override
	public void setAlpha(final int alpha) {
	}

	@Override
	public void setColorFilter(final ColorFilter cf) {
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	public void setDestinationPoint(final Point point) {
		this.point = point;
	}

	public void setAnchor(final TooltipManager.Gravity gravity, int padding) {
		this.gravity = gravity;
		this.padding = padding;
	}
}
