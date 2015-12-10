package it.sephiroth.android.library.tooltip;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static it.sephiroth.android.library.tooltip.TooltipManager.DBG;
import static it.sephiroth.android.library.tooltip.TooltipManager.log;

class ATooltipTextDrawable extends Drawable {
    static final String TAG = "ATooltipTextDrawable";
    private final RectF rectF;
    private final Path path;
    private Point point;
    private final Point tmpPoint = new Point();
    private final Paint bgPaint;
    private final Paint stPaint;
    private final float arrowRatio;
    private final float ellipseSize;
    private int padding = 0;
    private int arrowWeight = 0;
    private TooltipManager.Gravity gravity;

    public ATooltipTextDrawable(final Context context, final TooltipManager.Builder builder) {

        TypedArray theme =
            context.getTheme().obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes);
        this.ellipseSize = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_cornerRadius, 4);
        final int strokeWidth = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_strokeWeight, 30);
        final int backgroundColor = theme.getColor(R.styleable.TooltipLayout_ttlm_backgroundColor, 0);
        final int strokeColor = theme.getColor(R.styleable.TooltipLayout_ttlm_strokeColor, 0);
        this.arrowRatio = theme.getFloat(R.styleable.TooltipLayout_ttlm_arrowRatio, 1.4f);
        theme.recycle();

        this.rectF = new RectF();

        if (backgroundColor != 0) {
            bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(backgroundColor);
            bgPaint.setStyle(Paint.Style.FILL);
        } else {
            bgPaint = null;
        }

        if (strokeColor != 0) {
            stPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            stPaint.setColor(strokeColor);
            stPaint.setStyle(Paint.Style.STROKE);
            stPaint.setStrokeWidth(strokeWidth);
        } else {
            stPaint = null;
        }

        path = new Path();
    }

    void calculatePath(Rect outBounds) {
        if (DBG) {
            log(TAG, INFO, "calculatePath. padding: %d, gravity: %s, bounds: %s", padding, gravity, getBounds());
        }

        int left = outBounds.left + padding;
        int top = outBounds.top + padding;
        int right = outBounds.right - padding;
        int bottom = outBounds.bottom - padding;

        final float max_y = bottom - ellipseSize;
        final float max_x = right - ellipseSize;
        final float min_y = top + ellipseSize;
        final float min_x = left + ellipseSize;

        if (DBG) {
            log(TAG, VERBOSE, "rect: (%d, %d, %d, %d)", left, top, right, bottom);
            log(TAG, VERBOSE, "min_y: %g, max_y: %g", min_y, max_y);
            log(TAG, VERBOSE, "arrowWeight: %d, point: %s", arrowWeight, point);
        }

        boolean drawPoint = false;

        if (null != point && null != gravity) {
            tmpPoint.set(point.x, point.y);

            if (gravity == TooltipManager.Gravity.RIGHT || gravity == TooltipManager.Gravity.LEFT) {
                if (tmpPoint.y >= top && tmpPoint.y <= bottom) {
                    if (top + tmpPoint.y + arrowWeight > max_y) {
                        tmpPoint.y = (int) (max_y - arrowWeight - top);
                    } else if (top + tmpPoint.y - arrowWeight < min_y) {
                        tmpPoint.y = (int) (min_y + arrowWeight - top);
                    }
                    drawPoint = true;
                }
            } else {
                if (tmpPoint.x >= left && tmpPoint.x <= right) {
                    if (tmpPoint.x >= left && tmpPoint.x <= right) {
                        if (left + tmpPoint.x + arrowWeight > max_x) {
                            tmpPoint.x = (int) (max_x - arrowWeight - left);
                        } else if (left + tmpPoint.x - arrowWeight < min_x) {
                            tmpPoint.x = (int) (min_x + arrowWeight - left);
                        }
                        drawPoint = true;
                    }
                }
            }

            path.reset();

            // clamp the point..
            if (tmpPoint.y < top) {
                tmpPoint.y = top;
            } else if (tmpPoint.y > bottom) {
                tmpPoint.y = bottom;
            }
            if (tmpPoint.x < left) {
                tmpPoint.x = left;
            }
            if (tmpPoint.x > right) {
                tmpPoint.x = right;
            }

            // top/left
            path.moveTo(left + ellipseSize, top);

            if (drawPoint && gravity == TooltipManager.Gravity.BOTTOM) {
                path.lineTo(left + tmpPoint.x - arrowWeight, top);
                path.lineTo(left + tmpPoint.x, outBounds.top);
                path.lineTo(left + tmpPoint.x + arrowWeight, top);
            }

            // top/right
            path.lineTo(right - ellipseSize, top);
            path.quadTo(right, top, right, top + ellipseSize);

            if (drawPoint && gravity == TooltipManager.Gravity.LEFT) {
                path.lineTo(right, top + tmpPoint.y - arrowWeight);
                path.lineTo(outBounds.right, top + tmpPoint.y);
                path.lineTo(right, top + tmpPoint.y + arrowWeight);
            }

            // bottom/right
            path.lineTo(right, bottom - ellipseSize);
            path.quadTo(right, bottom, right - ellipseSize, bottom);

            if (drawPoint && gravity == TooltipManager.Gravity.TOP) {
                path.lineTo(left + tmpPoint.x + arrowWeight, bottom);
                path.lineTo(left + tmpPoint.x, outBounds.bottom);
                path.lineTo(left + tmpPoint.x - arrowWeight, bottom);
            }

            // bottom/left
            path.lineTo(left + ellipseSize, bottom);
            path.quadTo(left, bottom, left, bottom - ellipseSize);

            if (drawPoint && gravity == TooltipManager.Gravity.RIGHT) {
                path.lineTo(left, top + tmpPoint.y + arrowWeight);
                path.lineTo(outBounds.left, top + tmpPoint.y);
                path.lineTo(left, top + tmpPoint.y - arrowWeight);
            }

            // top/left
            path.lineTo(left, top + ellipseSize);
            path.quadTo(left, top, left + ellipseSize, top);
        } else {
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
        log(TAG, INFO, "onBoundsChange: %s", bounds);
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
        return PixelFormat.TRANSLUCENT;
    }

    public void setAnchor(final TooltipManager.Gravity gravity, int padding, @Nullable Point point) {
        log(TAG, INFO, "setAnchor(%s, %d, %s)", gravity, padding, point);

        if (gravity != this.gravity || padding != this.padding || !pointEquals(this.point, point)) {
            this.gravity = gravity;
            this.padding = padding;
            this.arrowWeight = (int) ((float) padding / arrowRatio);

            if (null != point) {
                this.point = new Point(point);
            } else {
                this.point = null;
            }

            final Rect bounds = getBounds();
            if (!bounds.isEmpty()) {
                calculatePath(getBounds());
                invalidateSelf();
            }
        }
    }

    boolean pointEquals(@Nullable Point a, @Nullable Point b) {
        return (a == null) ? (b == null) : a.equals(b);
    }
}
