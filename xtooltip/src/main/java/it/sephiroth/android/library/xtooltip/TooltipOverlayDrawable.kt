package it.sephiroth.android.library.xtooltip


import android.animation.*
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import timber.log.Timber

/**
 * Created by alessandro crugnola on 12/12/15.
 * alessandro.crugnola@gmail.com
 */
class TooltipOverlayDrawable(context: Context, defStyleResId: Int) : Drawable() {
    private val mOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mOuterRadius: Float = 0.toFloat()
    var innerRadius = 0f
        set(rippleRadius) {
            field = rippleRadius
            invalidateSelf()
        }
    private val mFirstAnimatorSet: AnimatorSet
    private val mSecondAnimatorSet: AnimatorSet
    private val mFirstAnimator: ValueAnimator
    private val mSecondAnimator: ValueAnimator
    private var mRepeatIndex: Int = 0
    private var mStarted: Boolean = false
    private val mOuterAlpha: Int
    private val mInnerAlpha: Int
    private var mRepeatCount = 1
    private var mDuration: Long = 400

    var outerAlpha: Int
        get() = mOuterPaint.alpha
        set(value) {
            mOuterPaint.alpha = value
            invalidateSelf()
        }

    var innerAlpha: Int
        get() = mInnerPaint.alpha
        set(value) {
            mInnerPaint.alpha = value
            invalidateSelf()
        }

    var outerRadius: Float
        get() = mOuterRadius
        set(value) {
            mOuterRadius = value
            invalidateSelf()
        }

    init {
        mOuterPaint.style = Paint.Style.FILL
        mInnerPaint.style = Paint.Style.FILL

        val array = context.theme.obtainStyledAttributes(defStyleResId, R.styleable.TooltipOverlay)

        for (i in 0 until array.indexCount) {
            val index = array.getIndex(i)

            if (index == R.styleable.TooltipOverlay_android_color) {
                val color = array.getColor(index, 0)
                mOuterPaint.color = color
                mInnerPaint.color = color

            } else if (index == R.styleable.TooltipOverlay_ttlm_repeatCount) {
                mRepeatCount = array.getInt(index, 1)

            } else if (index == R.styleable.TooltipOverlay_android_alpha) {
                val alpha = (array.getFloat(index, mInnerPaint.alpha / ALPHA_MAX) * 255).toInt()
                mInnerPaint.alpha = alpha
                mOuterPaint.alpha = alpha

            } else if (index == R.styleable.TooltipOverlay_ttlm_duration) {
                mDuration = array.getInt(index, 400).toLong()
            }
        }

        array.recycle()

        mOuterAlpha = outerAlpha
        mInnerAlpha = innerAlpha

        // first
        var fadeIn: Animator = ObjectAnimator.ofInt(this, "outerAlpha", 0, mOuterAlpha)
        fadeIn.duration = (mDuration * FADEIN_DURATION).toLong()

        var fadeOut: Animator = ObjectAnimator.ofInt(this, "outerAlpha", mOuterAlpha, 0, 0)
        fadeOut.startDelay = (mDuration * FADEOUT_START_DELAY).toLong()
        fadeOut.duration = (mDuration * (1.0 - FADEOUT_START_DELAY)).toLong()

        mFirstAnimator = ObjectAnimator.ofFloat(this, "outerRadius", 0f, 1f)
        mFirstAnimator.duration = mDuration

        mFirstAnimatorSet = AnimatorSet()
        mFirstAnimatorSet.playTogether(fadeIn, mFirstAnimator, fadeOut)
        mFirstAnimatorSet.interpolator = AccelerateDecelerateInterpolator()
        mFirstAnimatorSet.duration = mDuration

        // second
        fadeIn = ObjectAnimator.ofInt(this, "innerAlpha", 0, mInnerAlpha)
        fadeIn.duration = (mDuration * FADEIN_DURATION).toLong()

        fadeOut = ObjectAnimator.ofInt(this, "innerAlpha", mInnerAlpha, 0, 0)
        fadeOut.setStartDelay((mDuration * FADEOUT_START_DELAY).toLong())
        fadeOut.duration = (mDuration * (1.0 - FADEOUT_START_DELAY)).toLong()

        mSecondAnimator = ObjectAnimator.ofFloat(this, "innerRadius", 0f, 1f)
        mSecondAnimator.duration = mDuration

        mSecondAnimatorSet = AnimatorSet()
        mSecondAnimatorSet.playTogether(fadeIn, mSecondAnimator, fadeOut)
        mSecondAnimatorSet.interpolator = AccelerateDecelerateInterpolator()
        mSecondAnimatorSet.startDelay = (mDuration * SECOND_ANIM_START_DELAY).toLong()
        mSecondAnimatorSet.duration = mDuration

        mFirstAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            var cancelled: Boolean = false

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled && isVisible && ++mRepeatIndex < mRepeatCount) {
                    mFirstAnimatorSet.start()
                }
            }
        })

        mSecondAnimatorSet.addListener(object : AnimatorListenerAdapter() {
            var cancelled: Boolean = false

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                cancelled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled && isVisible && mRepeatIndex < mRepeatCount) {
                    mSecondAnimatorSet.startDelay = 0
                    mSecondAnimatorSet.start()
                }
            }
        })

    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val centerX = bounds.width() / 2
        val centerY = bounds.height() / 2
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), mOuterRadius, mOuterPaint)
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), innerRadius, mInnerPaint)

    }

    override fun setAlpha(i: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = isVisible != visible

        if (visible) {
            if (restart || !mStarted) {
                replay()
            }
        } else {
            stop()
        }

        return changed
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun onBoundsChange(bounds: Rect) {
        Timber.i("onBoundsChange: $bounds")
        super.onBoundsChange(bounds)
        mOuterRadius = (Math.min(bounds.width(), bounds.height()) / 2).toFloat()
        mFirstAnimator.setFloatValues(0f, mOuterRadius)
        mSecondAnimator.setFloatValues(0f, mOuterRadius)
    }

    override fun getIntrinsicWidth(): Int {
        return 96
    }

    override fun getIntrinsicHeight(): Int {
        return 96
    }

    fun play() {
        mRepeatIndex = 0
        mStarted = true
        mFirstAnimatorSet.start()
        mSecondAnimatorSet.startDelay = (mDuration * SECOND_ANIM_START_DELAY).toLong()
        mSecondAnimatorSet.start()
    }

    fun replay() {
        stop()
        play()
    }

    fun stop() {
        mFirstAnimatorSet.cancel()
        mSecondAnimatorSet.cancel()

        mRepeatIndex = 0
        mStarted = false

        innerRadius = 0f
        outerRadius = 0f
    }

    companion object {
        const val ALPHA_MAX = 255f
        const val FADEOUT_START_DELAY = 0.55
        const val FADEIN_DURATION = 0.3
        const val SECOND_ANIM_START_DELAY = 0.25
    }
}
