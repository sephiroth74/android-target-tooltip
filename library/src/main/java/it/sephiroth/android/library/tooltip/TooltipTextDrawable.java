package it.sephiroth.android.library.tooltip;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;

class TooltipTextDrawable extends Drawable {
    public static final float ARROW_RATIO_DEFAULT = 1.4f;
    public static final float ALPHA_MAX = 255f;
    static final String TAG = "TooltipTextDrawable";
    private final RectF rectF;
    private final Path path;
    private final Point tmpPoint = new Point();
    private final Rect outlineRect = new Rect();
    private final Paint bgPaint;
    private final Paint stPaint;
    private final float arrowRatio;
    private final float ellipseSize;
    private Point point;
    private int padding = 0;
    private int arrowWeight = 0;
    private Tooltip.Gravity gravity;

    public TooltipTextDrawable(final Context context, final Tooltip.Builder builder) {

        TypedArray theme =
            context.getTheme().obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes);
        this.ellipseSize = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_cornerRadius, 4);
        final int strokeWidth = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_strokeWeight, 2);
        final int backgroundColor = theme.getColor(R.styleable.TooltipLayout_ttlm_backgroundColor, 0);
        final int strokeColor = theme.getColor(R.styleable.TooltipLayout_ttlm_strokeColor, 0);
        this.arrowRatio = theme.getFloat(R.styleable.TooltipLayout_ttlm_arrowRatio, ARROW_RATIO_DEFAULT);
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

    @Override
    public void draw(final Canvas canvas) {
        if (null != bgPaint) {
            canvas.drawPath(path, bgPaint);
        }

        if (null != stPaint) {
            canvas.drawPath(path, stPaint);
        }
    }

    public void setAnchor(final Tooltip.Gravity gravity, int padding, @Nullable Point point) {
        if (gravity != this.gravity || padding != this.padding || !Utils.equals(this.point, point)) {
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

    void calculatePath(Rect outBounds) {
        int left = outBounds.left + padding;
        int top = outBounds.top + padding;
        int right = outBounds.right - padding;
        int bottom = outBounds.bottom - padding;

        final float maxY = bottom - ellipseSize;
        final float maxX = right - ellipseSize;
        final float minY = top + ellipseSize;
        final float minX = left + ellipseSize;

        if (null != point && null != gravity) {
            calculatePathWithGravity(outBounds, left, top, right, bottom, maxY, maxX, minY, minX);
        } else {
            rectF.set(left, top, right, bottom);
            path.addRoundRect(rectF, ellipseSize, ellipseSize, Path.Direction.CW);
        }
    }

    private void calculatePathWithGravity(
        final Rect outBounds, final int left, final int top, final int right, final int bottom, final float maxY, final float maxX,
        final float minY, final float minX) {
        boolean drawPoint =
            isDrawPoint(left, top, right, bottom, maxY, maxX, minY, minX, tmpPoint, point, gravity, arrowWeight);
        clampPoint(left, top, right, bottom, tmpPoint);

        path.reset();

        // top/left
        path.moveTo(left + ellipseSize, top);

        if (drawPoint && gravity == Tooltip.Gravity.BOTTOM) {
            path.lineTo(left + tmpPoint.x - arrowWeight, top);
            path.lineTo(left + tmpPoint.x, outBounds.top);
            path.lineTo(left + tmpPoint.x + arrowWeight, top);
        }

        // top/right
        path.lineTo(right - ellipseSize, top);
        path.quadTo(right, top, right, top + ellipseSize);

        if (drawPoint && gravity == Tooltip.Gravity.LEFT) {
            path.lineTo(right, top + tmpPoint.y - arrowWeight);
            path.lineTo(outBounds.right, top + tmpPoint.y);
            path.lineTo(right, top + tmpPoint.y + arrowWeight);
        }

        // bottom/right
        path.lineTo(right, bottom - ellipseSize);
        path.quadTo(right, bottom, right - ellipseSize, bottom);

        if (drawPoint && gravity == Tooltip.Gravity.TOP) {
            path.lineTo(left + tmpPoint.x + arrowWeight, bottom);
            path.lineTo(left + tmpPoint.x, outBounds.bottom);
            path.lineTo(left + tmpPoint.x - arrowWeight, bottom);
        }

        // bottom/left
        path.lineTo(left + ellipseSize, bottom);
        path.quadTo(left, bottom, left, bottom - ellipseSize);

        if (drawPoint && gravity == Tooltip.Gravity.RIGHT) {
            path.lineTo(left, top + tmpPoint.y + arrowWeight);
            path.lineTo(outBounds.left, top + tmpPoint.y);
            path.lineTo(left, top + tmpPoint.y - arrowWeight);
        }

        // top/left
        path.lineTo(left, top + ellipseSize);
        path.quadTo(left, top, left + ellipseSize, top);
    }

    private static boolean isDrawPoint(
        final int left, final int top, final int right, final int bottom, final float maxY, final float maxX, final float minY,
        final float minX, final Point tmpPoint, final Point point, final Tooltip.Gravity gravity,
        final int arrowWeight) {
        boolean drawPoint = false;
        tmpPoint.set(point.x, point.y);

        if (gravity == Tooltip.Gravity.RIGHT || gravity == Tooltip.Gravity.LEFT) {
            if (tmpPoint.y >= top && tmpPoint.y <= bottom) {
                if (top + tmpPoint.y + arrowWeight > maxY) {
                    tmpPoint.y = (int) (maxY - arrowWeight - top);
                } else if (top + tmpPoint.y - arrowWeight < minY) {
                    tmpPoint.y = (int) (minY + arrowWeight - top);
                }
                drawPoint = true;
            }
        } else {
            if (tmpPoint.x >= left && tmpPoint.x <= right) {
                if (tmpPoint.x >= left && tmpPoint.x <= right) {
                    if (left + tmpPoint.x + arrowWeight > maxX) {
                        tmpPoint.x = (int) (maxX - arrowWeight - left);
                    } else if (left + tmpPoint.x - arrowWeight < minX) {
                        tmpPoint.x = (int) (minX + arrowWeight - left);
                    }
                    drawPoint = true;
                }
            }
        }
        return drawPoint;
    }

    private static void clampPoint(
        final int left, final int top, final int right, final int bottom, final Point tmpPoint) {
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
    }

    @Override
    protected void onBoundsChange(final Rect bounds) {
        super.onBoundsChange(bounds);
        calculatePath(bounds);
    }

    public float getRadius() {
        return ellipseSize;
    }

    @Override
    public int getAlpha() {
        return bgPaint.getAlpha();
    }

    @Override
    public void setAlpha(final int alpha) {
        bgPaint.setAlpha(alpha);
        stPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(final ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @TargetApi (Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void getOutline(Outline outline) {
        copyBounds(outlineRect);
        outlineRect.inset(padding, padding);
        outline.setRoundRect(outlineRect, getRadius());
        if (getAlpha() < 255) {
            outline.setAlpha(0);
        }
        //outline.setAlpha(getAlpha() / ALPHA_MAX);
    }
}
