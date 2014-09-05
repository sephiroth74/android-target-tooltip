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

import static it.sephiroth.android.library.tooltip.TooltipManager.DBG;

class TooltipTextDrawable extends Drawable {
	static final String TAG = "ToolTipTextDrawable";

	private final RectF rectF;
	private final Path path;
	private Point point;

	private final Paint bgPaint;
	private final Paint stPaint;

	private final float arrowRatio;
	private final float ellipseSize;
	private final int strokeWidth;
	private final int strokeColor;
	private final int backgroundColor;

	private int padding = 0;
	private int arrowWeight = 0;

	private TooltipManager.Gravity gravity;

	public TooltipTextDrawable(final Context context, final TooltipManager.Builder builder) {

		TypedArray theme =
			context.getTheme().obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes);
		this.ellipseSize = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_cornerRadius, 4);
		this.strokeWidth = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_strokeWeight, 30);
		this.backgroundColor = theme.getColor(R.styleable.TooltipLayout_ttlm_backgroundColor, 0);
		this.strokeColor = theme.getColor(R.styleable.TooltipLayout_ttlm_strokeColor, 0);
		this.arrowRatio = theme.getFloat(R.styleable.TooltipLayout_ttlm_arrowRatio, 1.4f);
		theme.recycle();

		this.rectF = new RectF();

		if (backgroundColor != 0) {
			bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			bgPaint.setColor(this.backgroundColor);
			bgPaint.setStyle(Paint.Style.FILL);
		}
		else {
			bgPaint = null;
		}

		if (strokeColor != 0) {
			stPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			stPaint.setColor(strokeColor);
			stPaint.setStyle(Paint.Style.STROKE);
			stPaint.setStrokeWidth(strokeWidth);
		}
		else {
			stPaint = null;
		}

		path = new Path();
	}

	private void calculatePath(Rect outBounds) {
		if (DBG) Log.i(TAG, "calculateBounds, padding: " + padding + ", gravity: " + gravity);

		int left = outBounds.left + padding;
		int top = outBounds.top + padding;
		int right = outBounds.right - padding;
		int bottom = outBounds.bottom - padding;

		final float max_y = bottom - ellipseSize;
		final float max_x = right - ellipseSize;
		final float min_y = top + ellipseSize;
		final float min_x = left + ellipseSize;

		if (DBG) {
			Log.v(TAG, "left: " + left);
			Log.v(TAG, "top: " + top);
			Log.v(TAG, "right: " + right);
			Log.v(TAG, "bottom: " + bottom);
			Log.v(TAG, "min_y: " + min_y);
			Log.v(TAG, "max_y: " + max_y);
			Log.v(TAG, "arrowWeight: " + arrowWeight);
			Log.v(TAG, "point: " + point);
		}

		boolean drawPoint = false;

		if (null != point && null != gravity) {

			if (gravity == TooltipManager.Gravity.RIGHT || gravity == TooltipManager.Gravity.LEFT) {
				if (point.y >= top && point.y <= bottom) {
					if (top + point.y + arrowWeight > max_y) {
						point.y = (int) (max_y - arrowWeight - top);
					}
					else if (top + point.y - arrowWeight < min_y) {
						point.y = (int) (min_y + arrowWeight - top);
					}
					drawPoint = true;
				}
			}
			else {
				if (point.x >= left && point.x <= right) {
					if (point.x >= left && point.x <= right) {
						if (left + point.x + arrowWeight > max_x) {
							point.x = (int) (max_x - arrowWeight - left);
						}
						else if (left + point.x - arrowWeight < min_x) {
							point.x = (int) (min_x + arrowWeight - left);
						}
						drawPoint = true;
					}
				}
			}

			if (DBG) Log.w(TAG, "point: " + point);

			path.reset();

			// clamp the point..
			if (point.y < top) point.y = (int) (top);
			else if (point.y > bottom) point.y = (int) (bottom);
			if (point.x < left) point.x = (int) (left);
			if (point.x > right) point.x = (int) (right);

			// top/left
			path.moveTo(left + ellipseSize, top);

			if (drawPoint && gravity == TooltipManager.Gravity.BOTTOM) {
				path.lineTo(left + point.x - arrowWeight, top);
				path.lineTo(left + point.x, outBounds.top);
				path.lineTo(left + point.x + arrowWeight, top);
			}

			// top/right
			path.lineTo(right - ellipseSize, top);
			path.quadTo(right, top, right, top + ellipseSize);

			if (drawPoint && gravity == TooltipManager.Gravity.LEFT) {
				path.lineTo(right, top + point.y - arrowWeight);
				path.lineTo(outBounds.right, top + point.y);
				path.lineTo(right, top + point.y + arrowWeight);
			}

			// bottom/right
			path.lineTo(right, bottom - ellipseSize);
			path.quadTo(right, bottom, right - ellipseSize, bottom);

			if (drawPoint && gravity == TooltipManager.Gravity.TOP) {
				path.lineTo(left + point.x + arrowWeight, bottom);
				path.lineTo(left + point.x, outBounds.bottom);
				path.lineTo(left + point.x - arrowWeight, bottom);
			}

			// bottom/left
			path.lineTo(left + ellipseSize, bottom);
			path.quadTo(left, bottom, left, bottom - ellipseSize);

			if (drawPoint && gravity == TooltipManager.Gravity.RIGHT) {
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

		if (null != bgPaint) {
			canvas.drawPath(path, bgPaint);
		}

		if (null != stPaint) {
			canvas.drawPath(path, stPaint);
		}
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
		this.point = new Point(point);
	}

	public void setAnchor(final TooltipManager.Gravity gravity, int padding) {
		this.gravity = gravity;
		this.padding = padding;
		this.arrowWeight = (int) ((float) padding / arrowRatio);
	}
}
